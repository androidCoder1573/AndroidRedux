package com.cyworks.redux.atom

import com.cyworks.redux.ReduxContext
import com.cyworks.redux.state.State
import com.cyworks.redux.types.OnLogicAtomChanged

class LogicAtom<S : State> : Atom<S>() {
    private var onChanged: OnLogicAtomChanged<S>? = null

    fun setAtomChangedCB(onAtomChanged: OnLogicAtomChanged<S>?) {
        this.onChanged = onAtomChanged
    }

    fun doAtomChange(state: S, ctx: ReduxContext<S>?) {
        val changedProps = dep?.let { it() }
        if (changedProps.isNullOrEmpty()) {
            return
        }

//        val isChanged = !isEqual(changedProps, watchedProps)
//        if (isChanged) {
//            val oldDeps = watchedProps
//            watchedProps = changedProps
//            onChanged?.update(state, oldDeps, ctx)
//        }
    }
}