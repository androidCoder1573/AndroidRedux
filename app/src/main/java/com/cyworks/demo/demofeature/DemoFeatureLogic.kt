package com.cyworks.demo.demofeature

import com.cyworks.redux.atom.StatePropsWatcher
import com.cyworks.redux.lifecycle.LifeCycleAction
import com.cyworks.redux.logic.EffectCollector
import com.cyworks.redux.logic.LogicModule

class DemoFeatureLogic : LogicModule<DemoFeatureState> {
    override fun addLocalEffects(collect: EffectCollector<DemoFeatureState>) {
        collect.add(LifeCycleAction.ACTION_ON_CREATE) {  action, ctx ->
            val t: Thread = object : Thread() {
                override fun run() {
                    try {
                        sleep(2000L)
                    } catch (ie: InterruptedException) {
                        ie.printStackTrace()
                        currentThread().interrupt()
                    }

                    ctx?.updateState { state ->
                        state.isShowUI = true
                        state
                    }
                }
            }
            t.start()
        }
    }

    override fun subscribeProps(
        state: DemoFeatureState,
        watcher: StatePropsWatcher<DemoFeatureState>
    ) {}
}