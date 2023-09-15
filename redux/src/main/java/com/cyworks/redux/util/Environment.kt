package com.cyworks.redux.util

import android.view.View
import com.cyworks.redux.DispatchBus
import com.cyworks.redux.lifecycle.LifeCycleProxy
import com.cyworks.redux.state.ReflectTask
import com.cyworks.redux.state.ReflectTaskManager
import com.cyworks.redux.state.State
import com.cyworks.redux.store.Store
import com.cyworks.redux.types.Dispatch

/**
 * 保存当前组件所需要的一些环境信息
 */
class Environment private constructor() {
    /**
     * 页面LifeCycle 代理
     */
    internal var lifeCycleProxy: LifeCycleProxy? = null

    /**
     * 页面store
     */
    internal var store: Store<out State>? = null

    /**
     * 页面的Dispatch总线，只负责在页面内发送Effect Action
     */
    internal var pageDispatchBus: DispatchBus? = null

    /**
     * 当前组件对应的父组件的State
     */
    internal var parentState: State? = null

    /**
     * 当前组件对应的父组件的Effect Dispatch
     */
    internal var parentDispatch: Dispatch? = null

    /**
     * 当前组件的父View
     */
    internal var parentView: View? = null

    internal var taskManager: ReflectTaskManager? = null

    /**
     * 用于异步获取反射数据
     */
    internal var task: ReflectTask? = null

    /**
     * 做一些清理操作
     */
    fun clear() {
        if (pageDispatchBus != null) {
            pageDispatchBus!!.detach()
        }
        if (store != null) {
            store!!.clear()
        }
        lifeCycleProxy = null
        parentView = null
    }

    companion object {
        /**
         * 创建一个实例
         * @return Environment
         */
        fun of(): Environment {
            return Environment()
        }

        /**
         * 复制一些Env的公共信息
         */
        fun copy(env: Environment): Environment {
            val copy = Environment()
            copy.lifeCycleProxy = env.lifeCycleProxy
            copy.store = env.store
            copy.pageDispatchBus = env.pageDispatchBus
            copy.taskManager = env.taskManager
            return copy
        }
    }
}