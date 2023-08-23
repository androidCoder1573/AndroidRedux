package com.cyworks.demo.root

import android.os.Bundle
import com.cyworks.redux.state.State

class GoodInfo {
    var title: String? = null
    var price: String? = null

    override fun toString(): String {
        return "商品名: $title, 价格: $price"
    }
}

class RootComponentState : State() {
    var num: Int by this.ReactUIData(1)
    var goodInfo: GoodInfo? by this.ReactUIData(null)
}

fun rootComponentStateInit(props: Bundle?): RootComponentState {
    val state = RootComponentState()
    state.num = 1
    val info = GoodInfo()
    info.title = "电视"
    info.price = "10000"
    state.goodInfo = info
    return state
}