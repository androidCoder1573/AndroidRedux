package com.cyworks.redux;

import com.tencent.redux.action.Action;
import com.tencent.redux.interceptor.Interceptor;
import com.tencent.redux.prop.LocalPropsChanged;
import com.tencent.redux.reducer.Reducer;
import com.tencent.redux.reducer.SubReducer;
import com.tencent.redux.state.StateGetter;
import java.util.HashMap;
import java.util.List;

/**
 * Desc: 用于连接子组件和父组件，做几件事情：
 * 1、负责依赖PageState中的数据
 * 2、负责将子组件的Reducer生成SubReducer
 * <p></p>
 *
 * S：当前组件的State类型；
 * PS：父组件的State类型；
 *
 * @author randytu on 2020/8/2
 */
public abstract class LRConnector<S extends BaseComponentState, PS extends State> {
    /**
     * 当前Connector对应的子组件的状态获取接口, 使用接口的形式延迟获取state
     */
    private StateGetter<S> mGetter;

    /**
     * 当前Connector对应的子组件的私有属性变化监听器
     */
    private LocalPropsChanged mSelfPropsChanged;

    /**
     * 当前组件关联的父组件的State
     */
    private State mParentState;

    void setParentState(State parentState) {
        mParentState = parentState;
    }

    State getParentState() {
        return mParentState;
    }

    /**
     * 依赖Parent组件的属性，外部配置。
     */
    public abstract void parentStateCollector(S childState, PS parentState);

    /**
     * 通过此接口依赖全局store, 集合，无法区分泛型
     */
    public abstract void globalStateCollector(GlobalStoreWatcher<S> watcher);

    /**
     * 通过此接口配置与外界通信的interceptor
     *
     * @return InterceptorCollect
     */
    protected HashMap<Action, Interceptor<S>> interceptorCollector() {
        return null;
    }

    /**
     * 通过此接口获取View的占坑id或者对话框这类的布局id
     *
     * @return int
     */
    protected int getViewContainerIdForV() {
        return -1;
    };

    /**
     * 通过此接口获取View的占坑id或者对话框这类的布局id
     *
     * @return int
     */
    protected int getViewContainerIdForH() {
        return -1;
    }

    /**
     * 生成子组件对应的SubReducer。
     * note： 如果在子组件中修改全局store的状态，将会抛出RuntimeException
     *
     * @param reducer Reducer {@link Reducer}
     * @return SubReducer {@link SubReducer}
     */
    @SuppressWarnings("unchecked")
    final SubReducer subReducer(final Reducer reducer) {
        return (action, payload) -> {
            final S newProps = (S) reducer.doAction(mGetter, action, payload);

            if (newProps == null) {
                return null;
            }

            // 获取私有属性变化，并本地更新
            List<ReactiveProp<Object>> privateProps = newProps.getPrivatePropChanged();
            if (privateProps != null) {
                mSelfPropsChanged.onLocalPropsChanged(privateProps);
            }

            return newProps;
        };
    }

    /**
     * 注入子组件State获取接口
     *
     * @param getter SelfStateGetter {@link StateGetter}
     */
    final void injectGetter(StateGetter<S> getter) {
        mGetter = getter;
    }

    /**
     * 注入子组件私有属性变化监听器
     *
     * @param cb LocalPropsChanged {@link LocalPropsChanged}
     */
    final void injectLocalStateChangeCb(LocalPropsChanged cb) {
        mSelfPropsChanged = cb;
    }
}
