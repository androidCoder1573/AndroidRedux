package com.cyworks.redux.component

import android.os.Bundle
import androidx.annotation.CallSuper
import com.cyworks.redux.BaseController
import com.cyworks.redux.ReduxContextBuilder
import com.cyworks.redux.action.InnerActionTypes
import com.cyworks.redux.adapter.ReduxAdapter
import com.cyworks.redux.dependant.Dependant
import com.cyworks.redux.dependant.DependentCollect
import com.cyworks.redux.interceptor.InterceptorCollector
import com.cyworks.redux.interceptor.InterceptorManager
import com.cyworks.redux.logic.EffectCollector
import com.cyworks.redux.state.State
import com.cyworks.redux.store.GlobalStoreWatcher
import com.cyworks.redux.types.Dispose
import com.cyworks.redux.types.IPropsChanged
import com.cyworks.redux.types.IStateChange
import com.cyworks.redux.util.Environment
import com.cyworks.redux.util.IPlatform
import java.util.concurrent.Future

/**
 * Desc: Live-Redux框架是一个UI/逻辑完全分离的框架，LogicComponent内只针对状态管理，没有任何逻辑操作。
 *
 * 目的：实现逻辑层测试，剥离出UI之后更容易构建整个逻辑层测试。
 */
abstract class LogicComponent<S : State>(bundle: Bundle?) : Logic<S>(bundle) {
    /**
     * 当前组件的与页面的连接器
     */
    protected var connector: Connector<S, State>? = null

    /**
     * 用于观察全局store
     */
    private var globalStoreWatcher: GlobalStoreWatcher<S>? = null

    /**
     * 组件的依赖的子组件的集合
     */
    var dependencies: DependentCollect<State>? = null


    protected var stateChangedListener: IStateChange<S>? = null

    /**
     * 用于取消异步任务
     */
    private var future: Future<*>? = null

    /**
     * 当前组件的与页面的连接器
     */
    protected var adapter: ReduxAdapter<S>? = null

    protected var adapterDispose: Dispose? = null

    /**
     * 用于注入拦截器
     */
    private var interceptorManager: InterceptorManager? = null

    protected var controller: BaseController<S>? = null

    /**
     * 获取依赖的子组件集合
     *
     * @return Map 子组件集合
     */
    val childrenDependant: HashMap<String, Dependant<out State, State>>?
        get() = if (dependencies == null) {
            null
        } else dependencies!!.dependantMap

    /**
     * 获取依赖的列表组件的Adapter
     *
     * @return Dependant Adapter依赖
     */
    val adapterDependant: Dependant<out State, State>?
        get() = if (dependencies == null) {
            null
        } else null // dependencies.getAdapterDependant()

    init {
        if (dependencies == null) {
            dependencies = DependentCollect()
        }
        addDependencies(dependencies)
    }

    /**
     * 增加组件的依赖，子类如果有子组件，需要实现此方法
     * @param collect DependentCollect
     */
    protected fun addDependencies(collect: DependentCollect<State>?) {
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
        val parentState = connector?.parentState
        if (parentState == null) {
            return
        }

        // 标记开始merge
        state.startMergeState()

        state.setParentState(parentState)

        // 关联框架内部数据
        state.currentOrientation = parentState.currentOrientation

        // 生成依赖的属性
        connector?.dependParentState(state, parentState)

        // 创建全局store监听器
        globalStoreWatcher = GlobalStoreWatcher(cb, state)
        // 绑定全局store的state中的属性
        connector?.dependGlobalState(globalStoreWatcher!!)
        globalStoreWatcher?.generateDependant()

        // 标记结束merge，后续不可再继续开启
        state.endMergeState()
    }

    fun mergeInterceptor(manager: InterceptorManager, dep: Dependant<S, State>) {
        interceptorManager = manager

        val collect = dep.connector?.getInterceptorCollector()
        if (collect?.isOK() == true) {
            this.interceptorDispose = manager.addInterceptorEx(collect as InterceptorCollector<State>)
        }

        if (this.adapter != null) {
            this.adapterDispose = manager.addAdapter(this.adapter as ReduxAdapter<State>)
        }

        val map = this.dependencies?.dependantMap
        if (map != null) {
            for (d in map.values) {
                d.mergeInterceptor(manager)
            }
        }
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
    protected open fun makeStateChangeCB(): IStateChange<S>? {
        return null
    }

    @CallSuper
    protected fun createContext() {
        // 生成初始State
        val componentState = onCreateState(props)

        // 生成内部的Key映射表
        componentState.detectField()

        // 合并page State以及global State
        mergeState(componentState) { props ->
            if (context != null && props != null) {
                context!!.onStateChange(props)
            }
        }

        // 负责处理额外的事情
        onStateMerged(componentState)

        // 创建Context
        context = ReduxContextBuilder<S>()
            .setLogic(this)
            .setState(componentState)
            .setOnStateChangeListener(makeStateChangeCB())
            .setPlatform(createPlatform())
            .build()
        context!!.controller = controller
        context!!.setStateReady()
    }

    /**
     * 如果是UI组件，可能要设置额外的信息，比如组件是否可见，当前屏幕方向等
     * @param componentState 子组件state
     */
    protected open fun onStateMerged(componentState: S) {}

    /**
     * 每个组件下可能也会挂子组件，通过此方法初始化组件下挂载的子组件
     */
    fun initSubComponent() {
        if (dependencies == null) {
            return
        }

        val map: HashMap<String, Dependant<out State, State>>? = dependencies?.dependantMap
        if (map == null || map.isEmpty()) {
            return
        }

        val env = Environment.copy(environment!!)
        context?.state?.let {
            env.setParentState(it)
        }
        context?.effectDispatch?.let {
            env.setParentDispatch(it)
        }

        for (dependant in map.values) {
            dependant.initComponent(env)
        }
    }

    /**
     * 如果组件有列表型的UI，通过绑定框架提供的Adapter，这样列表型组件也可以纳入状态管理数据流中；
     * 通过此方法初始化Adapter，每个组件只可绑定一个Adapter，以保证组件的粒度可控。
     */
    fun initAdapter() {
//        if (dependencies == null) {
//            return
//        }
//        val dependant: HashMap<String, Dependant<out State, State>>? = dependencies?.dependantMap
//        if (dependant != null) {
//            val env = Environment.copy(environment!!)
//            context?.state?.let {
//                env.setParentState(it)
//            }
//            context?.effectDispatch?.let {
//                env.setParentDispatch(it)
//            }
//            dependant.initAdapter(env)
//        }
    }

    /**
     * 对组件来说，不需要关注这些内部action，防止用户错误的注册框架的action
     * @param effectCollector EffectCollect
     */
    override fun checkEffect(effectCollector: EffectCollector<S>?) {
        effectCollector?.remove(InnerActionTypes.INTERCEPT_ACTION_TYPE)
        effectCollector?.remove(InnerActionTypes.INSTALL_EXTRA_FEATURE_ACTION_TYPE)
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
     * @param env 组件需要的环境
     * @param connector 父组件的连接器
     */
    abstract fun install(env: Environment?, connector: Connector<S, State>?)

    @CallSuper
    override fun clear() {
        if (future != null && !future!!.isDone) {
            future!!.cancel(true)
        }
        globalStoreWatcher!!.clear()
        if (dependencies != null) {
            dependencies?.clear()
            dependencies = null
        }
    }
}