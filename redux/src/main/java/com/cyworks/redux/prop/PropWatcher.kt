package com.cyworks.redux.prop

import com.cyworks.redux.ReactiveProp
import com.cyworks.redux.ReduxContext
import com.cyworks.redux.State
import com.cyworks.redux.types.IPropChanged

/**
 * Desc: 用于观察组件中某一个属性的类;
 * 有一种情况需要这些观察者：比如需要监听某个状态改变然后做一些逻辑
 * @author randytu
 */
class PropWatcher<S : State> {
    /**
     * 保存针对属性的主动监听器, 因为这里是个数据集合，没办法统一泛型类型
     */
    private var listenerMap: HashMap<String, IPropChanged<S, Any>>? = null

    /**
     * 给外部一个机会监听自己组件内的State变化, 每次只能监听一个属性。
     *
     * @param key 当前组件属性对应的key
     * @param propChanged IPropChanged，用于响应数据变化
     */
    fun watchProp(key: String, propChanged: IPropChanged<S, Any>) {
        if (listenerMap == null) {
            listenerMap = HashMap()
        }
        listenerMap!![key] = propChanged
    }

    /**
     * 当属性发生变化时，通过此方法通知监听的观察者
     * @param props 当前组件变化的属性的列表
     * @param context 当前组件的ReduxContext
     */
    fun notifyPropChanged(props: List<ReactiveProp<Any>?>, context: ReduxContext<S>) {
        if (listenerMap == null || listenerMap!!.isEmpty()) {
            return
        }

        for (prop in props) {
            val iPropChanged: IPropChanged<S, Any>? = listenerMap!![prop?.key]
            if (prop != null) {
                iPropChanged?.onPropChanged(prop, context)
            }
        }
    }
}