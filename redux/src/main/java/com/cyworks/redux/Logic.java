package com.cyworks.redux;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.tencent.redux.effect.Effect;
import com.tencent.redux.effect.EffectCollect;
import com.tencent.redux.logic.ILogic;
import com.tencent.redux.logic.LogicModule;
import com.tencent.redux.prop.PropWatcher;
import com.tencent.redux.reducer.Reducer;
import com.tencent.redux.reducer.ReducerCollect;
import com.tencent.redux.reducer.SubReducer;
import com.tencent.redux.util.ILogger;

import java.util.List;

/**
 * Desc: Page以及Component的基类，封装组件和页面的公共逻辑，一个组件可能会有多个子组件。
 *
 * 关于一些逻辑对象的初始化问题：
 * 如果需要一些跟view相关的对象，比如：滚动事件监听等，我们可以在ViewBuilder中初始化View，然后
 * 在Effect更新。当然，这些对象最终会保持在state中，不过这些对象不能使用ReactiveProp来包裹。
 *
 * 针对复杂逻辑对象，内部可能有状态，建议使用ReduxObject来包裹。
 *
 * @author randytu on 2020/8/5
 */
public abstract class Logic<S extends State> implements ILogic<S> {
    /**
     * 组件的Reducer
     */
    protected Reducer<S> mReducer;

    /**
     * 组件的Effect
     */
    protected Effect<S> mEffect;

    /**
     * 组件的Context
     */
    protected ReduxContext<S> mContext;

    /**
     * 用于保存从父组件继承下来的属性
     */
    protected Environment mEnvironment;

    /**
     * 创建页面时，携带的Bundle参数
     */
    protected Bundle mBundle;

    /**
     * 用于监听本组件的属性变化
     */
    private PropWatcher<S> mWatcher;

    /**
     * Log 组件，组件内共享
     */
    protected final ILogger mLogger = ReduxManager.getInstance().getLogger();

    /**
     * 初始Reducer/Effect以及一些依赖
     * @param bundle 页面带来的参数
     */
    public Logic(Bundle bundle) {
        mBundle = bundle;
        initCollect();
    }

    private void initCollect() {
        // 初始化Reducer
        LogicModule<S> logicModule = getLogicModule();
        if (logicModule == null) {
            logicModule = new LogicModule<S>() {
                @Override
                public void registerReducer(@NonNull ReducerCollect<S> collect) {
                }

                @Override
                public void registerEffect(@NonNull EffectCollect<S> collect) {
                }
            };
        }

        // 初始化Reducer
        ReducerCollect<S> reducerCollect = new ReducerCollect<>();
        logicModule.registerReducer(reducerCollect);
        // 检查reducer的注册情况，防止覆盖内部action
        checkReducer(reducerCollect);
        mReducer = reducerCollect.getReducer();

        // 初始化Effect
        EffectCollect<S> effectCollect = new EffectCollect<>();
        logicModule.registerEffect(effectCollect);
        // 检查Effect的注册, 并注入一些框架内部的Action
        checkEffect(effectCollect);
        mEffect = effectCollect.getEffect();

        // 收集订阅的属性
        mWatcher = new PropWatcher<>();
        subscribeProps(mWatcher);
    }

    final PropWatcher<S> getPropWatcher() {
        return mWatcher;
    }

    public final ReduxContext<S> getContext() {
        return mContext;
    }

    final Environment getEnvironment() {
        return mEnvironment;
    }

    /**
     * 合并当前组件下的reducer为一个大Reducer
     *
     * @param list 用于存放组件的Reducer
     * @param connector 组件连接器
     */
    void mergeReducer(@NonNull List<SubReducer> list, LRConnector connector) {
        if (connector != null) {
            list.add(connector.subReducer(mReducer));
        }
    }

    /**
     * 检查当前组件注册的Reducer，主要做几件事情：
     * 1、检查组件有没有注册过框架内部的Action，这些外部注册需要无效化
     * 2、重新注册框架内部的Action
     *
     * @param reducerCollect ReducerCollect
     */
    protected void checkReducer(ReducerCollect<S> reducerCollect) {
        // sub class impl
    }

    /**
     * 检查当前组件注册的Effect，主要做几件事情：
     * 1、检查组件有没有注册过框架内部的Action，这些外部注册需要无效化
     * 2、重新注册框架内部的Action
     *
     * @param effectCollect EffectCollect
     */
    protected void checkEffect(EffectCollect<S> effectCollect) {
        // sub class impl
    }

    /**
     * 通过这个接口来订阅自己组件下的属性变化，这里需要调用watchProp注入
     *
     * @param watcher 属性订阅器
     */
    protected void subscribeProps(PropWatcher<S> watcher) {
        // sub class impl
    }

}
