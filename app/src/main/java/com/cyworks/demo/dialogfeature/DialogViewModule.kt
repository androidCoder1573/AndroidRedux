package com.cyworks.demo.dialogfeature

import android.view.View
import android.widget.TextView
import com.cyworks.R
import com.cyworks.redux.ReduxContext
import com.cyworks.redux.atom.UIPropsWatcher
import com.cyworks.redux.types.DepProps
import com.cyworks.redux.types.OnUIAtomChanged
import com.cyworks.redux.types.Reducer
import com.cyworks.redux.ui.ViewModule

class DialogViewModule : ViewModule<DialogState> {
    private val onNumChanged =
        OnUIAtomChanged<DialogState> { state, oldDeps, holder ->
            val textView: TextView? = holder?.getView(R.id.dialog_num_text)
            textView?.text = "页面计数: " + state.num
        }

    private val onLocalNumChanged = OnUIAtomChanged<DialogState> { state, oldDeps, holder ->
            val textView: TextView? = holder?.getView(R.id.dialog_local_num_text)
            textView?.text = "本地计数: " + state.localNum
        }

    override fun getView(context: ReduxContext<DialogState>, parent: View): View? {
        val v = View.inflate(context.platform.activity, R.layout.dialog_component_layout, null)
        v.findViewById<View>(R.id.dialog_set_local_text).setOnClickListener { v1: View? ->
            val next: Int = context.state.localNum + 1
            context.updateState(object : Reducer<DialogState> {
                override fun update(state: DialogState): DialogState {
                    state.localNum = next
                    return state
                }
            })
        }
        return v
    }

    override fun subscribeProps(state: DialogState, watcher: UIPropsWatcher<DialogState>?) {
        watcher?.watch(object : DepProps {
            override fun invoke(): Array<Any> {
                return arrayOf(state.num)
            }
        }, onNumChanged)

        watcher?.watch(object : DepProps {
            override fun invoke(): Array<Any> {
                return arrayOf(state.localNum)
            }
        }, onLocalNumChanged)
    }
}