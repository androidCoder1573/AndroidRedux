package com.cyworks.redux.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewStub
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.lifecycle.LifecycleOwner
import com.cyworks.redux.activityresult.ActivityResult
import com.cyworks.redux.activityresult.ReceiveResultFragment
import com.cyworks.redux.lifecycle.LifeCycleProxy
import com.cyworks.redux.permission.PermissionCallback
import com.cyworks.redux.permission.ProxyPermissionActivity
import com.cyworks.redux.permission.RequestPermission

/**
 * Desc: 框架内部实现的IPlatform，借助ReduxContext实现
 * @author randytu
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

    override fun startActivity(intent: Intent) {
        this.activity?.startActivity(intent)
    }

    override fun startActivityForResult(
        intent: Intent?, requestCode: Int,
        callback: ReceiveResultFragment.ActivityResultCallback?
    ) {
        val activity: Activity? = activity
        if (activity != null) {
            ActivityResult.startForResult(activity, intent, requestCode, callback)
        }
    }

    override fun requestPermission(
        requestCode: Int,
        callback: PermissionCallback?,
        permissions: MutableList<String>
    ) {
        val context = context ?: return
        RequestPermission.of()
            .permission(permissions)
            .requestCode(requestCode)
            .callback(callback)
            .request(ProxyPermissionActivity::class.java, context)
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