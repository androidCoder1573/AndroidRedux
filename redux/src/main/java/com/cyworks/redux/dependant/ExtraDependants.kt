package com.cyworks.redux.dependant

import com.cyworks.redux.Dependant
import com.cyworks.redux.state.State
import java.util.HashMap

/**
 * Desc: 存放额外的Feature, 目前存在这样的需求：
 * 如果开发者想要延迟安装一些组件，可以在某些时机发送
 * INSTALL_EXTRA_FEATURE_ACTION
 *
 * 组件通过此结构封装。
 */
class ExtraDependants<S : State> {
    /**
     * 保存额外feature的map
     */
    var mExtraFeature: HashMap<String, Dependant<out State, S>>? = null

    /**
     * 给page添加一个额外的feature
     * @param dependant [Dependant]
     */
    fun addExtDependant(dependant: Dependant<out State, S>) {
        if (mExtraFeature == null) {
            mExtraFeature = HashMap<String, Dependant<out State, S>>()
        }
        val key = "" + dependant.hashCode()
        if (mExtraFeature!!.containsKey(key)) {
            return
        }
        mExtraFeature!![key] = dependant
    }
}