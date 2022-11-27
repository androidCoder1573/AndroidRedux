package com.cyworks.redux.util

import android.os.Looper
import java.lang.RuntimeException

/**
 * Desc: thread 辅助类
 */
object ThreadUtil {
    /**
     * 检查操作是否在主线程执行
     * @param errMsg 展示错误信息
     */
    fun checkMainThread(errMsg: String) {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw RuntimeException(errMsg)
        }
    }
}