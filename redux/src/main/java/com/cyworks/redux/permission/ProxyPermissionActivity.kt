package com.cyworks.redux.permission

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.Window
import java.util.*

/**
 * 权限请求代理Activity, 目前是通过启动一个无界面的activity来操作的，后续准备使用androidx进行优化
 * @author randytu
 */
class ProxyPermissionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val intent = intent
        val permissions = intent.getStringArrayExtra(KEY_PERMISSION_LIST)
        if (permissions == null) {
            sPermissionListener = null
            finish()
            return
        }
        if (sPermissionListener != null && Build.VERSION.SDK_INT >= 23) {
            requestPermissions(permissions, 1)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (sPermissionListener != null) {
            sPermissionListener!!.onRequestPermissionsResult(Arrays.asList(*permissions), grantResults)
        }
        sPermissionListener = null
        finish()
    }

    override fun onDestroy() {
        sPermissionListener = null
        super.onDestroy()
    }

    companion object {
        /**
         * 权限列表的key
         */
        const val KEY_PERMISSION_LIST = "permission_list"

        /**
         * 用于接收系统权限申请结果的listener
         */
        private var sPermissionListener: PermissionListener? = null

        /**
         * 提前注入listener
         * @param permissionListener PermissionListener
         */
        @JvmStatic
        fun setPermissionListener(permissionListener: PermissionListener?) {
            sPermissionListener = permissionListener
        }
    }
}