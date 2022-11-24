package com.cyworks.redux

import android.os.SystemClock
import android.support.annotation.MainThread
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Semaphore

/**
 * Desc: 页面的store，为page服务，相当于在store增加属性修改的权限控制。
 *
 * 增加统一刷新时机，优化刷新性能。
 *
 * @author randytu on 2021/2/5
 */
internal class PageStore<S : BasePageState?>(
    @NonNull reducer: Reducer<State?>?,
    @NonNull state: S
) : Store<S>(reducer, state) {
    /**
     * 保存UI更新listener
     */
    private val mUIUpdaterListeners = CopyOnWriteArrayList<UIUpdater>()

    /**
     * 上次刷新的时间，防止刷新过快
     */
    private var mLastUpdateUITime: Long = 0

    /**
     * 是否正在修改State，用于规定刷新时机
     */
    protected var isModifyState = false

    /**
     * 注册的观察者列表，用于分发store的变化
     */
    private var mStateGetters: CopyOnWriteArrayList<ComponentStateGetter<State>>? = null

    /**
     * Page 容器是否展示
     */
    private var isPageVisible = false
    private val mLock = Any()
    private val mSemaphore = Semaphore(1)

    /**
     * 用于标记是否在运行UI更新
     */
    @Volatile
    private var isUIUpdateRun = false

    /**
     * 用于标记监控线程是否运行
     */
    private var isThreadRun = true

    /**
     * 是否需要运行UI更新，在Vsync回调中判断
     */
    private var isNeedUpdate = false

    /**
     * 新一帧的callback
     */
    private val mFrameCallback: FrameCallback = object : FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            // 没有UI监听器，或者UI未展示，或者处于销毁状态，则不进行Ui更新
            if (isDestroy || !isPageVisible || mUIUpdaterListeners.isEmpty()) {
                return
            }

            // 如果当前正在修改state
            if (isModifyState) {
                Choreographer.getInstance().postFrameCallback(this)
                return
            }

            // 单线程调用获取时间，性能可控
            val time = SystemClock.uptimeMillis()
            // 如果前后两次间隔时间过短或者当前不需要更新UI
            if (time - mLastUpdateUITime < NEXT_DRAW || !isNeedUpdate) {
                Choreographer.getInstance().postFrameCallback(this)
                return
            }

            // 记录本次UI更新的开始时间
            mLastUpdateUITime = time
            isUIUpdateRun = true
            mSemaphore.release()
            fireUpdateUI()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun initVsyncGuard() {
        val guardThread: Thread = object : Thread("VsyncGuard") {
            override fun run() {
                try {
                    mSemaphore.acquire()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                while (isThreadRun) {
                    synchronized(mLock) {
                        try {
                            if (isThreadRun) {
                                mLock.wait(NEXT_DRAW.toLong()) // 这里设置的vsync时间段
                                isUIUpdateRun = false
                                mSemaphore.acquire()
                            }
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
        guardThread.start()
    }

    /**
     * 如果是UI组件，需要接收Vsync信号进行刷新对齐，框架内部注册，开发者不需要关心
     * @param uiUpdater UIUpdater
     * @return 一个解注册器
     */
    @MainThread
    fun addUIUpdater(uiUpdater: UIUpdater?): IDispose? {
        if (uiUpdater == null) {
            return null
        }
        mUIUpdaterListeners.add(uiUpdater)
        return IDispose { mUIUpdaterListeners.remove(uiUpdater) }
    }

    /**
     * 当Page容器切到后台之后，停止更新UI操作
     */
    fun onPageHidden() {
        isPageVisible = false
        Choreographer.getInstance().removeFrameCallback(mFrameCallback)
    }

    /**
     * 当Page容器回到前台之后，启动UI更新操作，通过接收vsync信号，注册统一刷新逻辑
     */
    fun onPageVisible() {
        if (isPageVisible) {
            return
        }
        Choreographer.getInstance().removeFrameCallback(mFrameCallback)
        isPageVisible = true
        Choreographer.getInstance().postFrameCallback(mFrameCallback)
    }

    private fun fireUpdateUI() {
        for (uiUpdater in mUIUpdaterListeners) {
            if (!isUIUpdateRun) {
                break
            }
            uiUpdater.onNewFrameCome()
        }
        isNeedUpdate = false
    }

    override fun onDispatch(action: Action?, payload: Any?, stateGetter: StateGetter<S>) {
        isModifyState = true
        super.onDispatch(action, payload, stateGetter)
        isModifyState = false
    }

    override fun update(changedPropList: List<ReactiveProp<Any?>?>?) {
        val time = System.currentTimeMillis()
        // 定义最终变化列表
        val finalList: MutableList<ReactiveProp<Any?>?> = ArrayList()

        // 更新state
        for (prop in changedPropList!!) {
            val value = prop!!.value()

            // 寻找根属性
            val tempProp = prop.rootProp

            // 更新根属性的值
            tempProp!!.innerSetter(value)

            // 将根属性添加到变化列表中
            finalList.add(tempProp)
        }

        // 通知更新
        if (finalList.isEmpty()) {
            return
        }

        // 通知组件进行状态更新
        notifySubs(finalList)
        mLogger.d(
            ILogger.PERF_TAG, "page store update consumer: "
                    + (System.currentTimeMillis() - time)
        )
    }

    /**
     * 子组件注册进来，用于中间件获取state的时候使用
     * @param getter 当前组件的State 的getter
     * @return 一个反注册函数
     */
    fun setStateGetter(getter: ComponentStateGetter<State>?): IDispose? {
        if (getter == null) {
            return null
        }
        if (mStateGetters == null) {
            mStateGetters = CopyOnWriteArrayList<ComponentStateGetter<State>>()
        }
        mStateGetters!!.add(getter)
        return IDispose { mStateGetters!!.remove(getter) }
    }

    /**
     * 获取当前Store下的所有state, 目标是给middleware使用，思路是通过observer把每个组件的state传递过来。
     * 这里放到middle ware中，通过一个getter获取，全局store不提供类似功能
     *
     * @return 所有组件对应的属性的集合，HashMap
     */
    val allState: HashMap<String, State?>
        get() {
            val stateMap = HashMap<String, State?>()
            if (mStateGetters == null || mStateGetters!!.size < 1) {
                stateMap[state.getClass().getName()] = State.copyState<State>(state)
                return stateMap
            }
            for (getter in mStateGetters) {
                val state: State = getter.getState() ?: continue
                val stateKey = state.javaClass.name
                stateMap[stateKey] = state
            }
            return stateMap
        }

    fun markNeedUpdate() {
        if (isNeedUpdate) {
            // 解决vsync到来时时，isNeedUpdate被设置成true导致部分界面无法及时更新
            ReduxManager.getInstance().submitInMainThread { isNeedUpdate = true }
        }
        isNeedUpdate = true
    }

    public override fun clear() {
        super.clear()
        isThreadRun = false
        mSemaphore.release()
        if (mStateGetters != null) {
            mStateGetters!!.clear()
        }
    }

    /**
     * 对State变化更新UI，做了刷新对齐，通过vsync信号统一进行刷新
     */
    interface UIUpdater {
        /**
         * 框架内部实现这个方法，用于接收vsync信号
         */
        fun onNewFrameCome()
    }

    companion object {
        /**
         * 下一次更新UI的时间间隔，单位ms
         */
        private const val NEXT_DRAW = 16
    }

    /**
     * 创建一个页面级别的store
     * @param reducer 页面reducer的聚合
     * @param state 页面初始的状态
     */
    init {
        initVsyncGuard()
    }
}