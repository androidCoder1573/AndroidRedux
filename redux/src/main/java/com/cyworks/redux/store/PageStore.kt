package com.cyworks.redux.store

import com.cyworks.redux.prop.ReactiveProp
import com.cyworks.redux.state.State
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
    @Volatile private var isUIUpdateRun = false

    /**
     * 用于标记监控线程是否运行
     */
    @Volatile private var isThreadRun = true

    private val uiFresher: UIFresher

    private val onDraw: UIFresher.DrawCallback = object : UIFresher.DrawCallback {
        override fun onDraw():  Boolean {
            // 没有UI监听器，或者UI未展示，或者处于销毁状态，则不进行Ui更新
            if (isDestroy || !isPageVisible || uiUpdaterListeners.isEmpty()) {
                return false
            }

            isUIUpdateRun = true
            semaphore.release()
            return fireUpdateUI()
        }
    }

    init {
        uiFresher = UIFresher(onDraw)
        val guardThread: Thread = object : Thread("ReduxVsyncGuard") {
            override fun run() {
                try {
                    semaphore.acquire()
                } catch (e: InterruptedException) {
                    logger.printStackTrace("ReduxVsyncGuard", e)
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
                            logger.printStackTrace("ReduxVsyncGuard", e)
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

    private fun fireUpdateUI(): Boolean {
        for (uiUpdater in uiUpdaterListeners) {
            if (!isUIUpdateRun) {
                return true
            }
            uiUpdater.onNewFrameCome()
        }
        return false
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

    internal fun requestVsync() {
        uiFresher.requestNextDraw()
    }

    /**
     * 当Page容器切到后台之后，停止更新UI操作
     */
    internal fun onPageHidden() {
        isPageVisible = false
        uiFresher.stop()
    }

    /**
     * 当Page容器回到前台之后，启动UI更新操作，通过接收vsync信号，注册统一刷新逻辑
     */
    internal fun onPageVisible() {
        if (isPageVisible) {
            return
        }

        isPageVisible = true
        uiFresher.start()
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
            uiFresher.requestNextDraw()
        }

        logger.d(ILogger.PERF_TAG,
            "page store update consumer: " + (System.currentTimeMillis() - time))
    }

    override fun clear() {
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