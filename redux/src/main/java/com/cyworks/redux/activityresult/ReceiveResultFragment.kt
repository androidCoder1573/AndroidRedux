package com.cyworks.redux.activityresult

import android.app.Fragment
import android.util.SparseArray
import android.os.Bundle
import android.content.Intent

/**
 * Desc: 使用此Fragment来启动Activity，专用于优雅的获取onActivityResult
 * @author randytu
 */
class ReceiveResultFragment : Fragment() {
    /**
     * 保存回调对象
     */
    private val callbacks = SparseArray<ActivityResultCallback>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    /**
     * 启动某个Activity，并接受回调参数
     * @param intent 要启动Activity
     * @param requestCode 启动Activity对应的识别code
     * @param callback 回调对象
     */
    fun startForResult(intent: Intent?, requestCode: Int, callback: ActivityResultCallback) {
        callbacks.put(requestCode, callback)
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val callback = callbacks[requestCode]
        callbacks.remove(requestCode)
        callback?.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * 通过此接口接受Activity的Result结果
     */
    interface ActivityResultCallback {
        /**
         * 对接Fragment的onActivityResult
         * @param requestCode 请求码
         * @param resultCode 返回码
         * @param data 返回的数据
         */
        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    }
}