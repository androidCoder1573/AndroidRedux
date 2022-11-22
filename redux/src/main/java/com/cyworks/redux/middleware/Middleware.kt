package com.cyworks.redux.middleware

import com.cyworks.redux.State
import com.cyworks.redux.types.Dispatch

/**
 * Desc: MiddleWare, 用于对Reducer运行过程红进行一些拦截
 *
 * @author randytu on 2020/8/23
 */
interface Middleware {
    /**
     * 中间件对应的混入方法
     * @return Compose
     */
    fun middleware(getter: PageStateProvider): Compose<Dispatch>

    /**
     * 获取当前页面页面所有的State，并以Map的方式返回
     */
    interface PageStateProvider {
        /**
         * 获取当前页面的所有State
         * @return State HashMap
         */
        fun provider(): HashMap<String, State>?
    }
}