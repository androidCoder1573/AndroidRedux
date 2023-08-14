package com.cyworks.redux.logic

import com.cyworks.redux.ReduxContext
import com.cyworks.redux.action.Action
import com.cyworks.redux.action.ActionType
import com.cyworks.redux.lifecycle.LifeCycleAction
import com.cyworks.redux.state.State
import com.cyworks.redux.types.Effect

/**
 * Effect收集器，用于外部配置组件的Effect, 内部会将一个组件对应的Effect合并成一个.
 */
class EffectCollect<S : State> {
    /**
     * 获取合并之后的Effect
     */
    val effect: Effect<S>
        get() = innerEffect

    /**
     * Effect 集合
     */
    private val functions = HashMap<ActionType, Effect<S>>()

    private val innerEffect = Effect { action, ctx ->
        doAction(action, ctx)
    }

    /**
     * 给当前组件注册一个Action对应的Effect，保持可替换
     * @param action AbsAction
     * @param effect Effect
     */
    fun add(action: Action<Any>, effect: Effect<S>) {
        functions[action.type] = effect
    }

    fun remove(action: Action<Any>) {
        functions.remove(action.type)
    }

    fun remove(type: ActionType) {
        functions.remove(type)
    }

    private fun doAction(action: Action<Any>, ctx: ReduxContext<S>?): Boolean {
        if (functions.isNotEmpty()) {
            val effect: Effect<S>? = functions[action.type]
            if (effect != null) {
                effect.doAction(action, ctx)
                return true
            }
        }

        // 如果是生命周期的Action，每个组件自己消费掉，不会再向其他组件分发了
        return LifeCycleAction.isLifeCycle(action)
    }

    fun clear() {
        functions.clear()
    }
}