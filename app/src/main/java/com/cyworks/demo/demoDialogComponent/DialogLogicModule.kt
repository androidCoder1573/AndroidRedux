package com.cyworks.demo.demoDialogComponent

import com.cyworks.redux.IController
import com.cyworks.redux.atom.StatePropsWatcher
import com.cyworks.redux.logic.EffectCollector
import com.cyworks.redux.logic.LogicModule

class DialogLogicModule : LogicModule<DialogState> {
    override fun addLocalEffects(collect: EffectCollector<DialogState>) {
        collect.add(DialogActions.sOpenSelf) { action, ctx -> ctx?.showDialog(ComponentDialogFragment()) }
    }

    override fun subscribeProps(state: DialogState, watcher: StatePropsWatcher<DialogState>) {}

    override fun <C : IController> createController(): C? {
        return null
    }
}