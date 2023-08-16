package com.cyworks.redux.component

import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.cyworks.redux.action.Action
import com.cyworks.redux.lifecycle.LifeCycleAction
import com.cyworks.redux.lifecycle.LifeCycleProxy
import com.cyworks.redux.state.State
import com.cyworks.redux.store.PageStore
import com.cyworks.redux.types.Reducer
import com.cyworks.redux.util.ILogger
import com.cyworks.redux.util.IPlatform
import com.cyworks.redux.util.Platform

/**
 * 页面基类，页面只需要设置根布局即可，不需要更新UI之类的操作。
 *
 * 支持横竖屏切换：
 * 为了加快横竖屏切换的速度，框架内部实现了一套这样的横竖屏切换机制：
 * 在一套布局中实现两块UI：1、横屏区域 2、竖屏区域（空间换时间）
 * 在横竖屏切换的时候隐藏显示这两块区域即可。
 *
 * 这样需要开发者重写onConfigurationChanged来实现横竖屏切换。
 * todo：目前没有提供基于ViewModel的Store
 *
 * 如何进行切换？
 * [LivePage.requestOrientationChange] 此方法需要在收到onConfigurationChanged时调用
 */
abstract class LivePage<S : State> : LogicPage<S> {
    /**
     * 保存当前屏幕配置，旋转屏幕专用
     */
    private var lastOrientation = Configuration.ORIENTATION_PORTRAIT

    /**
     * 为什么要获取根View？
     * Android的界面要比前端复杂很多，fragment不像Activity可以调用setContentView来添加View，
     * 必须通过onCreateView返回，为了统一体验，做了这个妥协的操作。
     *
     * @return Page root view
     */
    val pageRootView: View?
        get() = if (environment == null) null else environment!!.rootView

    /**
     * 构造器，初始化Page，依赖外部传入的Lifecycle代理
     * @param rootId root view id
     * @param proxy 生命周期代理
     */
    constructor(@LayoutRes rootId: Int, proxy: LifeCycleProxy) : super(proxy) {
        val view = proxy.context?.let { bindView(it, rootId) }
        if (view != null) {
            environment!!.setRootView(view)
        }
        init(proxy)
    }

    /**
     * 构造器，初始化Page，依赖外部传入的Lifecycle代理
     * @param proxy LifeCycleProxy
     */
    constructor(rootView: View?, proxy: LifeCycleProxy) : super(proxy) {
        environment!!.setRootView(rootView!!)
        init(proxy)
    }

    private fun init(proxy: LifeCycleProxy) {
        if (proxy.context != null) {
            lastOrientation =proxy.context?.getResources()?.getConfiguration()?.orientation!!
        }

        // 注册生命周期
        // 这里要在创建界面之后再绑定观察者，否则会有时序问题, 比如根View还没创建好就开始构建子组件了
        addObserver(proxy)
    }

    private fun addObserver(proxy: LifeCycleProxy) {
        proxy.lifecycle?.addObserver(PageLifecycleObserver(this))
    }

    override fun createPlatform(): IPlatform? {
        val lifeCycleProxy: LifeCycleProxy? = environment?.lifeCycleProxy
        return lifeCycleProxy?.let { environment?.rootView?.let { it1 -> Platform(it, it1) } }
    }

    override fun onStateDetected(state: S) {
        state.innerSetProp("currentOrientation", lastOrientation)
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
        if (newConfig.orientation == lastOrientation || environment == null) {
            return
        }

        lastOrientation = newConfig.orientation
        context.updateState { state ->
            state.currentOrientation = lastOrientation
            state
        }
    }

    /**
     * 清理数据, 必要时外部实现
     */
    @CallSuper
    override fun destroy() {
        super.destroy()
        context.destroy()
        if (environment != null) {
            environment!!.clear()
            environment = null
        }
    }

    public override fun onCreate() {
        val time = SystemClock.uptimeMillis()
        super.onCreate()
        logger.d(ILogger.PERF_TAG, "page: <" + this.javaClass.simpleName + ">"
                    + " init consumer: " + (SystemClock.uptimeMillis() - time))
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
    private class PageLifecycleObserver constructor(p: LivePage<out State>) : LifecycleObserver {
        /**
         * 关联页面实例
         */
        private val page: LivePage<out State>

        init {
            page = p
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        fun onCreate() {
            page.onCreate()
            page.context.onLifecycle(Action(LifeCycleAction.ACTION_ON_CREATE, null))
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        fun onStart() {
            page.context.onLifecycle(Action(LifeCycleAction.ACTION_ON_START, null))
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun onResume() {
            page.startUIUpdate()
            page.context.onLifecycle(Action(LifeCycleAction.ACTION_ON_RESUME, null))
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun onPause() {
            page.context.onLifecycle(Action(LifeCycleAction.ACTION_ON_PAUSE, null))
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun onStop() {
            page.stopUIUpdate()
            page.context.onLifecycle(Action(LifeCycleAction.ACTION_ON_STOP, null))
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            page.context.onLifecycle(Action(LifeCycleAction.ACTION_ON_DESTROY, null))
            page.destroy()
        }
    }
}