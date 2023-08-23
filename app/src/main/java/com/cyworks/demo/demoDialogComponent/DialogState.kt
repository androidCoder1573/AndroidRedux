package com.cyworks.demo.demoDialogComponent

import com.cyworks.redux.state.State

class DialogState : State() {
    var num: Int by this.ReactUIData(0)
    var localNum: Int by this.ReactUIData(0)

    companion object {
        const val DIALOG_TAG = "demo_dialog_feature"
    }
}

fun dialogStateInit(): DialogState {
    val state = DialogState()
    state.num = 1
    state.localNum = 2
    return state
}
