package com.cyworks.redux.ui

import android.view.View
import com.cyworks.redux.ReduxContext
import com.cyworks.redux.atom.UIWatcher
import com.cyworks.redux.state.State

/**
 * Desc: 用于加载组件的布局/收集更新组件UI的接口
 */
interface ViewModule<S : State> {
    /**
     * 获取当前组件的View
     *
     * @param context ReduxContext
     * @return 组件的View实例
     */
    fun getView(context: ReduxContext<S>, parent: View)

    /**
     * 返回一个Map，映射数据跟UI之间的关联
     * @param state 当前组件的State
     */
    fun mapPropToAtom(state: S, watcher: UIWatcher<S>?)
}