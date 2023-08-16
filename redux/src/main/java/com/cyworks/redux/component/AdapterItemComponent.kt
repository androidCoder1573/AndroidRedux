package com.cyworks.redux.component

import android.view.View
import com.cyworks.redux.action.Action
import com.cyworks.redux.lifecycle.LifeCycleAction
import com.cyworks.redux.state.State
import com.cyworks.redux.util.ILogger

/**
 * 专门用于list的组件，流程稍微有点不一样
 */
abstract class AdapterItemComponent<S: State> : LiveComponent<S>(false) {
    fun createUI(): View? {
        uiController.initUI()
        return uiController.currentView
    }

    override fun onCreate() {
        val time = System.currentTimeMillis()

        // 1、创建Context
        createContext()

        // 不可懒加载
        installSubComponents()

        // 2、观察数据
        observeLifeCycle()

        // 3、发送onCreate Effect
        context.onLifecycle(Action(LifeCycleAction.ACTION_ON_CREATE, null))

        // 打印初始化的耗时
        logger.d(ILogger.PERF_TAG, "component: <" + javaClass.simpleName + ">"
                + "init consume: " + (System.currentTimeMillis() - time))
    }
}