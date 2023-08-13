package com.cyworks.redux

import com.cyworks.redux.action.Action
import com.cyworks.redux.component.BaseComponent
import com.cyworks.redux.component.LRConnector
import com.cyworks.redux.component.Logic
import com.cyworks.redux.component.LogicComponent
import com.cyworks.redux.state.State
import com.cyworks.redux.state.StateProxy
import com.cyworks.redux.store.GlobalStoreWatcher
import com.cyworks.redux.types.Interceptor
import com.cyworks.redux.types.StateGetter
import com.cyworks.redux.util.Environment

/**
 * Desc: 表示一个具体的依赖，一个组件对应唯一一个依赖。
 * 将子组件以及对应的Connector组合成一个Dependant，用于在父组件中表示依赖的子组件
 *
 * PS: 父组件的State，CS：当前组件的State
 */
class Dependant<CS : State, PS : State>() {
    /**
     * 组件实例
     */
    val logic: Logic<CS>
        get() = field

    /**
     * 组件对应的连接器
     */
    var connector: LRConnector<CS, State>? = null
        get() = field
        private set

    private fun createDefaultConnector(): LRConnector<CS, PS> {
        return object : LRConnector<CS, PS>() {
            override fun parentStateCollector(childState: CS, parentState: PS) {}
            override fun globalStateCollector(watcher: GlobalStoreWatcher<CS>?) {}
            override fun interceptorCollector(): HashMap<Action<Any>, Interceptor<CS>>? {
                return null
            }
        }
    }

    private fun initConnector(connector: LRConnector<CS, PS>?) {
        this.connector = connector as LRConnector<CS, State>?
        if (connector == null) {
            connector = createDefaultConnector() as LRConnector<CS, State>
        }
        // 注入子组件State的获取接口
        val stateGetter: StateGetter<CS> = object : StateGetter<CS> {
            override fun copy(): CS {
                val context = logic.context
                val state = context.state
                state?.setStateProxy(StateProxy())
                return state
            }
        }

        // 注入StateGetter
        connector!!.injectGetter(stateGetter)

        // 注入子组件私有属性变化时的监听器
        connector!!.injectLocalStateChangeCb(error.NonExistentClass { props ->
            val context = logic.context
            context!!.onStateChange(props)
        })
    }

    /**
     * 将每个子组件的Reducer合成一个SubReducer，SubReducer目的是为组件的Reducer增加一些功能
     *
     * @param list 外部传入一个SubReducer列表，用于收集子组件的SubReducer
     */
    fun createSubReducer(list: List<SubReducer?>?) {
        logic.mergeReducer(list, connector)
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
        if (!(logic is BaseComponent<*> || logic is LogicTestComponent)) {
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
            (logic as BaseComponent<BaseComponentState?>).show()
        }
    }

    /**
     * 隐藏组件UI
     */
    fun hide() {
        if (logic is BaseComponent<*>) {
            (logic as BaseComponent<BaseComponentState?>).hide()
        }
    }

    /**
     * 绑定组件UI
     */
    fun attach() {
        if (logic is BaseComponent<*>) {
            (logic as BaseComponent<BaseComponentState?>).attach()
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
            (logic as BaseComponent<BaseComponentState?>).detach()
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

    /**
     * 当前组件是否已经安装到父组件中
     * @return 是否已安装
     */
    val isInstalled: Boolean
        get() = if (logic is BaseComponent<*>) {
            (logic as BaseComponent<BaseComponentState?>).isInstalled()
        } else true

    /**
     * 构造器，创建一个组件依赖
     *
     * @param logic 对应的组件
     * @param connector 组件连接器
     */
    init {
        initConnector(connector)
    }
}