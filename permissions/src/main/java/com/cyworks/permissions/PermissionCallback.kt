package com.cyworks.permissions

/**
 * 自定义的权限请求结果的callback
 */
interface PermissionCallback {
    /**
     * 当权限申请被拒绝是的操作
     * @param grantPermissions 申请的权限
     * @param requestCode 申请权限的code
     */
    fun onPermissionDenied(grantPermissions: List<String>?, requestCode: Int)

    /**
     * 当权限申请成功的操作
     * @param grantPermissions 申请的权限
     * @param requestCode 申请权限的code
     */
    fun onPermissionGranted(grantPermissions: List<String>?, requestCode: Int)
}