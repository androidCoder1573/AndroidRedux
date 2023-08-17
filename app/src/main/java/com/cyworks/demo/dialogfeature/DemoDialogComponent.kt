package com.cyworks.demo.dialogfeature

import android.os.Bundle
import com.cyworks.redux.component.DialogComponent
import com.cyworks.redux.logic.LogicModule
import com.cyworks.redux.ui.ViewModule

class DemoDialogComponent : DialogComponent<DialogState>() {
    override fun createViewModule(): ViewModule<DialogState> {
        return DialogViewModule()
    }

    override fun onCreateState(bundle: Bundle?): DialogState {
        return dialogStateInit()
    }

    override fun createLogicModule(): LogicModule<DialogState>? {
        return DialogLogicModule()
    }
}