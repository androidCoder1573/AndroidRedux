package com.cyworks.demo.demofeature

import android.content.res.Configuration
import android.view.View
import android.widget.TextView
import com.cyworks.R
import com.cyworks.demo.publicactions.DemoPageActions
import com.cyworks.demo.userstore.UserStore
import com.cyworks.redux.ReduxContext
import com.cyworks.redux.atom.UIPropsWatcher
import com.cyworks.redux.types.DepProps
import com.cyworks.redux.types.OnUIAtomChanged
import com.cyworks.redux.ui.ViewModule
import com.cyworks.redux.util.IPlatform

class DemoViewModule : ViewModule<DemoFeatureState> {
    private val onNumChanged: OnUIAtomChanged<DemoFeatureState> =
        OnUIAtomChanged { state, oldDeps, holder ->
            val textView: TextView? = holder?.getView(R.id.component_text)
            textView?.text = "" + state.num
        }

    private val onNameChanged: OnUIAtomChanged<DemoFeatureState> =
        OnUIAtomChanged { state, oldDeps, holder ->
        val textView: TextView? = holder?.getView(R.id.component_text_1)
        textView?.text = state.name
    }

    private fun getVerticalView(context: ReduxContext<DemoFeatureState>): View? {
        val platform: IPlatform = context.platform
        val id: Int = platform.viewContainerIdForV
        val view: View? = platform.inflateStub(id, R.layout.component_layout)
        initView(view, context)
        return view
    }

    private fun getHorizontalView(context: ReduxContext<DemoFeatureState>): View? {
        val platform: IPlatform = context.platform
        val id: Int = platform.viewContainerIdForH
        val view: View? = platform.inflateStub(id, R.layout.h_component_layout)
        initView(view, context)
        return view
    }

    private fun initView(view: View?, context: ReduxContext<DemoFeatureState>) {
        view?.findViewById<View>(R.id.demo_launcher_dialog)?.setOnClickListener { v: View? ->
            context.dispatcher.dispatch(DemoPageActions.createOpenDemoDialogAction(true))
        }

        view?.findViewById<View>(R.id.component_bt)?.setOnClickListener { v: View? ->
            context.updateState { state ->
                val before = state.num
                state.num = before + 1
                state
            }
        }

        view?.findViewById<View>(R.id.component_bt_1)?.setOnClickListener { v: View? ->
            UserStore.instance.modifyUserName("888")
        }
        view?.findViewById<View>(R.id.component_bt_2)?.setOnClickListener { v: View? ->
            UserStore.instance.modifyUserNameAsync("666")
        }
    }

    override fun getView(context: ReduxContext<DemoFeatureState>, parent: View): View? {
        val orientation: Int = context.state.currentOrientation
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return getVerticalView(context)
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return getHorizontalView(context)
        }
        return null
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