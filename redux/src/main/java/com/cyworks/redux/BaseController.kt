package com.cyworks.redux

import com.cyworks.redux.action.Action
import com.cyworks.redux.logic.EffectCollect
import com.cyworks.redux.types.Effect
import com.cyworks.redux.util.IPlatform


/**
 * Desc: 默认的逻辑控制类，开发者继承此类用于实现
 * @author randytu
 */
class BaseController<S : State> {
    /**
     * ReduxContext, 用于更改状态以及组件间交互
     */
    var mReduxContext: ReduxContext<S>? = null
    var platform: IPlatform? = null

    /**
     * effect 收集器，用于组件间交互
     */
    private val mEffectCollect: EffectCollect<S> = EffectCollect()

    /**
     * 设置ReduxContext，并初始化部分成员
     * @param reduxContext ReduxContext
     */
    fun setReduxContext(reduxContext: ReduxContext<S>) {
        mReduxContext = reduxContext
        platform = getPlatform()
    }

    /**
     * 订阅action
     * @param action action
     * @param effect Effect
     */
    fun observeAction(action: Action<Any>, effect: Effect<S>) {
        mEffectCollect.add(action, effect)
    }

    private fun getPlatform(): IPlatform? {
        return if (mReduxContext == null) {
            null
        } else mReduxContext!!.platform
    }

    fun doAction(action: Action<Any>) {
        mEffectCollect.doAction(action, mReduxContext)
    }
}