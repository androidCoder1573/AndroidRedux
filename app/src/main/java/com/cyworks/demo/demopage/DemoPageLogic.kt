package com.cyworks.demo.demopage

import com.cyworks.redux.IController
import com.cyworks.redux.atom.StatePropsWatcher
import com.cyworks.redux.lifecycle.LifeCycleAction.ACTION_ON_CREATE
import com.cyworks.redux.logic.EffectCollector
import com.cyworks.redux.logic.LogicModule

class DemoPageLogic : LogicModule<DemoPageState> {
    override fun addLocalEffects(collect: EffectCollector<DemoPageState>) {
        collect.add(ACTION_ON_CREATE) { action, ctx ->
//            object : Thread() {
//                override fun run() {
//                    try {
//                        sleep(2000)
//                        // 安装额外子组件
//                        val extraFeatures: ExtraDependants<DemoPageState> = ExtraDependants()
//                        extraFeatures.addExtDependant(
//                           Dependant(ListDemoComponent(true), DemoListConnector())
//                        )
//                        ctx?.dispatchEffect(Action(INSTALL_EXTRA_FEATURE_ACTION_TYPE, extraFeatures))
//                    } catch (e: InterruptedException) {
//                        e.printStackTrace()
//                    }
//                }
//            }.start()
        }
    }

    override fun subscribeProps(state: DemoPageState, watcher: StatePropsWatcher<DemoPageState>) {}

    override fun <C : IController> createController(): C? {
        return null
    }
}