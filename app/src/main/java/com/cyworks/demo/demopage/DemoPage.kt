package com.cyworks.demo.demopage

import android.os.Bundle
import com.cyworks.demo.demopage.connectors.RootComponentConnector
import com.cyworks.demo.root.RootComponent
import com.cyworks.redux.component.Page
import com.cyworks.redux.dependant.Dependant
import com.cyworks.redux.dependant.DependentCollector
import com.cyworks.redux.lifecycle.LifeCycleProxy
import com.cyworks.redux.logic.LogicModule

class DemoPage(rootId: Int ,p: Bundle?, proxy: LifeCycleProxy) : Page<DemoPageState>(rootId, p, proxy) {
    override fun onCreateState(props: Bundle?): DemoPageState {
        return demoPageStateInit()
    }

    override fun createLogicModule(): LogicModule<DemoPageState> {
        return DemoPageLogic()
    }

    override fun addDependencies(collect: DependentCollector<DemoPageState>?) {
        collect?.addDependant(Dependant(RootComponent(false, props), RootComponentConnector()))
    }
}