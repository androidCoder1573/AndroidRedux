package com.cyworks.redux.store

import com.cyworks.redux.prop.ReactiveProp
import com.cyworks.redux.types.IPropsChanged
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

/**
 * 用于监听Store的变化, 通过ReduxContext注入到Store中, 框架内部负责创建StoreObserver，外部不可见。
 */
internal class StoreObserver(token: JvmType.Object, cb: IPropsChanged) {
    /**
     * 通知属性变化的callback
     */
    private val callback: IPropsChanged

    /**
     * 当前组件对应的State的token
     */
    val token: JvmType.Object

    init {
        callback = cb
        this.token = token
    }

    /**
     * 当store内部数据发生变化时，通知关心的组件状态变化了
     * @param props 变化的属性
     */
    fun onPropChanged(props: List<ReactiveProp<Any>>?) {
        if (props.isNullOrEmpty()) {
            return
        }
        callback.onPropsChanged(props)
    }
}