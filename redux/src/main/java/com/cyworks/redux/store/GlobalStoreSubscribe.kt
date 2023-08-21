package com.cyworks.redux.store

import com.cyworks.redux.state.State
import com.cyworks.redux.types.Dispose
import com.cyworks.redux.types.IPropsChanged
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

/**
 * 全局Store观察器，会在初始化时收集全局store中的数据并绑定观察者。
 * todo 当单个组件移除的时候，要主动删除listener
 */
class GlobalStoreSubscribe<CS : State> internal constructor(callback: IPropsChanged, s: CS) {
    /**
     * 内部维护组件对全局store的依赖，用Any防止类型不匹配
     */
    private val globalStoreBinderMap = HashMap<GlobalStore<out State>, ICombineGlobalState<CS, Any>>()

    /**
     * 保存dispose，用于清理操作，这里单个组件可能依赖多个全局store
     */
    private val disposeList: MutableList<Dispose> = ArrayList()

    /**
     * 依赖全局store的组件对应的state
     */
    private val state: CS = s

    /**
     * 要注入的监听器
     */
    private val cb: IPropsChanged = callback

    /**
     * 通过watch方法来注入全局store依赖
     *
     * @param store 当前要关联的全局store
     * @param bind 传入具体的关联方法
     */
    @Suppress("UNCHECKED_CAST")
    fun subscribe(store: GlobalStore<out State>, bind: ICombineGlobalState<CS, out Any>) {
        globalStoreBinderMap[store] = bind as ICombineGlobalState<CS, Any>
    }

    /**
     * 生成对全局store的属性依赖
     */
    internal fun generateDependant() {
        if (globalStoreBinderMap.isEmpty()) {
            return
        }

        for (store in globalStoreBinderMap.keys) {
            val iBind = globalStoreBinderMap[store] ?: continue
            val globalStoreState : State = store.copyState()
            state.setTargetState(globalStoreState)
            iBind.combine(state, globalStoreState)
            val token: JvmType.Object = state.token
            if (globalStoreState.isTheStateDependGlobalState(token)) {
                batchStoreObserver(store, token)
            }
        }
    }

    /**
     * 绑定全局store的属性变化监听器
     * @param store 全局store
     * @param token 依赖了此全局Store的组件的token
     */
    private fun batchStoreObserver(store: GlobalStore<*>?, token: JvmType.Object?) {
        if (store == null || token == null) {
            return
        }
        val dispose: Dispose? = store.observe(StoreObserver(token, cb))
        if (dispose != null) {
            disposeList.add(dispose)
        }
    }

    fun clear() {
        if (disposeList.isEmpty()) {
            return
        }

        for (dispose in disposeList) {
            dispose()
        }
    }

    /**
     * 用于关联全局store属性的接口
     */
    interface ICombineGlobalState<C, G> {
        /**
         * 关联全局store属性
         * @param childState 当前组件对的state
         * @param globalState 全局store的state
         */
        fun combine(childState: C, globalState: G)
    }
}