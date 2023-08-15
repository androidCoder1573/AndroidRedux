package com.cyworks.redux

import com.cyworks.redux.action.Action
import com.cyworks.redux.logic.EffectCollector
import com.cyworks.redux.state.State
import com.cyworks.redux.types.Effect
import com.cyworks.redux.util.IPlatform

/**
 * 默认的逻辑控制类，开发者继承此类用于实现
 */
class BaseController<S : State> {
    /**
     * ReduxContext, 用于更改状态以及组件间交互
     */
    protected lateinit var mReduxContext: ReduxContext<S>
    protected lateinit var platform: IPlatform

    /**
     * effect 收集器，用于组件间交互
     */
    private val mEffectCollector: EffectCollector<S> = EffectCollector()

    /**
     * 设置ReduxContext，并初始化部分成员
     * @param reduxContext ReduxContext
     */
    fun setReduxContext(reduxContext: ReduxContext<S>) {
        mReduxContext = reduxContext
        platform = reduxContext.platform
    }

    /**
     * 订阅action
     * @param action action
     * @param effect Effect
     */
    fun observeAction(action: Action<Any>, effect: Effect<S>) {
        mEffectCollector.add(action, effect)
    }

    fun doAction(action: Action<Any>) {
        mEffectCollector.effect.doAction(action, mReduxContext)
    }
}