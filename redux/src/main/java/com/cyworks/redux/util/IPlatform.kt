package com.cyworks.redux.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.lifecycle.LifecycleOwner
import com.cyworks.redux.activityresult.ReceiveResultFragment
import com.cyworks.redux.permission.PermissionCallback

/**
 * Desc: 处理平台相关的操作，比如activity跳转, 请求权限，关闭容器
 */
interface IPlatform {
    /**
     * 获取运行的Context，对应的是Application Context
     */
    val context: Context?

    /**
     * 返回宿主Activity
     */
    val activity: Activity?

    /**
     * 获取竖直状态下的View的占坑或者布局id
     */
    val viewContainerIdForV: Int

    /**
     * 获取水平状态下的View的占坑或者布局id
     * @return id int
     */
    val viewContainerIdForH: Int

    /**
     * 获取LifecycleOwner，一般是当前宿主的Activity
     * @return LifecycleOwner
     */
    val lifecycleOwner: LifecycleOwner?

    /**
     * inflate 当前组件的View，不适用于对话框
     * @param stubId 当前占坑的id
     * @param layoutId 当前布局id
     * @return View
     */
    fun inflateStub(@IdRes stubId: Int, @LayoutRes layoutId: Int): View?

    /**
     * 关闭当前页面对应的容器，比如关闭Activity，关闭对话框等
     */
    fun closePage()
}