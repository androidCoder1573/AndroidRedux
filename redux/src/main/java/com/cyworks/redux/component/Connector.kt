package com.cyworks.redux.component

import com.cyworks.redux.interceptor.InterceptorCollector
import com.cyworks.redux.prop.PropWatcher
import com.cyworks.redux.state.State
import com.cyworks.redux.store.GlobalStoreWatcher
import com.cyworks.redux.types.ComponentContextWrapper
import com.cyworks.redux.types.StateGetter

/**
 * Desc: 用于连接子组件和父组件，做几件事情：
 * 1、负责依赖PageState中的数据
 * 2、负责将子组件的Reducer生成SubReducer
 *
 *
 *
 * S：当前组件的State类型；
 * PS：父组件的State类型；
 */
abstract class Connector<CS : State, PS : State> {
    /**
     * 当前Connector对应的子组件的状态获取接口, 使用接口的形式延迟获取state
     */
    private var stateGetter: StateGetter<CS>? = null

    /**
     * 当前组件关联的父组件的State
     */
    var parentState: State? = null

    /**
     * 通过此接口获取View的占坑id或者对话框这类的布局id
     *
     * @return int
     */
    val viewContainerIdForV: Int
        get() = -1

    /**
     * 通过此接口获取View的占坑id或者对话框这类的布局id
     *
     * @return int
     */
    val viewContainerIdForH: Int
        get() = -1

    private var interceptCollector: InterceptorCollector<CS>? = null

    fun setParentState(parentState: State) {
        this.parentState = parentState
    }

    internal fun getParentState(): State? {
        return this.parentState
    }

    fun injectChildContextWrapper(wrapper: ComponentContextWrapper<CS>) {
        interceptCollector = InterceptorCollector(wrapper)
        if (interceptCollector != null) {
            interceptorCollector(interceptCollector!!)
        }
    }

    fun getInterceptorCollector(): InterceptorCollector<CS>? {
        return interceptCollector
    }

    /**
     * 依赖Parent组件的属性，外部配置
     */
    abstract fun dependParentState(childState: CS, parentState: PS)

    /**
     * 通过此接口依赖全局store, 集合，无法区分泛型
     */
    abstract fun dependGlobalState(watcher: GlobalStoreWatcher<CS>)

    /**
     * 通过此接口配置与外界通信的interceptor
     */
    abstract fun interceptorCollector(collect: InterceptorCollector<CS>)

    /**
     * 通过这个接口来订阅自己组件下的属性变化，这里需要调用watchProp注入
     *
     * @param watcher 属性订阅器
     */
    protected fun subscribeProps(watcher: PropWatcher<CS>) {
        // sub class impl
    }
}