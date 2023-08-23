package com.cyworks.demo.demoComponent

import android.os.Bundle
import com.cyworks.redux.state.State

class DemoFeatureState : State() {
    var num: Int by this.ReactUIData(0)
    var name: String by this.ReactUIData("")
    var age: Int by this.ReactUIData(0)
}

fun demoFeatureStateInit(props: Bundle?): DemoFeatureState {
    val state = DemoFeatureState()
    return state
}