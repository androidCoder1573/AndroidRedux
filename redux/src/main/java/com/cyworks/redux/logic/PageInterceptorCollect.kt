package com.cyworks.redux.logic

import androidx.collection.ArrayMap
import com.cyworks.redux.ReduxContext
import com.cyworks.redux.action.Action
import com.cyworks.redux.action.ActionType
import com.cyworks.redux.state.State
import com.cyworks.redux.types.Interceptor

class PageInterceptorCollect<S : State> {
    private val functions: ArrayMap<ActionType, Interceptor<S>> = ArrayMap()

    @Suppress("UNCHECKED_CAST")
    private val pageInterceptor: Interceptor<S> = Interceptor { action, ctx ->
        this.doAction(action as Action<Any>, ctx)
    }

    /**
     * 给当前组件注册一个Action对应的Effect，保持可替换
     * @param action AbsAction
     * @param interceptor
     */
    fun add(action: ActionType, interceptor: Interceptor<S>) {
        this.functions[action] = interceptor
    }

    fun remove(action: ActionType) {
        this.functions.remove(action)
    }

    /**
     * 获取合并之后的Effect，这里使用InnerEffect进行包装
     * @return PageEffect
     */
    fun getPageInterceptor(): Interceptor<S> {
        return this.pageInterceptor
    }

    private fun doAction(action: Action<Any>, ctx: ReduxContext<S>?) {
        if (functions.isNotEmpty()) {
            val effect = this.functions[action.type]
            effect?.doAction(action, ctx)
        }
    }
}