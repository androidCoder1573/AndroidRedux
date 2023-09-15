package com.cyworks.redux.hook

import com.cyworks.redux.IController
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.net.SocketException


object ProxyCreator {
    @Suppress("UNCHECKED_CAST")
    fun createProxy(obj: IController): IController {
        val iPmClass = obj::class.java
        val interfaces: List<Class<*>>? = getAllInterfaces(iPmClass)
        val ifs: Array<Class<*>?> =
            if (!interfaces.isNullOrEmpty()) interfaces.toTypedArray()
            else arrayOfNulls(0)
        return newProxyInstance(ifs as Array<Class<*>>, ProxyInvocationHandler(obj)) as IController
    }

    private fun newProxyInstance(
        interfaces: Array<Class<*>>,
        invocationHandler: InvocationHandler): Any {
        val loader = Thread.currentThread().contextClassLoader
        return Proxy.newProxyInstance(loader, interfaces, invocationHandler)
    }

    private fun getAllInterfaces(cls: Class<*>?): List<Class<*>>? {
        if (cls == null) {
            return null
        }
        val interfacesFound = LinkedHashSet<Class<*>>()
        getAllInterfaces(cls, interfacesFound)
        return ArrayList(interfacesFound)
    }

    private fun getAllInterfaces(cls: Class<*>, interfacesFound: HashSet<Class<*>>) {
        var temp: Class<*>? = cls
        while (temp != null) {
            val interfaces = temp.interfaces
            for (i in interfaces) {
                if (interfacesFound.add(i)) {
                    getAllInterfaces(i, interfacesFound)
                }
            }
            temp = temp.superclass
        }
    }

    /**
     * 判断某个异常是否已经在某个方法上声明了。
     */
    fun isMethodDeclaredThrowable(method: Method?, e: Throwable?): Boolean {
        if (e is RuntimeException) {
            return true
        }
        if (method == null || e == null) {
            return false
        }
        val es = method.exceptionTypes
        if (es.isEmpty()) {
            return false
        }

        try {
            val methodName = method.name
            val va = "accept" == methodName || "sendto" == methodName
            if (e is SocketException && va && method.declaringClass.name.indexOf("libcore") >= 0) {
                return true
            }
        } catch (e1: Throwable) {
            //DO NOTHING
        }
        for (aClass in es) {
            if (aClass.isInstance(e) || aClass.isAssignableFrom(e.javaClass)) {
                return true
            }
        }
        return false
    }
}