package com.cyworks.permissions

/**
 * 权限请求结果listener，用于直接对接Activity
 */
interface PermissionListener {
    /**
     * 权限申请结果操作
     * @param permissions 要申请的权限列表
     * @param grantResults 授予的权限列表
     */
    fun onRequestPermissionsResult(permissions: MutableList<String>, grantResults: IntArray?)
}