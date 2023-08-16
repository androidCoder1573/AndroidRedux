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

    fun doAtomChange(state: S, holder: ComponentViewHolder?) {
        val changedProps = dep?.let { it() }
        if (changedProps.isNullOrEmpty()) {
            return
        }

        val isChanged = isEqual(changedProps, watchedProps)
        if (isChanged) {
            val oldDeps = watchedProps
            watchedProps = changedProps
            onChanged?.update(state, oldDeps, holder)
        }
    }
}