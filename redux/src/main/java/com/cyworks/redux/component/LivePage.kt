package com.cyworks.redux.component

import android.arch.lifecycle.Lifecycle
import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import android.view.View
import com.cyworks.redux.store.PageStore

/**
 * 页面基类，页面只需要设置根布局即可，不需要更新UI之类的操作。
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
 * [LivePage.requestOrientationChange] 此方法需要在收到onConfigurationChanged时调用
 */
abstract class LivePage<S : BasePageState?> : LogicPage<S> {
    /**
     * 保存当前屏幕配置，旋转屏幕专用
     */
    private var mLastOrientation = Configuration.ORIENTATION_PORTRAIT

    /**
     * 构造器，初始化Page，依赖外部传入的Lifecycle代理
     * @param rootId root view id
     * @param proxy 生命周期代理
     */
    constructor(@LayoutRes rootId: Int, @NonNull proxy: LifeCycleProxy) : super(proxy) {
        environment!!.setRootView(bindView(proxy.getContext(), rootId))
        init(proxy)
    }

    /**
     * 构造器，初始化Page，依赖外部传入的Lifecycle代理
     * @param proxy LifeCycleProxy
     */
    constructor(@NonNull rootView: View?, @NonNull proxy: LifeCycleProxy) : super(proxy) {
        environment!!.setRootView(rootView!!)
        init(proxy)
    }

    private fun init(proxy: LifeCycleProxy) {
        mLastOrientation = proxy.getContext().getResources().getConfiguration().orientation

        // 注册生命周期
        // 这里要在创建界面之后再绑定观察者，否则会有时序问题, 比如根View还没创建好就开始构建子组件了
        addObserver(proxy)
    }

    private fun addObserver(proxy: LifeCycleProxy) {
        val lifecycle: Lifecycle = proxy.getLifecycle()
        if (lifecycle != null) {
            lifecycle.addObserver(PageLifecycleObserver(this))
        }
    }

    override fun createPlatform(): IPlatform {
        val lifeCycleProxy: LifeCycleProxy = environment.getLifeCycleProxy()
        return Platform(lifeCycleProxy, environment.getRootView())
    }

    override fun onStateDetected(state: S) {
        state.mCurrentOrientation.innerSetter(mLastOrientation)
    }

    /**
     * 获取Page对应布局的View实例
     */
    private fun bindView(context: Context, @LayoutRes layoutId: Int): View {
        return LayoutInflater.from(context).inflate(layoutId, null)
    }

    /**
     * 外部调用，用于横竖屏切换的时候的一些改变
     */
    fun requestOrientationChange(newConfig: Configuration) {
        if (newConfig.orientation == mLastOrientation || environment == null) {
            return
        }
        mLastOrientation = newConfig.orientation
        environment.getStore().dispatch(InnerActions.CHANGE_ORIENTATION, mLastOrientation)
    }

    /**
     * 清理数据, 必要时外部实现
     */
    @CallSuper
    override fun destroy() {
        super.destroy()
        if (context != null) {
            context!!.destroy()
        }
        if (environment != null) {
            environment!!.clear()
            environment = null
        }
    }

    /**
     * 为什么要获取根View？
     * Android的界面要比前端复杂很多，fragment不像Activity可以调用setContentView来添加View，
     * 必须通过onCreateView返回，为了统一体验，做了这个妥协的操作。
     *
     * @return Page root view
     */
    val pageRootView: View?
        get() = if (environment == null) null else environment.getRootView()

    public override fun onCreate() {
        val time = SystemClock.uptimeMillis()
        super.onCreate()
        mLogger.d(
            ILogger.PERF_TAG, "page: <" + this.javaClass.simpleName
                    + "> init consumer: " + (SystemClock.uptimeMillis() - time)
        )
    }

    private fun stopUIUpdate() {
        (environment!!.store as PageStore<S>?)!!.onPageHidden()
    }

    private fun startUIUpdate() {
        (environment!!.store as PageStore<S>?)!!.onPageVisible()
    }

    /**
     * 生命周期观察者
     */
    private class PageLifecycleObserver internal constructor(@NonNull page: LivePage<out BasePageState?>) :
        LifecycleObserver {
        /**
         * 关联页面实例
         */
        private val mPage: LivePage<out BasePageState?>
        @OnLifecycleEvent(Event.ON_CREATE)
        fun onCreate() {
            mPage.onCreate()
            mPage.context!!.onLifecycle(LifeCycleAction.ACTION_ON_CREATE)
        }

        @OnLifecycleEvent(Event.ON_START)
        fun onStart() {
            mPage.context!!.onLifecycle(LifeCycleAction.ACTION_ON_START)
        }

        @OnLifecycleEvent(Event.ON_RESUME)
        fun onResume() {
            mPage.startUIUpdate()
            mPage.context!!.onLifecycle(LifeCycleAction.ACTION_ON_RESUME)
        }

        @OnLifecycleEvent(Event.ON_PAUSE)
        fun onPause() {
            mPage.context!!.onLifecycle(LifeCycleAction.ACTION_ON_PAUSE)
        }

        @OnLifecycleEvent(Event.ON_STOP)
        fun onStop() {
            mPage.stopUIUpdate()
            mPage.context!!.onLifecycle(LifeCycleAction.ACTION_ON_STOP)
        }

        @OnLifecycleEvent(Event.ON_DESTROY)
        fun onDestroy() {
            mPage.context!!.onLifecycle(LifeCycleAction.ACTION_ON_DESTROY)
            mPage.destroy()
        }

        /**
         * 构造器，初始化生命周期观察者
         * @param page LRPage
         */
        init {
            mPage = page
        }
    }
}