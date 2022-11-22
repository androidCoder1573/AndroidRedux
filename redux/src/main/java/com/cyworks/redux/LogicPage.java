package com.cyworks.redux;

import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import com.tencent.redux.action.Action;
import com.tencent.redux.action.InnerActions;
import com.tencent.redux.beans.InterceptorBean;
import com.tencent.redux.dependant.DependentCollect;
import com.tencent.redux.dependant.ExtraDependants;
import com.tencent.redux.dispatcher.Dispatch;
import com.tencent.redux.effect.Effect;
import com.tencent.redux.effect.EffectCollect;
import com.tencent.redux.interceptor.Interceptor;
import com.tencent.redux.interceptor.InterceptorCollect;
import com.tencent.redux.interceptor.InterceptorPayload;
import com.tencent.redux.lifecycle.LifeCycleProxy;
import com.tencent.redux.middleware.Middleware;
import com.tencent.redux.middleware.Middleware.PageStateGetter;
import com.tencent.redux.middleware.MiddlewareUtil;
import com.tencent.redux.reducer.Reducer;
import com.tencent.redux.reducer.ReducerCollect;
import com.tencent.redux.reducer.ReducerUtils;
import com.tencent.redux.reducer.SubReducer;
import com.tencent.redux.util.IPlatform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Desc: Page = Feature + Effect + Reducer + middleware
 * Page最重要的功能：负责创建Store以及组织页面的Effect调度。
 *
 * 目的：将页面组装逻辑剥离到一个逻辑类，更方便的构建逻辑层测试。
 *
 * 为何要延迟加载Feature？
 * 这其实是有需求场景的，比如开发者需要等一些server的返回值才能决定目前的加载哪些组件，
 * 再比如：如果开发者需要定制组件的加载过程，也可以通过懒加载的方式进行。
 *
 * 针对组件间交互：
 * PageBus完全隔离掉子组件发送app级别的effect。
 *
 * @author randytu on 2021/6/27
 */
public abstract class LogicPage<S extends BasePageState> extends Logic<S> {
    /**
     * 当前App级别的Effect Bus
     */
    protected final DispatchBus mAppBus = ReduxManager.getInstance().getAppBus();

    /**
     * 页面依赖的Feature集合
     */
    protected DependentCollect<S> mDependencies;

    /**
     * Effect拦截器，用于实现子组件与子组件间的通信
     */
    protected Interceptor mInterceptor;

    /**
     * 用于取消异步任务
     */
    private Future<?> mFuture;

    /**
     * 构造器，初始Reducer/Effect以及一些依赖
     *
     * @param proxy LifeCycleProxy
     */
    public LogicPage(@NonNull LifeCycleProxy proxy) {
        super(proxy.getBundle());
        mEnvironment = Environment.of().setLifeCycleProxy(proxy);
        initDependencies();
        initInterceptor();
    }

    /**
     * 创建当前页面所用的Store，只在Page中创建页面Store
     */
    private void createStore(S state ) {
        // 1、将所有组件的reducer跟page Reducer合并
        Reducer<State> reducer = combineReducer();

        PageStore<BasePageState> store = new PageStore<>(reducer, state);
        mEnvironment.setStore(store);

        // 3、将Middleware链式化
        PageStateGetter getter = store::getAllState;
        List<Middleware> pageMiddleware = getReducerMiddleware();

        // 4、根据middleware生成最终的reducer
        final Dispatch dispatch = store.mDispatch;
        store.mDispatch =
                MiddlewareUtil.applyReducerMiddleware(pageMiddleware, dispatch, getter);
    }

    private void initDependencies() {
        // 获取Page对应的依赖集合
        if (mDependencies == null) {
            mDependencies = new DependentCollect<>();
        }
        addDependencies(mDependencies);
    }

    private void initInterceptor() {
        InterceptorCollect innerInterceptorCollect = new InterceptorCollect();

        HashMap<String, Dependant<? extends BaseComponentState, S>> map =
                mDependencies.getDependantMap();
        if (map == null || map.isEmpty()) {
            return;
        }

        for (Dependant<? extends BaseComponentState, S> dependant: map.values()) {
            LRConnector<? extends BaseComponentState, S> connector = dependant.getConnector();
            HashMap<Action, InterceptorBean> interceptors =
                    makeInterceptorForAction(connector, dependant);
            if (interceptors == null) {
                continue;
            }

            innerInterceptorCollect.add(interceptors);
        }

        mInterceptor = innerInterceptorCollect.getInterceptor();
    }

    private HashMap<Action, InterceptorBean> makeInterceptorForAction(
            LRConnector<? extends BaseComponentState, S> connector,
            Dependant<? extends BaseComponentState, S> dependant) {
        // 获取拦截器收集器
        HashMap<Action, ? extends Interceptor<? extends BaseComponentState>>
                interceptorCollect = connector.interceptorCollector();
        if (interceptorCollect == null || interceptorCollect.isEmpty()) {
            return null;
        }

        final HashMap<Action, InterceptorBean> interceptors = new HashMap<>();

        // 单一action可以被多个组件同时拦截，这个跟中间件是有区别的，
        // 中间件是一个全局性的操作，而拦截器是针对某个action的操作。
        // 这里创建Bean是为了让每个组件在拦截器里拿到的是自己对应的Context。
        for (Action action : interceptorCollect.keySet()) {
            InterceptorBean bean = new InterceptorBean();
            bean.mGetter = () -> dependant.getLogic().getContext();
            bean.mInterceptor = interceptorCollect.get(action);
            interceptors.put(action, bean);
        }

        return interceptors;
    }

    @SuppressWarnings("unchecked")
    private Reducer<State> combineReducer() {
        ArrayList<SubReducer> list = new ArrayList<>();

        // 合并所有组件的Reducer
        mergeReducer(list, null);

        // 合并sub reducer为Reducer
        Reducer childReducers = ReducerUtils.combineSubReducers(list);

        ArrayList<Reducer> reducers = new ArrayList<>();
        reducers.add(mReducer);
        reducers.add(childReducers);

        // 将所有的reducer合并成一个Reducer
        return ReducerUtils.combineReducers(reducers);
    }

    @Override
    final void mergeReducer(@NonNull List<SubReducer> list, LRConnector connector) {
        if (mDependencies != null) {
            mDependencies.mergerDependantReducer(list);
        }
    }

    @Override
    protected final void checkReducer(ReducerCollect<S> reducerCollect) {
        reducerCollect.remove(InnerActions.INTERCEPT_ACTION);
        reducerCollect.remove(InnerActions.INSTALL_EXTRA_FEATURE_ACTION);
        reducerCollect.remove(InnerActions.CHANGE_ORIENTATION);

        // 注册修改横竖屏状态的reducer
        reducerCollect.add(InnerActions.CHANGE_ORIENTATION,
                (getter, action, payload) -> {
                    S state = getter.copy();
                    state.mCurrentOrientation.set((int) payload);
                    return state;
                });
    }

    @Override
    protected final void checkEffect(EffectCollect<S> effectCollect) {
        effectCollect.remove(InnerActions.INTERCEPT_ACTION);
        effectCollect.remove(InnerActions.INSTALL_EXTRA_FEATURE_ACTION);
        effectCollect.remove(InnerActions.CHANGE_ORIENTATION);

        effectCollect.add(InnerActions.INTERCEPT_ACTION, makeInterceptEffect());
        effectCollect.add(
                InnerActions.INSTALL_EXTRA_FEATURE_ACTION,
                makeInstallExtraFeatureEffect());
    }

    @SuppressWarnings("unchecked")
    private Effect<S> makeInterceptEffect() {
        // 拦截Action的Effect
        return (action, ctx, payload) -> {
            if (!(payload instanceof InterceptorPayload)) {
                return;
            }

            InterceptorPayload interceptorPayload = (InterceptorPayload) payload;
            mInterceptor.doAction(interceptorPayload.mRealAction, ctx, payload);
        };
    }

    @SuppressWarnings("unchecked")
    private Effect<S> makeInstallExtraFeatureEffect() {
        // 安装额外的子组件的Effect
        return (action, ctx, payload) -> {
            if (!(payload instanceof ExtraDependants)) {
                return;
            }

            final HashMap<String, Dependant<? extends BaseComponentState, S>> extraFeatures =
                    ((ExtraDependants<S>)payload).mExtraFeature;
            if (extraFeatures == null || extraFeatures.isEmpty()) {
                return;
            }

            // 安装feature需要在主线程执行
            if (Looper.getMainLooper() == Looper.myLooper()) {
                installExtraDependant(extraFeatures);
                return;
            }

            ReduxManager.getInstance().submitInMainThread(() -> {
                installExtraDependant(extraFeatures);
            });
        };
    }

    private void installExtraDependant(HashMap<String,
            Dependant<? extends BaseComponentState, S>> extraDependants) {
        boolean hasNewFeature = false; // 防止多次调用重复安装

        // 当前Feature集合，这里保存的都是已经安装过的
        HashMap<String, Dependant<? extends BaseComponentState, S>> map =
                mDependencies.getDependantMap();

        // 子组件需要从父组件继承一些信息
        Environment env = Environment.copy(mEnvironment);
        env.setParentDispatch(env.getDispatchBus().getPageDispatch());
        env.setParentState(mEnvironment.getStore().mState);

        for (String key : extraDependants.keySet()) {
            if (map.containsKey(key)) {
                continue;
            }

            hasNewFeature = true;
            Dependant<? extends BaseComponentState, S> dependant = extraDependants.get(key);
            if (dependant != null) {
                map.put(key, dependant);
                // 安装子组件
                dependant.install(env);
            }
        }

        if (!hasNewFeature) {
            // 没有新的Feature被安装，直接返回
            return;
        }

        // 重新合并reducer
        mEnvironment.getStore().mReducer = combineReducer();

        // 重新收集拦截器
        initInterceptor();
    }

    /**
     * 创建Page bus，用于组件间Effect交互
     */
    private void createPageBus() {
        DispatchBus bus = new DispatchBus();
        bus.attach(mAppBus);
        mEnvironment.setDispatchBus(bus);
    }

    /**
     * 创建平台相关操作
     * @return IPlatform
     */
    protected IPlatform createPlatform() {
        return null;
    }

    /**
     *对于实际的Page组件，可能要设置额外的信息
     */
    protected void onStateDetected(S state) { }

    /**
     * 创建页面的ReduxContext，依赖一个初始的PureState
     */
    private void createContext() {
        if (ReduxManager.getInstance().getAsyncMode()) {
            createContextAsync();
        } else {
            createContextSync();
        }
    }

    private void createContextSync() {
        // 1、生成state
        final S state = onCreateState(mBundle);

        // 生成Key映射表
        state.detectField();

        // 负责处理额外的事情
        onStateDetected(state);

        // 2、创建Store
        createStore(state);

        // 3、创建Context
        mContext = new ReduxContextBuilder<S>()
                .setLogic(this)
                .setState(state)
                .setPlatform(createPlatform())
                .build();
        mContext.setController(getController());
        mContext.setStateReady();
    }

    /**
     * 异步模式，对性能极致需求
     */
    private void createContextAsync() {
        // 1、生成state
        final S state = onCreateState(mBundle);

        final Runnable detectFinishRunnable = () -> {
            mContext.setStateReady();
            // 负责处理额外的事情
            onStateDetected(state);
        };

        final Runnable detectRunnable = () -> {
            state.detectField();
            ReduxManager.getInstance().submitInMainThread(detectFinishRunnable);
        };

        // 整理state
        mFuture = ReduxManager.getInstance().submitInSubThread(detectRunnable);

        // 2、创建Store
        createStore(state);

        // 3、创建Context
        mContext = new ReduxContextBuilder<S>()
                .setLogic(this)
                .setState(state)
                .setPlatform(createPlatform())
                .build();
        mContext.setController(getController());
    }

    /**
     * Page收到容器onCreate生命周期时，执行的一些初始化操作
     */
    @CallSuper
    protected void onCreate() {
        // 1、创建Page bus，用于页面内Effect交互
        createPageBus();

        // 2、初始化Page的Context
        createContext();

        // 3、安装子组件
        installDependant();
    }

    /**
     * 安装子组件
     */
    private void installDependant() {
        HashMap<String, Dependant<? extends BaseComponentState, S>> map =
                mDependencies.getDependantMap();
        if (map == null || map.isEmpty()) {
            return;
        }

        // 子组件需要从父组件那边继承一些信息
        Environment env = Environment.copy(mEnvironment);
        env.setParentDispatch(env.getDispatchBus().getPageDispatch());
        env.setParentState(mEnvironment.getStore().mState);

        // 安装子组件
        for (Dependant<? extends BaseComponentState, S> dependant : map.values()) {
            dependant.install(env);
        }
    }

    /**
     * 创建页面的State，页面的State默认页面的一级组件都可以进行关联
     *
     * @return state {@link BasePageState}
     */
    public abstract S onCreateState(Bundle bundle);

    /**
     * 配置当前页面的Feature(依赖)集合PageDependantCollect，需要外部设置
     */
    public abstract void addDependencies(DependentCollect<S> collect);

    /**
     * 添加拦截 reducer 的 Middleware，开发这如果需要增加一些中间件拦截Action，可以通过此方法注入
     *
     * @return 中间件列表
     */
    protected List<Middleware> getReducerMiddleware() {
        // sub class can impl
        return null;
    }

    protected BaseController<S> getController() {
        return null;
    }

    @CallSuper
    protected void destroy() {
        if (mFuture != null && !mFuture.isDone()) {
            mFuture.cancel(true);
        }
    }
}
