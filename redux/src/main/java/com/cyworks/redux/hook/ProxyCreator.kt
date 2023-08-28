/*
**        DroidPlugin Project
**
** Copyright(c) 2015 Andy Zhang <zhangyong232@gmail.com>
**
** This file is part of DroidPlugin.
**
** DroidPlugin is free software: you can redistribute it and/or
** modify it under the terms of the GNU Lesser General Public
** License as published by the Free Software Foundation, either
** version 3 of the License, or (at your option) any later version.
**
** DroidPlugin is distributed in the hope that it will be useful,
** but WITHOUT ANY WARRANTY; without even the implied warranty of
** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
** Lesser General Public License for more details.
**
** You should have received a copy of the GNU Lesser General Public
** License along with DroidPlugin.  If not, see <http://www.gnu.org/licenses/lgpl.txt>
**
**/
package com.cyworks.redux.hook

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.net.SocketException

object ProxyCreator {
    fun newProxyInstance(
        loader: ClassLoader?, interfaces: Array<Class<*>?>?,
        invocationHandler: InvocationHandler?
    ): Any {
        return Proxy.newProxyInstance(loader, interfaces, invocationHandler)
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