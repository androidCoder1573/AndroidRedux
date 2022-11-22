package com.cyworks.redux.middleware

import com.cyworks.redux.types.Dispatch

/**
 * Desc: Middleware相关的辅助方法
 * @author randytu
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
        if (middleware == null || middleware.isEmpty()) {
            return initDispatch
        }
        val array: MutableList<Compose<Dispatch>> = ArrayList()
        val middlewareSize = middleware.size
        for (i in 0 until middlewareSize) {
            array.add(middleware[i].middleware(provider))
        }
        var initialValue: Dispatch = initDispatch
        for (i in 0 until middlewareSize) {
            initialValue = array[i].compose(initialValue)
        }
        return initialValue
    }
}