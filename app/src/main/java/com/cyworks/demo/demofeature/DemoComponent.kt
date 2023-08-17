package com.cyworks.demo.demofeature

import android.os.Bundle
import com.cyworks.redux.component.Component
import com.cyworks.redux.logic.LogicModule
import com.cyworks.redux.ui.ViewModule

class DemoComponent(lazyBindUI: Boolean) : Component<DemoFeatureState>(lazyBindUI) {
    override fun createViewModule(): ViewModule<DemoFeatureState> {
        return DemoViewModule()
    }

    override fun onCreateState(bundle: Bundle?): DemoFeatureState {
        return demoFeatureStateInit(bundle)
    }

    override fun createLogicModule(): LogicModule<DemoFeatureState>? {
        return DemoFeatureLogic()
    }
}