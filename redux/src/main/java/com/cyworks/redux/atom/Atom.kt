package com.cyworks.redux.atom

import com.cyworks.redux.state.State
import com.cyworks.redux.types.DepProps

/**
 * 用于描述一个一组UI数据对应的唯一UI更新，也即：一个UI只能绑定到对应的一个Atom上
 * 更新规则: 一块UI可能依赖一个或多个属性，当依赖的属性有任何一个变化，都会触发某块UI更新
 */
abstract class Atom<S : State> {
    internal var dep: DepProps? = null

    protected var keyList: Array<String>? = null

    protected var oldProps: Array<Any>? = null
    protected var newProps: Array<Any>? = null

    /**
     * 初始化当前Atom依赖的属性列表
     */
    fun setDepProps(dep: DepProps) {
        this.dep = dep
    }

    protected fun isChanged(changedKeys: List<String>?): Boolean {
        if (changedKeys.isNullOrEmpty() || keyList == null || keyList!!.isEmpty()) {
            return false
        }

        for (key in keyList!!) {
            if (changedKeys.contains(key)) {
                return true
            }
        }

        return false
    }

    internal fun addKeyList(list: ArrayList<String>) {
        val size = list.size
        keyList = Array(size) { "" }
        for (i in 0 until size) {
            keyList!![i] = list[i]
        }

        oldProps = arrayOf(size)
        newProps = arrayOf(size)
    }
}