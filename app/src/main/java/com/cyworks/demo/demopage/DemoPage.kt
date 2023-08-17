package com.cyworks.demo.demopage

import android.os.Bundle
import com.cyworks.redux.component.LivePage
import com.cyworks.demo.demofeature.DemoComponent
import com.cyworks.demo.demopage.connectors.DemoComponentConnector
import com.cyworks.demo.demopage.connectors.DialogConnector
import com.cyworks.demo.dialogfeature.DemoDialogComponent
import com.cyworks.redux.dependant.Dependant
import com.cyworks.redux.dependant.DependentCollector
import com.cyworks.redux.lifecycle.LifeCycleProxy
import com.cyworks.redux.logic.LogicModule

class DemoPage(rootId: Int, proxy: LifeCycleProxy) : LivePage<DemoPageState>(rootId, proxy) {
    override fun onCreateState(props: Bundle?): DemoPageState {
        return demoPageStateInit()
    }

    override fun createLogicModule(): LogicModule<DemoPageState> {
        return DemoPageLogic()
    }

    override fun addDependencies(collect: DependentCollector<DemoPageState>?) {
        collect?.addDependant(Dependant(DemoComponent(true), DemoComponentConnector()))
        collect?.addDependant(Dependant(DemoDialogComponent(), DialogConnector()))
    }
}