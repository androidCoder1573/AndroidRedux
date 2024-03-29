package com.cyworks.redux.component

import android.content.res.Configuration
import android.view.View
import androidx.collection.ArrayMap
import com.cyworks.redux.ReduxContext
import com.cyworks.redux.ReduxManager
import com.cyworks.redux.action.Action
import com.cyworks.redux.atom.UIPropsWatcher
import com.cyworks.redux.dependant.Dependant
import com.cyworks.redux.lifecycle.LifeCycleAction
import com.cyworks.redux.state.ReflectTask
import com.cyworks.redux.state.State
import com.cyworks.redux.ui.ComponentViewHolder
import com.cyworks.redux.ui.UIChangedBean
import com.cyworks.redux.ui.UIChangedType
import com.cyworks.redux.ui.ViewModule
import com.cyworks.redux.util.Environment
import com.cyworks.redux.util.ILogger

interface ViewModuleProvider<S : State> {
    fun provider(): ViewModule<S>
}

data class ComponentProxy<S : State>(
    val childrenDepMap: ArrayMap<String, Dependant<out State, S>>?,
    val token: String,
    val lazyBindUI: Boolean,
    val viewModuleProvider: ViewModuleProvider<S>,
)

/**
 * 对组件的UI操作抽取出来放到UI处理类中:
 * 1、初始化UI;
 * 2、设置Atom，用于UI的局部更新;
 * 3、设置UI组件的View的显示/隐藏/绑定/删除操作;
 */
class ComponentUIController<S : State>(private val proxy: ComponentProxy<S>) {
    /**
     * 当前组件的UI界面是否显示
     * 如果是延迟加载，表明初始状态时不加载UI, 后续由开发者自己控制加载时机
     */
    internal var isShow: Boolean = !proxy.lazyBindUI

    /**
     * Component自己的View，可以被detach，横屏UI
     */
    private var landscapeView: View? = null

    /**
     * Component自己的View，可以被detach, 竖屏UI
     */
    private var portraitView: View? = null

    /**
     * Component当前显示的View
     */
    internal var currentView: View? = null

    /**
     * 保存当前组件View的辅助类，方便在更新接口中获取view
     */
    internal var viewHolder: ComponentViewHolder? = null

    private val innerViewModule: ViewModule<S>

    /**
     * 用于监听本组件的属性变化, 并进行UI更新，运行在主线程不要做耗时操作
     */
    private var uiPropsWatcher: UIPropsWatcher<S>? = null

    /**
     * 上次的屏幕方向
     */
    internal var lastOrientation = Configuration.ORIENTATION_PORTRAIT

    private lateinit var context: ReduxContext<S>
    private lateinit var environment: Environment

    private val logger: ILogger = ReduxManager.instance.logger

    init {
        innerViewModule = object : ViewModule<S> {
            val viewModule = proxy.viewModuleProvider.provider()
            override fun getView(context: ReduxContext<S>, parent: View): View? {
                return viewModule.getView(context, parent)
            }

            override fun subscribeProps(state: S, watcher: UIPropsWatcher<S>?) {
                viewModule.subscribeProps(state, watcher)
            }
        }
    }

    internal fun setReduxContext(context: ReduxContext<S>) {
        this.context = context
    }

    internal fun setReduxEnv(env: Environment) {
        this.environment = env
    }

    fun onStateMerged(componentState: S) {
        // 获取初始的屏幕方向
        lastOrientation = componentState.currentOrientation

        // 设置状态 -- UI 监听
        makeUIWatcher(componentState)
    }

    /**
     * 为当前组件创建UI片段观察者
     */
    private fun makeUIWatcher(state: S) {
        // 创建属性订阅器
        uiPropsWatcher = UIPropsWatcher()
        innerViewModule.subscribeProps(state, uiPropsWatcher)
        uiPropsWatcher!!.generateKeyList(state)
    }

    /**
     * 组件UI展示
     *
     * @param needVisible 只有调用过程中的第一个View才需要显示
     */
    internal fun show(needVisible: Boolean = false) {
        // 如果已经绑定了，则直接走构建UI的逻辑
        if (lastOrientation == Configuration.ORIENTATION_PORTRAIT && portraitView == null
            || lastOrientation == Configuration.ORIENTATION_LANDSCAPE && landscapeView == null
        ) {
            initUI()
        }

        // 根组件进行展示
        if (needVisible) {
            if (currentView != null && currentView!!.visibility != View.VISIBLE) {
                currentView!!.visibility = View.VISIBLE
            }
        }

        // 更新一次UI
        context.runFullUpdate()

        // 通知组件当前组件UI发生变化了，给用户一个机会做一些善后处理, 首次初始化不需要这些
        sendUIChangedAction(UIChangedType.TYPE_VISIBILITY_CHANGE)
        showChildren()
    }

    private fun showChildren() {
        // attachAdapter()

        // 处理子组件
        val map: ArrayMap<String, Dependant<out State, S>> = proxy.childrenDepMap ?: return
        val size = map.size
        for (i in 0 until size) {
            val dependant = map.valueAt(i)
            if (!dependant.isInstalled) {
                installComponent(dependant)
                continue
            }

            // 如果绑定了，则走show的逻辑
            if (dependant.logic is BaseComponent<*>) {
                dependant.logic.uiController.show()
            }
        }
    }

    /**
     * 组件UI隐藏，跟detach还是有区别的，detach了之后只有像ViewPager/Dialog这类的组件，
     * 才能够重新安装，否则ViewStub被清除，导致重新加载布局时出现异常。
     *
     * @param needGone 只有调用过程中的第一个View才需要隐藏
     */
    internal fun hide(needGone: Boolean = false) {
        if (needGone) {
            if (currentView != null) {
                currentView!!.visibility = View.GONE
            }
        }
        hideChildren()
        setShow(false)

        // 通知组件当前组件UI发生变化了，给用户一个机会做一些善后处理
        sendUIChangedAction(UIChangedType.TYPE_VISIBILITY_CHANGE)
    }

    private fun hideChildren() {
        // detachAdapter()

        // 处理子组件
        val map: ArrayMap<String, Dependant<out State, S>> = proxy.childrenDepMap ?: return
        val size = map.size
        for (i in 0 until size) {
            val dependant = map.valueAt(i)
            if (dependant.logic is BaseComponent<*>) {
                dependant.logic.uiController.hide()
            }
        }
    }

    /**
     * 主要是执行View的attach操作，已经安装的组件不执行attach操作，
     * detach/attach 主要针对UI彻底销毁的场景。
     *
     * 绑定/删除操作针对普通View会出现detach之后，无法重新绑定的问题，不建议在普通场景调用。
     */
    internal fun attach() {
        // 如果已经绑定了，则直接走构建UI的逻辑
        initUI()

        // 更新一次UI
        context.runFullUpdate()
        attachChildren()
    }

    private fun attachChildren() {
        // attachAdapter()

        // 处理子组件
        val map: ArrayMap<String, Dependant<out State, S>> = proxy.childrenDepMap ?: return
        val size = map.size
        for (i in 0 until size) {
            val dependant = map.valueAt(i)
            // 走未绑定的逻辑
            if (!dependant.isInstalled) {
                installComponent(dependant)
                continue
            }

            // 如果已经绑定了，走attach流程
            if (dependant.logic is BaseComponent<*>) {
                dependant.logic.uiController.attach()
            } else {
//                if (logic is RootAdapter) {
//                    (logic as RootAdapter<BaseComponentState?>).attach()
//                }
            }
        }
    }

    private fun installComponent(dependant: Dependant<out State, S>) {
        val env = copyEnvironment()
        dependant.installComponent(env)
    }

    /**
     * 主要是执行View的detach操作，已经安装的组件不执行detach操作，
     * detach/attach 主要针对UI彻底销毁的场景。
     *
     * 绑定/删除操作针对普通View会出现detach之后，无法重新绑定的问题，不建议在普通场景调用。
     */
    internal fun detach() {
        detachChildren()

        // 设置不可见
        setShow(false)
        setViewNUll()
    }

    private fun detachChildren() {
        // detachAdapter()

        // 处理子组件
        val map: ArrayMap<String, Dependant<out State, S>> = proxy.childrenDepMap ?: return
        val size = map.size
        for (i in 0 until size) {
            val dependant = map.valueAt(i)
            if (dependant.logic is BaseComponent<*>) {
                dependant.logic.uiController.detach()
            } else {
//                    if (logic is RootAdapter) {
//                        (logic as RootAdapter<BaseComponentState?>).detach()
//                    }
            }
        }
    }

//    private fun attachAdapter() {
//        // 子组件也包含adapter, Adapter要单独处理，adapter是否可复用
//        val adapter: Dependant<out State, State>? = component.adapterDependant
//        adapter?.attach()
//    }
//
//    private fun detachAdapter() {
//        // 子组件也包含adapter, Adapter要单独处理
//        val adapter: Dependant<out State, State>? = component.adapterDependant
//        adapter?.detach()
//    }

    internal fun createUI() {
        if (!isShow) {
            return
        }

        initUI()
        context.runFirstUpdate()
        installSubComponents()
    }

    /**
     * 初始化组件UI，主要是创建UI实例，初始化Adapter，发起发起首次UI更新动作
     */
    private fun initUI() {
        // Adapter 要先于View初始化，因为创建View的过程中会初始化RecyclerView
        // component.initAdapter()

        // 根据屏幕方向，进行View的创建动作
        createView(lastOrientation)

        // 创建View Holder
        if (viewHolder != null) {
            viewHolder!!.dispose()
            viewHolder = null
        }
        viewHolder = currentView?.let { ComponentViewHolder(it) }

        // 设置显示状态
        setShow(true)
    }

    private fun setShow(show: Boolean) {
        isShow = show
        context.state.innerSetProp(State.IS_SHOW_UI_NAME, isShow)
    }

    /**
     * 当UI发生可见性/方向变化的时候，发送Action告知用户，进行一些清理操作
     */
    internal fun sendUIChangedAction(type: UIChangedType) {
        val uiChangedBean = UIChangedBean()
        uiChangedBean.orientation = lastOrientation
        uiChangedBean.isShow = isShow
        uiChangedBean.partialView = portraitView
        uiChangedBean.landscapeView = landscapeView
        uiChangedBean.uiChangeType = type
        context.dispatcher.dispatch(Action(LifeCycleAction.ACTION_ON_UI_CHANGED, uiChangedBean))
    }

    /**
     * 屏幕旋转时，进行View切换，内部进行一些安全检查，并在异常时进行捕获并继续抛出异常
     *
     * @param orientation 当前屏幕的旋转方向
     */
    internal fun createView(orientation: Int) {
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            setPortraitView()
            return
        }
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setLandscapeView()
        }
    }

    private fun setPortraitView() {
        if (portraitView == null) {
            portraitView = callViewBuilder()
        }

        // 如果用户没有设置竖屏UI，则屏幕旋转的时候直接使用横屏UI
        if (portraitView == null) {
            portraitView = landscapeView
        }
        if (portraitView == null) {
            throw RuntimeException("create view failed in portrait, component is: " + proxy.token)
        }
        currentView = portraitView
    }

    private fun setLandscapeView() {
        if (landscapeView == null) {
            landscapeView = callViewBuilder()
        }

        // 如果用户没有设置横屏UI，则屏幕旋转的时候直接使用竖屏UI
        if (landscapeView == null) {
            landscapeView = portraitView
        }
        if (landscapeView == null) {
            throw RuntimeException("create view failed in landscape, component is: " + proxy.token)
        }
        currentView = landscapeView
    }

    /**
     * 获取UI组件的View实例
     */
    private fun callViewBuilder(): View? {
        return try {
            innerViewModule.getView(context, environment.parentView!!)
        } catch (e: Exception) {
            // 这里可能会产生多种异常，比如空指针，重复添加等
            logger.printStackTrace(ILogger.ERROR_TAG, "call view builder fail: ", e)
            null
        }
    }

    /**
     * 当UI数据有更新时，通过此方法触发每个Atom进行更新
     *
     * @param state 当前最新的State
     * @param holder 当前UI组件的View Holder
     */
    fun callUIUpdate(state: S, changedKeys: HashSet<String>?, holder: ComponentViewHolder?) {
        uiPropsWatcher?.update(state, changedKeys, holder)
    }

    /**
     * 重置View Holder
     */
    internal fun resetViewHolder() {
        if (viewHolder != null) {
            viewHolder?.dispose()
        }
        viewHolder = currentView?.let { ComponentViewHolder(it) }
    }

    /**
     * 如果组件有列表型的UI，通过绑定框架提供的Adapter，这样列表型组件也可以纳入状态管理数据流中；
     * 通过此方法初始化Adapter，每个组件只可绑定一个Adapter，以保证组件的粒度可控。
     */
    internal fun initAdapter() {
//        if (dependencies == null) {
//            return
//        }
//        val dependant: HashMap<String, Dependant<out State, State>>? = dependencies?.dependantMap
//        if (dependant != null) {
//            val env = Environment.copy(environment!!)
//            context?.state?.let {
//                env.setParentState(it)
//            }
//            context?.effectDispatch?.let {
//                env.setParentDispatch(it)
//            }
//            dependant.initAdapter(env)
//        }
    }

    /**
     * 每个组件下可能也会挂子组件，通过此方法初始化组件下挂载的子组件
     */
    private fun installSubComponents() {
        val map: ArrayMap<String, Dependant<out State, S>> = proxy.childrenDepMap ?: return
        val size = map.size
        if (size < 1) {
            return
        }

        val env = copyEnvironment()
        env.task = ReflectTask(map.size, environment.taskManager?.executor!!)

        for (i in 0 until size) {
            val dependant = map.valueAt(i)
            dependant.installComponent(env)
        }

        environment.taskManager?.putTask(env.task!!)
    }

    private fun copyEnvironment(): Environment {
        val env = Environment.copy(environment)
        env.parentState = context.state
        env.parentView = currentView
        env.parentDispatch = context.effectDispatch
        return env
    }

    private fun setViewNUll() {
        // 清理一些View
        currentView = null
        landscapeView = null
        portraitView = null
        if (viewHolder != null) {
            viewHolder!!.dispose()
            viewHolder = null
        }
    }

    fun clear() {
        setViewNUll()
    }
}