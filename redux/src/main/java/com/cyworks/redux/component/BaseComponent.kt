package com.cyworks.redux.component

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.ArrayMap
import androidx.annotation.CallSuper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.cyworks.redux.ReduxManager
import com.cyworks.redux.action.Action
import com.cyworks.redux.lifecycle.LifeCycleAction
import com.cyworks.redux.lifecycle.LifeCycleProxy
import com.cyworks.redux.prop.ReactiveProp
import com.cyworks.redux.state.State
import com.cyworks.redux.types.IStateChange
import com.cyworks.redux.ui.ViewModule
import com.cyworks.redux.util.Environment
import com.cyworks.redux.util.ILogger
import com.cyworks.redux.util.IPlatform
import com.cyworks.redux.util.Platform

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
abstract class BaseComponent<S : State>(lazyBindUI: Boolean, p: Bundle?) : LogicComponent<S>(p) {
    /**
     * 组件是否bind到父组件上
     */
    private var installed: Boolean = false

    /**
     * 使用LiveData，防止因为生命周期导致界面异常
     */
    var liveData: MutableLiveData<String>? = null
    protected val changedStateMap = ArrayMap<String, List<ReactiveProp<Any>>>()

    /**
     * LiveData的Observer
     */
    var observer: Observer<String>? = null
    private var version = 0 // 状态变更版本

    /**
     * 将对UI的操作放在这里
     */
    internal val uiController: ComponentUIController<S>

    init {
        val componentProxy = ComponentProxy(
            childrenDepMap,
            this.javaClass.name,
            lazyBindUI,
            object : ViewModuleProvider<S> {
                override fun provider(): ViewModule<S> {
                    return createViewModule()
                }
            }
        )
        uiController = ComponentUIController(componentProxy)
    }

    fun isInstalled(): Boolean {
        return installed
    }

    /**
     * 使用LiveData观察数据，触发UI更新
     */
    fun observeUIData() {
        if (observer == null || environment.lifeCycleProxy == null) {
            return
        }

        environment.lifeCycleProxy!!.lifecycleOwner?.let { liveData?.observe(it, observer!!) }
    }

    override fun install(env: Environment, connector: Connector<S, State>?) {
        if (installed) {
            return
        }
        installed = true

        this.connector = connector
        environment = env
        uiController.setReduxEnv(environment)

        registerLifecycle()
    }

    @SuppressLint("ResourceType")
    override fun createPlatform(): IPlatform? {
        val lifeCycleProxy: LifeCycleProxy? = environment.lifeCycleProxy
        if (lifeCycleProxy == null) {
            logger.e("component", "can not create platform when lifeCycleProxy null")
            return null
        }

        val platform = Platform(lifeCycleProxy, environment.parentView)
        if (connector != null) {
            platform.setStubId(connector!!.viewContainerIdForV, connector!!.viewContainerIdForH)
        }

        return platform
    }

    override fun makeStateChangeCB(): IStateChange<S> {
        return IStateChange { changedProps ->
            version++
            changedStateMap[version.toString()] = changedProps
            liveData?.setValue(version.toString())
        }
    }

    override fun onStateMerged(componentState: S) {
        // 检查默认属性设置
        componentState.innerSetProp(State.IS_SHOW_UI_NAME, uiController.isShow)
        uiController.onStateMerged(componentState)
    }

    private fun registerLifecycle() {
        val lifeCycleProxy: LifeCycleProxy? = environment.lifeCycleProxy

        // 添加生命周期观察
        liveData = MutableLiveData()
        lifeCycleProxy?.lifecycle?.addObserver(ComponentLifeCycleObserver(this))
    }

    /**
     * 创建View模块，外部设置
     */
    abstract fun createViewModule(): ViewModule<S>

    @CallSuper
    override fun destroy() {
        super.destroy()
        observer?.let { liveData?.removeObserver(it) }
        context.destroy()
        uiController.clear()
        environment.clear()
    }

    /**
     * 用于初始化Component
     */
   internal fun onCreate() {
        val time = System.currentTimeMillis()

        // 创建Context
        createContext()
        uiController.setReduxContext(context)

        // 加载界面
        uiController.createUI()

        // 观察数据
        observeUIData()

        // 发送onCreate Effect
        context.onLifecycle(Action(LifeCycleAction.ACTION_ON_CREATE, null))

        if (ReduxManager.instance.enableLog) {
            // 打印初始化的耗时
            logger.d(ILogger.PERF_TAG, "component: <${javaClass.simpleName}>"
                    + " init consume: ${System.currentTimeMillis() - time}ms")
        }
    }

    /**
     * Activity生命周期监听，通过这种方式实现组件的生命周期自治
     */
    private class ComponentLifeCycleObserver(c: BaseComponent<out State>) :
        DefaultLifecycleObserver {

        private val component: BaseComponent<out State> = c

        override fun onCreate(owner: LifecycleOwner) {
            component.onCreate()
        }

        override fun onStart(owner: LifecycleOwner) {
            component.context.onLifecycle(Action(LifeCycleAction.ACTION_ON_START, null))
        }

        override fun onResume(owner: LifecycleOwner) {
            component.context.onLifecycle(Action(LifeCycleAction.ACTION_ON_RESUME, null))
        }

        override fun onPause(owner: LifecycleOwner) {
            component.context.onLifecycle(Action(LifeCycleAction.ACTION_ON_PAUSE, null))
        }

        override fun onStop(owner: LifecycleOwner) {
            component.context.onLifecycle(Action(LifeCycleAction.ACTION_ON_STOP, null))
        }

        override fun onDestroy(owner: LifecycleOwner) {
            // 这里的调用顺序不能乱
            component.context.onLifecycle(Action(LifeCycleAction.ACTION_ON_DESTROY, null))
            component.destroy()
        }
    }
}