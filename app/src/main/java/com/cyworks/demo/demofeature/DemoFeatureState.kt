package com.cyworks.demo.demofeature

import android.os.Bundle
import com.cyworks.redux.state.State

class DemoFeatureState : State() {
    var num: Int by this.ReactUIData(0)
    var name: String by this.ReactUIData("")
    var age: Int by this.ReactUIData(0)
    var controll: AddControl? by this.ReactUIData(null)
}

fun demoFeatureStateInit(props: Bundle?): DemoFeatureState {
    val state = DemoFeatureState()
    val control = AddControl()
    control.increase = true
    state.controll = control
    return state
}