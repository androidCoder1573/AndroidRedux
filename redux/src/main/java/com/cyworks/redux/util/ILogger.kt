package com.cyworks.redux.util

/**
 * Log接口，开发者可注入自己实现的log组件
 */
interface ILogger {

    fun v(tag: String, msg: String?)

    fun d(tag: String, msg: String?)

    fun i(tag: String, msg: String?)

    fun w(tag: String, msg: String?)

    fun e(tag: String, msg: String?)

    fun printStackTrace(tag: String, msg: String?, e: Throwable)

    fun printStackTrace(tag: String, e: Throwable)

    companion object {
        /**
         * 性能相关log tag
         */
        const val PERF_TAG = "Live_Redux_Perf"

        /**
         * 错误/异常相关log tag
         */
        const val ERROR_TAG = "Live_Redux_Error"

        /**
         * Action相关log tag
         */
        const val ACTION_TAG = "Live_Redux_Action"
    }
}