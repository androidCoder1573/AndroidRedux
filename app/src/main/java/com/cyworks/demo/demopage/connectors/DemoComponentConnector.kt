package com.cyworks.demo.demopage.connectors

import com.cyworks.R
import com.cyworks.redux.component.Connector
import com.cyworks.demo.demofeature.DemoFeatureState
import com.cyworks.demo.demopage.DemoPageState
import com.cyworks.demo.userstore.UserState
import com.cyworks.demo.userstore.UserStore
import com.cyworks.redux.interceptor.InterceptorCollector
import com.cyworks.redux.store.GlobalStoreSubscribe

class DemoComponentConnector : Connector<DemoFeatureState, DemoPageState>() {
    override val viewContainerIdForV: Int
        get() = R.id.component_stub

    override val viewContainerIdForH: Int
        get() = R.id.horizontal_component_stub

    override fun dependParentState(childState: DemoFeatureState, parentState: DemoPageState) {
        childState.num = parentState.num
    }

    override fun dependGlobalState(watcher: GlobalStoreSubscribe<DemoFeatureState>) {
        watcher.subscribe(UserStore.instance.store, object : GlobalStoreSubscribe.ICombineGlobalState<DemoFeatureState, UserState> {
            override fun combine(
                childState: DemoFeatureState,
                globalState: UserState
            ) {
                childState.age = globalState.age
                childState.name = globalState.name
            }
        })
    }

    override fun interceptorCollector(collect: InterceptorCollector<DemoFeatureState>) {}
}