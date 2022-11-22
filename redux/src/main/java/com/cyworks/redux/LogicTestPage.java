package com.cyworks.redux;

import android.support.annotation.NonNull;

import com.tencent.redux.action.Action;
import com.tencent.redux.lifecycle.LifeCycleProxy;

import java.util.HashMap;

/**
 * Desc: 用于逻辑层测试的页面类
 *
 * 如果开发者只想测试页面逻辑，可以在测试期间使用此类来测试页面功能，
 * 而非像单测那种单点测试。
 *
 * @author randytu on 2021/7/4
 */
public abstract class LogicTestPage<S extends BasePageState> extends LogicPage<S> {
    /**
     * 构造器，初始Reducer/Effect以及一些依赖
     *
     * @param proxy LifeCycleProxy
     */
    public LogicTestPage(@NonNull LifeCycleProxy proxy) {
        super(proxy);
        onCreate();
    }

    @Override
    protected final void onStateDetected(S state) { }

    @Override
    public final void onCreate() {
        super.onCreate();
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, State> getAllState() {
        PageStore<S> store = (PageStore<S>) mEnvironment.getStore();
        return store.getAllState();
    }

    /**
     * 测试期间，通过此方法触发生命周期切换
     * @param action Lifecycle Action
     */
    public void fireLifeAction(Action action) {
        mContext.onLifecycle(action);
    }
}
