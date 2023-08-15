package com.cyworks.redux

import com.cyworks.redux.component.Logic
import com.cyworks.redux.state.State
import com.cyworks.redux.types.IStateChange
import com.cyworks.redux.util.IPlatform

/**
 * Desc: 通过建造者模式创建一个ReduxContext.
 */
class ReduxContextBuilder<S : State> {
    /**
     * 组件状态监听器
     */
    var stateChangeListener: IStateChange<S>? = null
        private set

    /**
     * 组件实例
     */
    lateinit var logic: Logic<S>
        private set

    /**
     * 组件状态
     */
    lateinit var state: S
        private set

    /**
     * 平台操作相关
     */
    lateinit var platform: IPlatform
        private set

    fun setState(state: S): ReduxContextBuilder<S> {
        this.state = state
        return this
    }

    fun setOnStateChangeListener(onStateChange: IStateChange<S>?): ReduxContextBuilder<S> {
        stateChangeListener = onStateChange
        return this
    }

    fun setLogic(logic: Logic<S>): ReduxContextBuilder<S> {
        this.logic = logic
        return this
    }

    fun setPlatform(platform: IPlatform): ReduxContextBuilder<S> {
        this.platform = platform
        return this
    }

    /**
     * 创建一个ReduxContext实例
     *
     * @return ReduxContext
     */
    fun build(): ReduxContext<S> {
        return ReduxContext(this)
    }
}