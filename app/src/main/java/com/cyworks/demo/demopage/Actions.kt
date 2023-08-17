package com.cyworks.demo.demopage

import com.cyworks.redux.action.Action
import com.cyworks.redux.action.ActionType

class Actions {
    fun createModifyGoodInfo(): Action<Any> {
        return Action(modifyGoodInfo, null)
    }

    companion object {
        /**
         * 演示同步修改数据
         */
        @JvmField
        val modifyGoodInfo = ActionType("modifyGoodInfo")
    }
}