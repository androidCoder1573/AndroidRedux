package com.cyworks.redux

import com.cyworks.redux.action.Action
import com.cyworks.redux.types.Dispatch
import com.cyworks.redux.types.Dispose
import com.cyworks.redux.types.IBus

class DispatchBus internal constructor() : IBus {
    /**
     * 每个bus中，保存的dispatch需要根据Page以及父组件分别保存。
     * 目前需求的通信方式是，组件只能发action给Page，但是Page可以发送Action给所有的组件，所以这里区分一下
     */
    internal var pageDispatch: Dispatch? = null

    /**
     * 保存当前bus的父亲
     */
    private var parent: DispatchBus? = null

    /**
     * 保存当前bus的孩子，根bus会持有多个孩子
     */
    private val childList: MutableList<Dispatch>

    /**
     * 删除添加的某个DispatchBus
     */
    private var removeSelf: Dispose? = null
    private var isDetach = false

    /**
     * 初始化依赖列表，为了防止事件关系混乱，这个创建过程由框架来维护
     */
    init {
        childList = ArrayList<Dispatch>()
    }

    override fun register(dispatch: Dispatch?): Dispose? {
        if (isDetach || dispatch == null) {
            return null
        }

        childList.add(dispatch)
        return { childList.remove(dispatch) }
    }

    /**
     * 目前的通信方式是：组件只能发action给Page，但是Page可以发送Action给所有的组件，所以这里区分一下
     *
     * @param dispatch Dispatch
     */
    fun setPageEffectDispatch(dispatch: Dispatch?) {
        pageDispatch = dispatch
    }

    override fun attach(parent: IBus?) {
        if (parent !is DispatchBus) {
            return
        }
        this.parent = parent
        removeSelf?.let { it() }
        removeSelf = parent.register(this)
    }

    override fun detach() {
        isDetach = true
        removeSelf?.let { it() }
    }

    override fun broadcast(action: Action<Any>) {
        if (isDetach) {
            return
        }

        var parent = parent
        if (parent == null) {
            // 本身就在顶层
            innerBroadcast(action)
            return
        }

        // 查找最顶层
        while (parent!!.parent != null) {
            parent = parent.parent
        }
        parent.innerBroadcast(action)
    }

    /**
     * 设计这个方法的目的：希望广播只能由Page来接收，其他组件不能接收广播，
     * page接收之后可以通过拦截器的方式转给组件。
     */
    private fun innerBroadcast(action: Action<Any>) {
        if (childList.isEmpty()) {
            return
        }
        for (dispatch in childList) {
            if (dispatch !is DispatchBus) {
                continue
            }

            // 交给page来处理
            dispatch.pageDispatch?.dispatch(action)
        }
    }

    override fun dispatch(action: Action<out Any>) {
        dispatch(action as Action<Any>, null)
    }

    internal fun dispatch(action: Action<Any>, exclude: Dispatch?) {
        if (isDetach) {
            return
        }
        for (dispatcher in childList) {
            if (dispatcher !== exclude) {
                dispatcher.dispatch(action)
            }
        }
    }
}