package com.cyworks.redux.component

import android.os.Bundle
import androidx.annotation.CallSuper
import com.cyworks.redux.BaseController
import com.cyworks.redux.ReduxContextBuilder
import com.cyworks.redux.action.InnerActionTypes
import com.cyworks.redux.dependant.Dependant
import com.cyworks.redux.dependant.DependentCollector
import com.cyworks.redux.interceptor.InterceptorCollector
import com.cyworks.redux.interceptor.InterceptorManager
import com.cyworks.redux.logic.EffectCollector
import com.cyworks.redux.state.State
import com.cyworks.redux.state.StateType
import com.cyworks.redux.store.GlobalStoreSubscribe
import com.cyworks.redux.types.IPropsChanged
import com.cyworks.redux.types.IStateChange
import com.cyworks.redux.util.Environment
import com.cyworks.redux.util.IPlatform

/**
 * Live-Redux框架是一个UI/逻辑完全分离的框架，LogicComponent内只针对状态管理/组件逻辑操作，不包含UI。
 */
abstract class LogicComponent<S : State>(bundle: Bundle?) : Logic<S>(bundle) {
    /**
     * 当前组件的与页面的连接器
     */
    protected var connector: Connector<S, State>? = null

    /**
     * 用于观察全局store
     */
    private var globalStoreSubscribe: GlobalStoreSubscribe<S>? = null


    protected var stateChangedListener: IStateChange<S>? = null

    /**
     * 当前组件的与页面的连接器
     */
    // protected var adapter: ReduxAdapter<S>? = null

    // protected var adapterDispose: Dispose? = null

    /**
     * 获取依赖的列表组件的Adapter
     *
     * @return Dependant Adapter依赖
     */
//    val adapterDependant: Dependant<out State, State>?
//        get() = if (dependencies == null) {
//            null
//        } else null // dependencies.getAdapterDependant()

    /**
     * 组件的依赖的子组件的集合
     */
    private var dependencies: DependentCollector<State>? = null

    final override val childrenDepMap: HashMap<String, Dependant<out State, State>>?
        get() = if (dependencies == null) {
            null
        } else dependencies!!.dependantMap

    /**
     * 用于注入拦截器
     */
    private var interceptorManager: InterceptorManager? = null

    private var controller: BaseController<S>? = null

    init {
        if (dependencies == null) {
            dependencies = DependentCollector()
        }
        addDependencies(dependencies)
    }

    /**
     * 增加组件的依赖，子类如果有子组件，需要实现此方法
     * @param collect DependentCollect
     */
    protected fun addDependencies(collect: DependentCollector<State>?) {
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
        val parentState = connector?.pState ?: return

        // 标记开始merge
        state.startMergeState()

        state.setParentState(parentState)

        // 关联框架内部数据
        state.currentOrientation = parentState.currentOrientation

        // 生成依赖的属性
        connector?.dependParentState(state, parentState)

        // 创建全局store监听器
        globalStoreSubscribe = GlobalStoreSubscribe(cb, state)
        // 绑定全局store的state中的属性
        connector?.dependGlobalState(globalStoreSubscribe!!)
        globalStoreSubscribe?.generateDependant()

        // 标记结束merge，后续不可再继续开启
        state.endMergeState()
    }

    @Suppress("UNCHECKED_CAST")
    override fun mergeInterceptor(manager: InterceptorManager, selfDep: Dependant<S, State>) {
        interceptorManager = manager

        val collect = selfDep.connector?.getInterceptorCollector()
        if (collect?.isOK() == true) {
            this.interceptorDispose = manager.addInterceptorEx(collect as InterceptorCollector<State>)
        }

//        if (this.adapter != null) {
//            this.adapterDispose = manager.addAdapter(this.adapter as ReduxAdapter<State>)
//        }

        val map = dependencies?.dependantMap
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
        val state = onCreateState(props)
        state.stateType = StateType.COMPONENT_TYPE

        // 生成内部的Key映射表
        state.detectField()

        // 合并page State以及global State
        mergeState(state) { props ->
            if (props != null) {
                context.onStateChange(props)
            }
        }

        // 负责处理额外的事情
        onStateMerged(state)

        // 创建Context
        context = ReduxContextBuilder<S>()
            .setLogic(this)
            .setState(state)
            .setOnStateChangeListener(makeStateChangeCB())
            .setPlatform(createPlatform()!!)
            .build()
        context.controller = controller
        context.setStateReady()

        logicModule.subscribeProps(context.state, propsWatcher)
    }

    /**
     * 如果是UI组件，可能要设置额外的信息，比如组件是否可见，当前屏幕方向等
     * @param componentState 子组件state
     */
    protected open fun onStateMerged(componentState: S) {}

    /**
     * 对组件来说，不需要关注这些内部action，防止用户错误的注册框架的action
     * @param effectCollector
     */
    override fun checkEffect(effectCollector: EffectCollector<S>?) {
        effectCollector?.remove(InnerActionTypes.INTERCEPT_ACTION_TYPE)
        effectCollector?.remove(InnerActionTypes.INSTALL_EXTRA_FEATURE_ACTION_TYPE)
    }

    /**
     * 创建当前组件对应的State
     */
    abstract fun onCreateState(bundle: Bundle?): S

    /**
     * 用于父组件安装子组件接口，主要是给子组件注入环境和连接器
     *
     * @param env 组件需要的环境
     * @param connector 父组件的连接器
     */
    abstract fun install(env: Environment, connector: Connector<S, State>?)

    @CallSuper
    override fun clear() {
        globalStoreSubscribe!!.clear()
        if (dependencies != null) {
            dependencies?.clear()
            dependencies = null
        }
    }
}