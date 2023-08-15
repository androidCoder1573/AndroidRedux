package com.cyworks.redux.interceptor

import com.cyworks.redux.action.Action
import com.cyworks.redux.types.Dispatch

/**
 * 定义拦截数据结构，包括了真实的action以及要携来的数据
 */
data class InterceptorPayload(val realAction: Action<Any>, val exclude: Dispatch? = null)