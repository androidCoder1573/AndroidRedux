package com.cyworks.redux.prop

import com.cyworks.redux.state.State

/**
 * Desc: 将变化的组件状态封装成一个对象，方便使用LiveData进行观察
 */
class ChangedState<S : State> {
    /**
     * 组件上一次的状态
     */
    var mState: S? = null

    /**
     * 组件当前发生变化的属性列表, ChangedProp保存变更的属性，这里不能统一泛型
     */
    var mChangedProps: List<ReactiveProp<Any>>? = null
}