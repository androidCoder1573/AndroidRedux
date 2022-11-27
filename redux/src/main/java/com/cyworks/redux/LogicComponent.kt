package com.cyworks.redux

import android.support.annotation.CallSuper
import java.util.HashMap
import java.util.concurrent.Future

/**
 * Desc: Live-Redux框架是一个UI/逻辑完全分离的框架，LogicComponent内只针对状态管理，没有任何逻辑操作。
 *
 * 目的：实现逻辑层测试，剥离出UI之后更容易构建整个逻辑层测试。
 */
abstract class LogicComponent<S : BaseComponentState?>(bundle: Bundle?) : Logic<S>(bundle) {
    /**
     * 当前组件的与页面的连接器
     */
    @JvmField
    protected var mConnector: LRConnector<S, State?>? = null

    /**
     * 用于观察全局store
     */
    private var mGlobalStoreWatcher: GlobalStoreWatcher<S>? = null

    /**
     * 组件的依赖的子组件的集合
     */
    var mDependencies: DependentCollect<State?>? = null

    /**
     * 用于取消异步任务
     */
    private var mFuture: Future<*>? = null
    override fun mergeReducer(
        @NonNull list: MutableList<SubReducer?>,
        connector: LRConnector<*, *>?
    ) {
        super.mergeReducer(list, connector)
        mDependencies.mergerDependantReducer(list)
    }

    /**
     * 增加组件的依赖，子类如果有子组件，需要实现此方法
     * @param collect DependentCollect
     */
    protected fun addDependencies(collect: DependentCollect<State?>?) {
        // sub class impl
    }

    /**
     * 合并State，主要是合并两种State：
     * 1、合并父组件的属性；
     * 2、合并全局的State的属性。
     *
     * @param state 当前组件的State
     */
    private fun mergeState(state: S, cb: IPropsChanged) {
        val parentState = mConnector!!.parentState

        // 关联框架内部数据
        if (parentState is BaseComponentState) {
            state.mCurrentOrientation.dependantProp(
                (parentState as BaseComponentState?).mCurrentOrientation
            )
        } else if (parentState is BasePageState) {
            state.mCurrentOrientation.dependantProp(
                (parentState as BasePageState?).mCurrentOrientation
            )
        }

        // 生成依赖的属性
        mConnector!!.parentStateCollector(state, parentState)

        // 创建全局store监听器
        mGlobalStoreWatcher = GlobalStoreWatcher(cb, state)

        // 绑定全局store的state中的属性
        mConnector!!.globalStateCollector(mGlobalStoreWatcher)
        mGlobalStoreWatcher!!.generateDependant()
    }

    /**
     * 创建平台相关操作
     * @return IPlatform
     */
    protected open fun createPlatform(): IPlatform? {
        return null
    }

    /**
     * UI 组件实现这个方法, 返回UI更新器
     */
    protected open fun makeUIListener(): StateChangeForUI<S>? {
        return null
    }

    @CallSuper
    protected fun createContext() {
        if (ReduxManager.getInstance().getAsyncMode()) {
            createContextAsync()
        } else {
            createContextSync()
        }
    }

    private fun createContextSync() {
        // 生成初始State
        val componentState = onCreateState(mBundle)

        // 生成内部的Key映射表
        componentState.detectField()

        // 合并page State以及global State
        mergeState(componentState, IPropsChanged { props ->
            if (context != null) {
                context!!.onStateChange(props)
            }
        })

        // 负责处理额外的事情
        onStateDetected(componentState)

        // 创建Context
        context = ReduxContextBuilder<S>()
            .setLogic(this)
            .setState(componentState)
            .setOnStateChangeListener(makeUIListener())
            .setPlatform(createPlatform())
            .build()
        context.setController(controller)
        context!!.setStateReady()
    }

    /**
     * 异步模式，对性能极致需求
     */
    private fun createContextAsync() {
        // 生成初始State
        val componentState = onCreateState(mBundle)
        val detectFinishRunnable = Runnable {
            context!!.setStateReady()
            // 负责处理额外的事情
            onStateDetected(componentState)
        }
        val detectRunnable = Runnable {
            componentState.detectField()

            // 合并page State以及global State
            mergeState(componentState, IPropsChanged { props ->
                if (context != null) {
                    context!!.onStateChange(props)
                }
            })

            // 提交到主线程
            ReduxManager.getInstance().submitInMainThread(detectFinishRunnable)
        }

        // 检测以及merge state
        mFuture = ReduxManager.getInstance().submitInSubThread(detectRunnable)

        // 创建Context
        context = ReduxContextBuilder<S>()
            .setLogic(this)
            .setState(componentState)
            .setOnStateChangeListener(makeUIListener())
            .setPlatform(createPlatform())
            .build()
        context.setController(controller)
    }

    /**
     * 如果是UI组件，可能要设置额外的信息，比如组件是否可见，当前屏幕方向等
     * @param componentState BaseState
     */
    protected open fun onStateDetected(componentState: S) {
        // impl sub class
    }

    protected val controller: BaseController<S>?
        protected get() = null

    /**
     * 每个组件下可能也会挂子组件，通过此方法初始化组件下挂载的子组件
     */
    fun initSubComponent() {
        if (mDependencies == null) {
            return
        }
        val map: HashMap<String, Dependant<out BaseComponentState?, State>> =
            mDependencies.getDependantMap()
        if (map == null || map.isEmpty()) {
            return
        }
        val env = Environment.copy(environment!!)
        env.setParentState(context.getState())
            .setParentDispatch(context.getEffectDispatch())
        for (dependant in map.values) {
            dependant?.initComponent(env)
        }
    }

    /**
     * 如果组件有列表型的UI，通过绑定框架提供的Adapter，这样列表型组件也可以纳入状态管理数据流中；
     * 通过此方法初始化Adapter，每个组件只可绑定一个Adapter，以保证组件的粒度可控。
     */
    fun initAdapter() {
        if (mDependencies == null) {
            return
        }
        val dependant: Dependant<out BaseComponentState?, State> =
            mDependencies.getAdapterDependant()
        if (dependant != null) {
            val env = Environment.copy(environment!!)
            env.setParentState(context.getState())
                .setParentDispatch(context.getEffectDispatch())
            dependant.initAdapter(env)
        }
    }

    /**
     * 获取依赖的子组件集合
     *
     * @return Map 子组件集合
     */
    val childrenDependant: HashMap<String, Dependant<out Any?, State>>?
        get() = if (mDependencies == null) {
            null
        } else mDependencies.getDependantMap()

    /**
     * 获取依赖的列表组件的Adapter
     *
     * @return Dependant Adapter依赖
     */
    val adapterDependant: Dependant<out Any?, State>?
        get() = if (mDependencies == null) {
            null
        } else mDependencies.getAdapterDependant()

    /**
     * 组件不需要关心这些内部action
     * @param reducerCollect ReducerCollect
     */
    override fun checkReducer(@NonNull reducerCollect: ReducerCollect<S>) {
        reducerCollect.remove(InnerActions.INTERCEPT_ACTION)
        reducerCollect.remove(InnerActions.INSTALL_EXTRA_FEATURE_ACTION)
        reducerCollect.remove(InnerActions.CHANGE_ORIENTATION)
    }

    /**
     * 对组件来说，不需要关注这些内部action，防止用户错误的注册框架的action
     * @param effectCollect EffectCollect
     */
    override fun checkEffect(@NonNull effectCollect: EffectCollect<S>) {
        effectCollect.remove(InnerActions.INTERCEPT_ACTION)
        effectCollect.remove(InnerActions.INSTALL_EXTRA_FEATURE_ACTION)
    }

    /**
     * 创建当前组件对应的State
     *
     * @return PureState
     */
    abstract fun onCreateState(bundle: Bundle?): S

    /**
     * 用于父组件安装子组件接口，并注入环境和连接器
     *
     * @param environment 组件需要的环境
     * @param connector 父组件的连接器
     */
    abstract fun install(
        @NonNull environment: Environment?,
        @NonNull connector: LRConnector<S, State?>?
    )

    /**
     * 清理操作，需要子类重写
     */
    @CallSuper
    protected open fun clear() {
        if (mFuture != null && !mFuture!!.isDone) {
            mFuture!!.cancel(true)
        }
        mGlobalStoreWatcher!!.clear()
        if (mDependencies != null) {
            mDependencies.clear()
            mDependencies = null
        }
    }

    /**
     * 构造器，初始Reducer/Effect以及一些依赖
     *
     * @param bundle 页面传递下来的参数
     */
    init {
        if (mDependencies == null) {
            mDependencies = DependentCollect()
        }
        addDependencies(mDependencies)
    }
}