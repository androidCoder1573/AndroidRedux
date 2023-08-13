package com.cyworks.redux.state

import com.cyworks.redux.prop.ReactiveProp
import java.util.ArrayList

/**
 * Desc: 主要用于记录State的变化，具有几个功能：
 * 1、记录本次修改的私有属性
 * 2、记录本次修改的公共属性
 * 3、检查组件是否可以修改某个值
 *
 * 本类的实例会在执行Reducer期间注入到具体的[State]对象中
 * 这样做的主要目的是防止用户在非Reducer中更新UI属性，导致框架无法捕获属性变更。
 */
class StateProxy {
    /**
     * 每次执行reducer并不一定只更新一个属性， 用一个表来记录哪些数据发生了变化，
     * 当store更新界面的时候会统一提取这个表中的数据，进行统一更新
     */
    private val mChangeQueue: MutableList<ReactiveProp<Any>>

    init {
        mChangeQueue = ArrayList()
    }

    /**
     * 记录变化的属性，用于store通知时进行判断。
     *
     * @param prop 属性值 [ReactiveProp]
     */
    fun recordChangedProp(prop: ReactiveProp<Any>) {
        mChangeQueue.add(prop)
    }

    /**
     * 获取组件私有数据变化的情况。
     * @return ChangedProp列表
     */
    val changedPrivateProps: List<ReactiveProp<Any>>?
        get() {
            if (mChangeQueue.isEmpty()) {
                return null
            }
            val list: MutableList<ReactiveProp<Any>> = ArrayList()
            val it = mChangeQueue.iterator()
            while (it.hasNext()) {
                val changedProp = it.next()
                if (changedProp.isPrivateProp) {
                    list.add(changedProp)
                    it.remove()
                }
            }
            return if (list.isEmpty()) null else list
        }

    /**
     * 获取reducer执行完成后数据变化的情况。
     * @return ChangedProp列表
     */
    val changedPublicProps: List<ReactiveProp<Any>>?
        get() {
            if (mChangeQueue.isEmpty()) {
                return null
            }
            val list: MutableList<ReactiveProp<Any>> = ArrayList()
            for (changedProp in mChangeQueue) {
                if (!changedProp.isPrivateProp) {
                    list.add(changedProp)
                }
            }
            mChangeQueue.clear()
            return if (list.isEmpty()) null else list
        }

    /**
     * 如果状态内存在公共状态，且公共状态发生变化，则认为当前属性属性有变化，需要通知关注的组件。
     * @return 是否发生变化
     */
    val isChanged: Boolean
        get() {
            if (mChangeQueue.isEmpty()) {
                return false
            }
            for (prop in mChangeQueue) {
                if (!prop.isPrivateProp) {
                    return true
                }
            }
            return false
        }
}