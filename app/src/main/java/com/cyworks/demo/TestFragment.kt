package com.cyworks.demo

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import com.cyworks.R
import com.cyworks.redux.ReduxManager
import com.cyworks.demo.demopage.DemoPage
import com.cyworks.redux.lifecycle.LifeCycleProxy
import com.cyworks.redux.util.ILogger

/**
 * 页面由Fragment进行承载
 */
class TestFragment : Fragment() {
    var mPage: DemoPage? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        initEnv()

        mPage = DemoPage(R.layout.activity_main, object : LifeCycleProxy {
            override val context: Context?
                get() = this@TestFragment.context
            override val activity: Activity?
                get() = this@TestFragment.activity
            override val props: Bundle?
                get() = this@TestFragment.arguments
            override val lifecycle: Lifecycle
                get() = this@TestFragment.requireActivity().lifecycle
            override val lifecycleOwner: LifecycleOwner?
                get() = this@TestFragment.activity
            override val viewModelStore: ViewModelStore
                get() = this@TestFragment.requireActivity().viewModelStore
        })
    }

    private fun initEnv() {
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return mPage!!.pageRootView
    }

    override fun onResume() {
        super.onResume()
        val configuration = this.requireActivity().resources.configuration
        changeView(configuration)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        changeView(newConfig)
        mPage!!.requestOrientationChange(newConfig)
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