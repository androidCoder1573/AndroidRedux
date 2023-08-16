package com.cyworks.redux.component

import android.annotation.SuppressLint
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.OnLifecycleEvent
import com.cyworks.redux.ReduxContext
import com.cyworks.redux.action.Action
import com.cyworks.redux.dependant.Dependant
import com.cyworks.redux.lifecycle.LifeCycleAction
import com.cyworks.redux.lifecycle.LifeCycleProxy
import com.cyworks.redux.prop.ChangedState
import com.cyworks.redux.state.State
import com.cyworks.redux.types.IStateChange
import com.cyworks.redux.ui.ViewModule
import com.cyworks.redux.util.Environment
import com.cyworks.redux.util.ILogger
import com.cyworks.redux.util.IPlatform
import com.cyworks.redux.util.Platform

data class ComponentProxy<S : State>(
    val childrenDepMap: HashMap<String, Dependant<out State, State>>?,
    val environment: Environment?,
    val token: String,
    val lazyBindUI: Boolean,
    val viewModule: ViewModule<S>,
    val ctx: ReduxContext<S>
)

/**
 * 组件基类，框架内部实现，外部不能直接使用, 用来承载一个Redux组件.
 * Component = Action + State + Reducer + Effect + Dependant(依赖的子组件)
 *
 * 组件生命周期自治：组件通过注入LifecycleObserver，写法上不依赖activity等环境。
 * <p>
 * note:
 * 由于每个组件自己管理生命周期，可能会跟Page生命周期时机对不上，所以有些依赖生命周期的操作要比较小心。
 *
 * Component支持UI懒加载, 组件UI懒加载：
 * 组件初始化过程中，绑定UI是整个环节中最耗时的操作，如果能延后UI绑定操作，能一定程度上缓解初始化压力。
 * 框架提供了isShowUI来懒加载UI界面，开发者可以自己控制UI展示的时机。
 *
 * @params: lazyBindUI 是否延迟加载UI
 */
abstract class BaseComponent<S : State>(lazyBindUI: Boolean) : LogicComponent<S>(null) {
    /**
     * 使用LiveData包裹变更的状态数据，防止因为生命周期导致界面异常
     */
    var liveData: MutableLiveData<ChangedState<S>>? = null

    /**
     * LiveData的Observer
     */
    var observer: Observer<ChangedState<S>>? = null

    /**
     * 将对UI的操作放在这里
     */
    internal val uiController: ComponentUIController<S>

    init {
        val componentProxy = ComponentProxy(
            childrenDepMap,
            environment,
            this.javaClass.name,
            lazyBindUI,
            createViewModule(),
            context
        )
        uiController = ComponentUIController(componentProxy)
    }

    fun isInstalled(): Boolean {
        return uiController.installed
    }

    /**
     * 使用LiveData观察数据，触发UI更新
     */
    fun observeUIData() {
        if (environment == null || observer == null) {
            return
        }
        environment!!.lifeCycleProxy!!.lifecycleOwner?.let { liveData?.observe(it, observer!!) }
    }

    override fun install(env: Environment?, connector: Connector<S, State>?) {
        if (uiController.installed) {
            return
        }
        uiController.installed = true

        this.connector = connector
        environment = env

        // 获取启动参数
        // todo 都从统一的地方拿启动参数是否合理
        val lifeCycleProxy: LifeCycleProxy? = environment!!.lifeCycleProxy
        props = lifeCycleProxy?.props

        // 添加生命周期观察
        liveData = MutableLiveData()
        lifeCycleProxy?.lifecycle?.addObserver(ComponentLifeCycleObserver(this))
    }

    @SuppressLint("ResourceType")
    override fun createPlatform(): IPlatform? {
        val lifeCycleProxy: LifeCycleProxy? = environment?.lifeCycleProxy
        val platform = lifeCycleProxy?.let { environment!!.rootView?.let { it1 ->
            Platform(it, it1)
        } }
        if (connector != null) {
            platform?.setStubId(connector!!.viewContainerIdForV, connector!!.viewContainerIdForH)
        }
        return platform
    }

    override fun makeStateChangeCB(): IStateChange<S> {
        return IStateChange { state, changedProps ->
            val stateCompare = ChangedState(state, changedProps)
            liveData?.setValue(stateCompare)
        }
    }

    @CallSuper
    override fun clear() {
        super.clear()
        observer?.let { liveData?.removeObserver(it) }
        context.destroy()
        uiController.clear()
        environment = null
    }

    override fun onStateMerged(componentState: S) {
        componentState.innerSetProp("isShowUI", uiController.isShow) // 检查默认属性设置
        uiController.onStateMerged(componentState)
    }

    /**
     * 创建View模块，外部设置
     */
    abstract fun createViewModule(): ViewModule<S>

    /**
     * 用于初始化Component
     */
    protected open fun onCreate() {
        val time = System.currentTimeMillis()

        // 1、创建Context
        createContext()

        // 2、加载界面
        uiController.createUI()

        // 3、观察数据
        observeUIData()

        // 4、发送onCreate Effect
        context.onLifecycle(Action(LifeCycleAction.ACTION_ON_CREATE, null))

        // 打印初始化的耗时
        logger.d(ILogger.PERF_TAG, "component: <" + javaClass.simpleName + ">"
                    + "init consume: " + (System.currentTimeMillis() - time))
    }

    /**
     * Activity生命周期监听，通过这种方式实现组件的生命周期自治
     */
    private class ComponentLifeCycleObserver(component: BaseComponent<out State>) : LifecycleObserver {
        /**
         * 内部持有组件实例
         */
        private val component: BaseComponent<out State>

        init {
            this.component = component
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        fun onCreate() {
            component.onCreate()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onStart() {
            component.context.onLifecycle(Action(LifeCycleAction.ACTION_ON_START, null))
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun onResume() {
            component.context.onLifecycle(Action(LifeCycleAction.ACTION_ON_RESUME, null))
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun onPause() {
            component.context.onLifecycle(Action(LifeCycleAction.ACTION_ON_PAUSE, null))
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun onStop() {
            component.context.onLifecycle(Action(LifeCycleAction.ACTION_ON_STOP, null))
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            // 这里的调用顺序不能乱
            component.context.onLifecycle(Action(LifeCycleAction.ACTION_ON_DESTROY, null))
            component.clear()
        }
    }
}