package com.cyworks.redux.lifecycle

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore

/**
 * Desc: 代理Activity/Fragment的 Lifecycle
 */
interface LifeCycleProxy {
    /**
     * 获取当前界面的Context
     */
    val context: Context?

    /**
     * 获取组件依赖的Activity
     */
    val activity: Activity?

    /**
     * 获取Lifecycle
     */
    val lifecycle: Lifecycle?

    /**
     * 获取LifecycleOwner
     */
    val lifecycleOwner: LifecycleOwner?

    /**
     * 获取 ViewModelStore
     */
    val viewModelStore: ViewModelStore?

    /**
     * 关闭Page的宿主，外部可以直接实现，如果容器是activity也可以不实现
     */
    fun close() {}
}