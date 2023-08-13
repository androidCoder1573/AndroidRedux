package com.cyworks.redux.component

import android.view.View
import com.tencent.redux.beans.UIChangedBean
import java.util.ArrayList

/**
 * Desc: 一个UI组件的基类，主要针对组件的显示/隐藏，
 * 屏幕方向切换做了一些特殊处理
 */
abstract class LRComponent<S : BaseComponentState?>(lazyBindUI: Boolean) :
    BaseComponent<S>(lazyBindUI) {
    /**
     * UI数据变化时的回调，检查本次变化的数据，对可见性以及屏幕旋转做一些特殊处理
     *
     * @param stateCompare ChangedState 本次变化的state集合
     */
    private fun onDataChangedCB(stateCompare: ChangedState<S>) {
        if (mUIMixin.canNotUpdateUI() || environment == null) {
            return
        }
        val props: List<ReactiveProp<Any>> = stateCompare.mChangedProps
        // 检查属性是否合法
        if (props == null || props.isEmpty()) {
            return
        }

        // 将变化的属性的key抽离到一个列表中
        val propKeys: MutableList<String?> = ArrayList()
        for (prop in props) {
            propKeys.add(prop.key)
        }

        // 检查是否组件可见性发生变化
        if (visibilityChanged(propKeys)) {
            return
        }

        // 如果存在屏幕旋转，则优先处理屏幕旋转
        if (needHandleOrientation(propKeys)) {
            return
        }

        // 最后更新UI
        mUIMixin.callUIUpdate(stateCompare.mState, propKeys, mUIMixin.mViewHolder)
    }

    /**
     * 当前组件UI的可见性发生变化
     *
     * @param propKeys 当前组件变化的属性列表
     * @return 是否需要处理可见性变化
     */
    private fun visibilityChanged(propKeys: List<String?>): Boolean {
        val show: Boolean = context!!.state.isShowUI.value()
        if (!propKeys.contains("show_ui")) {
            // 当前变化的属性不包含可见性变化的属性
            return false
        }
        if (mUIMixin.isShow == show) {
            // 可见性没发生变化
            return false
        }

        // 更新可见性
        mUIMixin.isShow = show

        // 如果可见性发生变化，更新旋转方向
        mUIMixin.mLastOrientation = context!!.state.mCurrentOrientation.value()

        // 如果最新的状态是隐藏UI，则进行UI隐藏操作
        if (!mUIMixin.isShow) {
            mUIMixin.hide(true)
            return true
        }
        mUIMixin.show(true)
        return true
    }

    /**
     * 检查是否可以处理屏幕旋转
     * @param propKeys 当前状态变化的属性对应key
     * @return 是否可以处理屏幕旋转
     */
    private fun needHandleOrientation(propKeys: MutableList<String?>): Boolean {
        val orientationKey = "current_orientation"
        if (!propKeys.contains(orientationKey)) {
            return false
        }

        // 开发者不需要关心这个状态
        propKeys.remove(orientationKey)

        // 读取最新的屏幕方向
        val nowOrientation: Int = context!!.state.mCurrentOrientation.value()

        // 防重入
        if (mUIMixin.mLastOrientation == nowOrientation) {
            return false
        }

        // 方向不一致，执行切换
        mUIMixin.mLastOrientation = nowOrientation
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
        mLiveData.removeObserver(mObserver)
        mUIMixin.changeView(orientation)

        // 通知组件当前组件UI发生变化了，给用户一个机会做一些善后处理
        mUIMixin.sendUIChangedAction(UIChangedBean.TYPE_ORIENTATION_CHANGE)

        // 重新创建holder
        mUIMixin.resetViewHolder()

        // 观察数据，界面创建完成之后再进行观察，以防出现异常
        observe()

        // 重新设置UI状态
        context!!.runFullUpdate()
    }

    /**
     * 获取当前组件正在显示的View
     *
     * @return View
     */
    val currentView: View
        get() = mUIMixin.mCurrentView

    /**
     * 构造器，初始化组件的内部数据
     *
     * @param lazyBindUI 是否延迟加载UI
     */
    init {
        mObserver = Observer<ChangedState<S>> { stateCompare: ChangedState<S> ->
            onDataChangedCB(stateCompare)
        }
    }
}