package com.cyworks.demo

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
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
    INSTALL_EXTRA_COMPONENT,
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

    override fun onResume() {
        super.onResume()
        val configuration = this.resources.configuration
        changeView(configuration)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        changeView(newConfig)
        page!!.requestOrientationChange(newConfig)
    }

    private fun changeView(newConfig: Configuration) {
//        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
//            this.findViewById<View>(R.id.demo_horizontal).visibility =
//                View.GONE
//            this.findViewById<View>(R.id.demo_vertical).visibility =
//                View.VISIBLE
//        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            this.findViewById<View>(R.id.demo_vertical).visibility =
//                View.GONE
//            this.findViewById<View>(R.id.demo_horizontal).visibility =
//                View.VISIBLE
//        }
    }
}