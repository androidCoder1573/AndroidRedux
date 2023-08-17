package com.cyworks.demo.demopage.connectors

import com.cyworks.R
import com.cyworks.redux.action.Action
import com.cyworks.redux.component.Connector
import com.cyworks.demo.demopage.DemoPageState
import com.cyworks.demo.dialogfeature.DialogActions
import com.cyworks.demo.dialogfeature.DialogState
import com.cyworks.demo.publicactions.DemoPageActions
import com.cyworks.redux.interceptor.InterceptorCollector
import com.cyworks.redux.store.GlobalStoreSubscribe
import com.cyworks.redux.types.Interceptor

class DialogConnector : Connector<DialogState, DemoPageState>() {
    override val viewContainerIdForV: Int
        get() = R.layout.dialog_component_layout

    override val viewContainerIdForH: Int
        get() = R.layout.dialog_component_layout

    override fun dependParentState(childState: DialogState, parentState: DemoPageState) {
        childState.num = parentState.num
    }

    override fun dependGlobalState(watcher: GlobalStoreSubscribe<DialogState>) {}
    override fun interceptorCollector(collect: InterceptorCollector<DialogState>) {
        collect.addInterceptor(
            DemoPageActions.sOpenDemoDialog,
            Interceptor<DialogState> { action, ctx ->
                ctx?.dispatcher?.dispatch(Action(DialogActions.sOpenSelf, null))
            })
    }
}