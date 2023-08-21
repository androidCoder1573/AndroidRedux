package com.cyworks.demo.demopage

import com.cyworks.redux.atom.StatePropsWatcher
import com.cyworks.redux.lifecycle.LifeCycleAction.ACTION_ON_CREATE
import com.cyworks.redux.logic.EffectCollector
import com.cyworks.redux.logic.LogicModule

class DemoPageLogic : LogicModule<DemoPageState> {
    override fun addLocalEffects(collect: EffectCollector<DemoPageState>) {
        collect.add(ACTION_ON_CREATE) { action, ctx ->
            ctx?.updateState { state ->
                val goodInfo = GoodInfo()
                goodInfo.mTitle = "电脑+手机"
                goodInfo.mPrice = "20000"
                state.goodInfo = goodInfo
                state
            }

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
}