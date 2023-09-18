package com.cyworks.redux.hook

import com.cyworks.redux.ReduxManager
import com.cyworks.redux.util.ILogger
import java.lang.reflect.Method

class MethodProxy {
    @Synchronized
    @Throws(Throwable::class)
    fun callFunc(obj: Any?, method: Method, args: Array<Any>?): Any? {
        val time = System.currentTimeMillis()
        val invokeResult = method.invoke(obj, *(args ?: arrayOfNulls<Any>(0))) // 适应可变参数

        if (ReduxManager.instance.enableLog) {
            val consume = System.currentTimeMillis() - time
            ReduxManager.instance.logger.d(ILogger.PERF_TAG, "call component controller func consume: ${consume}ms")
        }
        return invokeResult
    }
}