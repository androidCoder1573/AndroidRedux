package com.cyworks.demo.userstore

import com.cyworks.redux.state.State

class UserState : State() {
    var name: String by this.ReactLogicData("")
    var age: Int by this.ReactLogicData(0)
}

fun userStateInit(): UserState {
    val state = UserState()
    state.name = "aaaa"
    state.age = 22
    return state
}