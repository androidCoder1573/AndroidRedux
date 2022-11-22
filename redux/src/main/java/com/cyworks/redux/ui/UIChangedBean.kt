package com.cyworks.redux.beans

import android.view.View

enum class UIChangedType {
    TYPE_UNKNOWN,
    TYPE_ORIENTATION_CHANGE, // 屏幕方向发生变化
    TYPE_VISIBILITY_CHANGE // 组件可见性发生变化
}

/**
 * Desc: UI发生变化时，比如屏幕旋转时、可见性变化时，给子组件的传递的参数bean，
 * 主要是给子组件一个机会去处理界面中的一些其他逻辑，比如界面中有异步任务的，或者存在定时器的
 *
 * orientation: 当前屏幕的旋转方向;
 * isShow: 当前组件是否可见;
 * uiChangeType: 当前UI变化的类型;
 * partialView: 当前组件的竖屏View;
 * landscapeView: 当前组件的横屏View
 */
data class UIChangedBean(var orientation: Int = 0,
                         var isShow: Boolean = true,
                         var uiChangeType: UIChangedType = UIChangedType.TYPE_UNKNOWN,
                         var partialView: View? = null,
                         var landscapeView: View? = null)