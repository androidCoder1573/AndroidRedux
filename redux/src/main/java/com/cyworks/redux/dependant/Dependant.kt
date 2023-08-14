package com.cyworks.redux.dependant

import com.cyworks.redux.ReduxContext
import com.cyworks.redux.component.BaseComponent
import com.cyworks.redux.component.Connector
import com.cyworks.redux.component.Logic
import com.cyworks.redux.component.LogicComponent
import com.cyworks.redux.interceptor.InterceptorBean
import com.cyworks.redux.interceptor.InterceptorCollector
import com.cyworks.redux.interceptor.InterceptorManager
import com.cyworks.redux.state.State
import com.cyworks.redux.state.StateProxy
import com.cyworks.redux.store.GlobalStoreWatcher
import com.cyworks.redux.types.ComponentContextWrapper
import com.cyworks.redux.types.StateGetter
import com.cyworks.redux.util.Environment

/**
 * 表示一个具体的依赖，一个组件对应唯一一个依赖。
 * 将子组件以及对应的Connector组合成一个Dependant，用于在父组件中表示依赖的子组件
 *
 * PS: 父组件的State
 * CS：当前组件的State
 */
class Dependant<CS : State, PS : State> {
    /**
     * 组件实例
     */
    val logic: Logic<CS>

    /**
     * 组件对应的连接器
     */
    var connector: Connector<CS, State>? = null
        private set

    /**
     * 当前组件是否已经安装到父组件中
     * @return 是否已安装
     */
    val isInstalled: Boolean
        get() = if (logic is BaseComponent<*>) {
            (logic as BaseComponent<*>).isInstalled()
        } else true

    constructor(logic: Logic<CS>, connector: Connector<CS, PS>?) {
        this.logic = logic
        this.initConnector(connector)
    }

    private fun initConnector(connector: Connector<CS, PS>?) {
        this.connector = connector as Connector<CS, State>?
        if (connector == null) {
            this.connector = createDefaultConnector() as Connector<CS, State>
        }

        // 注入子组件State的获取接口
        val stateGetter: StateGetter<CS> = object : StateGetter<CS> {
            override fun copy(): CS {
                val context = logic.context
                val state = context?.state
                state?.setStateProxy(StateProxy())
                return state
            }
        }

        // 注入子组件私有属性变化时的监听器
        connector!!.injectChildContextWrapper(object : ComponentContextWrapper<CS> {
            override fun getCtx(): ReduxContext<State> {
                return logic.context as ReduxContext<State>
            }

            override fun addPendingInterceptor(bean: InterceptorBean<CS>) {
                logic.addPendingInterceptor(bean)
            }
        })
    }

    private fun createDefaultConnector(): Connector<CS, PS> {
        return object : Connector<CS, PS>() {
            override fun dependParentState(childState: CS, parentState: PS) {
                // do nothing
            }

            override fun dependGlobalState(watcher: GlobalStoreWatcher<CS>) {
                // do nothing
            }

            override fun interceptorCollector(collect: InterceptorCollector<CS>) {
                // do nothing
            }
        }
    }

    protected fun install(env: Environment) {
        initComponent(env)
        // initAdapter(env)
    }

    /**
     * 对当前组件的子组件进行初始化操作
     * @param env 父组件的一些信息
     */
    fun initComponent(env: Environment) {
        if (logic !is BaseComponent<*>) { //  || logic is LogicTestComponent
            return
        }
        connector!!.parentState = env.parentState
        (logic as LogicComponent<CS>).install(env, connector)
    }

    /**
     * 如果组件中存在列表，调用此方法初始化Adapter
     * @param env 父组件的一些信息
     */
//    protected fun initAdapter(env: Environment) {
//        if (logic !is RootAdapter) {
//            return
//        }
//        connector!!.parentState = env.parentState
//        (mLogic as RootAdapter<CS?>).install(env, mConnector)
//    }

    /**
     * 显示组件UI
     */
    fun show() {
        if (logic is BaseComponent<*>) {
            (logic as BaseComponent<*>).show()
        }
    }

    /**
     * 隐藏组件UI
     */
    fun hide() {
        if (logic is BaseComponent<*>) {
            (logic as BaseComponent<*>).hide()
        }
    }

    /**
     * 绑定组件UI
     */
    fun attach() {
        if (logic is BaseComponent<*>) {
            (logic as BaseComponent<*>).attach()
            return
        }
//        if (logic is RootAdapter) {
//            (logic as RootAdapter<BaseComponentState?>).attach()
//        }
    }

    /**
     * 卸载组件UI
     */
    fun detach() {
        if (logic is BaseComponent<*>) {
            (logic as BaseComponent<*>).detach()
            return
        }
//        if (logic is RootAdapter) {
//            (logic as RootAdapter<BaseComponentState?>).detach()
//        }
    }

    /**
     * 如果界面存在列表，通过这个接口获取RootAdapter
     * @return RootAdapter
     */
//    protected val adapter: RootAdapter<CS?>?
//        protected get() = if (mLogic is RootAdapter) {
//            mLogic as RootAdapter<CS?>
//        } else null

    fun mergeInterceptor(manager: InterceptorManager) {
        logic.mergeInterceptor(manager, this as Dependant<CS, State>);
    }

}