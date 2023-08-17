package com.cyworks.demo.demopage

import com.cyworks.redux.state.State

class DemoPageState : State() {
    var num: Int by this.ReactUIData<Int>(1)

    var goodInfo: GoodInfo? by this.ReactUIData<GoodInfo?>(null)
}

fun demoPageStateInit(): DemoPageState {
    val state = DemoPageState()
    state.num = 1
    val info = GoodInfo()
    info.mTitle = "电视"
    info.mPrice = "10000"
    state.goodInfo = info
    return state
}