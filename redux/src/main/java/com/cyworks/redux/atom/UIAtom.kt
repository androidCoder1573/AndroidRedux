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
        val isChanged = isChanged(changedKeys, state)
        if (isChanged) {
            onChanged?.update(state, oldProps, holder)
        }
    }
}