package com.cyworks.redux

import com.cyworks.redux.state.State
import com.cyworks.redux.util.IPlatform

/**
 * 默认的逻辑控制类，开发者继承此类用于实现组件逻辑
 */
class BaseController<S : State> {
    /**
     * ReduxContext, 用于更改状态以及组件间交互
     */
    protected lateinit var reduxContext: ReduxContext<S>
    protected lateinit var platform: IPlatform

    /**
     * 设置ReduxContext，并初始化部分成员
     * @param reduxContext ReduxContext
     */
    fun setReduxContext(reduxContext: ReduxContext<S>) {
        this.reduxContext = reduxContext
        platform = reduxContext.platform
    }
}