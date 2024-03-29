package com.cyworks.demo.publicactions

import com.cyworks.redux.action.Action
import com.cyworks.redux.action.ActionType

object DemoPublicActions {
    val sOpenDemoDialog = ActionType("sOpenDemoDialog")

    fun createOpenDemoDialogAction(payload: Boolean): Action<Boolean> {
        return Action(sOpenDemoDialog, payload)
    }
}