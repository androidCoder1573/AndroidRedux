package com.cyworks.redux.component

import com.cyworks.redux.interceptor.InterceptorCollector
import com.cyworks.redux.state.State
import com.cyworks.redux.store.GlobalStoreSubscribe
import com.cyworks.redux.types.ComponentContextWrapper
import com.cyworks.redux.types.StateGetter

/**
 * 用于连接子组件和父组件，做几件事情：
 * 1、负责依赖PageState中的数据
 * 2、负责将子组件的Reducer生成SubReducer
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
    internal var pState: State? = null

    /**
     * 通过此接口获取View的占坑id或者对话框这类的布局id
     */
    abstract val viewContainerIdForV: Int

    /**
     * 通过此接口获取View的占坑id或者对话框这类的布局id
     */
    abstract val viewContainerIdForH: Int

    private var collector: InterceptorCollector<CS>? = null

    internal fun setParentState(parentState: State) {
        this.pState = parentState
    }

    internal fun getParentState(): State? {
        return this.pState
    }

    fun injectChildContextWrapper(wrapper: ComponentContextWrapper<CS>) {
        collector = InterceptorCollector(wrapper)
        interceptorCollect(collector!!)
    }

    fun getInterceptorCollector(): InterceptorCollector<CS>? {
        return collector
    }

    /**
     * 依赖Parent组件的属性，外部配置
     */
    abstract fun dependParentState(childState: CS, parentState: PS)

    /**
     * 通过此接口依赖全局store, 集合，无法区分泛型
     */
    abstract fun dependGlobalState(watcher: GlobalStoreSubscribe<CS>)

    /**
     * 通过此接口配置与外界通信的interceptor
     */
    abstract fun interceptorCollect(collect: InterceptorCollector<CS>)
}