package com.cyworks.redux.component

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.cyworks.redux.prop.ChangedState
import com.cyworks.redux.prop.ReactiveProp
import com.cyworks.redux.state.State
import com.cyworks.redux.ui.UIChangedType

/**
 * 一个UI组件的基类，主要针对组件的显示/隐藏，屏幕方向切换做了一些特殊处理
 */
abstract class Component<S : State>(lazyBindUI: Boolean, p: Bundle?) : BaseComponent<S>(lazyBindUI, p) {
    private val changedPropKeys: HashSet<String> = HashSet()

    val currentView: View?
        get() = uiController.currentView

    init {
        observer = Observer { stateCompare: ChangedState<S> ->
            onDataChangedCB(stateCompare)
        }
    }

    /**
     * UI数据变化时的回调，检查本次变化的数据，对可见性以及屏幕旋转做一些特殊处理
     * @param stateCompare [ChangedState] 本次变化的state集合
     */
    private fun onDataChangedCB(stateCompare: ChangedState<S>) {
        val props: List<ReactiveProp<Any>> = stateCompare.changedProps
        val size = props.size
        // 检查属性是否合法
        if (size < 1) {
            return
        }

        // 将变化的属性的key抽离到一个列表中
        changedPropKeys.clear()
        for (i in 0 until size) {
            val prop = props[i]
            val key = prop.key
            if (key != null && prop.isUIProp) {
                // 必须是UI属性
                changedPropKeys.add(key)
            }
        }

        // 检查是否组件可见性发生变化
        if (visibilityChanged(changedPropKeys)) {
            return
        }

        // 如果存在屏幕旋转，则优先处理屏幕旋转
        if (needHandleOrientation(changedPropKeys)) {
            return
        }

        // 最后更新UI
        uiController.callUIUpdate(stateCompare.lastState, changedPropKeys, uiController.viewHolder)
    }

    /**
     * 当前组件UI的可见性发生变化
     *
     * @param propKeys 当前组件变化的属性列表
     * @return 是否需要处理可见性变化
     */
    private fun visibilityChanged(propKeys: HashSet<String>): Boolean {
        if (!propKeys.contains(State.IS_SHOW_UI_NAME)) {
            // 当前变化的属性不包含可见性变化的属性
            return false
        }

        val show: Boolean = context.state.isShowUI
        if (uiController.isShow == show) {
            // 可见性没发生变化
            return false
        }

        // 更新可见性
        uiController.isShow = show

        // 如果可见性发生变化，更新旋转方向
        uiController.lastOrientation = context.state.currentOrientation

        // 如果最新的状态是隐藏UI，则进行UI隐藏操作
        if (!uiController.isShow) {
            uiController.hide(true)
            return true
        }
        uiController.show(true)
        return true
    }

    /**
     * 检查是否可以处理屏幕旋转
     * @param propKeys 当前状态变化的属性对应key
     * @return 是否可以处理屏幕旋转
     */
    private fun needHandleOrientation(propKeys: HashSet<String>): Boolean {
        val orientationKey = State.CURRENT_ORIENTATION_NAME
        if (!propKeys.contains(orientationKey)) {
            return false
        }

        // 开发者不需要关心这个状态
        propKeys.remove(orientationKey)

        // 读取最新的屏幕方向
        val nowOrientation: Int = context.state.currentOrientation

        // 防重入
        if (uiController.lastOrientation == nowOrientation) {
            return false
        }

        // 方向不一致，执行切换
        uiController.lastOrientation = nowOrientation
        onConfigurationChanged(nowOrientation)
        return true
    }

    /**
     * 当屏幕发生旋转的时候，组件需要进行屏幕适配.
     * 因为context注册store的过程是严格按照父组件 --- 子组件顺序的注册的，
     * 这里不会存在先通知到子组件再通知到父组件的情况。
     */
    private fun onConfigurationChanged(orientation: Int) {
        // 先移除观察者
        observer?.let { liveData?.removeObserver(it) }
        uiController.createView(orientation)

        // 通知组件当前组件UI发生变化了，给用户一个机会做一些善后处理
        uiController.sendUIChangedAction(UIChangedType.TYPE_ORIENTATION_CHANGE)

        // 重新创建holder
        uiController.resetViewHolder()

        // 观察数据，界面创建完成之后再进行观察，以防出现异常
        observeUIData()

        // 重新设置UI状态
        context.runFullUpdate()
    }
}