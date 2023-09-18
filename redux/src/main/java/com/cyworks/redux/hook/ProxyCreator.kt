package com.cyworks.redux.hook

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

object ProxyCreator {
    @Suppress("UNCHECKED_CAST")
    fun createProxy(obj: Any): Any {
        val iPmClass = obj::class.java
        val interfaces: List<Class<*>>? = getAllInterfaces(iPmClass)
        val ifs: Array<Class<*>?> =
            if (!interfaces.isNullOrEmpty()) interfaces.toTypedArray()
            else arrayOfNulls(0)
        return newProxyInstance(ifs as Array<Class<*>>, ProxyInvocationHandler(obj))
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
}