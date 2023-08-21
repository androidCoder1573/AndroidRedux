package com.cyworks.redux.atom

import com.cyworks.redux.state.State
import com.cyworks.redux.types.OnUIAtomChanged
import com.cyworks.redux.ui.ComponentViewHolder

class UIAtom<S : State> : Atom<S>() {
    /**
     * 当前依赖的属性关联的UI更新函数
     */
    private var onChanged: OnUIAtomChanged<S>? = null

    fun setAtomChangedCB(onAtomChanged: OnUIAtomChanged<S>?) {
        this.onChanged = onAtomChanged
    }

    fun doAtomChange(state: S, changedKeys: List<String>?, holder: ComponentViewHolder?) {
        val isChanged = isChanged(changedKeys)
        if (isChanged) {
            if (newProps != null && oldProps != null) {
                for (i in 0 until newProps!!.size) {
                    oldProps!![i] = newProps!![i]
                }
            }

            if (newProps != null && keyList != null) {
                for (i in 0 until newProps!!.size) {
                    val key: String = keyList!![i]
                    val prop = state.dataMap[key]
                    if (prop != null) {
                        newProps!![i] = prop.value()!!
                    }
                }
            }
            onChanged?.update(state, oldProps, holder)
        }
    }
}