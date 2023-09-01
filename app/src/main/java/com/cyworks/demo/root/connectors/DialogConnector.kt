package com.cyworks.demo.root.connectors

import com.cyworks.R
import com.cyworks.redux.action.Action
import com.cyworks.redux.component.Connector
import com.cyworks.demo.demoDialogComponent.DialogActions
import com.cyworks.demo.demoDialogComponent.DialogState
import com.cyworks.demo.publicactions.DemoPageActions
import com.cyworks.demo.root.RootComponentState
import com.cyworks.redux.interceptor.InterceptorCollector
import com.cyworks.redux.store.GlobalStoreSubscribe

class DialogConnector : Connector<DialogState, RootComponentState>() {
    override val viewContainerIdForV: Int
        get() = R.layout.dialog_component_layout

    override val viewContainerIdForH: Int
        get() = R.layout.dialog_component_layout

    override fun dependParentState(childState: DialogState, parentState: RootComponentState) {
        childState.num = parentState.num
    }

    override fun dependGlobalState(watcher: GlobalStoreSubscribe<DialogState>) {}
    override fun interceptorCollect(collect: InterceptorCollector<DialogState>) {
        collect.addInterceptor(DemoPageActions.sOpenDemoDialog) { _, ctx ->
            ctx?.dispatcher?.dispatch(Action(DialogActions.sOpenSelf, null))
        }
    }
}