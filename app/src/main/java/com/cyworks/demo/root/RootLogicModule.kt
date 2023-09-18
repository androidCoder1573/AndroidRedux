package com.cyworks.demo.root

import android.os.Bundle
import com.cyworks.demo.LaunchType
import com.cyworks.demo.demoComponent.DemoComponent
import com.cyworks.demo.root.connectors.DemoComponentConnector
import com.cyworks.redux.action.Action
import com.cyworks.redux.action.InnerActionTypes.INSTALL_EXTRA_FEATURE_ACTION_TYPE
import com.cyworks.redux.atom.StatePropsWatcher
import com.cyworks.redux.dependant.Dependant
import com.cyworks.redux.dependant.ExtraDependants
import com.cyworks.redux.lifecycle.LifeCycleAction
import com.cyworks.redux.logic.EffectCollector
import com.cyworks.redux.logic.LogicModule

class RootLogicModule(val props: Bundle?) : LogicModule<RootComponentState> {
    override fun addLocalEffects(collect: EffectCollector<RootComponentState>) {
        collect.add(LifeCycleAction.ACTION_ON_CREATE) { action, ctx ->
            ctx?.updateState { state ->
                val goodInfo = GoodInfo()
                goodInfo.title = "ROG手机+掌机"
                goodInfo.price = "15000"
                state.goodInfo = goodInfo
                state
            }

            val type = props?.getInt("LAUNCH_TYPE") ?: 0
            if (type == LaunchType.INSTALL_EXTRA_COMPONENT.ordinal) {
                object : Thread() {
                    override fun run() {
                        try {
                            sleep(2000)
                            // 安装额外子组件
                            val extraFeatures: ExtraDependants<RootComponentState> = ExtraDependants()
                            extraFeatures.addExtDependant(
                                Dependant(DemoComponent(false, props), DemoComponentConnector())
                            )
                            ctx?.dispatcher?.dispatch(Action(INSTALL_EXTRA_FEATURE_ACTION_TYPE, extraFeatures))
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                }.start()
            }
        }
    }

    override fun subscribeProps(
        state: RootComponentState,
        watcher: StatePropsWatcher<RootComponentState>
    ) {}

    override fun createController(): RootComponentController {
        return RootComponentController()
    }
}