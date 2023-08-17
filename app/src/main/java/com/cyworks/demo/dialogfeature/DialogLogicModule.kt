package com.cyworks.demo.dialogfeature

import com.cyworks.redux.atom.StatePropsWatcher
import com.cyworks.redux.logic.EffectCollector
import com.cyworks.redux.logic.LogicModule

class DialogLogicModule : LogicModule<DialogState> {
    override fun addLocalEffects(collect: EffectCollector<DialogState>) {
        collect.add(DialogActions.sOpenSelf) { action, ctx -> ctx?.showComponentDialog(ComponentDialogFragment()) }
    }

    override fun subscribeProps(state: DialogState, watcher: StatePropsWatcher<DialogState>) {}
}