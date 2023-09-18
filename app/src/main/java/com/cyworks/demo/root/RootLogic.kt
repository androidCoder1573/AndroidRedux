package com.cyworks.demo.root

import com.cyworks.redux.atom.StatePropsWatcher
import com.cyworks.redux.lifecycle.LifeCycleAction
import com.cyworks.redux.logic.EffectCollector
import com.cyworks.redux.logic.LogicModule

class RootLogic : LogicModule<RootComponentState> {
    override fun addLocalEffects(collect: EffectCollector<RootComponentState>) {
        collect.add(LifeCycleAction.ACTION_ON_CREATE) { action, ctx ->
            ctx?.updateState { state ->
                val goodInfo = GoodInfo()
                goodInfo.title = "ROG手机+掌机"
                goodInfo.price = "15000"
                state.goodInfo = goodInfo
                state
            }
        }
    }

    override fun subscribeProps(
        state: RootComponentState,
        watcher: StatePropsWatcher<RootComponentState>
    ) {}

    override fun createController(): RootComponentController? {
        return null
    }
}