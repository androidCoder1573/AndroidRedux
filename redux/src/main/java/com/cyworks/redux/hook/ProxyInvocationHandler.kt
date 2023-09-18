package com.cyworks.redux.hook

import com.cyworks.redux.ReduxManager
import com.cyworks.redux.annotations.EffectMethod
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class ProxyInvocationHandler(private var oldObj: Any) : InvocationHandler {
    var hookedMethodHandler = MethodProxy()

    @Throws(Throwable::class)
    override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any {
        try {
            if (method.isAnnotationPresent(EffectMethod::class.java)) {
                hookedMethodHandler.callFunc(oldObj, method, args)
                return Any()
            }

            method.invoke(oldObj, args)
            return Any()
        } catch (e: Throwable) {
            ReduxManager.instance.logger.printStackTrace("ProxyInvocationHandler", "exception ", e)
            return Any()
        }
    }
}