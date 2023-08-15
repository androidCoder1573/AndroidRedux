package com.cyworks.redux.middleware

import com.cyworks.redux.types.Dispatch

/**
 * Middleware相关的辅助方法
 */
object MiddlewareUtil {
    /**
     * 给Reducer Dispatch增加中间件操作
     * @param middleware Middleware
     */
    fun applyMiddleware(
        middleware: List<Middleware>?,
        initDispatch: Dispatch,
        provider: Middleware.PageStateProvider
    ): Dispatch {
        if (middleware.isNullOrEmpty()) {
            return initDispatch
        }

        val array: MutableList<Compose<Dispatch>> = ArrayList()

        for (middle in middleware) {
            array.add(middle.middleware(provider))
        }

        var initialValue: Dispatch = initDispatch
        for (com in array) {
            initialValue = com.compose(initialValue)
        }

        return initialValue
    }
}