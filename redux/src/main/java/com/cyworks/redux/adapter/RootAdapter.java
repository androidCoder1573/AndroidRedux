package com.cyworks.redux.adapter;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import com.tencent.redux.BaseComponentState;
import com.tencent.redux.BaseState;
import com.tencent.redux.Environment;
import com.tencent.redux.LRConnector;
import com.tencent.redux.LogicComponent;
import com.tencent.redux.ReduxContext;
import com.tencent.redux.action.Action;
import com.tencent.redux.prop.ChangedState;
import com.tencent.redux.state.StateChangeForUI;

/**
 * Desc: 组件的根Adapter，一个组件仅能有一个根Adapter。
 * <p></p>
 *
 * 如果Adapter是一个逻辑组件，作为每个组件的依附，每个组件仅有一个Adapter类型的逻辑组件。
 * 逻辑组件可通过context获得，从逻辑组件中可以取得实际的Adapter。
 * adapter内部会安装ViewHolder，如果每个ViewHolder被封装成一个真正的Feature，那么实现起来太重。
 * <p></p>
 *
 * 思路：只有RootAdapter具有操作State的能力。
 * 如果当前Adapter具有子Adapter，需要将父Adapter设置进去，触发Reducer的操作最终都会去更新根Adapter。
 *
 * 框架不再提供具体的Adapter实现，将具体的Adapter实现跟框架分离开来，
 * 开发者可以注入自己的列表Adapter实现，对开发者来说，实现起来更灵活，框架的外部依赖也会减少。
 */
public abstract class RootAdapter<S extends BaseComponentState> extends LogicComponent<S>
        implements IAdapter {
    /**
     * 当前RecyclerView对应的Adapter
     */
    protected RecyclerView.Adapter<RecyclerView.ViewHolder> mAdapter;

    /**
     * LiveData包装ChangedState，用于通知界面更新
     */
    private MutableLiveData<ChangedState<S>> mLiveData;

    /**
     * 是否已经bind到父组件上
     */
    private boolean isBind;

    /**
     * 当前UI是否展示
     */
    private boolean isUIShow;

    /**
     * 用于观察LiveData
     */
    private Observer<ChangedState<S>> mObserver;

    /**
     * 构造器，
     * 外部可能需要对Adapter进行定制，所以将Adapter作为参数传入
     */
    public RootAdapter(@NonNull RecyclerView.Adapter<RecyclerView.ViewHolder> adapter) {
        super(null);
        mAdapter = adapter;
    }

    private void initLiveData() {
        mLiveData = new MutableLiveData<>();
        LifecycleOwner owner = mEnvironment.getLifeCycleProxy().getLifecycleOwner();
        mObserver = this::onListChanged;
        mLiveData.observe(owner, mObserver);
    }

    @Override
    protected StateChangeForUI<S> makeUIListener() {
        return (state, changedProps) -> {
            if (!isUIShow) {
                return;
            }

            ChangedState<S> stateCompare = new ChangedState<>();
            stateCompare.mState = state;
            stateCompare.mChangedProps = changedProps;
            mLiveData.setValue(stateCompare);
        };
    }

    @Override
    public final void dispatchReducer(Action action, Object payload) {
        mContext.dispatchReducer(action, payload);
    }

    @Override
    public final void dispatchEffect(Action action, Object payload) {
        mContext.dispatchEffect(action, payload);
    }

    public final void attach() {
        isUIShow = true;
    }

    public final void detach() {
        isUIShow = false;
    }

    @Override
    public final void install(@NonNull Environment environment,
                        @NonNull LRConnector<S, BaseState> connector) {
        if (isBind) {
            return;
        }
        isBind = true;

        mEnvironment = environment;
        mBundle = mEnvironment.getLifeCycleProxy().getBundle();
        mConnector = connector;

        initLiveData();
        createContext();

        isUIShow = true;
    }

    public ReduxContext<S> getReduxContext() {
        return mContext;
    }

    /**
     * 返回当前列表真正的Adapter，RootAdapter只是对真实的Adapter进行了一次包装
     */
    public final RecyclerView.Adapter<RecyclerView.ViewHolder> getRealAdapter() {
        return mAdapter;
    }

    /**
     * 当列表的数据发生改变时，通过此接口通知给开发者。
     * 目的：将数据改变的逻辑放在外部，让开发者自己决定列表的diff方式。
     * @param changedState 当前改变的属性 {@link ChangedState}
     */
    public abstract void onListChanged(ChangedState<S> changedState);

    @Override
    @CallSuper
    public void clear() {
        super.clear();
        mLiveData.removeObserver(mObserver);
        mObserver = null;

        if (mContext != null) {
            mContext.destroy();
        }
        mEnvironment = null;
    }

}
