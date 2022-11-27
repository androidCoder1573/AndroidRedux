package com.cyworks.redux

import com.tencent.redux.action.Action
import java.util.HashMap

/**
 * Desc: 表示一个具体的依赖，一个组件对应唯一一个依赖。
 * 将子组件以及对应的Connector组合成一个Dependant，用于在父组件中表示依赖的子组件
 *
 * PS: 父组件的State，CS：当前组件的State
 */
class Dependant<CS : BaseComponentState?, PS : State?>(
    /**
     * 组件实例
     */
    @param:NonNull private val mLogic: Logic<CS?>, connector: LRConnector<CS?, PS?>?
) {
    /**
     * 组件对应的连接器
     */
    private var mConnector: LRConnector<CS?, State?>? = null
    private fun createDefaultConnector(): LRConnector<CS?, PS?> {
        return object : LRConnector<CS, PS>() {
            override fun parentStateCollector(childState: CS, parentState: PS) {}
            override fun globalStateCollector(watcher: GlobalStoreWatcher<CS>?) {}
            override fun interceptorCollector(): HashMap<Action, Interceptor<CS>> {
                return null
            }
        }
    }

    private fun initConnector(connector: LRConnector<CS?, PS?>?) {
        mConnector = connector as LRConnector<CS?, State?>?
        if (connector == null) {
            mConnector = createDefaultConnector() as LRConnector<CS?, State?>
        }
        // 注入子组件State的获取接口
        val stateGetter: StateGetter<CS> = label@ StateGetter<CS> {
            val context = mLogic.context ?: return@label null
            val state = context.state
            state?.setStateProxy(StateProxy())
            state
        }

        // 注入StateGetter
        mConnector!!.injectGetter(stateGetter)

        // 注入子组件私有属性变化时的监听器
        mConnector!!.injectLocalStateChangeCb(error.NonExistentClass { props ->
            val context = mLogic.context
            context!!.onStateChange(props)
        })
    }

    /**
     * 将每个子组件的Reducer合成一个SubReducer，SubReducer目的是为组件的Reducer增加一些功能
     *
     * @param list 外部传入一个SubReducer列表，用于收集子组件的SubReducer
     */
    fun createSubReducer(@NonNull list: List<SubReducer?>?) {
        mLogic.mergeReducer(list, mConnector)
    }

    protected fun install(@NonNull env: Environment) {
        initComponent(env)
        initAdapter(env)
    }

    /**
     * 对当前组件的子组件进行初始化操作
     * @param env 父组件的一些信息
     */
    fun initComponent(@NonNull env: Environment) {
        if (!(mLogic is BaseComponent<*> || mLogic is LogicTestComponent)) {
            return
        }
        mConnector!!.parentState = env.parentState
        (mLogic as LogicComponent<CS?>).install(env, mConnector)
    }

    /**
     * 如果组件中存在列表，调用此方法初始化Adapter
     * @param env 父组件的一些信息
     */
    protected fun initAdapter(@NonNull env: Environment) {
        if (mLogic !is RootAdapter) {
            return
        }
        mConnector!!.parentState = env.parentState
        (mLogic as RootAdapter<CS?>).install(env, mConnector)
    }

    /**
     * 显示组件UI
     */
    fun show() {
        if (mLogic is BaseComponent<*>) {
            (mLogic as BaseComponent<BaseComponentState?>).show()
        }
    }

    /**
     * 隐藏组件UI
     */
    fun hide() {
        if (mLogic is BaseComponent<*>) {
            (mLogic as BaseComponent<BaseComponentState?>).hide()
        }
    }

    /**
     * 绑定组件UI
     */
    fun attach() {
        if (mLogic is BaseComponent<*>) {
            (mLogic as BaseComponent<BaseComponentState?>).attach()
            return
        }
        if (mLogic is RootAdapter) {
            (mLogic as RootAdapter<BaseComponentState?>).attach()
        }
    }

    /**
     * 卸载组件UI
     */
    fun detach() {
        if (mLogic is BaseComponent<*>) {
            (mLogic as BaseComponent<BaseComponentState?>).detach()
            return
        }
        if (mLogic is RootAdapter) {
            (mLogic as RootAdapter<BaseComponentState?>).detach()
        }
    }

    /**
     * 如果界面存在列表，通过这个接口获取RootAdapter
     * @return RootAdapter
     */
    protected val adapter: RootAdapter<CS?>?
        protected get() = if (mLogic is RootAdapter) {
            mLogic as RootAdapter<CS?>
        } else null

    /**
     * 获取当前组件对应Connector
     * @return 返回Connector [LRConnector]
     */
    val connector: LRConnector<CS?, PS?>?
        get() = mConnector as LRConnector<CS?, PS?>?
    protected val logic: Logic<out Any?>
        protected get() = mLogic

    /**
     * 当前组件是否已经安装到父组件中
     * @return 是否已安装
     */
    val isInstalled: Boolean
        get() = if (mLogic is BaseComponent<*>) {
            (mLogic as BaseComponent<BaseComponentState?>).isInstalled()
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