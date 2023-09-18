package com.cyworks.demo.root

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import com.cyworks.demo.publicactions.DemoPublicActions
import com.cyworks.redux.ReduxContext
import com.cyworks.redux.annotations.EffectMethod

interface IRootComponentController {
    @EffectMethod
    fun openDialog(context: ReduxContext<RootComponentState>)

    @EffectMethod
    fun changeOrientation(context: ReduxContext<RootComponentState>)
}

class RootComponentController : IRootComponentController {
    override fun openDialog(context: ReduxContext<RootComponentState>) {
        context.dispatcher.dispatchToInterceptor(DemoPublicActions.createOpenDemoDialogAction(true))
    }

    override fun changeOrientation(context: ReduxContext<RootComponentState>) {
        val next = if (context.state.currentOrientation == Configuration.ORIENTATION_LANDSCAPE)
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        else  ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        context.platform.activity?.setRequestedOrientation(next)
    }
}