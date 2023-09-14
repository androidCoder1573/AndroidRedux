package com.cyworks.redux.atom

import com.cyworks.redux.state.State
import com.cyworks.redux.types.DepProps

/**
 * 用于描述一个一组UI数据对应的唯一UI更新，也即：一个UI只能绑定到对应的一个Atom上
 * 更新规则: 一块UI可能依赖一个或多个属性，当依赖的属性有任何一个变化，都会触发某块UI更新
 */
abstract class Atom<S : State> {
    internal var dep: DepProps? = null

    private var keyList: Array<String>? = null

    protected var oldProps: Array<Any>? = null
    private var newProps: Array<Any>? = null

    /**
     * 初始化当前Atom依赖的属性列表
     */
    fun setDepProps(dep: DepProps) {
        this.dep = dep
    }

    protected fun isChanged(changedKeys: HashSet<String>?, s: State): Boolean {
        var changed = false

        if (keyList == null || changedKeys == null || changedKeys.size < 1) {
            return false
        }

        val size = keyList!!.size
        if (size == 0) {
            return false
        }

        for (i in 0 until size) {
            if (changedKeys.contains(keyList!![i])) {
                changed = true
                break
            }
        }

        if (changed) {
            if (newProps != null && oldProps != null) {
                for (i in 0 until newProps!!.size) {
                    oldProps!![i] = newProps!![i]
                }
            }

            if (newProps != null) {
                for (i in 0 until newProps!!.size) {
                    val key: String = keyList!![i]
                    val prop = s.dataMap[key]
                    if (prop != null) {
                        newProps!![i] = prop.value()!!
                    }
                }
            }
        }

        return changed
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