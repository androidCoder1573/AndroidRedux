package com.cyworks.redux;

import com.tencent.redux.dispose.IDispose;
import com.tencent.redux.prop.IPropsChanged;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Desc: 全局Store观察器，会在初始化时收集全局store中的数据并绑定观察者。
 *
 * @author randytu on 2020/12/13
 */
public class GlobalStoreWatcher<CS extends BaseComponentState> {
    /**
     * 内部维护组件对全局store的依赖，减少开发者的工作量
     */
    private final HashMap<BaseGlobalStore, IBindGlobalState<CS, State>> mMap = new HashMap<>();

    /**
     * 保存dispose，用于清理操作
     */
    private final List<IDispose> mGlobalStoreWatcherDisposeList = new ArrayList<>();

    /**
     * 依赖全局store的组件对应的state
     */
    private final CS mState;

    /**
     * 要注入的监听器
     */
    private final IPropsChanged mCb;

    /**
     * 初始化全局Store watcher
     *
     * @param callback 组件State变化的callback
     * @param state GlobalStoreWatcher绑定的组件的State
     */
    GlobalStoreWatcher(IPropsChanged callback, CS state) {
        mCb = callback;
        mState = state;
    }

    /**
     * 通过watch方法来注入全局store依赖
     *
     * @param store 当前要关联的全局store
     * @param binder 传入具体的关联方法
     */
    public void watch(BaseGlobalStore store, IBindGlobalState<CS, State> binder) {
        mMap.put(store, binder);
    }

    /**
     * 生成对全局store的属性依赖
     */
    void generateDependant() {
        if (mMap.isEmpty()) {
            return;
        }

        for (BaseGlobalStore store : mMap.keySet()) {
            IBindGlobalState<CS, State> iBind = mMap.get(store);

            if (iBind == null) {
                continue;
            }

            State globalStoreState = store.getState();
            iBind.bind(mState, store, globalStoreState);

            String token = mState.getClass().getName();
            if (globalStoreState.isDependState(token)) {
                batchStoreObserver(store, token);
            }
        }
    }

    /**
     * 邦迪等全局store的属性变化监听器
     * @param store 全局store
     * @param token 依赖了此全局Store的组件的token
     */
    private void batchStoreObserver(BaseGlobalStore store, String token) {
        if (store == null || token == null) {
            return;
        }

        IDispose dispose = store.observe(new StoreObserver(mCb, token));
        if (dispose != null) {
            mGlobalStoreWatcherDisposeList.add(dispose);
        }
    }

    void clear() {
        if (mGlobalStoreWatcherDisposeList.isEmpty()) {
            return;
        }

        for (IDispose dispose : mGlobalStoreWatcherDisposeList) {
            dispose.dispose();
        }
    }

    /**
     * 用于关联全局store属性的接口
     */
    public interface IBindGlobalState<CS extends BaseComponentState, GS extends State> {

        /**
         * 关联全局store属性
         * @param childState 当前组件对的state
         * @param store 依赖的全局store
         * @param globalState 全局store的state
         */
        void bind(CS childState, BaseGlobalStore store, GS globalState);
    }
}
