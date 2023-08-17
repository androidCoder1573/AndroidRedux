package com.cyworks.demo.demodialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import com.cyworks.R
import com.cyworks.demo.demopage.DemoPage
import com.cyworks.redux.lifecycle.LifeCycleProxy

class TestDialogFragment : DialogFragment() {
    private var mPage: DemoPage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mPage = DemoPage(R.layout.activity_main, object : LifeCycleProxy {
            override val props: Bundle?
                get() = this@TestDialogFragment.arguments
            override val context: Context?
                get() = this@TestDialogFragment.context
            override val activity: Activity?
                get() = this@TestDialogFragment.activity
            override val lifecycle: Lifecycle
                get() = this@TestDialogFragment.requireActivity().lifecycle
            override val lifecycleOwner: LifecycleOwner?
                get() = this@TestDialogFragment.activity
            override val viewModelStore: ViewModelStore
                get() = this@TestDialogFragment.requireActivity().viewModelStore
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return mPage!!.pageRootView
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE) // 去掉dialog的title
        return dialog
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        val window = dialog!!.window
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent) // 设置window背景透明
            val lp = window.attributes
            lp.width = WindowManager.LayoutParams.MATCH_PARENT
            lp.height = WindowManager.LayoutParams.MATCH_PARENT
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDismiss(dialog: DialogInterface) {}
}