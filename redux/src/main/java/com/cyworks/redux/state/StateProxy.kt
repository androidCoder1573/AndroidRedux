package com.cyworks.redux.state

import com.cyworks.redux.prop.ReactiveProp

/**
 * 主要用于记录State的变化，具有几个功能：
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
    private val changeQueue: MutableList<ReactiveProp<Any>>

    private val privateChangedQueue = ArrayList<ReactiveProp<Any>>()
    private val publishChangedQueue = ArrayList<ReactiveProp<Any>>()

    init {
        changeQueue = ArrayList()
    }

    /**
     * 记录变化的属性，用于store通知时进行判断。
     * @param prop 属性值 [ReactiveProp]
     */
    fun recordChangedProp(prop: ReactiveProp<Any>) {
        changeQueue.add(prop)
    }

    /**
     * 获取组件私有数据变化的情况。
     * @return ChangedProp列表
     */
    val changedPrivateProps: List<ReactiveProp<Any>>?
        get() {
            if (changeQueue.isEmpty()) {
                return null
            }
            privateChangedQueue.clear()
            val it = changeQueue.iterator()
            while (it.hasNext()) {
                val changedProp = it.next()
                if (changedProp.isPrivateProp) {
                    privateChangedQueue.add(changedProp)
                    it.remove()
                }
            }
            return if (privateChangedQueue.isEmpty()) null else privateChangedQueue
        }

    /**
     * 获取reducer执行完成后数据变化的情况。
     * @return ChangedProp列表
     */
    val changedPublicProps: List<ReactiveProp<Any>>?
        get() {
            if (changeQueue.isEmpty()) {
                return null
            }
            publishChangedQueue.clear()
            for (changedProp in changeQueue) {
                if (!changedProp.isPrivateProp) {
                    publishChangedQueue.add(changedProp)
                }
            }
            changeQueue.clear()
            return if (publishChangedQueue.isEmpty()) null else publishChangedQueue
        }

    fun clear() {
        changeQueue.clear()
    }
}