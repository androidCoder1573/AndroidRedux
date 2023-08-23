package com.cyworks.demo.root

import android.os.Bundle
import com.cyworks.demo.LaunchType
import com.cyworks.demo.demoComponent.DemoComponent
import com.cyworks.demo.demoDialogComponent.DemoDialogComponent
import com.cyworks.demo.root.connectors.DemoComponentConnector
import com.cyworks.demo.root.connectors.DialogConnector
import com.cyworks.redux.component.Component
import com.cyworks.redux.dependant.Dependant
import com.cyworks.redux.dependant.DependentCollector
import com.cyworks.redux.logic.LogicModule
import com.cyworks.redux.ui.ViewModule

class RootComponent(lazyBindUI: Boolean, p: Bundle?) : Component<RootComponentState>(lazyBindUI, p) {
    override fun createViewModule(): ViewModule<RootComponentState> {
        return RootViewModule(props?.getInt("LAUNCH_TYPE") ?: 0)
    }

    override fun onCreateState(bundle: Bundle?): RootComponentState {
        return rootComponentStateInit(bundle)
    }

    override fun createLogicModule(): LogicModule<RootComponentState> {
        return RootLogic()
    }

    override fun addDependencies(collect: DependentCollector<RootComponentState>?) {
        val type = props?.getInt("LAUNCH_TYPE") ?: 0

        if (type == LaunchType.DEP_PARENT.ordinal) {
            collect?.addDependant(Dependant(DemoComponent(false, props), DemoComponentConnector()))
        } else if (type == LaunchType.DEP_GLOBAL.ordinal) {
            collect?.addDependant(Dependant(DemoComponent(false, props), DemoComponentConnector()))
        } else if (type == LaunchType.LAUNCH_DIALOG.ordinal) {
            collect?.addDependant(Dependant(DemoDialogComponent(props), DialogConnector()))
        } else if (type == LaunchType.DELAY_UI.ordinal) {
            collect?.addDependant(Dependant(DemoComponent(true, props), DemoComponentConnector()))
        } else if (type == LaunchType.CHANGE_ORIENTATION.ordinal) {
            collect?.addDependant(Dependant(DemoComponent(false, props), DemoComponentConnector()))
        }
    }
}