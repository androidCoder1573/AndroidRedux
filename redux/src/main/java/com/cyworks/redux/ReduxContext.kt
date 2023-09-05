package com.cyworks.redux

import android.os.Looper
import androidx.collection.ArrayMap
import com.cyworks.redux.action.Action
import com.cyworks.redux.action.InnerActionTypes
import com.cyworks.redux.component.DialogComponent
import com.cyworks.redux.component.Logic
import com.cyworks.redux.component.LogicPage
import com.cyworks.redux.dialog.ILRDialog
import com.cyworks.redux.hook.ProxyCreator
import com.cyworks.redux.interceptor.InterceptorPayload
import com.cyworks.redux.lifecycle.LifeCycleAction
import com.cyworks.redux.prop.ReactiveProp
import com.cyworks.redux.state.State
import com.cyworks.redux.state.StateProxy
import com.cyworks.redux.store.PageStore
import com.cyworks.redux.store.StoreObserver
import com.cyworks.redux.types.Dispatch
import com.cyworks.redux.types.Dispatcher
import com.cyworks.redux.types.Dispose
import com.cyworks.redux.types.IPropsChanged
import com.cyworks.redux.types.IStateChange
import com.cyworks.redux.types.Reducer
import com.cyworks.redux.types.StateGetter
import com.cyworks.redux.util.Environment
import com.cyworks.redux.util.ILogger
import com.cyworks.redux.util.IPlatform

/**
 * ReduxContext是开发者主要关心的类，主要用来做一些界面交互，发送Action等.
 *
 * 为什么将对store的操作封装成context对象？
 * context里主要做了这几件事情：
 * 1、创建Dispatch
 * 2、初始化store观察者，并注入到Store中
 * 3、初始化属性观察者，方便开发者观察组件内某些属性的变化
 * 4、维护一个本组件State的副本，方便进行界面更新
 *
 * 通过Context封装的方式，让组件对store的操作更加内聚
 */
class ReduxContext<S : State> internal constructor(builder: ReduxContextBuilder<S>) {
    /**
     * 平台操作相关
     */
    val platform: IPlatform

    /**
     * 组件对应的State
     */
    var state: S
        get() {
            if (isModifyState) {
                return field
            }
            // 返回的State不具备修改通知能力
            return State.copyState(field)
        }
        private set

    /**
     * ReduxContext对应的组件实例
     */
    private var logic: Logic<S>? = null

    /**
     * 当自身组件的状态发生变化时，通过此接口分发给UI
     */
    private var componentStateChangeListener: IStateChange<S>? = null

    /**
     * 保存对UI更新listener的反注册器，这里是注册对vsync同步信号的监听
     */
    private var uiUpdaterDispose: Dispose? = null

    /**
     * 保存对store观察的反注册器
     */
    private var storeObserverDispose: Dispose? = null

    /**
     * 保存注入的StateGetter的反注册器
     */
    private var stateGetterDispose: Dispose? = null

    /**
     * 保存对dispatch的反注册器
     */
    private var dispatchDispose: Dispose? = null

    /**
     * 存放当前组件已更新的属性，在下一次vsync信号过来时，用于UI更新
     */
    private var pendingChangedProps: ArrayMap<String, ReactiveProp<Any>>? = null

    /**
     * 保存一些父组件相关的数据
     */
    private var environment: Environment? = null

    private val logger: ILogger = ReduxManager.instance.logger

    private var isDestroy = false

    /**
     * 是否检测状态完成
     */
    private var isStateReady = false

    private var isModifyState = false

    /**
     * 存放因为异步操作而被挂起的action
     */
    private var pendingLifecycleActionList: ArrayList<Action<Any>>? = null
    private var pendingRunnable: ArrayList<Runnable>? = null

    private val changedProps: ArrayList<ReactiveProp<Any>> = ArrayList()
    private val changedKeys: ArrayList<String> = ArrayList()

    private val stateProxy = StateProxy()

    /**
     * 如果开发这不想使用Action驱动，可以通过传统的方式书写逻辑代码，需继承IController
     */
    private var originController: IController? = null
    private var controllerProxy: IController? = null

    /**
     * 可分发effect的dispatch
     */
    internal val effectDispatch: Dispatch = object : Dispatch {
        override fun dispatch(action: Action<out Any>) {
            if (isDestroy) {
                return
            }

            if (logic != null) {
                logger.i("Dispatcher", "<${logic!!.javaClass.name}>"
                        + " send effect action, <${action.type}>")
                val effect = logic?.effect
                effect?.doAction(action, this@ReduxContext)
            }
        }
    }

    val dispatcher: Dispatcher = object : Dispatcher {
        override fun dispatch(action: Action<out Any>) {
            effectDispatch.dispatch(action)
        }

        override fun dispatchToInterceptor(action: Action<out Any>) {
            if (isDestroy || environment == null) {
                return
            }

            val innerAction = Action(InnerActionTypes.INTERCEPT_ACTION_TYPE, InterceptorPayload(action))
            val bus = environment?.pageDispatchBus
            bus?.pageDispatch?.dispatch(innerAction)
            logger.i("Dispatcher","<${logic?.javaClass?.name}>"
                    + " send interceptor action, <${action.type}>")
        }

        override fun dispatchToParent(action: Action<out Any>) {
            if (isDestroy) {
                return
            }

            logger.i("Dispatcher", "<${logic?.javaClass?.name}>"
                    + " send parent effect action, <${action.type}>")
            val parentDispatch = environment?.parentDispatch
            parentDispatch?.dispatch(action)
        }

        override fun dispatchToAdapterItemComponents(action: Action<out Any>) {}

        override fun dispatchToSubComponents(action: Action<out Any>) {
            if (isDestroy) {
                return
            }

            dispatch(action)
            // 发给组件依赖的子组件
            if (logic != null) {
                val maps = logic?.childrenDepMap
                if (maps != null) {
                    for (dependant in maps.values) {
                        val logic = dependant.logic
                        val subCtx = logic.context
                        subCtx.dispatcher.dispatchToSubComponents(action)
                    }
                }
            }
        }
    }

    /**
     * 如果当前组件存在列表型UI，则可以通过组件实例获取到当前组件列表对应的Adapter
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
        environment = logic?.environment
        platform = builder.platform

        // 初始化Dispatch
        initPageDispatch()

        if (originController != null) {
            controllerProxy = ProxyCreator.createProxy(originController!!, this.javaClass.classLoader)
        }

        // 获取组件的初始state
        state = builder.state

        // 往store中注入State Getter，用于外部获取当前组件对应的State
        injectStateGetter()

        // 监听Store抛出来的变化
        val propsChanged = IPropsChanged { props ->
            if (props != null) {
                onStateChange(props)
            }
        }
        val observer = StoreObserver(state.token, propsChanged)
        storeObserverDispose = environment?.store?.observe(observer)

        // 当state变化时，需要触发给具体的组件，由组件进行UI以及数据逻辑
        componentStateChangeListener = builder.stateChangeListener

        // 注册Vsync同步信号，统一时机刷新UI
        injectUIUpdater()
    }

    private fun initPageDispatch() {
        if (logic !is LogicPage<*>) {
            return
        }

        val bus = this.environment?.pageDispatchBus
        // 创建负责分发Effect Action的Dispatch
        val pageEffectDispatch = Dispatch { action ->
            if (action.type == InnerActionTypes.INTERCEPT_ACTION_TYPE) {
                val realAction = (action.payload as InterceptorPayload).realAction
                logger.i("redux context", " <${logic?.javaClass?.name}> "
                        + "send interceptor action, real acton type <${realAction.type}>")
            }
            dispatcher.dispatch(action)
        }

        // 注册effect dispatch, 用于组件间交互
        if (bus != null) {
            this.dispatchDispose = bus.register(pageEffectDispatch)
        }

        // 为了防止组件发广播时，其他组件也可以接收此广播，导致组件间通信通过广播来进行。
        // 规定只有page才能接收广播，因此在此设置整个page的Effect分发状态。
        bus?.setPageEffectDispatch(pageEffectDispatch);
    }

    @Suppress("UNCHECKED_CAST")
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
        uiUpdaterDispose = (store as PageStore<out State>).addUIUpdater {
            if (pendingChangedProps != null && pendingChangedProps!!.isNotEmpty()) {
                changedProps.clear()
                changedProps.addAll(pendingChangedProps!!.values)
                pendingChangedProps!!.clear()
                componentStateChangeListener!!.onChange(state, changedProps)
            }
        }
    }

    fun <R: IController> getController(): R? {
        @Suppress("UNCHECKED_CAST")
        return controllerProxy as R?
    }

    fun updateState(reducer: Reducer<S>) {
        logger.d("redux context", "state: ${state.javaClass.name} will change prop")
        if (Looper.getMainLooper() == Looper.myLooper()) {
            innerUpdateState(reducer)
            return
        }
        ReduxManager.instance.submitInMainThread { innerUpdateState(reducer) }
    }

    private fun innerUpdateState(reducer: Reducer<S>) {
        if (isDestroy || environment == null || !isStateReady) {
            return
        }

        isModifyState = true

        stateProxy.clear()
        state.setStateProxy(stateProxy)

        val newState: S = reducer.update(state)

        // 获取私有属性变化，记录到本地记录，并请求vsync
        val privateProps = newState.privatePropChanged
        privateProps?.let { onStateChange(it) }

        // 公共属性
        environment!!.store!!.onStateChanged(newState)

        state.setStateProxy(null)
        isModifyState = false
    }

    /**
     * 当状态发生变化的时候，通过这个接口更新UI
     * @param props 变化的属性列表 [ReactiveProp]
     */
    internal fun onStateChange(props: List<ReactiveProp<Any>>) {
        changedKeys.clear()

        for (prop in props) {
            val key = prop.key
            if (key != null) {
                changedKeys.add(key)
                putChangedProp(key, prop)
                logger.d(ILogger.ACTION_TAG, "current changed prop is"
                        + " <" + key + "> in <" + logic?.javaClass?.simpleName + ">"
                )
            }
        }

        requestVsync()

        // 通知属性订阅者，状态发生了变化
        logic?.propsWatcher?.update(state, changedKeys, this)
    }

    /**
     * 首次创建UI时，根据是否更新初始值来展示UI
     */
    internal fun runFirstUpdate() {
        if (isDestroy) {
            return
        }

        if (!isStateReady) {
            if (pendingRunnable == null) {
                pendingRunnable = ArrayList()
            }
            pendingRunnable?.add(Runnable { runFirstUpdate() })
            return
        }

        val map = state.dataMap
        for (key in map.keys) {
            val reactiveProp = map[key]
            if (reactiveProp != null && reactiveProp.isUpdateWithInitValue) {
                putChangedProp(key, reactiveProp)
            }
        }
        requestVsync()
    }

    /**
     * 进行一次全量更新：主要用组件隐藏之后再显示的操作，或者横竖屏切换时的操作
     */
    internal fun runFullUpdate() {
        if (isDestroy) {
            return
        }

        if (!isStateReady) {
            if (pendingRunnable == null) {
                pendingRunnable = ArrayList()
            }
            pendingRunnable?.add(Runnable { runFirstUpdate() })
            return
        }

        val map = state.dataMap
        for (key in map.keys) {
            val prop = map[key]
            prop?.let { putChangedProp(key, it) }
        }
        requestVsync()
    }

    private fun putChangedProp(key: String?, reactiveProp: ReactiveProp<Any>) {
        if (pendingChangedProps == null) {
            pendingChangedProps = ArrayMap()
        }

        if (key != null) {
            pendingChangedProps?.set(key, reactiveProp)
        }
    }

    /**
     * 当私有属性变化时/全局store属性变化时/本地局部刷新时，需要主动请求vsync
     */
    private fun requestVsync() {
        val need = pendingChangedProps != null && pendingChangedProps!!.isNotEmpty()
        if (environment?.store is PageStore<*> && need) {
            (environment!!.store as PageStore<*>).requestVsync()
        }
    }

    /**
     * 发送全局广播，本方法在App级别是全局的, 只有page下的Effect才可以处理
     */
    fun broadcast(action: Action<Any>) {
        if (isDestroy || !isStateReady ||
            environment == null || environment!!.pageDispatchBus == null) {
            return
        }
        environment?.pageDispatchBus?.broadcast(action)
    }

    /**
     * 用于组件生命周期响应的方法
     */
    internal fun onLifecycle(action: Action<Any>) {
        if (isDestroy || !LifeCycleAction.isLifeCycle(action)) {
            return
        }

        if (!isStateReady) {
            if (pendingLifecycleActionList == null) {
                pendingLifecycleActionList = ArrayList()
            }
            pendingLifecycleActionList!!.add(action)
            return
        }
        dispatcher.dispatch(action)
    }

    /**
     * 当State 检测以及merge ready之后，调用此方法，然后会检查异步操作之后挂起的life cycle action
     */
    fun setStateReady() {
        isStateReady = true

        if (pendingRunnable != null) {
            pendingRunnable!!.forEach {
                it.run()
            }
            pendingRunnable = null
        }

        if (pendingLifecycleActionList != null) {
            pendingLifecycleActionList!!.forEach {
                onLifecycle(it)
            }
            pendingLifecycleActionList = null
        }
    }

    /**
     * 展示一个对话框组件
     */
    fun showDialog(dialog: ILRDialog?) {
        if (logic is DialogComponent) {
            (logic as DialogComponent<out State>).showDialog(dialog)
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
        logic = null
        environment = null
    }
}