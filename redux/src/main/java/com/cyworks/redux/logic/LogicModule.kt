package com.cyworks.redux.logic

import com.cyworks.redux.atom.StatePropsWatcher
import com.cyworks.redux.state.State

/**
 * 承载业务逻辑的接口
 */
interface LogicModule<S : State> {
    fun addLocalEffects(collect: EffectCollector<S>)

    /**
     * 通过这个接口来订阅自己组件下的属性变化
     */
    fun subscribeProps(state: S, watcher: StatePropsWatcher<S>)

    fun createController(): Any?
}