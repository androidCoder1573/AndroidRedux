package com.cyworks.redux.util

/**
 * Desc: Log接口，需要开发者注入自己实现的log组件
 */
interface ILogger {
    /**
     * V 级别的log
     * @param tag tag
     * @param msg String
     */
    fun v(tag: String, msg: String?)

    /**
     * debug 级别的log
     * @param tag tag
     * @param msg String
     */
    fun d(tag: String, msg: String?)

    /**
     * 提示 级别的log
     * @param tag tag
     * @param msg String
     */
    fun i(tag: String, msg: String?)

    /**
     * 警告 级别的log
     * @param tag tag
     * @param msg String
     */
    fun w(tag: String, msg: String?)

    /**
     * 错误 级别的log
     * @param tag tag
     * @param msg String
     */
    fun e(tag: String, msg: String?)

    /**
     * 打印Exception
     * @param tag tag
     * @param msg [String]
     * @param e [Throwable]
     */
    fun printStackTrace(tag: String, msg: String?, e: Throwable)

    /**
     * 打印Exception
     * @param tag tag
     * @param e [Throwable]
     */
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