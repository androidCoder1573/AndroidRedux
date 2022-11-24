package com.cyworks.redux

import com.tencent.redux.state.StateChangeForUI

/**
 * Desc: 通过建造者模式创建一个ReduxContext.
 *
 * @author randytu on 2020/12/15
 */
class ReduxContextBuilder<S : State?> {
    /**
     * 组件状态监听器
     */
    private var mStateChangeListener: StateChangeForUI<S>? = null

    /**
     * 组件实例
     */
    var logic: Logic<S>? = null
        private set

    /**
     * 组件状态
     */
    var state: S? = null
        private set

    /**
     * 平台操作相关
     */
    private var mPlatform: IPlatform? = null
    fun setState(state: S): ReduxContextBuilder<S> {
        this.state = state
        return this
    }

    fun setOnStateChangeListener(onStateChange: StateChangeForUI<S>?): ReduxContextBuilder<S> {
        mStateChangeListener = onStateChange
        return this
    }

    fun setLogic(logic: Logic<S>?): ReduxContextBuilder<S> {
        this.logic = logic
        return this
    }

    fun setPlatform(platform: IPlatform?): ReduxContextBuilder<S> {
        mPlatform = platform
        return this
    }

    val stateChangeListener: StateChangeForUI<S>?
        get() = mStateChangeListener
    val platform: IPlatform?
        get() = mPlatform

    /**
     * 创建一个ReduxContext实例
     *
     * @return ReduxContext
     */
    fun build(): ReduxContext<S> {
        return ReduxContext(this)
    }
}