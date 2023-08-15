package com.cyworks.redux.prop

import com.cyworks.redux.state.State

/**
 * 将变化的组件状态封装成一个对象，方便使用LiveData进行观察
 */
data class ChangedState<S : State>(var lastState: S, var changedProps: List<ReactiveProp<Any>>)