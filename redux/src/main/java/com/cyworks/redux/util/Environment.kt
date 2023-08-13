package com.cyworks.redux.util

import android.view.View
import com.cyworks.redux.DispatchBus
import com.cyworks.redux.lifecycle.LifeCycleProxy
import com.cyworks.redux.state.State
import com.cyworks.redux.store.Store
import com.cyworks.redux.types.Dispatch

/**
 * Desc: 保存当前组件所需要的一些环境信息
 */
class Environment private constructor() {
    /**
     * 页面LifeCycle 代理
     */
    private var mProxy: LifeCycleProxy? = null

    /**
     * 页面store
     */
    var store: Store<out State>? = null
        private set

    /**
     * 页面的Dispatch总线，只负责在页面内发送Effect Action
     */
    var dispatchBus: DispatchBus? = null
        private set

    /**
     * 当前组件对应的父组件的State
     */
    var parentState: State? = null
        private set

    /**
     * 当前页面的根View
     */
    var rootView: View? = null
        private set

    /**
     * 当前组件对应的父组件的Effect Dispatch
     */
    private var mParentDispatch: Dispatch? = null

    fun setLifeCycleProxy(proxy: LifeCycleProxy): Environment {
        mProxy = proxy
        return this
    }

    fun setStore(store: Store<out State>): Environment {
        this.store = store
        return this
    }

    fun setDispatchBus(bus: DispatchBus): Environment {
        dispatchBus = bus
        return this
    }

    fun setRootView(rootView: View): Environment {
        this.rootView = rootView
        return this
    }

    fun setParentState(parentState: State): Environment {
        this.parentState = parentState
        return this
    }

    fun setParentDispatch(parentDispatch: Dispatch): Environment {
        mParentDispatch = parentDispatch
        return this
    }

    val lifeCycleProxy: LifeCycleProxy?
        get() = mProxy

    val parentDispatch: Dispatch?
        get() = mParentDispatch

    /**
     * 做一些清理操作
     */
    fun clear() {
        if (dispatchBus != null) {
            dispatchBus!!.detach()
        }
        if (store != null) {
            store!!.clear()
        }
        mProxy = null
        rootView = null
    }

    companion object {
        /**
         * 创建一个实例
         * @return Environment
         */
        @JvmStatic
        fun of(): Environment {
            return Environment()
        }

        /**
         * 复制一些公用的信息
         * @param env 父组件的LRParent
         * @return Environment
         */
        @JvmStatic
        fun copy(env: Environment): Environment {
            val copy = Environment()
            copy.mProxy = env.mProxy
            copy.store = env.store
            copy.dispatchBus = env.dispatchBus
            copy.rootView = env.rootView
            return copy
        }
    }
}