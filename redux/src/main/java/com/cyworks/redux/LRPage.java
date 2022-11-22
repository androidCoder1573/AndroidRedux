package com.cyworks.redux;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.Lifecycle.Event;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.support.annotation.CallSuper;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;

import com.tencent.redux.action.InnerActions;
import com.tencent.redux.lifecycle.LifeCycleAction;
import com.tencent.redux.lifecycle.LifeCycleProxy;
import com.tencent.redux.util.ILogger;
import com.tencent.redux.util.IPlatform;
import com.tencent.redux.util.Platform;

/**
 * Desc: 页面基类，页面只需要设置根布局即可，不需要更新UI之类的操作。
 *
 * 支持横竖屏切换：
 * 为了加快横竖屏切换的速度，框架内部实现了一套这样的横竖屏切换机制：
 * 在一套布局中实现两块UI：1、横屏区域 2、竖屏区域
 * 在横竖屏切换的时候隐藏显示这两块区域即可。
 *
 * 这样需要开发者重写onConfigurationChanged来实现横竖屏切换。
 * todo：目前没有提供基于ViewModel的Store，后续会考虑。
 *
 * 如何进行切换？
 * {@link LRPage#requestOrientationChange(Configuration)} 此方法需要在收到onConfigurationChanged时调用
 * <pre>
 *     public void onConfigurationChanged(@NonNull Configuration newConfig) {
 *         super.onConfigurationChanged(newConfig);
 *         mPage.requestOrientationChange(newConfig);
 *     }
 * </pre>
 *
 * @author randytu on 2020/8/5
 */
public abstract class LRPage<S extends BasePageState> extends LogicPage<S> {
    /**
     * 保存当前屏幕配置，旋转屏幕专用
     */
    private int mLastOrientation = Configuration.ORIENTATION_PORTRAIT;

    /**
     * 构造器，初始化Page，依赖外部传入的Lifecycle代理
     * @param rootId root view id
     * @param proxy 生命周期代理
     */
    public LRPage(@LayoutRes int rootId, @NonNull LifeCycleProxy proxy) {
        super(proxy);

        mEnvironment.setRootView(bindView(proxy.getContext(), rootId));
        init(proxy);
    }

    /**
     * 构造器，初始化Page，依赖外部传入的Lifecycle代理
     * @param proxy LifeCycleProxy
     */
    public LRPage(@NonNull View rootView, @NonNull LifeCycleProxy proxy) {
        super(proxy);

        mEnvironment.setRootView(rootView);
        init(proxy);
    }

    private void init(LifeCycleProxy proxy) {
        mLastOrientation =
                proxy.getContext().getResources().getConfiguration().orientation;

        // 注册生命周期
        // 这里要在创建界面之后再绑定观察者，否则会有时序问题, 比如根View还没创建好就开始构建子组件了
        addObserver(proxy);
    }

    private void addObserver(LifeCycleProxy proxy) {
        Lifecycle lifecycle = proxy.getLifecycle();
        if (lifecycle != null) {
            lifecycle.addObserver(new PageLifecycleObserver(this));
        }
    }

    @Override
    protected final IPlatform createPlatform() {
        LifeCycleProxy lifeCycleProxy = mEnvironment.getLifeCycleProxy();
        return new Platform(lifeCycleProxy, mEnvironment.getRootView());
    }

    @Override
    protected final void onStateDetected(S state) {
        state.mCurrentOrientation.innerSetter(mLastOrientation);
    }

    /**
     * 获取Page对应布局的View实例
     */
    private View bindView(Context context, @LayoutRes int layoutId) {
        return LayoutInflater.from(context).inflate(layoutId, null);
    }

    /**
     * 外部调用，用于横竖屏切换的时候的一些改变
     */
    public void requestOrientationChange(Configuration newConfig) {
        if (newConfig.orientation == mLastOrientation || mEnvironment == null) {
            return;
        }

        mLastOrientation = newConfig.orientation;
        mEnvironment.getStore().dispatch(InnerActions.CHANGE_ORIENTATION, mLastOrientation);
    }

    /**
     * 清理数据, 必要时外部实现
     */
    @CallSuper
    protected void destroy() {
        super.destroy();

        if (mContext != null) {
            mContext.destroy();
        }

        if (mEnvironment != null) {
            mEnvironment.clear();
            mEnvironment = null;
        }
    }

    /**
     * 为什么要获取根View？
     * Android的界面要比前端复杂很多，fragment不像Activity可以调用setContentView来添加View，
     * 必须通过onCreateView返回，为了统一体验，做了这个妥协的操作。
     *
     * @return Page root view
     */
    public final View getPageRootView() {
        return mEnvironment == null ? null : mEnvironment.getRootView();
    }

    @Override
    public final void onCreate() {
        final long time = SystemClock.uptimeMillis();
        super.onCreate();
        mLogger.d(ILogger.PERF_TAG, "page: <" + this.getClass().getSimpleName()
                + "> init consumer: " + (SystemClock.uptimeMillis() - time));
    }

    @SuppressWarnings("unchecked")
    private void stopUIUpdate() {
        ((PageStore<S>)mEnvironment.getStore()).onPageHidden();
    }

    @SuppressWarnings("unchecked")
    private void startUIUpdate() {
        ((PageStore<S>)mEnvironment.getStore()).onPageVisible();
    }

    /**
     * 生命周期观察者
     */
    private static final class PageLifecycleObserver implements LifecycleObserver {

        /**
         * 关联页面实例
         */
        private final LRPage<? extends BasePageState> mPage;

        /**
         * 构造器，初始化生命周期观察者
         * @param page LRPage
         */
        PageLifecycleObserver(@NonNull LRPage<? extends BasePageState> page) {
            mPage = page;
        }

        @OnLifecycleEvent(Event.ON_CREATE)
        public void onCreate() {
            mPage.onCreate();
            mPage.mContext.onLifecycle(LifeCycleAction.ACTION_ON_CREATE);
        }

        @OnLifecycleEvent(Event.ON_START)
        public void onStart() {
            mPage.mContext.onLifecycle(LifeCycleAction.ACTION_ON_START);
        }

        @OnLifecycleEvent(Event.ON_RESUME)
        public void onResume() {
            mPage.startUIUpdate();
            mPage.mContext.onLifecycle(LifeCycleAction.ACTION_ON_RESUME);
        }

        @OnLifecycleEvent(Event.ON_PAUSE)
        public void onPause() {
            mPage.mContext.onLifecycle(LifeCycleAction.ACTION_ON_PAUSE);
        }

        @OnLifecycleEvent(Event.ON_STOP)
        public void onStop() {
            mPage.stopUIUpdate();
            mPage.mContext.onLifecycle(LifeCycleAction.ACTION_ON_STOP);
        }

        @OnLifecycleEvent(Event.ON_DESTROY)
        public void onDestroy() {
            mPage.mContext.onLifecycle(LifeCycleAction.ACTION_ON_DESTROY);
            mPage.destroy();
        }
    }
}
