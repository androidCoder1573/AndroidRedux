package com.cyworks.redux;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.View;

import com.tencent.redux.action.Action;
import com.tencent.redux.activityresult.ReceiveResultFragment.ActivityResultCallback;
import com.tencent.redux.effect.Effect;
import com.tencent.redux.effect.EffectCollect;
import com.tencent.redux.permission.PermissionCallback;
import com.tencent.redux.reducer.PureReducer;
import com.tencent.redux.util.IPlatform;

/**
 * Desc: 默认的逻辑控制类，开发者继承此类用于实现
 *
 * @author randytu on 2021/10/19
 */
public class BaseController<S extends State> {
    /**
     * ReduxContext, 用于更改状态以及组件间交互
     */
    protected ReduxContext<S> mReduxContext;

    /**
     * 当前容器下(Page)的Android context
     */
    protected Context mContext;

    /**
     * 当前容器下(Page)的Android activity
     */
    protected Activity mActivity;

    /**
     * effect 收集器，用于组件间交互
     */
    private final EffectCollect<S> mEffectCollect = new EffectCollect<>();

    public BaseController() {
    }

    /**
     * 设置ReduxContext，并初始化部分成员
     * @param reduxContext ReduxContext
     */
    public final void setReduxContext(@NonNull ReduxContext<S> reduxContext) {
        mReduxContext = reduxContext;

        IPlatform platform = getPlatform();
        if (platform != null) {
            mContext = platform.getContext();
            mActivity = platform.getActivity();
        }
    }

    /**
     * 订阅action
     * @param action action
     * @param effect Effect
     */
    public final void observeAction(Action action, Effect<S> effect) {
        mEffectCollect.add(action, effect);
    }

    public final void closePage() {
        IPlatform platform = getPlatform();
        if (platform != null) {
            platform.closePage();
        }
    }

    public final void startActivity(Intent intent) {
        IPlatform platform = getPlatform();
        if (platform != null) {
            platform.startActivity(intent);
        }
    }

    public final void startActivityForResult(Intent intent, int requestCode,
            ActivityResultCallback callback) {
        IPlatform platform = getPlatform();
        if (platform != null) {
            platform.startActivityForResult(intent, requestCode, callback);
        }
    }

    public final void requestPermission(int requestCode, PermissionCallback callback,
            String... permissions) {
        IPlatform platform = getPlatform();
        if (platform != null) {
            platform.requestPermission(requestCode, callback, permissions);
        }
    }

    public final void updateState(PureReducer<S> pureReducer) {
        if (mReduxContext == null || pureReducer == null) {
            return;
        }

        mReduxContext.updateState(pureReducer);
    }

    public int getViewContainerIdForV() {
        IPlatform platform = getPlatform();
        if (platform == null) {
            return -1;
        }

        return platform.getViewContainerIdForV();
    }

    public int getViewContainerIdForH() {
        IPlatform platform = getPlatform();
        if (platform == null) {
            return -1;
        }

        return platform.getViewContainerIdForH();
    }

    public final View inflateStub(@IdRes int viewContainerId, @LayoutRes int layoutId) {
        IPlatform platform = getPlatform();
        if (platform == null) {
            return null;
        }

        return platform.inflateStub(viewContainerId, layoutId);
    }

    private IPlatform getPlatform() {
        if (mReduxContext == null) {
            return null;
        }

        return mReduxContext.getPlatform();
    }

    public void doAction(Action action, Object payload) {
        mEffectCollect.doAction(action, mReduxContext, payload);
    }
}
