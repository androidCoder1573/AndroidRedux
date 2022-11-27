package com.cyworks.redux

import com.cyworks.redux.store.PageStore
import com.cyworks.redux.store.StoreObserver
import com.cyworks.redux.types.Dispose
import java.util.ArrayList
import java.util.HashMap

/**
 * Desc: Redux Context, context是开发者主要关心的类，主要用来做一些界面交互，发送Action等.
 *
 * 为什么将对store的操作封装成context对象？
 * context里主要做了这几件事情：
 * 1、创建Dispatch：包括Effect以及Reducer。
 * 2、初始化store观察者，并注入到Store中
 * 3、初始化属性观察者，方便开发者观察组件内某些属性的变化
 * 4、维护一个本组件State的副本，方便进行界面更新
 * 5、维护了平台相关的一些操作，比如权限控制，启动activity等。
 *
 * 通过Context封装的方式，让组件对store的操作更加内聚。
 */
class ReduxContext<S : State> internal constructor(builder: ReduxContextBuilder<S>) {
    /**
     * ReduxContext对应的组件实例
     */
    private var logic: Logic<S>?

    /**
     * 组件对应的State
     */
    private val mState: S?

    /**
     * 当状态发生变化时，通过此接口分发给UI
     */
    private val stateChangeListener: StateChangeForUI<S>?

    /**
     * 保存对state provider的反注册器
     */
    private var stateGetterDispose: Dispose? = null

    /**
     * 保存对dispatch的反注册器
     */
    private var dispatchDispose: Dispose? = null

    /**
     * 保存对UI更新listener的反注册器
     */
    private var uiUpdaterDispose: Dispose? = null

    /**
     * 保存对store观察的反注册器
     */
    private val storeObserverDispose: Dispose? = null

    /**
     * 存放当前组件已更新的属性，在下一次vsync信号过来时，用于UI更新
     */
    private var mPendingChangedProps: HashMap<String?, ReactiveProp<Any>>? = null

    /**
     * 可分发effect的dispatch
     */
    private var effectDispatch: Dispatch? = null

    /**
     * 保存一些父组件相关的数据
     */
    private var environment: Environment?

    /**
     * 平台操作相关
     */
    private val mPlatform: IPlatform

    /**
     * log组件
     */
    private val mLogger: ILogger = ReduxManager.instance.logger

    /**
     * 是否销毁了
     */
    private var isDestroy = false

    /**
     * 是否检测状态完成
     */
    @Volatile
    private var isStateReady = false

    /**
     * 存放因为异步操作而被挂起的action
     */
    private var mPendingLifeCycleActionList: ArrayList<Action>? = null

    /**
     * 如果开发这不想使用Action驱动，可以通过传统的方式书写逻辑代码，需继承BaseController
     */
    private var mController: BaseController<S>? = null
    var controller: BaseController<S>?
        get() = mController
        set(controller) {
            mController = controller
            if (mController == null) {
                mController = BaseController()
            }
            mController!!.setReduxContext(this)
            initDispatch()
        }

    private fun initDispatch() {
        mReducerDispatch = Dispatch { action, payload ->
            mLogger.d(
                ILogger.ACTION_TAG, ("reducer action is <"
                        + action.getName()) + ">, in <" + mLogic!!.javaClass.simpleName.toString() + ">"
            )
            if (mEnvironment != null) {
                val store = mEnvironment!!.store
                store?.dispatch(action, payload)
            }
        }

        // 创建负责分发Effect Action的Dispatch
        mEffectDispatch = Dispatch { action, payload ->
            mLogger.d(
                ILogger.ACTION_TAG, ("effect action is <"
                        + action.getName()) + ">, in <" + mLogic!!.javaClass.simpleName.toString() + ">"
            )
            mLogic!!.mEffect.doAction(action, this, payload)
            // 如果是非严格模式，这个Effect的action还会发送到其他组件中

            // 如果不是私有action，则拦截此action，并交给感兴趣的组件处理
            // 比如: 考虑这个场景：礼物模块发送了一个礼物，其他模块要同时进行一些响应。
            if (!action.isPrivate()) {
                mLogger.d(
                    ILogger.ACTION_TAG, "action is <" + action.getName()
                        .toString() + "> is public action, will send to any component in page"
                )
                // Interceptor只能由Page来拦截, 拦截时排除自己
                dispatchToPage(
                    InnerActions.INTERCEPT_ACTION,
                    InterceptorPayload(action, payload, mEffectDispatch)
                )
            }
        }
        val bus = mEnvironment!!.dispatchBus
        // 注册effect dispatch, 用于组件间交互
        mDispatchDispose = bus!!.registerReceiver(mEffectDispatch)
        if (mLogic is LogicPage<*>) {
            // 为了防止组件发广播时，其他组件也可以接收此广播，导致组件间通信通过广播来进行。
            // 规定只有page才能接收广播，因此在此设置整个page的Effect分发状态。
            bus.setPageEffectDispatch(mEffectDispatch)
        }
    }

    val effectDispatch: Dispatch?
        get() = mEffectDispatch

    private fun initStateGetter() {
        // 主要给store来用
        val getter: ComponentStateGetter<S?> = ComponentStateGetter<S> { state }
        val store = mEnvironment!!.store
        if (store is PageStore<*>) {
            mStateGetterDispose = (store as PageStore<out State?>)
                .setStateGetter(getter as ComponentStateGetter<State?>)
        }
    }

    private fun registerUIUpdater() {
        val store = mEnvironment!!.store
        if (mStateChangeListener == null || store !is PageStore<*>) {
            return
        }

        // 接收Vsync信号，优化刷新性能
        mUIUpdaterDispose = (store as PageStore<*>).addUIUpdater(UIUpdater {
            if (mPendingChangedProps != null && !mPendingChangedProps!!.isEmpty()) {
                val props: List<ReactiveProp<Any>> = ArrayList(
                    mPendingChangedProps!!.values
                )
                mPendingChangedProps!!.clear()
                mStateChangeListener.onChange(mState, props)
            }
        })
    }

    /**
     * 当开发者不使用action的时候，本方法用于更新State
     */
    protected fun updateState(reducer: PureReducer<S>) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            innerUpdateState(reducer)
            return
        }
        ReduxManager.instance.submitInMainThread { innerUpdateState(reducer) }
    }

    private fun innerUpdateState(reducer: PureReducer<S>) {
        val state = state
        state?.setStateProxy(StateProxy())
        val newState: S = reducer.update(state) ?: return

        // 获取私有属性变化，并本地更新
        val privateProps = newState.privatePropChanged
        privateProps?.let { onStateChange(it) }
        mEnvironment!!.store!!.onStateChanged(newState)
    }

    /**
     * 当状态发生变化的时候，通过这个接口更新UI
     * @param props 变化的属性列表 [ReactiveProp]
     */
    fun onStateChange(props: List<ReactiveProp<Any>>) {
        if (mLogic == null) {
            return
        }
        for (prop in props) {
            val key = prop.key
            putChangedProp(key, prop)
            mLogger.d(
                ILogger.ACTION_TAG, "current changed prop is <"
                        + key + "> in <" + mLogic!!.javaClass.simpleName + ">"
            )
        }
        markNeedUpdate()

        // 通知属性订阅者，状态发生了变化
        mLogic!!.propWatcher.notifyPropChanged(props, this)
    }

    /**
     * 进行一次全量更新：
     * 主要用组件隐藏之后再显示的操作，或者横竖屏切换时的操作
     */
    fun runFullUpdate() {
        val map = mState!!.dataMap
        for (key in map.keys) {
            val prop = map[key]
            prop?.let { putChangedProp(key, it) }
        }
        markNeedUpdate()
    }

    /**
     * 首次创建UI时，根据是否更新初始值来展示UI
     */
    fun firstUpdate() {
        val map = mState!!.dataMap
        for (key in map.keys) {
            val reactiveProp = map[key]
            if (reactiveProp != null && reactiveProp.isUpdateWithInitValue) {
                putChangedProp(key, reactiveProp)
            }
        }
        markNeedUpdate()
    }

    private fun putChangedProp(key: String?, reactiveProp: ReactiveProp<Any>) {
        if (mPendingChangedProps == null) {
            mPendingChangedProps = HashMap()
        }
        mPendingChangedProps!![key] = reactiveProp
    }

    private fun markNeedUpdate() {
        val need = mPendingChangedProps != null && !mPendingChangedProps!!.isEmpty()
        if (mEnvironment!!.store is PageStore<*> && need) {
            (mEnvironment!!.store as PageStore<*>?)!!.markNeedUpdate()
        }
    }

    fun isSameEffectDispatch(dispatch: Dispatch): Boolean {
        return dispatch === mEffectDispatch
    }

    /**
     * 获取当前组件的State, 这里返回的State不具备修改通知能力
     *
     * @return 当前组件的State
     */
    val state: S?
        get() = State.copyState<S>(mState)

    /**
     * 分发Reducer Action
     *
     * @param action 携带的Action
     * @param payload 携带的参数
     */
    fun dispatchReducer(action: Action?, payload: Any?) {
        if (isDestroy || !isStateReady) {
            return
        }
        mReducerDispatch.dispatch(action, payload)
    }

    /**
     * 分发 Effect Action
     *
     * @param action 携带的Action
     * @param payload 携带的参数
     */
    fun dispatchEffect(action: Action?, payload: Any?) {
        if (isDestroy || !isStateReady) {
            return
        }
        mEffectDispatch.dispatch(action, payload)
    }

    /**
     * 子组件直接给Page发送action, 只能Effect来接收
     *
     * @param action Action
     * @param payload 携带的参数
     */
    fun dispatchToPage(action: Action?, payload: Any?) {
        if (isDestroy || !isStateReady || mEnvironment == null) {
            return
        }
        val bus = mEnvironment!!.dispatchBus
        bus!!.pageDispatch!!.dispatch(action, payload)
    }

    /**
     * 子组件发action给父组件，只能使用Effect来接收
     *
     * @param action Action
     * @param payload 携带的参数
     */
    fun dispatchToParent(action: Action, payload: Any?) {
        if (isDestroy || !isStateReady || mEnvironment == null) {
            return
        }
        if (!action.isPrivate()) {
            mLogger.w(ILogger.ERROR_TAG, "only private action can interact parent & child")
            return
        }

        // 交给父组件的dispatch
        val parentDispatch: Dispatch? = mEnvironment!!.parentDispatch
        if (parentDispatch != null) {
            parentDispatch.dispatch(action, payload)
        }
    }

    /**
     * 父组件发action给子组件，只能使用Effect来接收
     *
     * @param action Action
     * @param payload 携带的参数
     */
    fun dispatchToChildren(action: Action, payload: Any) {
        if (isDestroy || !isStateReady || mEnvironment == null) {
            return
        }
        if (!action.isPrivate()) {
            mLogger.w(ILogger.ERROR_TAG, "only private action can interact parent & child")
            return
        }
        val component: LogicComponent<BaseComponentState?>? =
            mLogic as LogicComponent<BaseComponentState?>?
        dispatchToSubComponent(component, action, payload)
        dispatchToAdapter(component, action, payload)
    }

    private fun dispatchToSubComponent(
        component: LogicComponent<BaseComponentState?>?,
        action: Action, payload: Any
    ) {
        // 发给组件依赖的子组件
        val maps: HashMap<String, Dependant<out BaseComponentState?, State>>? =
            component!!.childrenDependant
        if (maps != null) {
            for (dependant in maps.values) {
                val logic: Logic<out BaseComponentState?> = dependant.logic
                logic.context.mEffectDispatch.dispatch(action, payload)
            }
        }
    }

    private fun dispatchToAdapter(
        component: LogicComponent<BaseComponentState?>?,
        action: Action, payload: Any
    ) {
        val dependant: Dependant<out BaseComponentState?, State>? =
            component!!.adapterDependant
        if (dependant != null) {
            val logic: Logic<out BaseComponentState?> = dependant.logic
            logic.context.mEffectDispatch.dispatch(action, payload)
        }
    }

    /**
     * 发送全局广播，本方法在App级别是全局的, 只有page下的Effect才可以处理
     *
     * @param action 要分发的Action
     * @param payload 携带的参数
     */
    fun broadcast(action: Action?, payload: Any?) {
        if (isDestroy || !isStateReady || mEnvironment == null) {
            return
        }
        mEnvironment!!.dispatchBus!!.broadcast(action, payload)
    }

    /**
     * 发送页面内广播，仅Page可以发送页面内广播
     *
     * @param action Action
     * @param payload 参数
     */
    fun broadcastInPage(action: Action?, payload: Any?) {
        if (mEnvironment == null || !stateReady() || mLogic !is LogicPage<*>) {
            return
        }
        mEnvironment!!.dispatchBus!!.dispatch(action, payload)
    }

    /**
     * 用于组件生命周期响应的方法
     *
     * @param action Action
     */
    fun onLifecycle(action: Action) {
        if (isDestroy || !LifeCycleAction.isLifeCycle(action)) {
            return
        }
        if (!isStateReady) {
            if (mPendingLifeCycleActionList == null) {
                mPendingLifeCycleActionList = ArrayList<Action>()
            }
            mPendingLifeCycleActionList!!.add(action)
            return
        }
        dispatchEffect(action, null)
    }

    /**
     * 如果当前组件存在列表型UI，则可以通过组件实例获取到当前组件列表对应的Adapter
     *
     * @return 组件列表对应的实际的Adapter
     */
    val rootAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>?
        get() {
            if (mLogic == null || mLogic is LogicPage<*>) {
                return null
            }
            val dependant: Dependant<out BaseComponentState?, State>? =
                (mLogic as LogicComponent<out BaseComponentState?>).adapterDependant
            val adapter: RootAdapter<out BaseComponentState?>? = dependant?.adapter
            return if (adapter == null) null else adapter.getRealAdapter()
        }
    val platform: IPlatform
        get() = mPlatform

    /**
     * 当State 检测以及merge ready之后，调用此方法，然后会检查异步操作之后挂起的life cycle action
     */
    fun setStateReady() {
        isStateReady = true
        if (mPendingLifeCycleActionList == null) {
            return
        }
        val size = mPendingLifeCycleActionList!!.size
        if (size > 0) {
            val copy: List<Action> = ArrayList<Any?>(mPendingLifeCycleActionList)
            for (i in 0 until size) {
                onLifecycle(copy[i])
            }
            mPendingLifeCycleActionList!!.clear()
        }
    }

    /**
     * 是否状态ready
     * @return boolean
     */
    fun stateReady(): Boolean {
        return isStateReady
    }

    /**
     * 展示一个对话框组件
     */
    fun showComponentDialog(dialog: ILRDialog?) {
        if (mLogic is LRDialogComponent<*>) {
            (mLogic as LRDialogComponent<out BaseComponentState?>).showDialog(dialog)
        }
    }

    /**
     * 清理接口，做一些清理的事情
     */
    fun destroy() {
        isDestroy = true
        mState!!.clear()
        mDispatchDispose?.let { it() }
        mStoreObserverDispose?.let { it() }
        mUIUpdaterDispose?.let { it() }
        mStateGetterDispose?.let { it() }
        logic = null
        mEnvironment = null
    }

    /**
     * 构造器，通过builder创建
     * @param builder ReduxContextBuilder
     */
    init {
        mLogic = builder.logic
        mEnvironment = mLogic.getEnvironment()
        mPlatform = builder.platform

        // 初始化Dispatch
        initDispatch()

        // 获取组件的初始state
        mState = builder.state

        // 初始化State Getter，用注册到Store中，获取当前组件对应的State
        initStateGetter()

        // 监听Store抛出来的变化
        val propsChanged = IPropsChanged { props: List<ReactiveProp<Any>> -> onStateChange(props) }
        val observer = StoreObserver(propsChanged, mState.javaClass.getName())
        mStoreObserverDispose = mEnvironment.store!!.observe(observer)

        // UI观察者，当state变化时，需要触发ui刷新
        mStateChangeListener = builder.stateChangeListener

        // 注册Vsync同步信号，统一时机刷新UI
        registerUIUpdater()
    }
}