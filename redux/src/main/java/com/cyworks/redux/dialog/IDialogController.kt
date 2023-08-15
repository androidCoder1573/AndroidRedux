package com.cyworks.redux.dialog

import android.view.View

/**
 * 框架提供给外部的对话框接口，内部有唯一实现，主要目的是提供组件的UI以及对话框销毁事件
 */
interface IDialogController {
    /**
     * 获取当前组件的View实例
     * @return [View]
     */
    fun getView(): View?

    /**
     * 对话框销毁的回调，框架内做一些清理操作
     */
    fun onDialogDismiss()
}