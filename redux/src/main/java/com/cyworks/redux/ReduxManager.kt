package com.cyworks.redux

import android.util.Log
import com.cyworks.redux.util.ILogger
import com.cyworks.redux.util.MainThreadExecutor
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Desc: 保存一些Redux运行过程中的全局对象
 *
 * 1、提供App级别的Effect Bus，方便页面之间通过此BUS进行交互，这里页面间的交互主要指的是广播，
 * 我们限制了广播接收者只能是page，防止了组件间直接通过广播进行交互。
 *
 * 2、提供日志实例，方便调试查看log。
 */
class ReduxManager private constructor() {
    /**
     * 创建一个App级别的Bus，作为所有页面级别Bus的根
     */
    val appBus = DispatchBus()

    /**
     * 框架内部logger
     */
    private val defaultLogger: ILogger = object : ILogger {
        override fun v(tag: String, msg: String?) {
            Log.v(tag, msg ?: "")
        }

        override fun d(tag: String, msg: String?) {
            Log.d(tag, msg ?: "")
        }

        override fun i(tag: String, msg: String?) {
            Log.i(tag, msg ?: "")
        }

        override fun w(tag: String, msg: String?) {
            Log.w(tag, msg ?: "")
        }

        override fun e(tag: String, msg: String?) {
            Log.e(tag, msg ?: "")
        }

        override fun printStackTrace(tag: String, msg: String?, e: Throwable) {
            Log.e(tag, msg ?: ("" + e.toString()))
        }

        override fun printStackTrace(tag: String, e: Throwable) {
            Log.e(tag, e.toString())
        }
    }

    /**
     * 需要开发者自己注入logger组件，建议开发者在开发过程中使用自己实现的log组件，release时不需要设置
     */
    var logger: ILogger = defaultLogger
        set(l) {
            if (l != null) {
                field = l
            }
        }

    /**
     * 初始化页面/组件的时候，是否启用异步模式
     */
    var asyncMode = false
        private set

    /**
     * 执行State检测的线程池
     */
    private val executor = ThreadPoolExecutor(1, 1, 0,
        TimeUnit.SECONDS, LinkedBlockingQueue(),
        ThreadFactory { Thread("Live_Redux_bg_thread") })

    /**
     * 用于在主线程上执行一些操作
     */
    private var mainThreadExecutor: MainThreadExecutor? = null

    fun setAsyncMode() {
        asyncMode = true
    }

    /**
     * 提交一个任务, 在子线程中运行
     * @param runnable Runnable
     * @return Future
     */
    fun submitInSubThread(runnable: Runnable): Future<*> {
        return executor.submit(runnable)
    }

    /**
     * 提交一个任务, 在主线程中运行
     * @param runnable Runnable
     */
    fun submitInMainThread(runnable: Runnable) {
        if (mainThreadExecutor == null) {
            mainThreadExecutor = MainThreadExecutor()
        }
        mainThreadExecutor!!.execute(runnable)
    }

    companion object {
        val instance = ReduxManager()
    }
}