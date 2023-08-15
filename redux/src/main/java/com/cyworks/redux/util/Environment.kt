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
    private var proxy: LifeCycleProxy? = null

    /**
     * 页面store
     */
    internal var store: Store<out State>? = null
        private set

    /**
     * 页面的Dispatch总线，只负责在页面内发送Effect Action
     */
    internal var dispatchBus: DispatchBus? = null
        private set

    /**
     * 当前组件对应的父组件的State
     */
    internal var parentState: State? = null
        private set

    /**
     * 当前页面的根View
     */
    internal var rootView: View? = null
        private set

    /**
     * 当前组件对应的父组件的Effect Dispatch
     */
    internal var parentDispatch: Dispatch? = null

    val lifeCycleProxy: LifeCycleProxy?
        get() = proxy

    fun setLifeCycleProxy(proxy: LifeCycleProxy): Environment {
        this.proxy = proxy
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
        this.parentDispatch = parentDispatch
        return this
    }

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
        proxy = null
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
            copy.proxy = env.proxy
            copy.store = env.store
            copy.dispatchBus = env.dispatchBus
            copy.rootView = env.rootView
            return copy
        }
    }
}