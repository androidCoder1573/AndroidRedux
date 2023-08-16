package com.cyworks.redux.store

import com.cyworks.redux.state.State
import com.cyworks.redux.types.Dispose
import com.cyworks.redux.types.IPropsChanged

/**
 * 全局Store观察器，会在初始化时收集全局store中的数据并绑定观察者。
 */
class GlobalStoreSubscribe<CS : State> internal constructor(callback: IPropsChanged, state: CS) {
    /**
     * 内部维护组件对全局store的依赖，减少开发者的工作量
     */
    private val globalStoreBinderMap = HashMap<GlobalStore<State>, ICombineGlobalState<CS, State>>()

    /**
     * 保存dispose，用于清理操作
     */
    private val disposeList: MutableList<Dispose> = ArrayList()

    /**
     * 依赖全局store的组件对应的state
     */
    private val state: CS

    /**
     * 要注入的监听器
     */
    private val cb: IPropsChanged

    init {
        cb = callback
        this.state = state
    }

    /**
     * 通过watch方法来注入全局store依赖
     *
     * @param store 当前要关联的全局store
     * @param bind 传入具体的关联方法
     */
    fun subscribe(store: GlobalStore<State>, bind: ICombineGlobalState<CS, State>) {
        globalStoreBinderMap[store] = bind
    }

    /**
     * 生成对全局store的属性依赖
     */
    fun generateDependant() {
        if (globalStoreBinderMap.isEmpty()) {
            return
        }

        for (store in globalStoreBinderMap.keys) {
            val iBind = globalStoreBinderMap[store] ?: continue
            val globalStoreState = store.copyState()
            iBind.combine(state, store, globalStoreState)
            val token: String = state.javaClass.name
            if (globalStoreState.isDependGlobalState(token)) {
                batchStoreObserver(store, token)
            }
        }
    }

    /**
     * 邦迪等全局store的属性变化监听器
     * @param store 全局store
     * @param token 依赖了此全局Store的组件的token
     */
    private fun batchStoreObserver(store: GlobalStore<*>?, token: String?) {
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
    interface ICombineGlobalState<CS : State, GS : State> {
        /**
         * 关联全局store属性
         * @param childState 当前组件对的state
         * @param store 依赖的全局store
         * @param globalState 全局store的state
         */
        fun combine(childState: CS, store: GlobalStore<State>, globalState: GS)
    }
}