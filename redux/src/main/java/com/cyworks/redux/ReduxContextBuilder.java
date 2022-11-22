package com.cyworks.redux;

import com.tencent.redux.state.StateChangeForUI;
import com.tencent.redux.util.IPlatform;

/**
 * Desc: 通过建造者模式创建一个ReduxContext.
 *
 * @author randytu on 2020/12/15
 */
public final class ReduxContextBuilder<S extends State> {
    /**
     * 组件状态监听器
     */
    private StateChangeForUI<S> mStateChangeListener;

    /**
     * 组件实例
     */
    private Logic<S> mLogic;

    /**
     * 组件状态
     */
    private S mState;

    /**
     * 平台操作相关
     */
    private IPlatform mPlatform;

    public ReduxContextBuilder<S> setState(S state) {
        mState = state;
        return this;
    }

    public ReduxContextBuilder<S> setOnStateChangeListener(StateChangeForUI<S> onStateChange) {
        mStateChangeListener = onStateChange;
        return this;
    }

    public ReduxContextBuilder<S> setLogic(Logic<S> logic) {
        mLogic = logic;
        return this;
    }

    public ReduxContextBuilder<S> setPlatform(IPlatform platform) {
        mPlatform = platform;
        return this;
    }

    public StateChangeForUI<S> getStateChangeListener() {
        return mStateChangeListener;
    }

    public Logic<S> getLogic() {
        return mLogic;
    }

    public S getState() {
        return mState;
    }

    public IPlatform getPlatform() {
        return mPlatform;
    }

    /**
     * 创建一个ReduxContext实例
     *
     * @return ReduxContext
     */
    public ReduxContext<S> build() {
        return new ReduxContext<>(this);
    }
}
