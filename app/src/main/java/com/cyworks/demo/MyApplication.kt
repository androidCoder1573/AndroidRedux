package com.cyworks.demo

import android.app.Application
import android.content.Context
import android.util.Log
import com.cyworks.redux.ReduxManager
import com.cyworks.redux.util.ILogger

class MyApplication : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        initEnv()
    }

    private fun initEnv() {
        ReduxManager.instance.enableDebugLog(true)
        ReduxManager.instance.logger = (object : ILogger {
            override fun v(tag: String, msg: String?) {
                if (msg != null) {
                    Log.v(tag, msg)
                }
            }

            override fun d(tag: String, msg: String?) {
                if (msg != null) {
                    Log.d(tag, msg)
                }
            }

            override fun i(tag: String, msg: String?) {
                if (msg != null) {
                    Log.i(tag, msg)
                }
            }

            override fun w(tag: String, msg: String?) {
                Log.w(tag, msg!!)
            }

            override fun e(tag: String, msg: String?) {
                Log.e(tag, msg!!)
            }

            override fun printStackTrace(tag: String, msg: String?, e: Throwable) {
                Log.wtf("exception: $tag", msg, e)
            }

            override fun printStackTrace(tag: String, e: Throwable) {
                Log.wtf("exception: $tag", null, e)
            }
        })
    }
}