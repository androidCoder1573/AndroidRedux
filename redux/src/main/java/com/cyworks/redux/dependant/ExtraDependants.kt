package com.cyworks.redux.dependant

import androidx.collection.ArrayMap
import com.cyworks.redux.state.State

/**
 * 目前存在这样的需求：如果开发者想要延迟安装一些组件，可以在某些时机发送
 * INSTALL_EXTRA_FEATURE_ACTION
 */
class ExtraDependants<S : State> {
    /**
     * 保存额外Dep
     */
    var extra: ArrayMap<String, Dependant<out State, S>>? = null

    /**
     * 给page添加一个额外的dep
     * @param dependant [Dependant]
     */
    fun addExtDependant(dependant: Dependant<out State, S>) {
        if (extra == null) {
            extra = ArrayMap<String, Dependant<out State, S>>()
        }

        val key = dependant.hashCode().toString()
        if (extra!!.containsKey(key)) {
            return
        }
        extra!![key] = dependant
    }
}