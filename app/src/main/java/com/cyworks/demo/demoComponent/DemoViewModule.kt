package com.cyworks.demo.demoComponent

import android.content.res.Configuration
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.cyworks.R
import com.cyworks.demo.LaunchType
import com.cyworks.demo.userstore.UserStore
import com.cyworks.redux.ReduxContext
import com.cyworks.redux.atom.UIPropsWatcher
import com.cyworks.redux.types.DepProps
import com.cyworks.redux.types.OnUIAtomChanged
import com.cyworks.redux.ui.ViewModule
import com.cyworks.redux.util.IPlatform

class DemoViewModule(private val type: Int) : ViewModule<DemoFeatureState> {
    private val onNumChanged: OnUIAtomChanged<DemoFeatureState> =
        OnUIAtomChanged { state, oldDeps, holder ->
            val textView: TextView? = holder?.getView(R.id.component_text)
            textView?.text = "计数: ${state.num}"
        }

    private val onNameChanged: OnUIAtomChanged<DemoFeatureState> =
        OnUIAtomChanged { state, oldDeps, holder ->
            val textView: TextView? = holder?.getView(R.id.component_text_1)
            textView?.text = "Age: ${state.age}, Name: ${state.name}"
        }

    private fun getVerticalView(context: ReduxContext<DemoFeatureState>): View? {
        val platform: IPlatform = context.platform
        val id: Int = platform.viewContainerIdForV
        val view: View? = platform.inflateStub(id, R.layout.component_layout)
        initView(view, context)
        return view
    }

//    private fun getHorizontalView(context: ReduxContext<DemoFeatureState>): View? {
//        val platform: IPlatform = context.platform
//        val id: Int = platform.viewContainerIdForH
//        val view: View? = platform.inflateStub(id, R.layout.h_component_layout)
//        initView(view, context)
//        return view
//    }

    private fun initView(view: View?, context: ReduxContext<DemoFeatureState>) {
        val btn = view?.findViewById<Button>(R.id.demo_component_btn)

        btn?.setOnClickListener { v: View? ->
            if (type == LaunchType.DEP_PARENT.ordinal) {
                context.updateState { state ->
                    val before = state.num
                    state.num = before + 1
                    state
                }
            } else if (type == LaunchType.DEP_GLOBAL.ordinal) {
                UserStore.instance.modifyUserName("bbb${Math.random()}")
            }
        }

        if (type != LaunchType.DEP_PARENT.ordinal && type != LaunchType.DEP_GLOBAL.ordinal) {
            btn?.visibility = View.GONE
        }
    }

    override fun getView(context: ReduxContext<DemoFeatureState>, parent: View): View? {
//        val orientation: Int = context.state.currentOrientation
//        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
//            return getVerticalView(context)
//        }

//        else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            return getHorizontalView(context)
//        }

        return getVerticalView(context)
    }

    override fun subscribeProps(state: DemoFeatureState, watcher: UIPropsWatcher<DemoFeatureState>?) {
        watcher?.watch(object : DepProps {
            override fun invoke(): Array<Any> {
                val array: Array<Any> = arrayOf(1)
                array[0] = state.num
                return array
            }
        }, onNumChanged)

        watcher?.watch(object : DepProps {
            override fun invoke(): Array<Any> {
                val array: Array<Any> = arrayOf(1)
                array[0] = state.name
                return array
            }
        }, onNameChanged)
    }
}