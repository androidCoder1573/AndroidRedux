package com.cyworks.redux.component

import android.os.Bundle
import android.os.Looper
import androidx.annotation.CallSuper
import com.cyworks.redux.BaseController
import com.cyworks.redux.DispatchBus
import com.cyworks.redux.ReduxContext
import com.cyworks.redux.ReduxContextBuilder
import com.cyworks.redux.ReduxManager
import com.cyworks.redux.action.Action
import com.cyworks.redux.action.InnerActionTypes
import com.cyworks.redux.dependant.Dependant
import com.cyworks.redux.dependant.DependentCollector
import com.cyworks.redux.dependant.ExtraDependants
import com.cyworks.redux.interceptor.InterceptorManager
import com.cyworks.redux.interceptor.InterceptorPayload
import com.cyworks.redux.lifecycle.LifeCycleProxy
import com.cyworks.redux.logic.EffectCollector
import com.cyworks.redux.state.State
import com.cyworks.redux.state.StateType
import com.cyworks.redux.store.PageStore
import com.cyworks.redux.types.Effect
import com.cyworks.redux.types.Interceptor
import com.cyworks.redux.util.Environment
import com.cyworks.redux.util.IPlatform

/**
 * Page最重要的功能：负责创建Store以及组织页面的Effect调度。
 * 将页面组装逻辑剥离到一个逻辑类，更方便的构建逻辑层测试。
 *
 * 为何要延迟加载Dep
 * 这其实是有需求场景的，比如开发者需要等一些server的返回值才能决定目前的加载哪些组件，
 * 再比如：如果开发者需要定制组件的加载过程，也可以通过懒加载的方式进行。
 */
abstract class LogicPage<S : State>(proxy: LifeCycleProxy) : Logic<S>(proxy.props) {
    /**
     * 当前App级别的Effect Bus
     */
    private val appBus: DispatchBus = ReduxManager.instance.appBus

    /**
     * 拦截器，用于实现子组件与子组件间的通信
     */
    protected var interceptor: Interceptor<S>? = null

    private val interceptorManager: InterceptorManager = InterceptorManager()

    /**
     * 页面依赖的Feature集合
     */
    private var dependencies: DependentCollector<S>? = null

    override val childrenDepMap: HashMap<String, Dependant<out State, State>>?
        get() = if (dependencies == null) {
            null
        } else dependencies!!.dependantMap as HashMap<String, Dependant<out State, State>>

    protected val controller: BaseController<S>?
        protected get() = null

    init {
        environment = Environment.of()
        environment.lifeCycleProxy  = proxy
        initDependencies()

        // 开始添加拦截器
        interceptor = interceptorManager.getInterceptor() as Interceptor<S>
        initInterceptor()
    }

    private fun initDependencies() {
        // 获取Page对应的依赖集合
        if (dependencies == null) {
            dependencies = DependentCollector()
        }
        addDependencies(dependencies)
    }

    private fun initInterceptor() {
        val map: HashMap<String, Dependant<out State, S>>? = dependencies?.dependantMap
        if (map.isNullOrEmpty()) {
            return
        }

        for (dependant in map.values) {
            dependant.mergeInterceptor(this.interceptorManager)
        }
    }

    /**
     * Page收到容器onCreate生命周期时，执行的一些初始化操作
     */
    @CallSuper
    protected open fun onCreate() {
        // 创建Page bus，用于页面内Effect交互
        createPageBus()

        // 初始化Page的Context
        createContext()

        // 安装子组件
        installSubComponents()
    }

    /**
     * 创建Page bus，用于组件间Effect交互
     */
    private fun createPageBus() {
        val bus = DispatchBus()
        bus.attach(appBus)
        environment.dispatchBus= bus
    }

    /**
     * 创建页面的ReduxContext，依赖一个初始的PureState
     */
    private fun createContext() {
        // 1、生成state
        val state = onCreateState(props)
        state.setStateType(StateType.PAGE_TYPE)

        // 生成Key映射表
        state.detectField()

        // 负责处理额外的事情
        onStateDetected(state)

        // 2、创建Store
        createStore(state)

        // 3、创建Context
        context = ReduxContextBuilder<S>()
            .setLogic(this)
            .setState(state)
            .setPlatform(createPlatform()!!)
            .build()
        context.controller = controller
        context.setStateReady()

        logicModule.subscribeProps(context.state, propsWatcher)
    }

    /**
     * 创建当前页面所用的Store，只在Page中创建页面Store
     */
    private fun createStore(state: S) {
        val store: PageStore<State> = PageStore(state)
        environment.store = store
    }

    /**
     * 安装子组件
     */
    private fun installSubComponents() {
        val map: HashMap<String, Dependant<out State, S>>? = dependencies?.dependantMap
        if (map.isNullOrEmpty()) {
            return
        }

        // 子组件需要从父组件那边继承一些信息
        val env = copyEnvToChild()

        // 安装子组件
        for (dependant in map.values) {
            dependant.install(env)
        }
    }

    protected abstract fun copyEnvToChild(): Environment

    final override fun checkEffect(effectCollector: EffectCollector<S>?) {
        effectCollector?.remove(InnerActionTypes.INTERCEPT_ACTION_TYPE)
        effectCollector?.remove(InnerActionTypes.INSTALL_EXTRA_FEATURE_ACTION_TYPE)
        effectCollector?.remove(InnerActionTypes.CHANGE_ORIENTATION_TYPE)

        effectCollector?.add(InnerActionTypes.INTERCEPT_ACTION_TYPE, makeInterceptEffect())
        effectCollector?.add(InnerActionTypes.INSTALL_EXTRA_FEATURE_ACTION_TYPE,
            makeInstallExtraFeatureEffect())
    }

    private fun makeInterceptEffect(): Effect<S> {
        // 拦截Action的Effect
        val effect: Effect<S> = Effect { action, ctx ->
            if (action.payload is InterceptorPayload) {
                val interceptorPayload: InterceptorPayload = action.payload as InterceptorPayload
                interceptor?.doAction(interceptorPayload.realAction, ctx)
            }
        }

        return effect
    }

    private fun makeInstallExtraFeatureEffect(): Effect<S> {
        // 安装额外的子组件的Effect
        val effect: Effect<S> = object : Effect<S> {
            override fun doAction(action: Action<out Any>, ctx: ReduxContext<S>?) {
                if (action.payload !is ExtraDependants<*>) {
                    return
                }

                val extraFeatures: HashMap<String, Dependant<out State, S>>? =
                    (action.payload as ExtraDependants<S>).extra
                if (extraFeatures.isNullOrEmpty()) {
                    return
                }

                // 安装feature需要在主线程执行
                if (Looper.getMainLooper() == Looper.myLooper()) {
                    installExtraDependant(extraFeatures)
                    return
                }

                ReduxManager.instance.submitInMainThread { installExtraDependant(extraFeatures) }
            }
        }

        return effect
    }

    private fun installExtraDependant(extraDependants: HashMap<String, Dependant<out State, S>>) {
        var hasNewDep = false // 防止多次调用重复安装

        // 当前Feature集合，这里保存的都是已经安装过的
        val map: HashMap<String, Dependant<out State, S>>? = dependencies?.dependantMap

        // 子组件需要从父组件继承一些信息
        val env = copyEnvToChild()

        for (key in extraDependants.keys) {
            if (map?.containsKey(key) == true) {
                continue
            }

            hasNewDep = true
            val dependant: Dependant<out State, S>? = extraDependants[key]
            if (dependant != null) {
                map?.set(key, dependant)
                // 安装子组件
                dependant.install(env)
            }
        }

        if (!hasNewDep) {
            // 没有新的Feature被安装，直接返回
            return
        }

        // 重新收集拦截器
        initInterceptor()
    }

    /**
     * 创建平台相关操作
     * @return IPlatform
     */
    protected open fun createPlatform(): IPlatform? {
        return null
    }

    /**
     * 对于实际的Page组件，可能要设置额外的信息
     */
    protected open fun onStateDetected(state: S) {}

    /**
     * 创建页面的State，页面的State默认页面的一级组件都可以进行关联
     *
     * @return state [State]
     */
    abstract fun onCreateState(props: Bundle?): S

    /**
     * 配置当前页面的Feature(依赖)集合PageDependantCollect，需要外部设置
     */
    abstract fun addDependencies(collect: DependentCollector<S>?)// sub class can impl

    @CallSuper
    protected open fun destroy() {}
}