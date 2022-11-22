package com.cyworks.redux.lifecycle

import com.cyworks.redux.action.Action
import com.cyworks.redux.action.ActionType

/**
 * Desc: Life Cycle Action
 * @author randytu
 */
object LifeCycleAction {
    // 这里要把生命周期定义成私Action有的原因是：每个组件都能拿到当前页面的生命周期，不需要进行广播
    val ACTION_ON_CREATE = ActionType("activity_on_create")
    val ACTION_ON_START = ActionType("activity_on_start")
    val ACTION_ON_RESUME = ActionType("activity_on_resume")
    val ACTION_ON_PAUSE = ActionType("activity_on_pause")
    val ACTION_ON_STOP = ActionType("activity_on_stop")
    val ACTION_ON_DESTROY = ActionType("activity_on_destroy")
    val ACTION_ON_UI_CHANGED = ActionType("component_ui_changed")

    /**
     * 判断Action是否是生命周期的Action
     * @param action Action
     * @return 是否是生命周期的Action
     */
    fun isLifeCycle(action: Action<Any>): Boolean {
        val type = action.type;
        return type == ACTION_ON_CREATE
                || type == ACTION_ON_START
                || type == ACTION_ON_RESUME
                || type == ACTION_ON_PAUSE
                || type == ACTION_ON_STOP
                || type == ACTION_ON_DESTROY
                || type == ACTION_ON_UI_CHANGED
    }
}