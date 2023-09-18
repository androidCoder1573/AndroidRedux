package com.cyworks.demo.demoComponent

import com.cyworks.demo.LaunchType
import com.cyworks.demo.userstore.UserStore
import com.cyworks.redux.ReduxContext
import com.cyworks.redux.annotations.EffectMethod

interface IDemoController {
    @EffectMethod
    fun onBtnClick(type: Int, context: ReduxContext<DemoFeatureState>)
}

class DemoController : IDemoController {
    @EffectMethod
    override fun onBtnClick(type: Int, context: ReduxContext<DemoFeatureState>) {
        if (type == LaunchType.DEP_PARENT.ordinal) {
            context.updateState { state ->
                val before = state.num
                state.num = before + 1
                state
            }
        } else if (type == LaunchType.DEP_GLOBAL.ordinal) {
            UserStore.instance.modifyUserName("bbb${Math.random()}")
        }
    }
}