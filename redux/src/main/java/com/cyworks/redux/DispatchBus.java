package com.cyworks.redux;

import android.support.annotation.NonNull;
import com.tencent.redux.action.Action;
import com.tencent.redux.dispatcher.Dispatch;
import com.tencent.redux.dispatcher.IBus;
import com.tencent.redux.dispose.IDispose;
import java.util.ArrayList;
import java.util.List;

/**
 * Desc: 用于分发Effect，目的：扩展Effect在组件间/页面间交互的能力。
 *
 * reudx bus的设计思路：每个bus都有共同的父亲，父亲会保存多个孩子。
 * 每个页面只有一个DispatchBus。
 * reudx bus理论上是一个二级结构，因为整个APP的结构是全局Store + Page，理论上没有三级Page
 *
 * @author randytu on 2020/6/5
 */
public final class DispatchBus implements IBus {
    /**
     * 保存当前bus的父亲
     */
    private DispatchBus mParent;

    /**
     * 保存当前bus的孩子，根bus会持有多个孩子
     */
    private final List<Dispatch> mChildList;

    /**
     * 删除添加的某个DispatchBus
     */
    private IDispose mRemoveSelf;

    private boolean isDetach;

    /**
     * 每个bus中，保存的dispatch需要分Page以及组件，目前需求的通信方式是，组件只能发action给Page，
     * 但是Page可以发送Action给所有的组件，所以这里区分一下
     */
    private Dispatch mPageDispatch;

    /**
     * 初始化依赖列表，为了防止事件关系混乱，这个创建过程由框架来维护
     */
    DispatchBus() {
        mChildList = new ArrayList<>();
    }

    /**
     * 目前的通信方式是：组件只能发action给Page，但是Page可以发送Action给所有的组件，所以这里区分一下
     *
     * @param dispatch Dispatch
     */
    final void setPageEffectDispatch(Dispatch dispatch) {
        mPageDispatch = dispatch;
    }

    Dispatch getPageDispatch() {
        return mPageDispatch;
    }

    @Override
    public void attach(@NonNull IBus parent) {
        if (!(parent instanceof DispatchBus)) {
            return;
        }

        mParent = (DispatchBus) parent;
        if (mRemoveSelf != null) {
            mRemoveSelf.dispose();
        }
        mRemoveSelf = parent.registerReceiver(this);
    }

    @Override
    public void detach() {
        isDetach = true;
        if (mRemoveSelf != null) {
            mRemoveSelf.dispose();
        }
    }

    @Override
    public void broadcast(Action action, Object payload) {
        if (isDetach) {
            return;
        }

        DispatchBus parent = mParent;
        if (parent == null) {
            // 本身就在顶层
            innerBroadcast(action, payload);
            return;
        }

        // 查找最顶层
        while (parent.mParent != null) {
            parent = parent.mParent;
        }
        parent.innerBroadcast(action, payload);
    }

    /**
     * 设计这个方法的目的：希望广播只能由Page来接收，其他组件不能接收广播，
     * page接收之后可以通过拦截器的方式转给组件。
     *
     * @param action Action
     * @param payload 参数
     */
    private void innerBroadcast(Action action, Object payload) {
        if (mChildList.isEmpty()) {
            return;
        }

        for (Dispatch dispatch : mChildList) {
            if (!(dispatch instanceof DispatchBus)) {
                continue;
            }

            DispatchBus dispatchBus = (DispatchBus) dispatch;

            // 交给page来处理
            if (dispatchBus.mPageDispatch != null) {
                dispatchBus.mPageDispatch.dispatch(action, payload);
            }
        }
    }

    @Override
    public IDispose registerReceiver(final Dispatch dispatch) {
        if (!isDetach && dispatch != null) {
            mChildList.add(dispatch);

            return () -> mChildList.remove(dispatch);
        }

        return null;
    }

    public void dispatch(Action action, Object payload, Dispatch exclude) {
        if (isDetach) {
            return;
        }

        for (Dispatch dispatch : mChildList) {
            if (dispatch != exclude) {
                dispatch.dispatch(action, payload);
            }
        }
    }

    @Override
    public void dispatch(Action action, Object payload) {
        dispatch(action, payload, null);
    }
}
