package com.cyworks.redux.dialog

import android.app.Activity

/**
 * 对话框接口，如果要使用Android Redux的对话框组件，对话框必须要实现这个接口
 */
interface ILRDialog {
    /**
     * 设置一个Dialog内部接口，主要是给Dialog提供View，Dialog销毁时的一些额外操作，
     * IDialog框架内部创建，不需要用户创建
     *
     * @param dialogController IDialogController
     */
    fun setIDialog(dialogController: IDialogController)

    /**
     * 展示对话框，这个对话框不限定类型，不管是V4的对话框还是androidX的对话框，都走这个接口，
     * 具体实现需要用户来做，框架只是转调。
     *
     * @param activity Activity
     */
    fun showDialog(activity: Activity)

    /**
     * 对话框销毁操作，为什么要多这个接口？
     * 主要是因为怕开发者忘记调用dismiss，导致框架缺失了对话框销毁的清理操作
     */
    fun onDialogDismiss()

    /**
     * 关闭对话框
     */
    fun closeDialog()
}