package com.cyworks.redux.store

import android.view.Choreographer

class UIFresher internal constructor(private val drawer: DrawCallback) {
    private var hasRequestNextDraw: Boolean = false

    private var drawing: Boolean = false

    private val frameCallback: Choreographer.FrameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            drawing = true
            drawer.onDraw()
            drawing = false
        }
    }

    fun requestNextDraw() {
        if (hasRequestNextDraw) {
            return
        }
        hasRequestNextDraw = true
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    fun start() {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        requestNextDraw()
    }

    fun stop() {
        hasRequestNextDraw = false
        drawing = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    interface DrawCallback {
        fun onDraw()
    }
}