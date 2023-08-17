package com.cyworks.redux.atom

import com.cyworks.redux.state.State
import com.cyworks.redux.types.DepProps

/**
 * 用于描述一个一组UI数据对应的唯一UI更新，也即：一个UI只能绑定到对应的一个Atom上。
 * 更新规则: 一块UI可能依赖一个或多个属性，当依赖的属性有任何一个变化，都会触发某块UI更新。
 */
abstract class Atom<S : State> {
    /**
     * 当前Atom依赖的属性，任意属性发生变化，这时候就会触发UI更新
     */
    protected var watchedProps: Array<Any>? = null

    protected var dep: DepProps? = null

    /**
     * 初始化当前Atom依赖的属性列表
     */
    fun setDepProps(dep: DepProps) {
        this.dep = dep
    }

    protected fun isEqual(first: Array<Any>?, second: Array<Any>?): Boolean {
        if (first == null || second == null) {
            return false
        }

        if (first.size != second.size) {
            return false
        }

        return first.zip(second).all { (a, b) -> a == b }
    }
}