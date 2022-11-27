package com.cyworks.redux.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewStub
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.lifecycle.LifecycleOwner
import com.cyworks.redux.lifecycle.LifeCycleProxy

/**
 * Desc: 框架内部实现的IPlatform，借助ReduxContext实现
 */
class Platform(proxy: LifeCycleProxy, root: View) : IPlatform {
    /**
     * 生命周期代理
     */
    private val mProxy: LifeCycleProxy

    private val mRoot: View?

    override var viewContainerIdForV = 0
        private set

    override var viewContainerIdForH = 0
        private set

    fun setStubId(vStubId: Int, hStubId: Int) {
        viewContainerIdForV = vStubId
        viewContainerIdForH = hStubId
    }

    override val context: Context?
        get() = mProxy.context

    override val activity: Activity?
        get() = mProxy.activity

    override val lifecycleOwner: LifecycleOwner?
        get() = mProxy.lifecycleOwner

    override fun closePage() {
        mProxy.close()
    }

    @SuppressLint("ResourceType")
    override fun inflateStub(@IdRes stubId: Int, @LayoutRes layoutId: Int): View? {
        return try {
            if (mRoot == null || stubId <= 0) {
                return null
            }
            val viewStubView: ViewStub = mRoot.findViewById(stubId)
            viewStubView.layoutResource = layoutId
            viewStubView.inflate()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 创建一个平台操作实例。
     *
     * @param proxy 页面的生命周期代理
     */
    init {
        mProxy = proxy
        mRoot = root
    }
}