package com.cyworks.redux;

import android.annotation.SuppressLint;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.OnLifecycleEvent;
import android.os.SystemClock;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import com.tencent.redux.lifecycle.LifeCycleAction;
import com.tencent.redux.lifecycle.LifeCycleProxy;
import com.tencent.redux.prop.ChangedState;
import com.tencent.redux.state.StateChangeForUI;
import com.tencent.redux.ui.ViewModule;
import com.tencent.redux.util.ILogger;
import com.tencent.redux.util.IPlatform;
import com.tencent.redux.util.Platform;

/**
 * Desc:组件基类，框架内部实现，外部不能直接使用, 用来承载一个Redux组件.
 * Component = Action + State + Reducer + Effect + Dependant(依赖的子组件)
 *
 * 组件生命周期自治：组件通过注入LifecycleObserver，写法上不依赖activity等环境。
 *
 * note:
 * 由于每个组件自己管理生命周期，可能会跟Page生命周期时机对不上，所以有些依赖生命周期的操作要比较小心。
 *
 * Component支持UI懒加载, 组件UI懒加载：
 * 组件初始化过程中，绑定UI是整个环节中最耗时的操作，如果能延后UI绑定操作，能一定程度上缓解初始化压力。
 * 框架提供了{@link BaseComponentState#isShowUI} 来懒加载UI界面，开发者可以自己控制UI展示的时机。
 */
public abstract class BaseComponent<S extends BaseComponentState> extends LogicComponent<S> {
    /**
     * 使用LiveData包裹变更的状态数据，防止因为生命周期导致界面异常
     */
    MutableLiveData<ChangedState<S>> mLiveData;

    /**
     * LiveData的Observer
     */
    Observer<ChangedState<S>> mObserver;

    /**
     * 将对UI的操作放在这里
     */
    final ComponentUIMixin<S> mUIMixin;

    /**
     * 构造器，初始一些组件的内部数据
     *
     * @param lazyBindUI 是否延迟加载UI
     */
    public BaseComponent(boolean lazyBindUI) {
        super(null);
        mUIMixin = new ComponentUIMixin<>(this, lazyBindUI);
    }

    void show() {
        if (mUIMixin != null) {
            mUIMixin.show(false);
        }
    }

    void hide() {
        if (mUIMixin != null) {
            mUIMixin.hide(false);
        }
    }

    void attach() {
        if (mUIMixin != null) {
            mUIMixin.attach();
        }
    }

    void detach() {
        if (mUIMixin != null) {
            mUIMixin.detach();
        }
    }

    /**
     * 使用LiveData观察数据，触发UI更新
     */
    final void observe() {
        if (environment == null || mObserver == null) {
            return;
        }

        mLiveData.observe(environment.getLifeCycleProxy().getLifecycleOwner(), mObserver);
    }

    @Override
    public final void install(@NonNull Environment environment,
                              @NonNull LRConnector<S, State> connector) {
        if (mUIMixin.isBind) {
            return;
        }
        mUIMixin.isBind = true;

        mConnector = connector;
        environment = environment;

        // 获取启动参数
        LifeCycleProxy lifeCycleProxy = environment.getLifeCycleProxy();
        mBundle = lifeCycleProxy.getBundle();

        // 添加生命周期观察
        mLiveData = new MutableLiveData<>();
        Lifecycle lifecycle = lifeCycleProxy.getLifecycle();
        if (lifecycle != null) {
            lifecycle.addObserver(new ComponentLifeCycleObserver(this));
        }
    }

    boolean isInstalled() {
        return mUIMixin.isBind;
    }

    @SuppressLint("ResourceType")
    @Override
    protected final IPlatform createPlatform() {
        LifeCycleProxy lifeCycleProxy = environment.getLifeCycleProxy();

        Platform platform = new Platform(lifeCycleProxy, environment.getRootView());
        if (mConnector != null) {
            platform.setStubId(
                    mConnector.getViewContainerIdForV(),
                    mConnector.getViewContainerIdForH()
            );
        }

        return platform;
    }

    @Override
    protected final StateChangeForUI<S> makeUIListener() {
        return (state, changedProps) -> {
            ChangedState<S> stateCompare = new ChangedState<>();
            stateCompare.mState = state;
            stateCompare.mChangedProps = changedProps;
            mLiveData.setValue(stateCompare);
        };
    }

    /**
     * 获取View模块，外部设置
     *
     * @return ViewModule
     */
    public abstract ViewModule<S> getViewModule();

    @CallSuper
    @Override
    protected void clear() {
        super.clear();
        mLiveData.removeObserver(mObserver);

        if (context != null) {
            context.destroy();
        }

        mUIMixin.clear();
        environment = null;
    }

    @Override
    protected final void onStateDetected(S componentState) {
        // 检查默认属性设置
        componentState.isShowUI.innerSetter(mUIMixin.isShow);

        // 获取初始的屏幕方向
        mUIMixin.mLastOrientation = componentState.mCurrentOrientation.value();

        // 设置状态 -- UI 监听
        mUIMixin.makeUIWatcher(componentState);

        // 运行首次UI更新
        mUIMixin.firstUpdate();
    }

    /**
     * 用于初始化Component
     */
    private void onCreate() {
        final long time = SystemClock.uptimeMillis();

        // 1、创建Context
        createContext();

        // 2、如果不懒加载，直接加载界面
        if (mUIMixin.isShow) {
            mUIMixin.initUI();
            mUIMixin.firstUpdate();

            // 遍历依赖
            initSubComponent();
        }

        // 3、观察数据
        observe();

        // 4、发送onCreate Effect
        context.onLifecycle(LifeCycleAction.ACTION_ON_CREATE);

        // 打印初始化的耗时
        mLogger.d(ILogger.PERF_TAG, "component: <"
                + getClass().getSimpleName()
                + "> init consumer: " + (SystemClock.uptimeMillis() - time));
    }

    /**
     * Activity生命周期监听，通过这种方式实现组件的生命周期自治
     */
    private static final class ComponentLifeCycleObserver implements LifecycleObserver {

        /**
         * 内部持有组件实例
         */
        private final BaseComponent<? extends BaseComponentState> mComponent;

        /**
         * 构造器，初始化生命周期观察者
         *
         * @param component AbsComponent
         */
        public ComponentLifeCycleObserver(
                @NonNull BaseComponent<? extends BaseComponentState> component) {
            mComponent = component;
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        public void onCreate() {
            mComponent.onCreate();
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        public void onStart() {
            mComponent.context.onLifecycle(LifeCycleAction.ACTION_ON_START);
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        public void onResume() {
            mComponent.context.onLifecycle(LifeCycleAction.ACTION_ON_RESUME);
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        public void onPause() {
            mComponent.context.onLifecycle(LifeCycleAction.ACTION_ON_PAUSE);
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        public void onStop() {
            mComponent.context.onLifecycle(LifeCycleAction.ACTION_ON_STOP);
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        public void onDestroy() {
            // 这里的调用顺序不能乱
            mComponent.context.onLifecycle(LifeCycleAction.ACTION_ON_DESTROY);
            mComponent.clear();
        }
    }
}
