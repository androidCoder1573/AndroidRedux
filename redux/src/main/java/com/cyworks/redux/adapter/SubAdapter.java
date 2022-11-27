package com.cyworks.redux.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import com.tencent.redux.BaseComponentState;
import com.tencent.redux.action.Action;

/**
 * Desc: 如果RecyclerView存在多层嵌套，那么这里其实还是需要子Adapter，子Adapter必须存在一个父亲
 */
public abstract class SubAdapter implements IAdapter {
    /**
     * 当前RecyclerView对应的Adapter
     */
    protected RecyclerView.Adapter<RecyclerView.ViewHolder> mAdapter;

    /**
     * 如果存在父亲，则所有的交互都依赖RootAdapter, 否则自己是Root
     */
    protected IAdapter mParentAdapter;

    /**
     * 构造器，SubAdapter构造时需要传递parent。
     * @param parentAdapter BaseAdapter
     */
    public SubAdapter(@NonNull IAdapter parentAdapter) {
        mParentAdapter = parentAdapter;
    }

    /**
     * 获取根Adapter, 主要用于二级或者更多级列表发送Action
     * @return RootAdapter
     */
    @SuppressWarnings("unchecked")
     private RootAdapter<BaseComponentState> getRootAdapter() {
        IAdapter adapter = mParentAdapter;
        if (adapter == null) {
            return null;
        }

        if (mParentAdapter instanceof RootAdapter) {
            return (RootAdapter) mParentAdapter;
        }

        while (adapter != null) {
            if (adapter instanceof RootAdapter) {
                return (RootAdapter) adapter;
            } else if (adapter instanceof SubAdapter) {
                adapter = ((SubAdapter)adapter).mParentAdapter;
            }
        }

        return null;
    }

    @Override
    public final void dispatchReducer(Action action, Object payload) {
        RootAdapter adapter = getRootAdapter();

        if (adapter != null) {
            adapter.dispatchReducer(action, payload);
        }
    }

    @Override
    public final void dispatchEffect(Action action, Object payload) {
        RootAdapter adapter = getRootAdapter();

        if (adapter != null) {
            adapter.dispatchEffect(action, payload);
        }
    }
}
