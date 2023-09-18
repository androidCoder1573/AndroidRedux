package com.cyworks.demo.root

import android.view.View
import android.widget.Button
import android.widget.TextView
import com.cyworks.R
import com.cyworks.demo.LaunchType
import com.cyworks.redux.ReduxContext
import com.cyworks.redux.atom.UIPropsWatcher
import com.cyworks.redux.types.DepProps
import com.cyworks.redux.types.OnUIAtomChanged
import com.cyworks.redux.ui.ViewModule
import com.cyworks.redux.util.IPlatform

class RootViewModule(private val type: Int) : ViewModule<RootComponentState> {
    private val onStateChanged: OnUIAtomChanged<RootComponentState> =
        OnUIAtomChanged { state, _, holder ->
            val t1: TextView? = holder?.getView(R.id.root_num)
            t1?.text = "计数: ${state.num}"

            val t2: TextView? = holder?.getView(R.id.root_goods_info)
            t2?.text = state.goodInfo.toString()
        }

    private fun getVerticalView(context: ReduxContext<RootComponentState>): View? {
        val platform: IPlatform = context.platform
        val id: Int = platform.viewContainerIdForV
        return platform.inflateStub(id, R.layout.root_ui)
    }

//    private fun getHorizontalView(context: ReduxContext<RootComponentState>): View? {
//        val platform: IPlatform = context.platform
//        val id: Int = platform.viewContainerIdForH
//        return platform.inflateStub(id, R.layout.h_component_layout)
//    }

    override fun getView(context: ReduxContext<RootComponentState>, parent: View): View? {
//        val orientation: Int = context.state.currentOrientation
//        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
//            return getVerticalView(context)
//        }

//        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            return getHorizontalView(context)
//        }

        val view = getVerticalView(context)
        val btn = view?.findViewById<Button>(R.id.root_launcher_dialog)
        if (btn != null && type == LaunchType.LAUNCH_DIALOG.ordinal) {
            btn.visibility = View.VISIBLE
            btn.setOnClickListener {
                (context.getController() as IRootComponentController?)?.openDialog(context)
            }
        } else if (btn != null && type == LaunchType.CHANGE_ORIENTATION.ordinal) {
            btn.text = "切换屏幕方向"
            btn.visibility = View.VISIBLE
            btn.setOnClickListener {
                (context.getController() as IRootComponentController?)?.changeOrientation(context)
            }
        }

        return view
    }

    override fun subscribeProps(state: RootComponentState, watcher: UIPropsWatcher<RootComponentState>?) {
        watcher?.watch(object : DepProps {
            override fun invoke(): Array<Any> {
                val array: Array<Any> = Array(2) {}
                array[0] = state.num
                array[1] = state.goodInfo!!
                return array
            }
        }, onStateChanged)
    }
}