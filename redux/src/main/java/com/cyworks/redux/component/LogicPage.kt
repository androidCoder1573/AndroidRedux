package com.cyworks.redux.component

import android.support.annotation.CallSuper
import com.cyworks.redux.*
import com.cyworks.redux.dependant.Dependant
import com.cyworks.redux.state.State
import com.cyworks.redux.store.PageStore
import com.cyworks.redux.util.Environment
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.Future

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
abstract class LogicPage<S : BasePageState?>(@NonNull proxy: LifeCycleProxy) :
    Logic<S>(proxy.getBundle()) {
    /**
     * 当前App级别的Effect Bus
     */
    protected val mAppBus: DispatchBus = ReduxManager.getInstance().getAppBus()

    /**
     * 页面依赖的Feature集合
     */
    protected var mDependencies: DependentCollect<S>? = null

    /**
     * Effect拦截器，用于实现子组件与子组件间的通信
     */
    protected var mInterceptor: Interceptor? = null

    /**
     * 用于取消异步任务
     */
    private var mFuture: Future<*>? = null

    /**
     * 创建当前页面所用的Store，只在Page中创建页面Store
     */
    private fun createStore(state: S) {
        // 1、将所有组件的reducer跟page Reducer合并
        val reducer: Reducer<State> = combineReducer()
        val store: PageStore<State> = PageStore(reducer, state)
        environment!!.setStore(store)

        // 3、将Middleware链式化
        val getter: PageStateGetter = store::allState
        val pageMiddleware: List<Middleware>? = reducerMiddleware

        // 4、根据middleware生成最终的reducer
        val dispatch: Dispatch? = store.mDispatch
        store.mDispatch = MiddlewareUtil.applyReducerMiddleware(pageMiddleware, dispatch, getter)
    }

    private fun initDependencies() {
        // 获取Page对应的依赖集合
        if (mDependencies == null) {
            mDependencies = DependentCollect()
        }
        addDependencies(mDependencies)
    }

    private fun initInterceptor() {
        val innerInterceptorCollect = InterceptorCollect()
        val map: HashMap<String, Dependant<out BaseComponentState?, S>> =
            mDependencies.getDependantMap()
        if (map == null || map.isEmpty()) {
            return
        }
        for (dependant in map.values) {
            val connector: Connector<out BaseComponentState?, S> = dependant.connector
            val interceptors: HashMap<Any, Any> = makeInterceptorForAction(connector, dependant)
                ?: continue
            innerInterceptorCollect.add(interceptors)
        }
        mInterceptor = innerInterceptorCollect.getInterceptor()
    }

    private fun makeInterceptorForAction(
        connector: Connector<out BaseComponentState?, S>,
        dependant: Dependant<out BaseComponentState?, S>
    ): HashMap<Action, InterceptorBean>? {
        // 获取拦截器收集器
        val interceptorCollect: HashMap<Action, out Interceptor<out BaseComponentState?>?>? =
            connector.interceptorCollector()
        if (interceptorCollect == null || interceptorCollect.isEmpty()) {
            return null
        }
        val interceptors: HashMap<Action, InterceptorBean> = HashMap<Action, InterceptorBean>()

        // 单一action可以被多个组件同时拦截，这个跟中间件是有区别的，
        // 中间件是一个全局性的操作，而拦截器是针对某个action的操作。
        // 这里创建Bean是为了让每个组件在拦截器里拿到的是自己对应的Context。
        for (action in interceptorCollect.keys) {
            val bean = InterceptorBean()
            bean.mGetter = { dependant.logic.context }
            bean.mInterceptor = interceptorCollect[action]
            interceptors[action] = bean
        }
        return interceptors
    }

    private fun combineReducer(): Reducer<State> {
        val list: ArrayList<SubReducer?> = ArrayList<SubReducer?>()

        // 合并所有组件的Reducer
        mergeReducer(list, null)

        // 合并sub reducer为Reducer
        val childReducers: Reducer = ReducerUtils.combineSubReducers(list)
        val reducers: ArrayList<Reducer> = ArrayList<Reducer>()
        reducers.add(mReducer)
        reducers.add(childReducers)

        // 将所有的reducer合并成一个Reducer
        return ReducerUtils.combineReducers(reducers)
    }

    override fun mergeReducer(
        @NonNull list: MutableList<SubReducer?>,
        connector: Connector<*, *>?
    ) {
        if (mDependencies != null) {
            mDependencies.mergerDependantReducer(list)
        }
    }

    override fun checkReducer(reducerCollect: ReducerCollect<S>) {
        reducerCollect.remove(InnerActions.INTERCEPT_ACTION)
        reducerCollect.remove(InnerActions.INSTALL_EXTRA_FEATURE_ACTION)
        reducerCollect.remove(InnerActions.CHANGE_ORIENTATION)

        // 注册修改横竖屏状态的reducer
        reducerCollect.add(
            InnerActions.CHANGE_ORIENTATION
        ) { getter, action, payload ->
            val state: S = getter.copy()
            state.mCurrentOrientation.set(payload as Int)
            state
        }
    }

    override fun checkEffect(effectCollect: EffectCollect<S>) {
        effectCollect.remove(InnerActions.INTERCEPT_ACTION)
        effectCollect.remove(InnerActions.INSTALL_EXTRA_FEATURE_ACTION)
        effectCollect.remove(InnerActions.CHANGE_ORIENTATION)
        effectCollect.add(InnerActions.INTERCEPT_ACTION, makeInterceptEffect())
        effectCollect.add(
            InnerActions.INSTALL_EXTRA_FEATURE_ACTION,
            makeInstallExtraFeatureEffect()
        )
    }

    private fun makeInterceptEffect(): Effect<S> {
        // 拦截Action的Effect
        return label@ Effect<S> { action, ctx, payload ->
            if (payload !is InterceptorPayload) {
                return@label
            }
            val interceptorPayload: InterceptorPayload = payload as InterceptorPayload
            mInterceptor.doAction(interceptorPayload.mRealAction, ctx, payload)
        }
    }

    private fun makeInstallExtraFeatureEffect(): Effect<S> {
        // 安装额外的子组件的Effect
        return label@ Effect<S> { action, ctx, payload ->
            if (payload !is ExtraDependants) {
                return@label
            }
            val extraFeatures: HashMap<String, Dependant<out BaseComponentState?, S>> =
                (payload as ExtraDependants<S>).mExtraFeature
            if (extraFeatures == null || extraFeatures.isEmpty()) {
                return@label
            }

            // 安装feature需要在主线程执行
            if (Looper.getMainLooper() == Looper.myLooper()) {
                installExtraDependant(extraFeatures)
                return@label
            }
            ReduxManager.getInstance().submitInMainThread { installExtraDependant(extraFeatures) }
        }
    }

    private fun installExtraDependant(extraDependants: HashMap<String, Dependant<out BaseComponentState?, S>>) {
        var hasNewFeature = false // 防止多次调用重复安装

        // 当前Feature集合，这里保存的都是已经安装过的
        val map: HashMap<String, Dependant<out BaseComponentState?, S>> =
            mDependencies.getDependantMap()

        // 子组件需要从父组件继承一些信息
        val env = Environment.copy(environment!!)
        env.setParentDispatch(env.dispatchBus!!.pageDispatch!!)
        env.setParentState(environment!!.store!!.state!!)
        for (key in extraDependants.keys) {
            if (map.containsKey(key)) {
                continue
            }
            hasNewFeature = true
            val dependant: Dependant<out BaseComponentState?, S>? = extraDependants[key]
            if (dependant != null) {
                map[key] = dependant
                // 安装子组件
                dependant.install(env)
            }
        }
        if (!hasNewFeature) {
            // 没有新的Feature被安装，直接返回
            return
        }

        // 重新合并reducer
        environment!!.store!!.mReducer = combineReducer()

        // 重新收集拦截器
        initInterceptor()
    }

    /**
     * 创建Page bus，用于组件间Effect交互
     */
    private fun createPageBus() {
        val bus = DispatchBus()
        bus.attach(mAppBus)
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
        if (ReduxManager.getInstance().getAsyncMode()) {
            createContextAsync()
        } else {
            createContextSync()
        }
    }

    private fun createContextSync() {
        // 1、生成state
        val state = onCreateState(mBundle)

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
            .setPlatform(createPlatform())
            .build()
        context!!.controller = controller
        context!!.setStateReady()
    }

    /**
     * 异步模式，对性能极致需求
     */
    private fun createContextAsync() {
        // 1、生成state
        val state = onCreateState(mBundle)
        val detectFinishRunnable = Runnable {
            context!!.setStateReady()
            // 负责处理额外的事情
            onStateDetected(state)
        }
        val detectRunnable = Runnable {
            state.detectField()
            ReduxManager.getInstance().submitInMainThread(detectFinishRunnable)
        }

        // 整理state
        mFuture = ReduxManager.getInstance().submitInSubThread(detectRunnable)

        // 2、创建Store
        createStore(state)

        // 3、创建Context
        context = ReduxContextBuilder<S>()
            .setLogic(this)
            .setState(state)
            .setPlatform(createPlatform())
            .build()
        context!!.controller = controller
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
        val map: HashMap<String, Dependant<out BaseComponentState?, S>> =
            mDependencies.getDependantMap()
        if (map == null || map.isEmpty()) {
            return
        }

        // 子组件需要从父组件那边继承一些信息
        val env = Environment.copy(environment!!)
        env.setParentDispatch(env.dispatchBus!!.pageDispatch!!)
        env.setParentState(environment!!.store!!.state!!)

        // 安装子组件
        for (dependant in map.values) {
            dependant.install(env)
        }
    }

    /**
     * 创建页面的State，页面的State默认页面的一级组件都可以进行关联
     *
     * @return state [BasePageState]
     */
    abstract fun onCreateState(bundle: Bundle?): S

    /**
     * 配置当前页面的Feature(依赖)集合PageDependantCollect，需要外部设置
     */
    abstract fun addDependencies(collect: DependentCollect<S>?)// sub class can impl

    /**
     * 添加拦截 reducer 的 Middleware，开发这如果需要增加一些中间件拦截Action，可以通过此方法注入
     *
     * @return 中间件列表
     */
    protected val reducerMiddleware: List<Any>?
        protected get() =// sub class can impl
            null
    protected val controller: BaseController<S>?
        protected get() = null

    @CallSuper
    protected open fun destroy() {
        if (mFuture != null && !mFuture!!.isDone) {
            mFuture!!.cancel(true)
        }
    }

    /**
     * 构造器，初始Reducer/Effect以及一些依赖
     *
     * @param proxy LifeCycleProxy
     */
    init {
        environment = Environment.of().setLifeCycleProxy(proxy)
        initDependencies()
        initInterceptor()
    }
}