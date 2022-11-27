package com.cyworks.redux

import com.tencent.redux.action.Action
import java.util.HashMap

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
abstract class LRConnector<S : BaseComponentState?, PS : State?> {
    /**
     * 当前Connector对应的子组件的状态获取接口, 使用接口的形式延迟获取state
     */
    private var mGetter: StateGetter<S>? = null

    /**
     * 当前Connector对应的子组件的私有属性变化监听器
     */
    private var mSelfPropsChanged: LocalPropsChanged? = null

    /**
     * 当前组件关联的父组件的State
     */
    var parentState: State? = null

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
    open fun interceptorCollector(): HashMap<Action?, Interceptor<S>?>? {
        return null
    }

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
     * 生成子组件对应的SubReducer。
     * note： 如果在子组件中修改全局store的状态，将会抛出RuntimeException
     *
     * @param reducer Reducer [Reducer]
     * @return SubReducer [SubReducer]
     */
    fun subReducer(reducer: Reducer): SubReducer {
        return label@ SubReducer { action, payload ->
            val newProps: S = reducer.doAction(mGetter, action, payload) as S ?: return@label null

            // 获取私有属性变化，并本地更新
            val privateProps: List<ReactiveProp<Any>> = newProps.getPrivatePropChanged()
            if (privateProps != null) {
                mSelfPropsChanged.onLocalPropsChanged(privateProps)
            }
            newProps
        }
    }

    /**
     * 注入子组件State获取接口
     *
     * @param getter SelfStateGetter [StateGetter]
     */
    fun injectGetter(getter: StateGetter<S>?) {
        mGetter = getter
    }

    /**
     * 注入子组件私有属性变化监听器
     *
     * @param cb LocalPropsChanged [LocalPropsChanged]
     */
    fun injectLocalStateChangeCb(cb: LocalPropsChanged?) {
        mSelfPropsChanged = cb
    }
}