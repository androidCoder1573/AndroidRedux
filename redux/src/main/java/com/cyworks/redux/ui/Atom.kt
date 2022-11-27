package com.cyworks.redux.ui

import com.cyworks.redux.State
import com.cyworks.redux.types.DepProps

/**
 * Desc: 用于描述一个一组UI数据对应的唯一UI更新，
 * 也即：一个UI只能绑定到对应的一个Atom上。
 *
 * 更新规则: 一块UI可能依赖一个或多个属性，当依赖的属性有任何一个变化，都会触发某块UI更新。
 */
class Atom<S : State> {
    /**
     * 当前Atom依赖的属性，任意属性发生变化，这时候就会触发UI更新
     */
    private var watchedProps: Array<Any>? = null

    /**
     * 当前依赖的属性关联的UI更新函数
     */
    private var onChanged: OnAtomChanged<S>? = null

    private var dep: DepProps? = null

    /**
     * 初始化当前Atom依赖的属性列表
     */
    fun setDep(dep: DepProps) {
        this.dep = dep
    }

    fun setAtomChangedCB(onAtomChanged: OnAtomChanged<S>?) {
        this.onChanged = onAtomChanged
    }

    fun doAtomChange(state: S, holder: ComponentViewHolder?) {
        val changedProps = dep?.let { it() }
        if (changedProps == null || changedProps.isEmpty()) {
            return
        }

        val isChanged = isEqual(changedProps, watchedProps)
        if (isChanged) {
            val oldDeps = watchedProps;
            watchedProps = changedProps;
            onChanged?.update(state, oldDeps, holder)
        }
    }

    private fun isEqual(first: Array<Any>?, second: Array<Any>?): Boolean {
        if (first == null || second == null) {
            return false;
        }
        if (first.size != second.size) {
            return false
        }

        return first.zip(second).all { (a, b) -> a == b }
    }
}