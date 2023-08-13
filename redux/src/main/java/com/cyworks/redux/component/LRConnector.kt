package com.cyworks.redux.component

import com.cyworks.redux.action.Action
import com.cyworks.redux.prop.PropWatcher
import com.cyworks.redux.state.State
import com.cyworks.redux.store.GlobalStoreWatcher
import com.cyworks.redux.types.Interceptor
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
abstract class LRConnector<S : State, PS : State> {
    /**
     * 当前Connector对应的子组件的状态获取接口, 使用接口的形式延迟获取state
     */
    private var stateGetter: StateGetter<S>? = null

    /**
     * 当前Connector对应的子组件的私有属性变化监听器
     */
    private var selfPropsChanged: LocalPropsChanged? = null

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

    /**
     * 依赖Parent组件的属性，外部配置。
     */
    abstract fun parentStateCollector(childState: S, parentState: PS)

    /**
     * 通过此接口依赖全局store, 集合，无法区分泛型
     */
    abstract fun globalStateCollector(watcher: GlobalStoreWatcher<S>?)

    /**
     * 通过此接口配置与外界通信的interceptor
     *
     * @return InterceptorCollect
     */
    open fun interceptorCollector(): HashMap<Action<Any>, Interceptor<S>>? {
        return null
    }

    /**
     * 注入子组件私有属性变化监听器
     *
     * @param cb LocalPropsChanged [LocalPropsChanged]
     */
    fun injectLocalStateChangeCb(cb: LocalPropsChanged) {
        selfPropsChanged = cb
    }

    /**
     * 通过这个接口来订阅自己组件下的属性变化，这里需要调用watchProp注入
     *
     * @param watcher 属性订阅器
     */
    protected fun subscribeProps(watcher: PropWatcher<S>) {
        // sub class impl
    }
}