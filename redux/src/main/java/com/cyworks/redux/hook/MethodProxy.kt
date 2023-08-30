package com.cyworks.redux.hook

import com.cyworks.redux.ReduxManager
import java.lang.reflect.Method

class MethodProxy {
    @Synchronized
    @Throws(Throwable::class)
    fun callFunc(receiver: Any?, method: Method, args: Array<Any>): Any? {
        val time = System.currentTimeMillis()
        val invokeResult = method.invoke(receiver, *args)
        val consumer = System.currentTimeMillis() - time
        ReduxManager.instance.logger.d("Redux Context", "method call consumer: $consumer")
        return invokeResult
    }
}