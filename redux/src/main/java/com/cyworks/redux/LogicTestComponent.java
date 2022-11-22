package com.cyworks.redux;

import android.support.annotation.NonNull;

import com.tencent.redux.action.Action;
import com.tencent.redux.dependant.DependentCollect;
import com.tencent.redux.lifecycle.LifeCycleAction;
import com.tencent.redux.lifecycle.LifeCycleProxy;
import com.tencent.redux.state.StateChangeForUI;

/**
 * Desc: 用于逻辑层测试的组件类
 *
 * 如果开发者只想测试页面逻辑，可以在测试期间使用此类来测试页面功能，
 * 而非像单测那种单点测试。
 *
 * @author randytu on 2021/7/4
 */
public abstract class LogicTestComponent<S extends BaseComponentState> extends LogicComponent<S> {
    /**
     * 是否已经安装到父组件上
     */
    protected boolean isBind;

    /**
     * 构造器，初始Reducer/Effect以及一些依赖
     */
    public LogicTestComponent() {
        super(null);
    }

    @Override
    protected final void onStateDetected(S componentState) { }

    @Override
    protected final StateChangeForUI<S> makeUIListener() {
        return null;
    }

    /**
     * 用于初始化Component
     */
    private void onCreate() {
        createContext();

        initSubComponent();

        mContext.onLifecycle(LifeCycleAction.ACTION_ON_CREATE);
    }

    @Override
    public final void install(@NonNull Environment environment,
                              @NonNull LRConnector<S, State> connector) {
        if (isBind) {
            return;
        }
        isBind = true;

        mConnector = connector;
        mEnvironment = environment;

        // 获取启动参数
        LifeCycleProxy lifeCycleProxy = mEnvironment.getLifeCycleProxy();
        mBundle = lifeCycleProxy.getBundle();

        // 转调
        onCreate();
    }

    /**
     * 添加一个子组件，需要在构造方法中调用
     */
    public void addSubComponent(@NonNull Dependant<? extends BaseComponentState, State> dependant) {
        if (mDependencies == null) {
            mDependencies = new DependentCollect<>();
        }

        mDependencies.addDependant(dependant);
    }

    /**
     * 测试期间，通过此方法触发生命周期切换
     * @param action Lifecycle Action
     */
    public void fireLifeAction(Action action) {
        mContext.onLifecycle(action);
    }

}
