package com.cyworks.redux.atom

import com.cyworks.redux.ReduxContext
import com.cyworks.redux.state.State
import com.cyworks.redux.types.OnLogicAtomChanged

class LogicAtom<S : State> : Atom<S>() {
    private var onChanged: OnLogicAtomChanged<S>? = null

    fun setAtomChangedCB(onAtomChanged: OnLogicAtomChanged<S>?) {
        this.onChanged = onAtomChanged
    }

    fun doAtomChange(state: S, changedKeys: List<String>?, ctx: ReduxContext<S>?) {
        val isChanged = isChanged(changedKeys, state)
        if (isChanged) {
            onChanged?.update(state, oldProps, ctx)
        }
    }
}