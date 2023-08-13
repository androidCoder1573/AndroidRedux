package com.cyworks.redux.interceptor

import com.cyworks.redux.state.State
import com.cyworks.redux.ReduxContext
import com.cyworks.redux.action.Action
import com.cyworks.redux.types.ContextProvider
import com.cyworks.redux.types.Interceptor

/**
 * Desc: Interceptor收集器
 */
class InterceptorCollect<CS: State>(private val contextProvider: ContextProvider<CS>) {
    /**
     * Interceptor 集合
     */
    val interceptorMap: HashMap<Action<Any>, InterceptorBean<CS>> = HashMap()

    val isEmpty: Boolean
        get() = interceptorMap.isEmpty()

    /**
     * 获取合并之后的Interceptor，这里使用InnerInterceptor进行包装
     * @return InnerInterceptor
     */
    val interceptor = object: Interceptor<State> {
        override fun doAction(action: Action<Any>, ctx: ReduxContext<State>?) {
            doAction(action)
        }
    }

    fun addInterceptor(action: Action<Any>, interceptor: Interceptor<CS>) {
        val bean = InterceptorBean(interceptor, contextProvider)
        interceptorMap[action] = bean
    }

    private fun doAction(action: Action<Any>) {
        val bean = this.interceptorMap[action]
        val interceptor = bean?.interceptor
        val provider = bean?.provider
        if (interceptor != null && provider != null) {
            provider.provider()?.let { interceptor.doAction(action, it) }
        }
    }
}