package com.cyworks.redux.component

import android.content.res.Configuration
import android.view.View
import com.cyworks.redux.ReduxManager
import com.cyworks.redux.action.Action
import com.cyworks.redux.atom.PropsWatcher
import com.cyworks.redux.beans.UIChangedBean
import com.cyworks.redux.beans.UIChangedType
import com.cyworks.redux.dependant.Dependant
import com.cyworks.redux.lifecycle.LifeCycleAction
import com.cyworks.redux.state.State
import com.cyworks.redux.ui.ComponentViewHolder
import com.cyworks.redux.ui.ViewModule
import com.cyworks.redux.util.Environment
import com.cyworks.redux.util.ILogger

/**
 * 对组件的UI操作抽取出来放到UI处理类中:
 * 1、初始化UI;
 * 2、设置Atom，用于UI的局部更新;
 * 3、设置UI组件的View的显示/隐藏以及绑定/删除操作;
 *
 * todo: 可以考虑将此类让开发者实现，更加定制化的操作UI
 */
class ComponentUIController<S : State>(private val component: BaseComponent<S>, lazyBindUI: Boolean) {
    /**
     * 组件是否bind到父组件上
     */
    internal var isBind: Boolean = false

    /**
     * 当前组件的UI界面是否显示
     */
    internal var isShow: Boolean = false

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
    var currentView: View? = null

    /**
     * 保存当前组件View的辅助类，方便在更新接口中获取view
     */
    var viewHolder: ComponentViewHolder? = null

    /**
     * 当前组件对应的UI的更新接口
     */
    private val viewModule: ViewModule<S>?

    /**
     * 用于监听本组件的属性变化, 并进行UI更新，运行在主线程不要做耗时操作
     */
    protected var propsWatcher: PropsWatcher<S>? = null

    /**
     * Log 组件
     */
    private val logger: ILogger = ReduxManager.instance.logger

    /**
     * 上次的屏幕方向
     */
    var lastOrientation = Configuration.ORIENTATION_PORTRAIT

    private var isRunFirstUpdate = false

    init {
        viewModule = component.viewModule
        // 如果是延迟加载，表明初始状态时不加载UI
        isShow = !lazyBindUI
    }

    /**
     * 为当前组件创建UI片段观察者
     */
    internal fun makeUIWatcher(state: S) {
        if (viewModule == null) {
            return
        }

        // 创建UI更新器
        propsWatcher = PropsWatcher()
        viewModule.subscribeProps(state, propsWatcher)
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
     * 组件UI展示
     *
     * @param needVisible 只有调用过程中的第一个View才需要显示
     */
    internal fun show(needVisible: Boolean) {
        // 如果已经绑定了，则直接走构建UI的逻辑
        if (lastOrientation == Configuration.ORIENTATION_PORTRAIT && portraitView == null
            || lastOrientation == Configuration.ORIENTATION_LANDSCAPE && landscapeView == null
        ) {
            initUI()
        }

        // 根组件进行展示
        if (needVisible) {
            if (currentView != null) {
                currentView!!.visibility = View.VISIBLE
            }
        }

        // 更新一次UI
        fullUpdate()

        // 通知组件当前组件UI发生变化了，给用户一个机会做一些善后处理
        // 首次初始化不需要这些
        sendUIChangedAction(UIChangedType.TYPE_VISIBILITY_CHANGE)
        showChildren()
    }

    private fun showChildren() {
        attachAdapter()

        // 处理子组件
        val map: HashMap<String, Dependant<out S, State>>? = component.childrenDependant
        if (map == null || map.isEmpty()) {
            return
        }
        for (dependant in map.values) {
            if (dependant == null) {
                continue
            }
            if (!dependant.isInstalled) {
                installComponent(dependant)
                continue
            }

            // 如果绑定了，则走show的逻辑
            dependant.show()
        }
    }

    /**
     * 组件UI隐藏，跟detach还是有区别的，detach了之后只有像ViewPager/Dialog这类的组件，
     * 才能够重新安装，否则ViewStub被清除，导致重新加载布局时出现异常。
     *
     * @param needGone 只有调用过程中的第一个View才需要隐藏
     */
    internal fun hide(needGone: Boolean) {
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
        detachAdapter()

        // 处理子组件
        val map: HashMap<String, Dependant<out S, State>>? = component.childrenDependant
        if (map != null && !map.isEmpty()) {
            for (dependant in map.values) {
                dependant.hide()
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
        fullUpdate()
        attachChildren()
    }

    private fun attachChildren() {
        attachAdapter()

        // 处理子组件
        val map: HashMap<String, Dependant<out S, State>>? = component.childrenDependant
        if (map == null || map.isEmpty()) {
            return
        }

        for (dependant in map.values) {
            // 走未绑定的逻辑
            if (!dependant.isInstalled) {
                installComponent(dependant)
                continue
            }

            // 如果已经绑定了，走attach流程
            dependant.attach()
        }
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

    private fun detachChildren() {
        if (component == null) {
            return
        }
        detachAdapter()

        // 处理子组件
        val map: HashMap<String, Dependant<out S, State>>? = component.childrenDependant
        if (map != null && !map.isEmpty()) {
            for (dependant in map.values) {
                dependant.detach()
            }
        }
    }

    private fun installComponent(dependant: Dependant<out S, State>) {
        val context = component.context
        val env = Environment.copy(component.environment!!)
        env.setParentState(context!!.state)
            .setParentDispatch(context.effectDispatch)
        dependant.initComponent(env)
    }

    private fun attachAdapter() {
        // 子组件也包含adapter, Adapter要单独处理，adapter是否可复用
        val adapter: Dependant<out S, State>? = component.adapterDependant
        adapter?.attach()
    }

    private fun detachAdapter() {
        // 子组件也包含adapter, Adapter要单独处理
        val adapter: Dependant<out S, State>? = component.adapterDependant
        adapter?.detach()
    }

    /**
     * 初始化组件UI，主要是创建UI实例，初始化Adapter，发起发起首次UI更新动作
     */
    internal fun initUI() {
        // 绑定View
        if (viewModule == null) {
            return
        }

        // Adapter 要先于View初始化，因为创建View的过程中会初始化RecyclerView
        component.initAdapter()

        // 根据屏幕方向，进行View的初始化操作
        changeView(lastOrientation)

        // 创建View Holder
        if (viewHolder != null) {
            viewHolder!!.dispose()
            viewHolder = null
        }
        viewHolder = currentView?.let { ComponentViewHolder(it) }

        // 设置显示状态
        setShow(true)
    }

    /**
     * 首次渲染时，对UI更新一次数据
     */
    internal fun firstUpdate() {
        if (currentView == null || isRunFirstUpdate) {
            return
        }
        val context = component.context
        if (context != null && context.stateReady()) {
            isRunFirstUpdate = true
            context.firstUpdate()
        }
    }

    /**
     * 触发一次全量更新
     */
    internal fun fullUpdate() {
        val context = component.context
        context?.runFullUpdate()
    }

    private fun setShow(show: Boolean) {
        isShow = show
        val context = component.context
        context?.state?.isShowUI?.innerSetter(isShow)
    }

    /**
     * 当UI发生可见性/方向变化的时候，发送Action告知用户，进行一些清理操作
     *
     * @param type UI变化的类型
     */
    internal fun sendUIChangedAction(type: UIChangedType) {
        val uiChangedBean = UIChangedBean()
        uiChangedBean.orientation = lastOrientation
        uiChangedBean.isShow = isShow
        uiChangedBean.partialView = portraitView
        uiChangedBean.landscapeView = landscapeView
        uiChangedBean.uiChangeType = type
        val context = component.context
        context?.dispatchEffect(Action(LifeCycleAction.ACTION_ON_UI_CHANGED, uiChangedBean))
    }

    /**
     * 屏幕旋转时，进行View切换，内部进行一些安全检查，并在异常时进行捕获并继续抛出异常
     *
     * @param orientation 当前屏幕的旋转方向
     */
    internal fun changeView(orientation: Int) {
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
            throw RuntimeException(
                "create view failed in portrait, component is: " + component.javaClass.name
            )
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
            throw RuntimeException(
                "create view failed in landscape, component is: " + component.javaClass.name
            )
        }
        currentView = landscapeView
    }

    /**
     * 获取UI组件的View实例
     * @return 组件View实例
     */
    private fun callViewBuilder(): View? {
        val context = component.context
        if (viewModule == null || context == null) {
            return null
        }

        try {
            return viewModule.getView(context, component.rootView)
        } catch (e: Exception) {
            // 这里可能会产生多种异常，比如空指针，重复添加等
            logger.printStackTrace(ILogger.ERROR_TAG, "call view builder fail: ", e)
            return null
        }
    }

    /**
     * 针对UI组件来说，需要检查是否可以更新UI
     * @return 当mViewUpdater不为null时，此时说明当前组件存在UI，可以更新UI
     */
    fun canNotUpdateUI(): Boolean {
        return viewModule == null
    }

    /**
     * 当UI数据有更新时，通过此方法触发每个Atom进行更新
     *
     * @param state 当前最新的State
     * @param holder 当前UI组件的View Holder
     */
    fun callUIUpdate(state: S, holder: ComponentViewHolder?) {
        propsWatcher?.update(state, holder)
    }

    fun clear() {
        setViewNUll()
    }
}