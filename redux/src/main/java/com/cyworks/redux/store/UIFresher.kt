package com.cyworks.redux.store

import android.view.Choreographer

/**
 * 按需刷新，不会一直请求vsync，防止不必要的性能损耗
 */
class UIFresher internal constructor(private val vsync: VsyncCallback) {
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
            val needRequestNext = vsync.onVsync() || needRequestVsyncWhenDrawFinish
            drawing = false
            needRequestVsyncWhenDrawFinish = false
            if (needRequestNext) {
                requestNextDraw()
            }
        }
    }

    fun requestNextDraw() {
        if (hasRequestNextDraw) {
            // 已经请求了vsync，则不继续请求
            return
        }

        if (drawing) {
            // 如果当前正在绘制中，则设置请求下一次的标记，防止属性变化不会触发UI刷新的问题
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

    interface VsyncCallback {
        fun onVsync(): Boolean
    }
}