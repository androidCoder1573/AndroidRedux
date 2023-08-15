package com.cyworks.redux.logic

import com.cyworks.redux.state.State

/**
 * 承载业务逻辑的接口
 */
interface LogicModule<S : State> {
    fun addLocalEffects(collect: EffectCollector<S>)
}