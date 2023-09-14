package com.cyworks.demo.demoComponent

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.cyworks.redux.state.State

class DemoFeatureState : State() {
    var num: Int by this.ReactUIData(0)
    var name: String by this.ReactUIData("")
    var age: Int by this.ReactUIData(0)

    val handler = Handler(Looper.getMainLooper())
}

fun demoFeatureStateInit(props: Bundle?): DemoFeatureState {
    val state = DemoFeatureState()
    return state
}