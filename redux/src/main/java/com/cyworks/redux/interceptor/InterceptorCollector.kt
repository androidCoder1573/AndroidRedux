package com.cyworks.redux.interceptor

import com.cyworks.redux.ReduxContext
import com.cyworks.redux.action.Action
import com.cyworks.redux.action.ActionType
import com.cyworks.redux.state.State
import com.cyworks.redux.types.ComponentContextWrapper
import com.cyworks.redux.types.Interceptor

/**
 * Interceptor收集器, 这里的状态是当前组件的状态
 */
class InterceptorCollector<S: State>(private val contextWrapper: ComponentContextWrapper<S>) {
    internal val interceptorMap: HashMap<ActionType, InterceptorBean<S>> = HashMap()

    val isEmpty: Boolean
        get() = interceptorMap.isEmpty()

    /**
     * 获取合并之后的Interceptor
     */
    val interceptor = Interceptor<State> { action, ctx -> doAction(action) }

    fun isOK(): Boolean {
        return interceptorMap.size > 0
    }

    fun addInterceptor(action: Action<Any>, interceptor: Interceptor<S>) {
        val bean = InterceptorBean(interceptor, contextWrapper)
        interceptorMap[action.type] = bean
    }

    fun addInterceptor(action: ActionType, interceptor: Interceptor<S>) {
        val bean = InterceptorBean(interceptor, contextWrapper)
        interceptorMap[action] = bean
    }

    @Suppress("UNCHECKED_CAST")
    private fun doAction(action: Action<out Any>) {
        val bean = this.interceptorMap[action.type]
        val interceptor = bean?.interceptor
        val ctxProvider = bean?.ctxProvider
        if (interceptor != null && ctxProvider != null) {
            interceptor.doAction(action, ctxProvider.getCtx() as ReduxContext<S>)
        }
    }
}