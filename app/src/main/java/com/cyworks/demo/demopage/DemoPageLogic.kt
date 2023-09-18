package com.cyworks.demo.demopage

import com.cyworks.redux.atom.StatePropsWatcher
import com.cyworks.redux.logic.EffectCollector
import com.cyworks.redux.logic.LogicModule

class DemoPageLogic : LogicModule<DemoPageState> {
    override fun addLocalEffects(collect: EffectCollector<DemoPageState>) {}

    override fun subscribeProps(state: DemoPageState, watcher: StatePropsWatcher<DemoPageState>) {}

    override fun createController(): DemoPageController {
        return DemoPageController()
    }
}