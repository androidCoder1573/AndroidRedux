package com.cyworks.demo.demopage.connectors

import com.cyworks.R
import com.cyworks.demo.demopage.DemoPageState
import com.cyworks.demo.root.RootComponentState
import com.cyworks.redux.component.Connector
import com.cyworks.redux.interceptor.InterceptorCollector
import com.cyworks.redux.store.GlobalStoreSubscribe

class RootComponentConnector : Connector<RootComponentState, DemoPageState>() {
    override val viewContainerIdForV: Int
        get() = R.id.page_component_stub

    override val viewContainerIdForH: Int
        get() = R.id.horizontal_page_component_stub

    override fun dependParentState(childState: RootComponentState, parentState: DemoPageState) {}

    override fun dependGlobalState(watcher: GlobalStoreSubscribe<RootComponentState>) {}

    override fun interceptorCollector(collect: InterceptorCollector<RootComponentState>) {}
}