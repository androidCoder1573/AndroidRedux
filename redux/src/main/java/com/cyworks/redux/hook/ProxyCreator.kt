package com.cyworks.redux.hook

import com.cyworks.redux.IController
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.net.SocketException


object ProxyCreator {
    @Suppress("UNCHECKED_CAST")
    fun createProxy(obj: IController, loader: ClassLoader): IController {
        val iPmClass = obj::class.java
        val interfaces: List<Class<*>>? = getAllInterfaces(iPmClass)
        val ifs: Array<Class<*>?> =
            if (!interfaces.isNullOrEmpty()) interfaces.toTypedArray()
            else arrayOfNulls(0)

        return newProxyInstance(loader, ifs as Array<Class<*>>, ProxyInvocationHandler(obj)) as IController
    }

    private fun newProxyInstance(
        loader: ClassLoader,
        interfaces: Array<Class<*>>,
        invocationHandler: InvocationHandler): Any {
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
    @JvmStatic
    fun isMethodDeclaredThrowable(method: Method?, e: Throwable?): Boolean {
        if (e is RuntimeException) {
            return true
        }
        if (method == null || e == null) {
            return false
        }
        val es = method.exceptionTypes
        if (es == null || es.isEmpty()) {
            return false
        }

//bugfix,这个问题我也不知道为什么出现，先这么处理吧。
//        java.lang.RuntimeException: Socket closed
//        at com.morgoo.droidplugin.c.c.i.invoke(Unknown Source)
//        at $Proxy9.accept(Native Method)
//        at java.net.PlainSocketImpl.accept(PlainSocketImpl.java:98)
//        at java.net.ServerSocket.implAccept(ServerSocket.java:202)
//        at java.net.ServerSocket.accept(ServerSocket.java:127)
//        at com.qihoo.appstore.h.b.run(Unknown Source)
//        at java.lang.Thread.run(Thread.java:864)
//        Caused by: java.net.SocketException: Socket closed
//        at libcore.io.Posix.accept(Native Method)
//        at libcore.io.BlockGuardOs.accept(BlockGuardOs.java:55)
//        at java.lang.reflect.Method.invokeNative(Native Method)
//        at java.lang.reflect.Method.invoke(Method.java:511)
//        ... 7 more
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