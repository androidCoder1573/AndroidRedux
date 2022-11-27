package com.cyworks.redux.logic

import com.cyworks.redux.State

/**
 * Desc: 承载业务逻辑的接口
 */
interface LogicModule<S : State> {
    fun addLocalEffects(collect: EffectCollect<S>)
}