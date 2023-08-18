package com.cyworks.redux.store

import android.view.Choreographer

class UIFresher internal constructor(private val drawer: DrawCallback) {
    // 是否已经请求了vsync
    private var hasRequestNextDraw: Boolean = false

    // 是否在执行ui更新中
    private var drawing: Boolean = false

    // 是否需要在ui更新结束后继续请求
    private var needRequestVsyncWhenDrawFinish = false

    private val frameCallback: Choreographer.FrameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            hasRequestNextDraw = false
            drawing = true
            val needRequestNext = drawer.onDraw() || needRequestVsyncWhenDrawFinish
            drawing = false
            if (needRequestNext) {
                requestNextDraw()
            }
            needRequestVsyncWhenDrawFinish= false
        }
    }

    fun requestNextDraw() {
        if (hasRequestNextDraw) {
            return
        }

        if (drawing) {
            needRequestVsyncWhenDrawFinish = true
            return
        }

        hasRequestNextDraw = true
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun start() {
        requestNextDraw()
    }

    fun stop() {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        hasRequestNextDraw = false
        drawing = false
        needRequestVsyncWhenDrawFinish = false
    }

    interface DrawCallback {
        fun onDraw(): Boolean
    }
}