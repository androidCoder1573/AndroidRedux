package com.cyworks.demo.root.connectors

import com.cyworks.R
import com.cyworks.redux.component.Connector
import com.cyworks.demo.demoComponent.DemoFeatureState
import com.cyworks.demo.root.RootComponentState
import com.cyworks.demo.userstore.UserState
import com.cyworks.demo.userstore.UserStore
import com.cyworks.redux.interceptor.InterceptorCollector
import com.cyworks.redux.store.GlobalStoreSubscribe

class DemoComponentConnector : Connector<DemoFeatureState, RootComponentState>() {
    override val viewContainerIdForV: Int
        get() = R.id.root_component_stub

    override val viewContainerIdForH: Int
        get() = -1

    override fun dependParentState(childState: DemoFeatureState, parentState: RootComponentState) {
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

    override fun interceptorCollect(collect: InterceptorCollector<DemoFeatureState>) {}
}