package com.cyworks.demo

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import com.cyworks.R
import com.cyworks.demo.demopage.DemoPage
import com.cyworks.redux.lifecycle.LifeCycleProxy

enum class LaunchType {
    DEP_PARENT,
    DEP_GLOBAL,
    LAUNCH_DIALOG,
    DELAY_UI,
    CHANGE_ORIENTATION
}

class TestActivity : AppCompatActivity() {
    var page: DemoPage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val props = intent.getBundleExtra("Mode")

        page = DemoPage(R.layout.page_ui, props, object : LifeCycleProxy {
            override val context: Context?
                get() = this@TestActivity.applicationContext
            override val activity: Activity
                get() = this@TestActivity
            override val lifecycle: Lifecycle
                get() = this@TestActivity.lifecycle
            override val lifecycleOwner: LifecycleOwner
                get() = this@TestActivity
            override val viewModelStore: ViewModelStore
                get() = this@TestActivity.viewModelStore
        })

        setContentView(page!!.pageRootView)
    }
}