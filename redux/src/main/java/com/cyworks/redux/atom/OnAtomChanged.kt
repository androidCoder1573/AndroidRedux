package com.cyworks.redux.atom

import com.cyworks.redux.state.State
import com.cyworks.redux.ui.ComponentViewHolder

/**
 * 当Atom对应的Key发生变化时，触发对应UI的更新方法
 */
fun interface OnAtomChanged<S : State> {
    /**
     * 更新UI，这里抽象更彻底，某一个组件内部的UI，只能绑定
     */
    fun update(state: S, oldDeps: List<Any>?, holder: ComponentViewHolder?)
}