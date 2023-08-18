package com.cyworks.redux.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewStub
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.lifecycle.LifecycleOwner
import com.cyworks.redux.component.DialogComponent
import com.cyworks.redux.dialog.ILRDialog
import com.cyworks.redux.lifecycle.LifeCycleProxy
import com.cyworks.redux.state.State

/**
 * 框架内部实现的IPlatform，借助ReduxContext实现
 */
class Platform(proxy: LifeCycleProxy, root: View?) : IPlatform {
    /**
     * 生命周期代理
     */
    private val proxy: LifeCycleProxy

    private val root: View?

    override var viewContainerIdForV = -1
        private set

    override var viewContainerIdForH = -1
        private set

    override val context: Context?
        get() = proxy.context

    override val activity: Activity?
        get() = proxy.activity

    override val lifecycleOwner: LifecycleOwner?
        get() = proxy.lifecycleOwner

    init {
        this.proxy = proxy
        this.root = root
    }

    fun setStubId(vStubId: Int, hStubId: Int) {
        viewContainerIdForV = vStubId
        viewContainerIdForH = hStubId
    }

    override fun closePage() {
        proxy.close()
    }

    @SuppressLint("ResourceType")
    override fun inflateStub(@IdRes stubId: Int, @LayoutRes layoutId: Int): View? {
        return try {
            if (root == null || stubId <= 0) {
                return null
            }
            val viewStubView: ViewStub = root.findViewById(stubId)
            viewStubView.layoutResource = layoutId
            viewStubView.inflate()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 展示一个对话框组件
     */
    override fun showComponentDialog(dialog: ILRDialog?) {
        if (dialog is DialogComponent<*>) {
            (dialog as DialogComponent<out State>).showDialog(dialog)
        }
    }
}