package com.cyworks.permissions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 对外曝光的类，用于请求权限，
 * 比如在Effect中需要请求某个权限，而又没有activity上下文的时候，这种方式是可取的。
 */
class RequestPermission private constructor() : PermissionListener {
    /**
     * 请求权限时带的code
     */
    private var requestCode = 0

    /**
     * 请求的权限列表
     */
    private var permissions: List<String>? = null

    /**
     * 请求权限成功或失败的回调
     */
    private var callback: PermissionCallback? = null

    /**
     * 设置要请求的权限
     * @param permissions 权限列表
     * @return [RequestPermission]
     */
    fun permission(permissions: List<String>?): RequestPermission {
        this.permissions = permissions
        return this
    }

    /**
     * 设置请求码
     * @param requestCode 权限请求code
     * @return [RequestPermission]
     */
    fun requestCode(requestCode: Int): RequestPermission {
        this.requestCode = requestCode
        return this
    }

    /**
     * 设置请求权限是否成功的Callback
     * @param callback [PermissionCallback]
     * @return [RequestPermission]
     */
    fun callback(callback: PermissionCallback?): RequestPermission {
        this.callback = callback
        return this
    }

    /**
     * 这里可能需要区分跑在主进程还是Web进程, 需要从外部设置listener
     */
    fun request(
        activityClass: Class<out ProxyPermissionActivity?>?,
        context: Context
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onGranted()
            return
        }

        val requestedPermissions = getDeniedPermissions(context)
        if (requestedPermissions.isEmpty()) {
            onGranted()
            return
        }

        val intent = Intent(context, activityClass)
        ProxyPermissionActivity.setPermissionListener(this)
        intent.putExtra(ProxyPermissionActivity.KEY_PERMISSION_LIST, requestedPermissions)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    private fun getDeniedPermissions(context: Context): Array<String> {
        val deniedList: MutableList<String> = ArrayList(1)
        for (permission in permissions!!) {
            if (ContextCompat.checkSelfPermission(context, permission)
                !== PackageManager.PERMISSION_GRANTED
            ) {
                deniedList.add(permission)
            }
        }
        return deniedList.toTypedArray()
    }

    /**
     * 实现权限结果回调接口
     * @param permissions 要申请的权限列表
     * @param grantResults 授予的权限列表
     */
    override fun onRequestPermissionsResult(
        permissions: MutableList<String>,
        grantResults: IntArray?
    ) {
        val deniedList: MutableList<String> = ArrayList()
        for (i in permissions.indices) {
            if (grantResults!![i] != PackageManager.PERMISSION_GRANTED) {
                deniedList.add(permissions[i])
            }
        }
        if (deniedList.isEmpty()) {
            onGranted()
        } else {
            onDenied(deniedList)
        }
    }

    private fun onGranted() {
        if (callback != null) {
            callback!!.onPermissionGranted(permissions, requestCode)
        }
    }

    private fun onDenied(permissions: MutableList<String>) {
        if (callback != null) {
            callback!!.onPermissionDenied(permissions, requestCode)
        }
    }

    companion object {
        fun of(): RequestPermission {
            return RequestPermission()
        }
    }
}