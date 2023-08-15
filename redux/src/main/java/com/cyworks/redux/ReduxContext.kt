package com.cyworks.redux

import android.os.Looper
import com.cyworks.redux.action.Action
import com.cyworks.redux.action.InnerActionTypes
import com.cyworks.redux.component.LiveDialogComponent
import com.cyworks.redux.component.Logic
import com.cyworks.redux.component.LogicComponent
import com.cyworks.redux.component.LogicPage
import com.cyworks.redux.dependant.Dependant
import com.cyworks.redux.dialog.ILRDialog
import com.cyworks.redux.interceptor.InterceptorPayload
import com.cyworks.redux.lifecycle.LifeCycleAction
import com.cyworks.redux.prop.ReactiveProp
import com.cyworks.redux.state.State
import com.cyworks.redux.state.StateProxy
import com.cyworks.redux.store.PageStore
import com.cyworks.redux.store.StoreObserver
import com.cyworks.redux.types.*
import com.cyworks.redux.util.Environment
import com.cyworks.redux.util.ILogger
import com.cyworks.redux.util.IPlatform

/**
 * ReduxContext是开发者主要关心的类，主要用来做一些界面交互，发送Action等.
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
    private var logic: Logic<S>

    /**
     * 组件对应的State
     */
    var state: S
        get() = State.copyState(field) // 返回的State不具备修改通知能力
        private set

    /**
     * 当状态发生变化时，通过此接口分发给UI
     */
    private var componentStateChangeListener: IStateChange<S>? = null

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
    private var storeObserverDispose: Dispose? = null

    /**
     * 存放当前组件已更新的属性，在下一次vsync信号过来时，用于UI更新
     */
    private var pendingChangedProps: HashMap<String, ReactiveProp<Any>>? = null

    /**
     * 可分发effect的dispatch
     */
    var effectDispatch: Dispatch? = null
        private set

    /**
     * 保存一些父组件相关的数据
     */
    private var environment: Environment? = null

    /**
     * 平台操作相关
     */
    val platform: IPlatform

    private val logger: ILogger = ReduxManager.instance.logger

    /**
     * 是否销毁了
     */
    private var isDestroy = false

    /**
     * 是否检测状态完成
     */
    @Volatile private var isStateReady = false

    /**
     * 存放因为异步操作而被挂起的action
     */
    private var pendingLifeCycleActionList: ArrayList<Action<Any>>? = null

    /**
     * 如果开发这不想使用Action驱动，可以通过传统的方式书写逻辑代码，需继承BaseController
     */
    var controller: BaseController<S>? = null
        internal set(controller) {
            field = controller
            if (field == null) {
                field = BaseController()
            }
            field!!.setReduxContext(this)
            initDispatch()
        }

    /**
     * 如果当前组件存在列表型UI，则可以通过组件实例获取到当前组件列表对应的Adapter
     *
     * @return 组件列表对应的实际的Adapter
     */
//    val rootAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>?
//        get() {
//            if (logic == null || logic is LogicPage<*>) {
//                return null
//            }
//            val dependant: Dependant<out BaseComponentState?, State>? =
//                (logic as LogicComponent<out BaseComponentState?>).adapterDependant
//            val adapter: RootAdapter<out BaseComponentState?>? = dependant?.adapter
//            return if (adapter == null) null else adapter.getRealAdapter()
//        }

    init {
        logic = builder.logic
        environment = logic.environment
        platform = builder.platform

        // 初始化Dispatch
        initDispatch()

        // 获取组件的初始state
        state = builder.state

        // 初始化State Getter，用注册到Store中，获取当前组件对应的State
        injectStateGetter()

        // 监听Store抛出来的变化
        val propsChanged = IPropsChanged { props ->
            if (props != null) {
                onStateChange(props)
            }
        }
        val observer = StoreObserver(state.hashCode().toString(), propsChanged)
        storeObserverDispose = environment?.store?.observe(observer)

        // 当state变化时，需要触发给具体的组件，由组件进行UI以及数据逻辑
        componentStateChangeListener = builder.stateChangeListener

        // 注册Vsync同步信号，统一时机刷新UI
        injectUIUpdater()
    }

    private fun initDispatch() {
        // 创建负责分发Effect Action的Dispatch
        effectDispatch = Dispatch { action ->
            logger.d(ILogger.ACTION_TAG, "effect action is <" + action.type.name + ">,"
                        + " in <" + logger.javaClass.simpleName.toString() + ">")

            logic.effect?.doAction(action, this)
            // Interceptor只能由Page来拦截, 拦截时排除自己
            dispatchToPage(Action(InnerActionTypes.INTERCEPT_ACTION_TYPE,
                InterceptorPayload(action, effectDispatch)))
        }

        val bus = environment!!.dispatchBus
        // 注册effect dispatch, 用于组件间交互
        dispatchDispose = bus!!.register(effectDispatch)
        if (logic is LogicPage<*>) {
            // 为了防止组件发广播时，其他组件也可以接收此广播，导致组件间通信通过广播来进行。
            // 规定只有page才能接收广播，因此在此设置整个page的Effect分发状态。
            bus.setPageEffectDispatch(effectDispatch)
        }
    }

    private fun injectStateGetter() {
        // 给页面store用，用于获取当前组件的state
        val getter: StateGetter<S> = StateGetter { state }
        val store = environment!!.store
        if (store is PageStore<*>) {
            stateGetterDispose = (store as PageStore<out State>)
                .addStateGetter(getter as StateGetter<State>)
        }
    }

    private fun injectUIUpdater() {
        val store = environment!!.store
        if (componentStateChangeListener == null || store !is PageStore<*>) {
            return
        }

        // 接收Vsync信号，优化刷新性能
        uiUpdaterDispose = (store as PageStore<State>).addUIUpdater {
            if (pendingChangedProps != null && pendingChangedProps!!.isNotEmpty()) {
                val props: List<ReactiveProp<Any>> = ArrayList(
                    pendingChangedProps!!.values
                )
                pendingChangedProps!!.clear()
                pendingChangedProps.onChange(state, props)
            }
        }
    }

    /**
     * 当开发者不使用action的时候，本方法用于更新State
     */
    fun updateState(reducer: Reducer<S>) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            innerUpdateState(reducer)
            return
        }
        ReduxManager.instance.submitInMainThread { innerUpdateState(reducer) }
    }

    private fun innerUpdateState(reducer: Reducer<S>) {
        state.setStateProxy(StateProxy())
        val newState: S = reducer.update(state) ?: return

        // 获取私有属性变化，并本地更新
        val privateProps = newState.privatePropChanged
        privateProps?.let { onStateChange(it) }
        environment!!.store!!.onStateChanged(newState)
    }

    /**
     * 当状态发生变化的时候，通过这个接口更新UI
     * @param props 变化的属性列表 [ReactiveProp]
     */
    fun onStateChange(props: List<ReactiveProp<Any>>) {
        for (prop in props) {
            val key = prop.getKey()
            putChangedProp(key, prop)
            logger.d(ILogger.ACTION_TAG, "current changed prop is <" + key + ">"
                    + "in <" + logic.javaClass.simpleName + ">"
            )
        }

        markNeedUpdate()

        // 通知属性订阅者，状态发生了变化
        logic.propWatcher.notifyPropChanged(props, this)
    }

    /**
     * 首次创建UI时，根据是否更新初始值来展示UI
     */
    fun runFirstUpdate() {
        val map = state.dataMap
        for (key in map.keys) {
            val reactiveProp = map[key]
            if (reactiveProp != null && reactiveProp.isUpdateWithInitValue) {
                putChangedProp(key, reactiveProp)
            }
        }
        markNeedUpdate()
    }

    /**
     * 进行一次全量更新：主要用组件隐藏之后再显示的操作，或者横竖屏切换时的操作
     */
    fun runFullUpdate() {
        val map = state.dataMap
        for (key in map.keys) {
            val prop = map[key]
            prop?.let { putChangedProp(key, it) }
        }
        markNeedUpdate()
    }

    private fun putChangedProp(key: String?, reactiveProp: ReactiveProp<Any>) {
        if (pendingChangedProps == null) {
            pendingChangedProps = HashMap()
        }

        if (key != null) {
            pendingChangedProps?.set(key, reactiveProp)
        }
    }

    private fun markNeedUpdate() {
        val need = pendingChangedProps != null && !pendingChangedProps!!.isEmpty()
        if (environment!!.store is PageStore<*> && need) {
            (environment!!.store as PageStore<*>?)!!.markNeedUpdate()
        }
    }

    fun isSameEffectDispatch(dispatch: Dispatch): Boolean {
        return dispatch == effectDispatch
    }

    /**
     * 分发 Effect Action
     *
     * @param action 携带的Action
     * @param payload 携带的参数
     */
    fun dispatchEffect(action: Action<Any>?) {
        if (isDestroy || !isStateReady) {
            return
        }

        if (action != null) {
            effectDispatch?.dispatch(action)
        }
    }

    /**
     * 子组件直接给Page发送action, 只能Effect来接收
     *
     * @param action Action
     */
    fun dispatchToPage(action: Action<Any>) {
        if (isDestroy || !isStateReady || environment == null) {
            return
        }
        val bus = environment!!.dispatchBus
        bus!!.pageDispatch!!.dispatch(action)
    }

    /**
     * 子组件发action给父组件，只能使用Effect来接收
     *
     * @param action Action
     */
    fun dispatchToParent(action: Action<Any>) {
        if (isDestroy || !isStateReady || environment == null) {
            return
        }

        // 交给父组件的dispatch
        val parentDispatch: Dispatch? = environment!!.parentDispatch
        parentDispatch?.dispatch(action)
    }

    /**
     * 父组件发action给子组件，只能使用Effect来接收
     *
     * @param action Action
     */
    fun dispatchToChildren(action: Action<Any>) {
        if (isDestroy || !isStateReady || environment == null) {
            return
        }

        val component: LogicComponent<State>? = logic as LogicComponent<State>?
        dispatchToSubComponent(component, action)
        // dispatchToAdapter(component, action, payload)
    }

    private fun dispatchToSubComponent(component: LogicComponent<State>?, action: Action<Any>) {
        // 发给组件依赖的子组件
        val maps: HashMap<String, Dependant<out State, State>>? = component!!.childrenDependant
        if (maps != null) {
            for (dependant in maps.values) {
                val logic: Logic<out State> = dependant.logic
                logic.context.effectDispatch?.dispatch(action)
            }
        }
    }

//    private fun dispatchToAdapter(component: LogicComponent<State>?, action: Action<Any>) {
//        val dependant: Dependant<out State, State>? = component!!.adapterDependant
//        if (dependant != null) {
//            val logic: Logic<out State> = dependant.logic
//            logic.context.effectDispatch?.dispatch(action)
//        }
//    }

    /**
     * 发送全局广播，本方法在App级别是全局的, 只有page下的Effect才可以处理
     *
     * @param action 要分发的Action
     */
    fun broadcast(action: Action<Any>) {
        if (isDestroy || !isStateReady || environment == null) {
            return
        }
        environment!!.dispatchBus!!.broadcast(action)
    }

    /**
     * 发送页面内广播，仅Page可以发送页面内广播
     *
     * @param action Action
     */
    fun broadcastInPage(action: Action<Any>) {
        if (environment == null || !stateReady() || logic !is LogicPage<*>) {
            return
        }
        environment!!.dispatchBus!!.dispatch(action)
    }

    /**
     * 用于组件生命周期响应的方法
     *
     * @param action Action
     */
    fun onLifecycle(action: Action<Any>) {
        if (isDestroy || !LifeCycleAction.isLifeCycle(action)) {
            return
        }

        if (!isStateReady) {
            if (pendingLifeCycleActionList == null) {
                pendingLifeCycleActionList = ArrayList()
            }
            pendingLifeCycleActionList!!.add(action)
            return
        }
        dispatchEffect(action)
    }

    /**
     * 当State 检测以及merge ready之后，调用此方法，然后会检查异步操作之后挂起的life cycle action
     */
    fun setStateReady() {
        isStateReady = true
        if (pendingLifeCycleActionList == null) {
            return
        }
        val size = pendingLifeCycleActionList!!.size
        if (size > 0) {
            val copy: List<Action<Any>> = ArrayList(pendingLifeCycleActionList!!)
            copy.forEach {
                onLifecycle(it)
            }
            pendingLifeCycleActionList!!.clear()
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
        if (logic is LiveDialogComponent<*>) {
            (logic as LiveDialogComponent<out State>).showDialog(dialog)
        }
    }

    /**
     * 清理接口，做一些清理的事情
     */
    fun destroy() {
        isDestroy = true
        state.clear()
        dispatchDispose?.let { it() }
        storeObserverDispose?.let { it() }
        uiUpdaterDispose?.let { it() }
        stateGetterDispose?.let { it() }
        // logic = null
        environment = null
    }
}