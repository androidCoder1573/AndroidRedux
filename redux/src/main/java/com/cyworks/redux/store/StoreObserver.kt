package com.cyworks.redux.store

import com.cyworks.redux.prop.ReactiveProp
import com.cyworks.redux.types.IPropsChanged

/**
 * Desc: 用于监听Store的变化, 通过ReduxContext注入到Store中,
 * 框架内部负责创建StoreObserver，外部不可见。
 */
class StoreObserver(cb: IPropsChanged, token: String) {
    /**
     * 通知属性变化的callback
     */
    private val mCB: IPropsChanged

    /**
     * 当前组件对应的State的类名
     */
    val token: String

    /**
     * 创建一个StoreObserver
     *
     * @param cb 属性变化监听器
     * @param token 当前StoreObserver对应状态的状态的class name
     */
    init {
        mCB = cb
        this.token = token
    }

    /**
     * 当store内部数据发生变化时，通知关心的组件状态变化了
     *
     * @param props 变化的属性
     */
    fun onPropChanged(props: List<ReactiveProp<Any>>?) {
        if (props.isNullOrEmpty()) {
            return
        }
        mCB.onPropsChanged(props)
    }
}