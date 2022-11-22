package com.cyworks.redux;

import android.os.SystemClock;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.view.Choreographer;

import com.tencent.redux.action.Action;
import com.tencent.redux.dispose.IDispose;
import com.tencent.redux.reducer.Reducer;
import com.tencent.redux.state.ComponentStateGetter;
import com.tencent.redux.state.StateGetter;
import com.tencent.redux.util.ILogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;

/**
 * Desc: 页面的store，为page服务，相当于在store增加属性修改的权限控制。
 *
 * 增加统一刷新时机，优化刷新性能。
 *
 * @author randytu on 2021/2/5
 */
final class PageStore<S extends BasePageState> extends Store<S> {
    /**
     * 下一次更新UI的时间间隔，单位ms
     */
    private static final int NEXT_DRAW = 16;

    /**
     * 保存UI更新listener
     */
    private final CopyOnWriteArrayList<UIUpdater> mUIUpdaterListeners =
            new CopyOnWriteArrayList<>();

    /**
     * 上次刷新的时间，防止刷新过快
     */
    private long mLastUpdateUITime;

    /**
     * 是否正在修改State，用于规定刷新时机
     */
    protected boolean isModifyState;

    /**
     * 注册的观察者列表，用于分发store的变化
     */
    private CopyOnWriteArrayList<ComponentStateGetter<State>> mStateGetters;

    /**
     * Page 容器是否展示
     */
    private boolean isPageVisible;

    private final Object mLock = new Object();
    private final Semaphore mSemaphore = new Semaphore(1);

    /**
     * 用于标记是否在运行UI更新
     */
    private volatile boolean isUIUpdateRun = false;

    /**
     * 用于标记监控线程是否运行
     */
    private boolean isThreadRun = true;

    /**
     * 是否需要运行UI更新，在Vsync回调中判断
     */
    private boolean isNeedUpdate = false;

    /**
     * 新一帧的callback
     */
    private final Choreographer.FrameCallback mFrameCallback = new Choreographer.FrameCallback() {

        @Override
        public void doFrame(long frameTimeNanos) {
            // 没有UI监听器，或者UI未展示，或者处于销毁状态，则不进行Ui更新
            if (isDestroy || !isPageVisible || mUIUpdaterListeners.isEmpty()) {
                return;
            }

            // 如果当前正在修改state
            if (isModifyState) {
                Choreographer.getInstance().postFrameCallback(this);
                return;
            }

            // 单线程调用获取时间，性能可控
            long time = SystemClock.uptimeMillis();
            // 如果前后两次间隔时间过短或者当前不需要更新UI
            if (time - mLastUpdateUITime < NEXT_DRAW || !isNeedUpdate) {
                Choreographer.getInstance().postFrameCallback(this);
                return;
            }

            // 记录本次UI更新的开始时间
            mLastUpdateUITime = time;

            isUIUpdateRun = true;
            mSemaphore.release();

            fireUpdateUI();
            Choreographer.getInstance().postFrameCallback(this);
        }
    };

    /**
     * 创建一个页面级别的store
     * @param reducer 页面reducer的聚合
     * @param state 页面初始的状态
     */
    PageStore(@NonNull Reducer<State> reducer, @NonNull S state) {
        super(reducer, state);
        initVsyncGuard();
    }

    private void initVsyncGuard() {
        Thread guardThread = new Thread("VsyncGuard") {
            @Override
            public void run() {
                try {
                    mSemaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                while (isThreadRun) {
                    synchronized (mLock) {
                        try {
                            if (isThreadRun) {
                                mLock.wait(NEXT_DRAW); // 这里设置的vsync时间段
                                isUIUpdateRun = false;
                                mSemaphore.acquire();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        guardThread.start();
    }

    /**
     * 如果是UI组件，需要接收Vsync信号进行刷新对齐，框架内部注册，开发者不需要关心
     * @param uiUpdater UIUpdater
     * @return 一个解注册器
     */
    @MainThread
    IDispose addUIUpdater(UIUpdater uiUpdater) {
        if (uiUpdater == null) {
            return null;
        }

        mUIUpdaterListeners.add(uiUpdater);
        return () -> mUIUpdaterListeners.remove(uiUpdater);
    }

    /**
     * 当Page容器切到后台之后，停止更新UI操作
     */
    void onPageHidden() {
        isPageVisible = false;
        Choreographer.getInstance().removeFrameCallback(mFrameCallback);
    }

    /**
     * 当Page容器回到前台之后，启动UI更新操作，通过接收vsync信号，注册统一刷新逻辑
     */
    void onPageVisible() {
        if (isPageVisible) {
            return;
        }

        Choreographer.getInstance().removeFrameCallback(mFrameCallback);
        isPageVisible = true;
        Choreographer.getInstance().postFrameCallback(mFrameCallback);
    }

    private void fireUpdateUI() {
        for (UIUpdater uiUpdater : mUIUpdaterListeners) {
            if (!isUIUpdateRun) {
                break;
            }
            uiUpdater.onNewFrameCome();
        }

        isNeedUpdate = false;
    }

    @Override
    protected void onDispatch(Action action, Object payload, StateGetter<S> stateGetter) {
        isModifyState = true;
        super.onDispatch(action, payload, stateGetter);
        isModifyState = false;
    }

    @Override
    protected final void update(List<ReactiveProp<Object>> changedPropList) {
        final long time = System.currentTimeMillis();
        // 定义最终变化列表
        List<ReactiveProp<Object>> finalList = new ArrayList<>();

        // 更新state
        for (ReactiveProp<Object> prop : changedPropList) {
            Object value = prop.value();

            // 寻找根属性
            ReactiveProp<Object> tempProp = prop.getRootProp();

            // 更新根属性的值
            tempProp.innerSetter(value);

            // 将根属性添加到变化列表中
            finalList.add(tempProp);
        }

        // 通知更新
        if (finalList.isEmpty()) {
            return;
        }

        // 通知组件进行状态更新
        notifySubs(finalList);

        mLogger.d(ILogger.PERF_TAG, "page store update consumer: "
                + (System.currentTimeMillis() - time));
    }

    /**
     * 子组件注册进来，用于中间件获取state的时候使用
     * @param getter 当前组件的State 的getter
     * @return 一个反注册函数
     */
    IDispose setStateGetter(ComponentStateGetter<State> getter) {
        if (getter == null) {
            return null;
        }

        if (mStateGetters == null) {
            mStateGetters = new CopyOnWriteArrayList<>();
        }

        mStateGetters.add(getter);
        return () -> mStateGetters.remove(getter);
    }

    /**
     * 获取当前Store下的所有state, 目标是给middleware使用，思路是通过observer把每个组件的state传递过来。
     * 这里放到middle ware中，通过一个getter获取，全局store不提供类似功能
     *
     * @return 所有组件对应的属性的集合，HashMap
     */
    HashMap<String, State> getAllState() {
        HashMap<String, State> stateMap = new HashMap<>();
        if (mStateGetters == null || mStateGetters.size() < 1) {
            stateMap.put(mState.getClass().getName(), State.copyState(mState));
            return stateMap;
        }

        for (ComponentStateGetter<State> getter : mStateGetters) {
            State state = getter.getState();
            if (state == null) {
                continue;
            }

            String stateKey = state.getClass().getName();
            stateMap.put(stateKey, state);
        }

        return stateMap;
    }

    public void markNeedUpdate() {
        if (isNeedUpdate) {
            // 解决vsync到来时时，isNeedUpdate被设置成true导致部分界面无法及时更新
            ReduxManager.getInstance().submitInMainThread(() -> {
                isNeedUpdate = true;
            });
        }
        isNeedUpdate = true;
    }

    @Override
    public void clear() {
        super.clear();
        isThreadRun = false;
        mSemaphore.release();
        if (mStateGetters != null) {
            mStateGetters.clear();
        }
    }

    /**
     * 对State变化更新UI，做了刷新对齐，通过vsync信号统一进行刷新
     */
    public interface UIUpdater {
        /**
         * 框架内部实现这个方法，用于接收vsync信号
         */
        void onNewFrameCome();
    }
}
