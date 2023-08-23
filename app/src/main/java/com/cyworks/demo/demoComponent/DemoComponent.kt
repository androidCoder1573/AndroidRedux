package com.cyworks.demo.demoComponent

import android.os.Bundle
import com.cyworks.redux.component.Component
import com.cyworks.redux.logic.LogicModule
import com.cyworks.redux.ui.ViewModule

class DemoComponent(lazyBindUI: Boolean, p: Bundle?) : Component<DemoFeatureState>(lazyBindUI, p) {
    override fun createViewModule(): ViewModule<DemoFeatureState> {
        return DemoViewModule(props?.getInt("LAUNCH_TYPE") ?: 0)
    }

    override fun onCreateState(bundle: Bundle?): DemoFeatureState {
        return demoFeatureStateInit(bundle)
    }

    override fun createLogicModule(): LogicModule<DemoFeatureState> {
        return DemoFeatureLogic()
    }
}