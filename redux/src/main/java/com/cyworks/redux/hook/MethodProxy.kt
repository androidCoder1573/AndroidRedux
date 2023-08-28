package com.cyworks.redux.hook

import java.lang.reflect.Method

class MethodProxy {
    @Synchronized
    @Throws(Throwable::class)
    fun callFunc(receiver: Any?, method: Method, args: Array<Any>): Any? {
        val suc = beforeInvoke(receiver, method, args)
        var invokeResult: Any? = null
        if (!suc) {
            invokeResult = method.invoke(receiver, *args)
        }
        afterInvoke(receiver, method, args, invokeResult)
        return invokeResult
    }

    /**
     * 在某个方法被调用之前执行，如果返回true，则不执行原始的方法，否则执行原始方法
     */
    @Throws(Throwable::class)
    protected fun beforeInvoke(receiver: Any?, method: Method?, args: Array<Any>?): Boolean {
        return false
    }

    @Throws(Throwable::class)
    protected fun afterInvoke(
        receiver: Any?,
        method: Method?,
        args: Array<Any>?,
        invokeResult: Any?
    ) {}
}