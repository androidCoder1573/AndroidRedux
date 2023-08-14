package com.cyworks.redux.interceptor

import com.cyworks.redux.state.State
import com.cyworks.redux.types.ComponentContextWrapper
import com.cyworks.redux.types.Interceptor

/**
 * 拦截器数据结构，用于单个拦截器的描述.
 *
 * interceptor: 对于某个Action来说，要拦截的拦截器实现;
 * ctxProvider: 当前组件对应的Context的getter, 因为需要ReduxContext的时候，可能还没有创建ReduxContext;
 */
data class InterceptorBean<S: State>(
    var interceptor: Interceptor<S>? = null,
    var ctxProvider: ComponentContextWrapper<S>? = null
)