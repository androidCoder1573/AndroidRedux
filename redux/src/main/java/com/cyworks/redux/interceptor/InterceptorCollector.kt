package com.cyworks.redux.interceptor

import com.cyworks.redux.ReduxContext
import com.cyworks.redux.action.Action
import com.cyworks.redux.state.State
import com.cyworks.redux.types.ComponentContextWrapper
import com.cyworks.redux.types.Interceptor

/**
 * Interceptor收集器
 */
class InterceptorCollector<CS: State>(private val contextWrapper: ComponentContextWrapper<CS>) {
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

    fun isOK(): Boolean {
        return interceptorMap.size > 0
    }

    fun addInterceptor(action: Action<Any>, interceptor: Interceptor<CS>) {
        val bean = InterceptorBean(interceptor, contextWrapper)
        interceptorMap[action] = bean
    }

    private fun doAction(action: Action<Any>) {
        val bean = this.interceptorMap[action]
        val interceptor = bean?.interceptor
        val ctxProvider = bean?.ctxProvider
        if (interceptor != null && ctxProvider != null) {
            interceptor.doAction(action, ctxProvider.getCtx() as ReduxContext<CS>)
        }
    }
}