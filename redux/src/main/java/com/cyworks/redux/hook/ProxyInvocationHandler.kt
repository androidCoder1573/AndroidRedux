package com.cyworks.redux.hook

import android.text.TextUtils
import com.cyworks.redux.annotations.EffectMethod
import com.cyworks.redux.hook.ProxyCreator.isMethodDeclaredThrowable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class ProxyInvocationHandler(private var oldObj: Any) : InvocationHandler {
    var hookedMethodHandler = MethodProxy()

    @Throws(Throwable::class)
    override fun invoke(proxy: Any, method: Method, args: Array<Any>): Any {
        return try {
            if (method.isAnnotationPresent(EffectMethod::class.java)) {
                hookedMethodHandler.callFunc(oldObj, method, args)!!
            } else method.invoke(oldObj, *args)
        } catch (e: Throwable) {
            if (isMethodDeclaredThrowable(method, e)) {
                throw e
            } else {
                val runtimeException =
                    if (!TextUtils.isEmpty(e.message)) RuntimeException(e.message) else RuntimeException()
                runtimeException.initCause(e)
                throw runtimeException
            }
        }
    }
}