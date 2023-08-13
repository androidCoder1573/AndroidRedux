package com.cyworks.redux.store

import android.os.SystemClock
import android.view.Choreographer
import com.cyworks.redux.ReduxManager
import com.cyworks.redux.state.State
import com.cyworks.redux.prop.ReactiveProp
import com.cyworks.redux.types.Dispose
import com.cyworks.redux.types.StateGetter
import com.cyworks.redux.types.UIFrameUpdater
import com.cyworks.redux.util.ILogger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Semaphore

/**
 * 页面的store，为page服务，相当于在store增加属性修改的权限控制。
 * 增加统一刷新时机，优化刷新性能。
 */
class PageStore<S : State>(state: S) : Store<S>(state) {
    /**
     * 保存UI更新listener
     */
    private val uiUpdaterListeners = CopyOnWriteArrayList<UIFrameUpdater>()

    /**
     * 上次刷新的时间，防止刷新过快
     */
    private var lastUpdateUITime: Long = 0L

    /**
     * 是否正在修改State，用于规定刷新时机
     */
    private var isModifyState = false

    /**
     * 注册的观察者列表，用于分发store的变化
     */
    private var stateGetters: CopyOnWriteArrayList<StateGetter<State>>? = null

    /**
     * Page 容器是否展示
     */
    private var isPageVisible = false

    private val lock = Object()
    private val semaphore = Semaphore(1)

    /**
     * 用于标记是否在运行UI更新
     */
    @Volatile
    private var isUIUpdateRun = false

    /**
     * 用于标记监控线程是否运行
     */
    @Volatile
    private var isThreadRun = true

    /**
     * 是否需要运行UI更新，在Vsync回调中判断
     */
    private var isNeedUpdate = false

    /**
     * 创建一个页面级别的store
     */
    init {
        val guardThread: Thread = object : Thread("VsyncGuard") {
            override fun run() {
                try {
                    semaphore.acquire()
                } catch (e: InterruptedException) {
                    logger.printStackTrace("VsyncGuard", e)
                }
                while (isThreadRun) {
                    synchronized(lock) {
                        try {
                            if (isThreadRun) {
                                lock.wait(NEXT_DRAW.toLong()) // 这里设置的vsync时间段
                                isUIUpdateRun = false
                                semaphore.acquire()
                            }
                        } catch (e: InterruptedException) {
                            logger.printStackTrace("VsyncGuard", e)
                        }
                    }
                }
            }
        }
        guardThread.start()
    }

    /**
     * 获取当前Store下的所有state, 目标是给middleware使用，思路是通过observer把每个组件的state传递过来。
     * 这里放到middle ware中，通过一个getter获取，全局store不提供类似功能
     */
    val allState: HashMap<String, State>
        get() {
            val stateMap = HashMap<String, State>()
            val key = state.hashCode().toString()
            if (stateGetters == null || stateGetters!!.size < 1) {
                stateMap[key] = State.copyState<State>(state)
                return stateMap
            }
            stateGetters!!.forEach {
                val state: State = it.copy()
                stateMap[key] = state
            }
            return stateMap
        }

    /**
     * 新一帧的callback
     */
    private val frameCallback: Choreographer.FrameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            // 没有UI监听器，或者UI未展示，或者处于销毁状态，则不进行Ui更新
            if (isDestroy || !isPageVisible || uiUpdaterListeners.isEmpty()) {
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
            if (time - lastUpdateUITime < NEXT_DRAW || !isNeedUpdate) {
                Choreographer.getInstance().postFrameCallback(this)
                return
            }

            // 记录本次UI更新的开始时间
            lastUpdateUITime = time
            isUIUpdateRun = true
            semaphore.release()
            fireUpdateUI()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun fireUpdateUI() {
        for (uiUpdater in uiUpdaterListeners) {
            if (!isUIUpdateRun) {
                break
            }
            uiUpdater.onNewFrameCome()
        }
        isNeedUpdate = false
    }

    /**
     * 子组件注册进来，用于中间件获取state的时候使用
     * @param getter 当前组件的State 的getter
     * @return 一个反注册函数
     */
    internal fun addStateGetter(getter: StateGetter<State>?): Dispose? {
        if (getter == null) {
            return null
        }
        if (stateGetters == null) {
            stateGetters = CopyOnWriteArrayList<StateGetter<State>>()
        }
        stateGetters!!.add(getter)
        return { stateGetters!!.remove(getter) }
    }

    /**
     * 如果是UI组件，需要接收Vsync信号进行刷新对齐，框架内部注册，开发者不需要关心
     * @param uiUpdater UIUpdater
     * @return 一个解注册器
     */
    internal fun addUIUpdater(uiUpdater: UIFrameUpdater): Dispose {
        uiUpdaterListeners.add(uiUpdater)
        return { uiUpdaterListeners.remove(uiUpdater) }
    }

    internal fun markNeedUpdate() {
        if (isNeedUpdate) {
            // 解决vsync到来时时，isNeedUpdate被设置成true导致部分界面无法及时更新
            ReduxManager.instance.submitInMainThread { isNeedUpdate = true }
        }
        isNeedUpdate = true
    }

    /**
     * 当Page容器切到后台之后，停止更新UI操作
     */
    internal fun onPageHidden() {
        isPageVisible = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
    }

    /**
     * 当Page容器回到前台之后，启动UI更新操作，通过接收vsync信号，注册统一刷新逻辑
     */
    internal fun onPageVisible() {
        if (isPageVisible) {
            return
        }
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        isPageVisible = true
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun update(changedPropList: List<ReactiveProp<Any>>) {
        val time = System.currentTimeMillis()

        // 定义最终变化列表
        val finalList: MutableList<ReactiveProp<Any>> = ArrayList()

        // 更新state
        changedPropList.forEach {
            // 寻找根属性
            val tempProp = it.rootProp
            // 更新根属性的值
            tempProp.innerSetter(it.value())
            // 将根属性添加到变化列表中
            finalList.add(tempProp)
        }

        // 通知更新
        if (finalList.isNotEmpty()) {
            // 通知组件进行状态更新
            notifySubs(finalList)
        }

        logger.d(ILogger.PERF_TAG,
            "page store update consumer: " + (System.currentTimeMillis() - time))
    }

    fun getType(): StoreType {
        return StoreType.PAGE
    }

    public override fun clear() {
        super.clear()
        isThreadRun = false
        semaphore.release()
        if (stateGetters != null) {
            stateGetters!!.clear()
        }
    }

    companion object {
        /**
         * 下一次更新UI的时间间隔，单位ms
         */
        private const val NEXT_DRAW = 16
    }
}