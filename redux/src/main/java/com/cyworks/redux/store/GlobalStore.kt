package com.cyworks.redux.store

import com.cyworks.redux.action.Action
import com.cyworks.redux.logic.EffectCollector
import com.cyworks.redux.state.State
import com.cyworks.redux.state.StateProxy
import com.cyworks.redux.state.StateType
import com.cyworks.redux.types.CreateGlobalState
import com.cyworks.redux.types.Effect
import com.cyworks.redux.types.Reducer
import com.cyworks.redux.util.ThreadUtil
import kotlin.reflect.full.memberProperties

/**
 * 带Effect的Store，为全局Store优化的产物，增加Effect处理能力，方便全局store处理异步请求。
 * 主要是因为store不具备ReduxContext，导致无法发送处理Effect的Action。
 * 给开发这一个选择，可以使用action来驱动全局store，也可以不使用，直接通过面向对象的方式操作
 */
open class GlobalStore<S : State> : Store<S> {
    /**
     * 全局store扩展的内部effect
     */
    private var effect: Effect<S>? = null

    private val stateProxy = StateProxy()

    constructor()

    constructor(creator: CreateGlobalState<S>) : this() {
        init(creator)
    }

    private fun init(creator: CreateGlobalState<S>) {
        val effectCollector: EffectCollector<S> = EffectCollector()
        addEffects(effectCollector)
        effect = effectCollector.effect
        state = creator.create()
        state.stateType = StateType.GLOBAL_TYPE
        state.detectField(state.javaClass.kotlin.memberProperties)
    }

    /**
     * 注入Effect，不强制实现，如果开发者使用action驱动，需要实现此方法
     */
    open fun addEffects(effectCollector: EffectCollector<S>?) {
        // sub class maybe impl
    }

    /**
     * 扩展分发Effect Action的能力，全局Store本身可以通过单例获取
     * @param action Action
     */
    fun dispatchEffect(action: Action<Any>) {
        effect?.doAction(action, null)
    }

    /**
     * 更新状态
     */
    fun updateState(reducer: Reducer<S>) {
        ThreadUtil.checkMainThread("update state must be called in main thread!")
        state.setStateProxy(stateProxy)
        val newState = reducer.update(state)
        onStateChanged(newState)
        state.setStateProxy(null)
    }
}