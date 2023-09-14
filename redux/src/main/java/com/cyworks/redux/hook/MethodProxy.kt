package com.cyworks.redux.hook

import com.cyworks.redux.ReduxManager
import com.cyworks.redux.util.ILogger
import java.lang.reflect.Method

class MethodProxy {
    @Synchronized
    @Throws(Throwable::class)
    fun callFunc(receiver: Any?, method: Method, args: Array<Any>): Any? {
        val time = System.currentTimeMillis()
        val invokeResult = method.invoke(receiver, *args)
        val consume = System.currentTimeMillis() - time
        if (ReduxManager.instance.enableLog) {
            ReduxManager.instance.logger.d(ILogger.PERF_TAG, "call component controller func consume: ${consume}ms")
        }
        return invokeResult
    }
}