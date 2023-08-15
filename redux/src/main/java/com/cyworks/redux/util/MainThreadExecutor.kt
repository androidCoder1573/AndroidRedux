package com.cyworks.redux.util

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

/**
 * 主线程Executor
 */
class MainThreadExecutor : Executor {
    private val handler = Handler(Looper.getMainLooper())

    override fun execute(command: Runnable) {
        handler.post(command)
    }
}