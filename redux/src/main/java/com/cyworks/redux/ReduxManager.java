package com.cyworks.redux;

import com.tencent.redux.util.ILogger;
import com.tencent.redux.util.MainThreadExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Desc: 保存一些Redux运行过程中的全局对象
 *
 * 1、提供App级别的Effect Bus，方便页面之间通过此BUS进行交互，这里页面间的交互主要指的是广播，
 * 我们限制了广播接收者只能是page，防止了组件间直接通过广播进行交互。
 *
 * 2、提供日志实例，方便调试查看log。
 *
 * @author randytu on 2020/6/5
 */
public final class ReduxManager {
    /**
     * 饿汉式
     */
    private static final ReduxManager sInstance = new ReduxManager();

    /**
     * 创建一个App级别的Bus，作为所有页面级别Bus的根
     */
    private final DispatchBus mAppBus = new DispatchBus();

    /**
     * 需要开发者自己注入logger组件，建议开发者在开发过程中使用自己实现的log组件，release时不需要设置
     */
    private ILogger mLogger;

    /**
     * 框架内部logger为空实现
     */
    private final ILogger mDefaultLogger = new ILogger() {
        @Override
        public void v(String tag, String msg) {}

        @Override
        public void d(String tag, String msg) {}

        @Override
        public void i(String tag, String msg) {}

        @Override
        public void w(String tag, String msg) {}

        @Override
        public void e(String tag, String msg) {}

        @Override
        public void printStackTrace(String tag, String msg, Throwable e) {}

        @Override
        public void printStackTrace(String tag, Throwable e) {}
    };

    /**
     * 初始化页面/组件的时候，是否启用异步模式
     */
    private boolean isUseAsyncMode;

    /**
     * 执行State检测的线程池
     */
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    /**
     * 用于在主线程上执行一些操作
     */
    private MainThreadExecutor mMainThreadExecutor;

    private ReduxManager() {}

    public static ReduxManager getInstance() {
        return sInstance;
    }

    /**
     * 获取App级别的Bus
     * @return DispatchBus
     */
    public DispatchBus getAppBus() {
        return mAppBus;
    }

    public void setLogger(ILogger logger) {
        if (mLogger == null) {
            mLogger = logger;
        }
    }

    public void setAsyncMode() {
        isUseAsyncMode = true;
    }

    public boolean getAsyncMode() {
        return isUseAsyncMode;
    }

    public ILogger getLogger() {
        if (mLogger == null) {
            return mDefaultLogger;
        }
        return mLogger;
    }

    /**
     * 提交一个任务, 在子线程中运行
     * @param runnable Runnable
     * @return Future
     */
    public Future<?> submitInSubThread(Runnable runnable) {
        return mExecutor.submit(runnable);
    }

    /**
     * 提交一个任务, 在主线程中运行
     * @param runnable Runnable
     */
    public void submitInMainThread(Runnable runnable) {
        if (mMainThreadExecutor == null) {
            mMainThreadExecutor = new MainThreadExecutor();
        }
        mMainThreadExecutor.execute(runnable);
    }
}
