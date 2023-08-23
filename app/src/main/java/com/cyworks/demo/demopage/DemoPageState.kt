package com.cyworks.demo.demopage

import com.cyworks.redux.state.State

class DemoPageState : State() {}

fun demoPageStateInit(): DemoPageState {
    return DemoPageState()
}