package com.cyworks.redux

import com.tencent.redux.dispose.IDispose
import java.util.ArrayList
import java.util.HashMap

/**
 * Desc: 全局Store观察器，会在初始化时收集全局store中的数据并绑定观察者。
 */
class GlobalStoreWatcher<CS : BaseComponentState?> internal constructor(
    callback: IPropsChanged,
    state: CS
) {
    /**
     * 内部维护组件对全局store的依赖，减少开发者的工作量
     */
    private val mMap = HashMap<BaseGlobalStore<*>, IBindGlobalState<CS, State?>>()

    /**
     * 保存dispose，用于清理操作
     */
    private val mGlobalStoreWatcherDisposeList: MutableList<IDispose?> = ArrayList<IDispose?>()

    /**
     * 依赖全局store的组件对应的state
     */
    private val mState: CS

    /**
     * 要注入的监听器
     */
    private val mCb: IPropsChanged

    /**
     * 通过watch方法来注入全局store依赖
     *
     * @param store 当前要关联的全局store
     * @param binder 传入具体的关联方法
     */
    fun watch(store: BaseGlobalStore<*>, binder: IBindGlobalState<CS, State?>) {
        mMap[store] = binder
    }

    /**
     * 生成对全局store的属性依赖
     */
    fun generateDependant() {
        if (mMap.isEmpty()) {
            return
        }
        for (store in mMap.keys) {
            val iBind = mMap[store] ?: continue
            val globalStoreState = store.state
            iBind.bind(mState, store, globalStoreState)
            val token: String = mState.getClass().getName()
            if (globalStoreState!!.isDependState(token)) {
                batchStoreObserver(store, token)
            }
        }
    }

    /**
     * 邦迪等全局store的属性变化监听器
     * @param store 全局store
     * @param token 依赖了此全局Store的组件的token
     */
    private fun batchStoreObserver(store: BaseGlobalStore<*>?, token: String?) {
        if (store == null || token == null) {
            return
        }
        val dispose: IDispose? = store.observe(StoreObserver(mCb, token))
        if (dispose != null) {
            mGlobalStoreWatcherDisposeList.add(dispose)
        }
    }

    fun clear() {
        if (mGlobalStoreWatcherDisposeList.isEmpty()) {
            return
        }
        for (dispose in mGlobalStoreWatcherDisposeList) {
            dispose.dispose()
        }
    }

    /**
     * 用于关联全局store属性的接口
     */
    interface IBindGlobalState<CS : BaseComponentState?, GS : State?> {
        /**
         * 关联全局store属性
         * @param childState 当前组件对的state
         * @param store 依赖的全局store
         * @param globalState 全局store的state
         */
        fun bind(childState: CS, store: BaseGlobalStore<*>?, globalState: GS)
    }

    /**
     * 初始化全局Store watcher
     *
     * @param callback 组件State变化的callback
     * @param state GlobalStoreWatcher绑定的组件的State
     */
    init {
        mCb = callback
        mState = state
    }
}