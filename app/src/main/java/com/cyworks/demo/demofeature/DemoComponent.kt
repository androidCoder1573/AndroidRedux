package com.cyworks.demo.demofeature

import android.os.Bundle
import com.cyworks.redux.component.LiveComponent
import com.cyworks.redux.logic.LogicModule
import com.cyworks.redux.ui.ViewModule

class DemoComponent(lazyBindUI: Boolean) : LiveComponent<DemoFeatureState>(lazyBindUI) {
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