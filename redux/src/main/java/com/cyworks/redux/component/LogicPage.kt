package com.cyworks.redux.component

import android.os.Bundle
import android.os.Looper
import androidx.annotation.CallSuper
import com.cyworks.redux.*
import com.cyworks.redux.action.Action
import com.cyworks.redux.action.InnerActionTypes
import com.cyworks.redux.dependant.Dependant
import com.cyworks.redux.dependant.DependentCollect
import com.cyworks.redux.dependant.ExtraDependants
import com.cyworks.redux.interceptor.InterceptorBean
import com.cyworks.redux.interceptor.InterceptorCollector
import com.cyworks.redux.interceptor.InterceptorPayload
import com.cyworks.redux.lifecycle.LifeCycleProxy
import com.cyworks.redux.logic.EffectCollector
import com.cyworks.redux.state.State
import com.cyworks.redux.store.PageStore
import com.cyworks.redux.types.Effect
import com.cyworks.redux.types.Interceptor
import com.cyworks.redux.util.Environment
import com.cyworks.redux.util.IPlatform

/**
 * Desc: Page = Feature + Effect + Reducer + middleware
 * Page最重要的功能：负责创建Store以及组织页面的Effect调度。
 *
 * 目的：将页面组装逻辑剥离到一个逻辑类，更方便的构建逻辑层测试。
 *
 * 为何要延迟加载Feature？
 * 这其实是有需求场景的，比如开发者需要等一些server的返回值才能决定目前的加载哪些组件，
 * 再比如：如果开发者需要定制组件的加载过程，也可以通过懒加载的方式进行。
 *
 * 针对组件间交互：
 * PageBus完全隔离掉子组件发送app级别的effect。
 */
abstract class LogicPage<S : State>(proxy: LifeCycleProxy) : Logic<S>(proxy.props) {
    /**
     * 当前App级别的Effect Bus
     */
    protected val appBus: DispatchBus = ReduxManager.instance.appBus

    /**
     * 页面依赖的Feature集合
     */
    protected var dependencies: DependentCollect<S>? = null

    /**
     * Effect拦截器，用于实现子组件与子组件间的通信
     */
    protected var interceptor: Interceptor<S>? = null

    /**
     * 添加拦截 reducer 的 Middleware，开发这如果需要增加一些中间件拦截Action，可以通过此方法注入
     *
     * @return 中间件列表
     */
    protected val reducerMiddleware: List<Any>?
        protected get() = null
    protected val controller: BaseController<S>?
        protected get() = null

    init {
        environment = Environment.of().setLifeCycleProxy(proxy)
        initDependencies()
        initInterceptor()
    }

    /**
     * 创建当前页面所用的Store，只在Page中创建页面Store
     */
    private fun createStore(state: S) {
        val store: PageStore<State> = PageStore(state)
        environment!!.setStore(store)
    }

    private fun initDependencies() {
        // 获取Page对应的依赖集合
        if (dependencies == null) {
            dependencies = DependentCollect()
        }
        addDependencies(dependencies)
    }

    private fun initInterceptor() {
        val innerInterceptorCollect = InterceptorCollector<S>()
        val map: HashMap<String, Dependant<out State, S>>? = dependencies.dependantMap
        if (map.isNullOrEmpty()) {
            return
        }
        for (dependant in map.values) {
            val connector: Connector<out State, S>? = dependant.connector
            val interceptors: HashMap<Any, Any> = makeInterceptorForAction(connector, dependant)
                ?: continue
            innerInterceptorCollect.add(interceptors)
        }
        interceptor = innerInterceptorCollect.getInterceptor()
    }

    private fun makeInterceptorForAction(
        connector: Connector<out State, S>,
        dependant: Dependant<out State, S>): HashMap<Action<Any>, InterceptorBean>? {
        // 获取拦截器收集器
        val interceptorCollect: HashMap<Action<Any>, out Interceptor<out State>>? =


            connector.interceptorCollector()
        if (interceptorCollect == null || interceptorCollect.isEmpty()) {
            return null
        }
        val interceptors: HashMap<Action, InterceptorBean> = HashMap<Action, InterceptorBean>()

        for (action in interceptorCollect.keys) {
            val bean = InterceptorBean()
            bean.mGetter = { dependant.logic.context }
            bean.mInterceptor = interceptorCollect[action]
            interceptors[action] = bean
        }
        return interceptors
    }

    override fun checkEffect(effectCollector: EffectCollector<S>?) {
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
        val effect: Effect<S> = label@ Effect { action, ctx ->
            if (action.payload !is ExtraDependants<*>) {
                return@label
            }
            val extraFeatures: HashMap<String, Dependant<out State, S>>? = (action.payload as ExtraDependants<*>).extra
            if (extraFeatures.isNullOrEmpty()) {
                return@label
            }

            // 安装feature需要在主线程执行
            if (Looper.getMainLooper() == Looper.myLooper()) {
                installExtraDependant(extraFeatures)
                return@label
            }

            ReduxManager.instance.submitInMainThread { installExtraDependant(extraFeatures) }
        }

        return effect
    }

    private fun installExtraDependant(extraDependants: HashMap<String, Dependant<out State, S>>) {
        var hasNewDep = false // 防止多次调用重复安装

        // 当前Feature集合，这里保存的都是已经安装过的
        val map: HashMap<String, Dependant<out State, S>>? = dependencies?.dependantMap

        // 子组件需要从父组件继承一些信息
        val env = environment?.let { Environment.copy(it) }
        env?.setParentDispatch(env.dispatchBus!!.pageDispatch!!)
        env?.setParentState(environment!!.store!!.state)

        for (key in extraDependants.keys) {
            if (map?.containsKey(key) == true) {
                continue
            }

            hasNewDep = true
            val dependant: Dependant<out State, S>? = extraDependants[key]
            if (dependant != null) {
                map?.set(key, dependant)
                if (env != null) {
                    // 安装子组件
                    dependant.install(env)
                }
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
     * 创建Page bus，用于组件间Effect交互
     */
    private fun createPageBus() {
        val bus = DispatchBus()
        bus.attach(appBus)
        environment!!.setDispatchBus(bus)
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
     * 创建页面的ReduxContext，依赖一个初始的PureState
     */
    private fun createContext() {
        // 1、生成state
        val state = onCreateState(props)

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
    }

    /**
     * Page收到容器onCreate生命周期时，执行的一些初始化操作
     */
    @CallSuper
    protected open fun onCreate() {
        // 1、创建Page bus，用于页面内Effect交互
        createPageBus()

        // 2、初始化Page的Context
        createContext()

        // 3、安装子组件
        installDependant()
    }

    /**
     * 安装子组件
     */
    private fun installDependant() {
        val map: HashMap<String, Dependant<out State, S>>? = dependencies?.dependantMap
        if (map.isNullOrEmpty()) {
            return
        }

        // 子组件需要从父组件那边继承一些信息
        val env = environment?.let { Environment.copy(it) }
        env?.setParentDispatch(env.dispatchBus!!.pageDispatch!!)
        env?.setParentState(environment!!.store!!.state)

        // 安装子组件
        for (dependant in map.values) {
            if (env != null) {
                dependant.install(env)
            }
        }
    }

    /**
     * 创建页面的State，页面的State默认页面的一级组件都可以进行关联
     *
     * @return state [State]
     */
    abstract fun onCreateState(props: Bundle?): S

    /**
     * 配置当前页面的Feature(依赖)集合PageDependantCollect，需要外部设置
     */
    abstract fun addDependencies(collect: DependentCollect<S>?)// sub class can impl

    @CallSuper
    protected open fun destroy() {}
}