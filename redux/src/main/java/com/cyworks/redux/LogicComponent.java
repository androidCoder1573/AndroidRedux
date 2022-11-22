package com.cyworks.redux;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import com.tencent.redux.action.InnerActions;
import com.tencent.redux.dependant.DependentCollect;
import com.tencent.redux.effect.EffectCollect;
import com.tencent.redux.prop.IPropsChanged;
import com.tencent.redux.reducer.ReducerCollect;
import com.tencent.redux.reducer.SubReducer;
import com.tencent.redux.state.StateChangeForUI;
import com.tencent.redux.util.IPlatform;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Desc: Live-Redux框架是一个UI/逻辑完全分离的框架，LogicComponent内只针对状态管理，没有任何逻辑操作。
 *
 * 目的：实现逻辑层测试，剥离出UI之后更容易构建整个逻辑层测试。
 *
 * @author randytu on 2021/6/27
 */
public abstract class LogicComponent<S extends BaseComponentState> extends Logic<S> {
    /**
     * 当前组件的与页面的连接器
     */
    protected LRConnector<S, State> mConnector;

    /**
     * 用于观察全局store
     */
    private GlobalStoreWatcher<S> mGlobalStoreWatcher;

    /**
     * 组件的依赖的子组件的集合
     */
    public DependentCollect<State> mDependencies;

    /**
     * 用于取消异步任务
     */
    private Future<?> mFuture;

    /**
     * 构造器，初始Reducer/Effect以及一些依赖
     *
     * @param bundle 页面传递下来的参数
     */
    public LogicComponent(Bundle bundle) {
        super(bundle);
        if (mDependencies == null) {
            mDependencies = new DependentCollect<>();
        }
        addDependencies(mDependencies);
    }

    @Override
    public final void mergeReducer(@NonNull List<SubReducer> list, LRConnector connector) {
        super.mergeReducer(list, connector);
        mDependencies.mergerDependantReducer(list);
    }

    /**
     * 增加组件的依赖，子类如果有子组件，需要实现此方法
     * @param collect DependentCollect
     */
    protected void addDependencies(DependentCollect<State> collect) {
        // sub class impl
    }

    /**
     * 合并State，主要是合并两种State：
     * 1、合并父组件的属性；
     * 2、合并全局的State的属性。
     *
     * @param state 当前组件的State
     */
    private void mergeState(S state, IPropsChanged cb) {
        State parentState = mConnector.getParentState();

        // 关联框架内部数据
        if (parentState instanceof BaseComponentState) {
            state.mCurrentOrientation.dependantProp(
                    ((BaseComponentState) parentState).mCurrentOrientation);
        } else if (parentState instanceof BasePageState) {
            state.mCurrentOrientation.dependantProp(
                    ((BasePageState) parentState).mCurrentOrientation);
        }

        // 生成依赖的属性
        mConnector.parentStateCollector(state, parentState);

        // 创建全局store监听器
        mGlobalStoreWatcher = new GlobalStoreWatcher<>(cb, state);

        // 绑定全局store的state中的属性
        mConnector.globalStateCollector(mGlobalStoreWatcher);
        mGlobalStoreWatcher.generateDependant();
    }

    /**
     * 创建平台相关操作
     * @return IPlatform
     */
    protected IPlatform createPlatform() {
        return null;
    }

    /**
     * UI 组件实现这个方法, 返回UI更新器
     */
    protected StateChangeForUI<S> makeUIListener() {
        return null;
    }

    @CallSuper
    protected void createContext() {
        if (ReduxManager.getInstance().getAsyncMode()) {
            createContextAsync();
        } else {
            createContextSync();
        }
    }

    private void createContextSync() {
        // 生成初始State
        final S componentState = onCreateState(mBundle);

        // 生成内部的Key映射表
        componentState.detectField();

        // 合并page State以及global State
        mergeState(componentState, props -> {
            if (mContext != null) {
                mContext.onStateChange(props);
            }
        });

        // 负责处理额外的事情
        onStateDetected(componentState);

        // 创建Context
        mContext = new ReduxContextBuilder<S>()
                .setLogic(this)
                .setState(componentState)
                .setOnStateChangeListener(makeUIListener())
                .setPlatform(createPlatform())
                .build();
        mContext.setController(getController());
        mContext.setStateReady();
    }

    /**
     * 异步模式，对性能极致需求
     */
    private void createContextAsync() {
        // 生成初始State
        final S componentState = onCreateState(mBundle);

        final Runnable detectFinishRunnable = () -> {
            mContext.setStateReady();
            // 负责处理额外的事情
            onStateDetected(componentState);
        };

        final Runnable detectRunnable = () -> {
            componentState.detectField();

            // 合并page State以及global State
            mergeState(componentState, props -> {
                if (mContext != null) {
                    mContext.onStateChange(props);
                }
            });

            // 提交到主线程
            ReduxManager.getInstance().submitInMainThread(detectFinishRunnable);
        };

        // 检测以及merge state
        mFuture = ReduxManager.getInstance().submitInSubThread(detectRunnable);

        // 创建Context
        mContext = new ReduxContextBuilder<S>()
                .setLogic(this)
                .setState(componentState)
                .setOnStateChangeListener(makeUIListener())
                .setPlatform(createPlatform())
                .build();
        mContext.setController(getController());
    }

    /**
     *  如果是UI组件，可能要设置额外的信息，比如组件是否可见，当前屏幕方向等
     * @param componentState BaseState
     */
    protected void onStateDetected(S componentState) {
        // impl sub class
    }

    protected BaseController<S> getController() {
        return null;
    }

    /**
     * 每个组件下可能也会挂子组件，通过此方法初始化组件下挂载的子组件
     */
    void initSubComponent() {
        if (mDependencies == null) {
            return;
        }

        HashMap<String, Dependant<? extends BaseComponentState, State>> map =
                mDependencies.getDependantMap();
        if (map == null || map.isEmpty()) {
            return;
        }

        Environment env = Environment.copy(mEnvironment);
        env.setParentState(mContext.getState())
           .setParentDispatch(mContext.getEffectDispatch());

        for (Dependant<? extends BaseComponentState, State> dependant : map.values()) {
            if (dependant != null) {
                dependant.initComponent(env);
            }
        }
    }

    /**
     * 如果组件有列表型的UI，通过绑定框架提供的Adapter，这样列表型组件也可以纳入状态管理数据流中；
     * 通过此方法初始化Adapter，每个组件只可绑定一个Adapter，以保证组件的粒度可控。
     */
    void initAdapter() {
        if (mDependencies == null) {
            return;
        }

        Dependant<? extends BaseComponentState, State> dependant =
                mDependencies.getAdapterDependant();

        if (dependant != null) {
            Environment env = Environment.copy(mEnvironment);
            env.setParentState(mContext.getState())
                    .setParentDispatch(mContext.getEffectDispatch());
            dependant.initAdapter(env);
        }
    }

    /**
     * 获取依赖的子组件集合
     *
     * @return Map 子组件集合
     */
    HashMap<String, Dependant<? extends BaseComponentState, State>> getChildrenDependant() {
        if (mDependencies == null) {
            return null;
        }

        return mDependencies.getDependantMap();
    }

    /**
     * 获取依赖的列表组件的Adapter
     *
     * @return Dependant Adapter依赖
     */
    Dependant<? extends BaseComponentState, State> getAdapterDependant() {
        if (mDependencies == null) {
            return null;
        }

        return mDependencies.getAdapterDependant();
    }

    /**
     * 组件不需要关心这些内部action
     * @param reducerCollect ReducerCollect
     */
    @Override
    protected final void checkReducer(@NonNull ReducerCollect<S> reducerCollect) {
        reducerCollect.remove(InnerActions.INTERCEPT_ACTION);
        reducerCollect.remove(InnerActions.INSTALL_EXTRA_FEATURE_ACTION);
        reducerCollect.remove(InnerActions.CHANGE_ORIENTATION);
    }

    /**
     * 对组件来说，不需要关注这些内部action，防止用户错误的注册框架的action
     * @param effectCollect EffectCollect
     */
    @Override
    protected final void checkEffect(@NonNull EffectCollect<S> effectCollect) {
        effectCollect.remove(InnerActions.INTERCEPT_ACTION);
        effectCollect.remove(InnerActions.INSTALL_EXTRA_FEATURE_ACTION);
    }

    /**
     * 创建当前组件对应的State

     * @return PureState
     */
    public abstract S onCreateState(Bundle bundle);

    /**
     * 用于父组件安装子组件接口，并注入环境和连接器
     *
     * @param environment 组件需要的环境
     * @param connector 父组件的连接器
     */
    public abstract void install(@NonNull Environment environment,
                                 @NonNull LRConnector<S, State> connector);

    /**
     * 清理操作，需要子类重写
     */
    @CallSuper
    protected void clear() {
        if (mFuture != null && !mFuture.isDone()) {
            mFuture.cancel(true);
        }
        mGlobalStoreWatcher.clear();
        if (mDependencies != null) {
            mDependencies.clear();
            mDependencies = null;
        }
    }
}
