package com.cyworks.redux

import com.cyworks.redux.action.Action
import com.cyworks.redux.types.Dispatch
import com.cyworks.redux.types.Dispose
import com.cyworks.redux.types.IBus
import java.util.ArrayList

class DispatchBus internal constructor() : IBus {
    /**
     * 保存当前bus的父亲
     */
    private var mParent: DispatchBus? = null

    /**
     * 保存当前bus的孩子，根bus会持有多个孩子
     */
    private val mChildList: MutableList<Dispatch>

    /**
     * 删除添加的某个DispatchBus
     */
    private var mRemoveSelf: Dispose? = null
    private var isDetach = false

    /**
     * 每个bus中，保存的dispatch需要分Page以及组件，目前需求的通信方式是，组件只能发action给Page，
     * 但是Page可以发送Action给所有的组件，所以这里区分一下
     */
    private var mPageDispatch: Dispatch? = null

    /**
     * 初始化依赖列表，为了防止事件关系混乱，这个创建过程由框架来维护
     */
    init {
        mChildList = ArrayList<Dispatch>()
    }

    /**
     * 目前的通信方式是：组件只能发action给Page，但是Page可以发送Action给所有的组件，所以这里区分一下
     *
     * @param dispatch Dispatch
     */
    fun setPageEffectDispatch(dispatch: Dispatch?) {
        mPageDispatch = dispatch
    }

    val pageDispatch: Dispatch?
        get() = mPageDispatch

    override fun attach(parent: IBus?) {
        if (parent !is DispatchBus) {
            return
        }
        mParent = parent
        mRemoveSelf?.let { it() }
        mRemoveSelf = parent.registerReceiver(this)
    }

    override fun detach() {
        isDetach = true
        mRemoveSelf?.let { it() }
    }

    override fun broadcast(action: Action<Any>) {
        if (isDetach) {
            return
        }
        var parent = mParent
        if (parent == null) {
            // 本身就在顶层
            innerBroadcast(action)
            return
        }

        // 查找最顶层
        while (parent!!.mParent != null) {
            parent = parent.mParent
        }
        parent.innerBroadcast(action)
    }

    /**
     * 设计这个方法的目的：希望广播只能由Page来接收，其他组件不能接收广播，
     * page接收之后可以通过拦截器的方式转给组件。
     */
    private fun innerBroadcast(action: Action<Any>) {
        if (mChildList.isEmpty()) {
            return
        }
        for (dispatch in mChildList) {
            if (dispatch !is DispatchBus) {
                continue
            }

            // 交给page来处理
            dispatch.mPageDispatch?.dispatch(action)
        }
    }

    override fun registerReceiver(dispatch: Dispatch): Dispose? {
        if (!isDetach) {
            mChildList.add(dispatch)
            return { mChildList.remove(dispatch) }
        }
        return null
    }

    override fun dispatch(action: Action<Any>) {
        dispatch(action, null)
    }

    fun dispatch(action: Action<Any>, exclude: Dispatch?) {
        if (isDetach) {
            return
        }
        for (dispatcher in mChildList) {
            if (dispatcher !== exclude) {
                dispatcher.dispatch(action)
            }
        }
    }
}