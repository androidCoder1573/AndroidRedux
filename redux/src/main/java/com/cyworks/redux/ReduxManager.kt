package com.cyworks.redux

import com.tencent.redux.util.ILogger
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Desc: 保存一些Redux运行过程中的全局对象
 *
 * 1、提供App级别的Effect Bus，方便页面之间通过此BUS进行交互，这里页面间的交互主要指的是广播，
 * 我们限制了广播接收者只能是page，防止了组件间直接通过广播进行交互。
 *
 * 2、提供日志实例，方便调试查看log。
 *
 * @author randytu on 2020/6/5
 */
class ReduxManager private constructor() {
    /**
     * 获取App级别的Bus
     * @return DispatchBus
     */
    /**
     * 创建一个App级别的Bus，作为所有页面级别Bus的根
     */
    val appBus = DispatchBus()

    /**
     * 需要开发者自己注入logger组件，建议开发者在开发过程中使用自己实现的log组件，release时不需要设置
     */
    private var mLogger: ILogger? = null

    /**
     * 框架内部logger为空实现
     */
    private val mDefaultLogger: ILogger = object : ILogger() {
        fun v(tag: String?, msg: String?) {}
        fun d(tag: String?, msg: String?) {}
        fun i(tag: String?, msg: String?) {}
        fun w(tag: String?, msg: String?) {}
        fun e(tag: String?, msg: String?) {}
        fun printStackTrace(tag: String?, msg: String?, e: Throwable?) {}
        fun printStackTrace(tag: String?, e: Throwable?) {}
    }

    /**
     * 初始化页面/组件的时候，是否启用异步模式
     */
    var asyncMode = false
        private set

    /**
     * 执行State检测的线程池
     */
    private val mExecutor = Executors.newSingleThreadExecutor()

    /**
     * 用于在主线程上执行一些操作
     */
    private var mMainThreadExecutor: MainThreadExecutor? = null
    fun setAsyncMode() {
        asyncMode = true
    }

    var logger: ILogger
        get() = if (mLogger == null) {
            mDefaultLogger
        } else mLogger
        set(logger) {
            if (mLogger == null) {
                mLogger = logger
            }
        }

    /**
     * 提交一个任务, 在子线程中运行
     * @param runnable Runnable
     * @return Future
     */
    fun submitInSubThread(runnable: Runnable?): Future<*> {
        return mExecutor.submit(runnable)
    }

    /**
     * 提交一个任务, 在主线程中运行
     * @param runnable Runnable
     */
    fun submitInMainThread(runnable: Runnable?) {
        if (mMainThreadExecutor == null) {
            mMainThreadExecutor = MainThreadExecutor()
        }
        mMainThreadExecutor.execute(runnable)
    }

    companion object {
        /**
         * 饿汉式
         */
        val instance = ReduxManager()
    }
}