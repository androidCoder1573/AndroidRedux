package com.cyworks.redux.interceptor

import com.cyworks.redux.State
import com.cyworks.redux.types.ContextProvider
import com.cyworks.redux.types.Interceptor

/**
 * Desc: 拦截器数据结构，用于单个拦截器的描述，内部使用。
 *
 * 这个类存在的原因：Feature拦截到广播的action之后，会将对应的action转发到组件内部，
 * 此时只能用组件自己的ReduxContext来转发，因此需要这样包装一层。
 *
 * interceptor: 对于某个Action来说，要拦截的拦截器实现;
 * provider: 当前组件对应的Context的getter
 * 因为需要ReduxContext的时候，可能还没有创建ReduxContext;
 */
data class InterceptorBean<S: State>(
    var interceptor: Interceptor<S>? = null,
    var provider: ContextProvider<S>? = null
)