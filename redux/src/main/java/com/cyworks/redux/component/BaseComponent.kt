package com.cyworks.redux.component

import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.OnLifecycleEvent
import com.cyworks.redux.lifecycle.LifeCycleAction
import com.cyworks.redux.prop.ChangedState
import com.cyworks.redux.state.State
import com.cyworks.redux.util.Environment

/**
 * Desc:组件基类，框架内部实现，外部不能直接使用, 用来承载一个Redux组件.
 * Component = Action + State + Reducer + Effect + Dependant(依赖的子组件)
 *
 * 组件生命周期自治：组件通过注入LifecycleObserver，写法上不依赖activity等环境。
 *
 * note:
 * 由于每个组件自己管理生命周期，可能会跟Page生命周期时机对不上，所以有些依赖生命周期的操作要比较小心。
 *
 * Component支持UI懒加载, 组件UI懒加载：
 * 组件初始化过程中，绑定UI是整个环节中最耗时的操作，如果能延后UI绑定操作，能一定程度上缓解初始化压力。
 * 框架提供了[isShowUI] 来懒加载UI界面，开发者可以自己控制UI展示的时机。
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
    val uiMixin: ComponentUIController<S>?

    /**
     * 构造器，初始一些组件的内部数据
     *
     * @param lazyBindUI 是否延迟加载UI
     */
    init {
        uiMixin = ComponentUIController(this, lazyBindUI)
    }

    fun show() {
        uiMixin?.show(false)
    }

    fun hide() {
        uiMixin?.hide(false)
    }

    fun attach() {
        uiMixin?.attach()
    }

    fun detach() {
        uiMixin?.detach()
    }

    /**
     * 使用LiveData观察数据，触发UI更新
     */
    fun observe() {
        if (environment == null || observer == null) {
            return
        }
        liveData.observe(environment!!.lifeCycleProxy!!.lifecycleOwner, observer)
    }

    override fun install(environment: Environment?, connector: LRConnector<S, State>?) {
        var environment = environment
        if (uiMixin.isBind) {
            return
        }

        uiMixin.isBind = true
        mConnector = connector
        environment = environment

        // 获取启动参数
        val lifeCycleProxy: LifeCycleProxy? = environment!!.lifeCycleProxy
        mBundle = lifeCycleProxy.getBundle()

        // 添加生命周期观察
        liveData = MutableLiveData()
        val lifecycle: Lifecycle = lifeCycleProxy.getLifecycle()
        if (lifecycle != null) {
            lifecycle.addObserver(ComponentLifeCycleObserver(this))
        }
    }

    @SuppressLint("ResourceType")
    override fun createPlatform(): IPlatform? {
        val lifeCycleProxy: LifeCycleProxy? = environment!!.lifeCycleProxy
        val platform = Platform(lifeCycleProxy, environment!!.rootView)
        if (mConnector != null) {
            platform.setStubId(
                mConnector!!.viewContainerIdForV,
                mConnector!!.viewContainerIdForH
            )
        }
        return platform
    }

    override fun makeUIListener(): StateChangeForUI<S> {
        return StateChangeForUI<S> { state, changedProps ->
            val stateCompare: ChangedState<S> = ChangedState()
            stateCompare.mState = state
            stateCompare.mChangedProps = changedProps
            liveData.setValue(stateCompare)
        }
    }

    /**
     * 获取View模块，外部设置
     *
     * @return ViewModule
     */
    abstract val viewModule: ViewModule<S>?
    @CallSuper
    override fun clear() {
        super.clear()
        liveData.removeObserver(observer)
        if (context != null) {
            context!!.destroy()
        }
        uiMixin!!.clear()
        environment = null
    }

    override fun onStateDetected(componentState: S) {
        // 检查默认属性设置
        componentState.isShowUI.innerSetter(uiMixin!!.isShow)

        // 获取初始的屏幕方向
        uiMixin.lastOrientation = componentState.mCurrentOrientation.value()

        // 设置状态 -- UI 监听
        uiMixin.makeUIWatcher(componentState)

        // 运行首次UI更新
        uiMixin.firstUpdate()
    }

    /**
     * 用于初始化Component
     */
    private fun onCreate() {
        val time = SystemClock.uptimeMillis()

        // 1、创建Context
        createContext()

        // 2、如果不懒加载，直接加载界面
        if (uiMixin!!.isShow) {
            uiMixin.initUI()
            uiMixin.firstUpdate()

            // 遍历依赖
            initSubComponent()
        }

        // 3、观察数据
        observe()

        // 4、发送onCreate Effect
        context!!.onLifecycle(LifeCycleAction.ACTION_ON_CREATE)

        // 打印初始化的耗时
        mLogger.d(
            ILogger.PERF_TAG, "component: <"
                    + javaClass.simpleName
                    + "> init consumer: " + (SystemClock.uptimeMillis() - time)
        )
    }

    /**
     * Activity生命周期监听，通过这种方式实现组件的生命周期自治
     */
    private class ComponentLifeCycleObserver(component: BaseComponent<out State>) : LifecycleObserver {
        /**
         * 内部持有组件实例
         */
        private val mComponent: BaseComponent<out State>

        /**
         * 构造器，初始化生命周期观察者
         *
         * @param component AbsComponent
         */
        init {
            mComponent = component
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        fun onCreate() {
            mComponent.onCreate()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onStart() {
            mComponent.context!!.onLifecycle(LifeCycleAction.ACTION_ON_START)
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun onResume() {
            mComponent.context!!.onLifecycle(LifeCycleAction.ACTION_ON_RESUME)
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun onPause() {
            mComponent.context!!.onLifecycle(LifeCycleAction.ACTION_ON_PAUSE)
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun onStop() {
            mComponent.context!!.onLifecycle(LifeCycleAction.ACTION_ON_STOP)
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            // 这里的调用顺序不能乱
            mComponent.context!!.onLifecycle(LifeCycleAction.ACTION_ON_DESTROY)
            mComponent.clear()
        }
    }
}