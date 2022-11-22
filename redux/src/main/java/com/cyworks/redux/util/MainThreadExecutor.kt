package com.cyworks.redux.util

import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import java.util.concurrent.Executor

/**
 * Desc: 主线程Executor
 * @author randytu
 */
class MainThreadExecutor : Executor {
    /**
     * 主线程Handler
     */
    private val handler = Handler(Looper.getMainLooper())

    override fun execute(@NonNull command: Runnable) {
        handler.post(command)
    }
}