package com.cyworks.activityresults

import android.app.Activity
import android.content.Intent

/**
 * 当在Effect中通过startActivityForResult打开新页面时，需要获取结果，
 * 这样就会导致在原Activity中的onResultActivity中处理大量不相关的逻辑，
 * 这些逻辑可以通过本类内聚到具体的Effect中，实现Activity业务代码解耦。
 */
object ActivityResult {
    private const val TAG = "Fragment_ActivityResult"

    /**
     * 创建用于接收activity result的Fragment
     * @param activity Activity
     * @return ReceiveResultFragment
     */
    private fun getResultFragment(activity: Activity): ReceiveResultFragment {
        val fragmentManager = activity.getFragmentManager()
        var receiveResultFragment = fragmentManager.findFragmentByTag(TAG)
        if (receiveResultFragment == null) {
            receiveResultFragment = ReceiveResultFragment()
            fragmentManager
                .beginTransaction()
                .add(receiveResultFragment, TAG)
                .commitAllowingStateLoss()
            fragmentManager.executePendingTransactions()
        }
        return receiveResultFragment as ReceiveResultFragment
    }

    /**
     * 启动某个Activity并接受回传参数
     * @param activity 当前Activity
     * @param intent 要启动的Activity对应的Intent
     * @param requestCode 启动时带的识别code
     * @param callback 接收activity result的回调
     */
    fun startForResult(
        activity: Activity, intent: Intent?, requestCode: Int,
        callback: ReceiveResultFragment.ActivityResultCallback?
    ) {
        val receiveResultFragment = getResultFragment(activity)
        if (callback != null) {
            receiveResultFragment.startForResult(intent, requestCode, callback)
        }
    }
}