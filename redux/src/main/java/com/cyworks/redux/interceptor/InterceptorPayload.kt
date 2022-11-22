package com.cyworks.redux.interceptor

import com.cyworks.redux.action.Action
import com.cyworks.redux.types.Dispatch

/**
 * 定义拦截数据结构，包括了真实的action以及要携来的数据
 */
class InterceptorPayload(action: Action<Any>, exclude: Dispatch? = null) {
    /**
     * 真实的Action
     */
    var realAction: Action<Any>

    /**
     * 拦截器自身可能消费来自组件自身发出的action，所以需要排除
     */
    var mExclude: Dispatch?

    /**
     * 初始化拦截器数据结构
     */
    init {
        realAction = action
        mExclude = exclude
    }
}