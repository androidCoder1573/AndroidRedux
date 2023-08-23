package com.cyworks.demo

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import com.cyworks.R
import com.cyworks.demo.demopage.DemoPage
import com.cyworks.redux.lifecycle.LifeCycleProxy

/**
 * 页面由Fragment进行承载
 */
class TestFragment : Fragment() {
    var page: DemoPage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        page = DemoPage(R.layout.page_ui, arguments, object : LifeCycleProxy {
            override val context: Context?
                get() = this@TestFragment.context
            override val activity: Activity?
                get() = this@TestFragment.activity
            override val lifecycle: Lifecycle
                get() = this@TestFragment.requireActivity().lifecycle
            override val lifecycleOwner: LifecycleOwner?
                get() = this@TestFragment.activity
            override val viewModelStore: ViewModelStore
                get() = this@TestFragment.requireActivity().viewModelStore
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return page!!.pageRootView
    }

    override fun onResume() {
        super.onResume()
        val configuration = this.requireActivity().resources.configuration
        changeView(configuration)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        changeView(newConfig)
        page!!.requestOrientationChange(newConfig)
    }

    private fun changeView(newConfig: Configuration) {
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            this.requireActivity().findViewById<View>(R.id.demo_horizontal).visibility =
                View.GONE
            this.requireActivity().findViewById<View>(R.id.demo_vertical).visibility =
                View.VISIBLE
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            this.requireActivity().findViewById<View>(R.id.demo_vertical).visibility =
                View.GONE
            this.requireActivity().findViewById<View>(R.id.demo_horizontal).visibility =
                View.VISIBLE
        }
    }
}