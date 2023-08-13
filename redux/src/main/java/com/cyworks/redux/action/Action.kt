package com.cyworks.redux.action

/**
 * 声明ActionType，用于标记某个Action
 */
data class ActionType(var name: String)

/**
 * Desc: 框架内部定义的一些Action，用于框架所需处理的一些事情。
 */
object InnerActionTypes {
    /**
     * 所有的组件的公共action如果想被拦截，都通过这个action
     */
    val INTERCEPT_ACTION_TYPE = ActionType("action_inner_intercept")

    /**
     * 页面在某些时候可能需要再安装一些Feature，提供这种能力给外部，在某些时机加载其他Feature
     */
    val INSTALL_EXTRA_FEATURE_ACTION_TYPE = ActionType("action_install_extra_feature")

    /**
     * 当横竖屏切换时，修改state
     */
    val CHANGE_ORIENTATION_TYPE = ActionType("action_change_orientation")
}

/**
 * Desc: 使用对象来表示Action，增强组件之间的隔离，并可以动态设置Action带的参数
 */
open class Action<T: Any>(var type: ActionType, var payload: T?) {

    override fun toString(): String {
        return "Action{name='${type.name}'}"
    }
}