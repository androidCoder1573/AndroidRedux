package com.cyworks.demo.demoDialogComponent

import android.app.Activity
import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import com.cyworks.redux.dialog.IDialogController
import com.cyworks.redux.dialog.ILRDialog

class ComponentDialogFragment : DialogFragment(), ILRDialog {
    /**
     * 控制对话框的接口
     */
    private var mDialogInterface: IDialogController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return mDialogInterface!!.getView()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        val window = dialog!!.window
        if (window != null) {
            val lp = window.attributes
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        onDialogDismiss()
    }

    override fun setIDialog(dialogController: IDialogController) {
        mDialogInterface = dialogController
    }

    override fun showDialog(activity: Activity) {
        show(activity.fragmentManager, DialogState.DIALOG_TAG)
    }

    override fun onDialogDismiss() {
        mDialogInterface!!.onDialogDismiss()
    }

    override fun closeDialog() {
        dismissAllowingStateLoss()
    }
}