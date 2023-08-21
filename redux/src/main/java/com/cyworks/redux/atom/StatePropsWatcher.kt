package com.cyworks.redux.atom

import com.cyworks.redux.ReduxContext
import com.cyworks.redux.state.State
import com.cyworks.redux.types.DepProps
import com.cyworks.redux.types.OnLogicAtomChanged

class StatePropsWatcher<S : State> {
    private val atomList = ArrayList<LogicAtom<S>>()

    fun watch(dep: DepProps, changed: OnLogicAtomChanged<S>) {
        val atom = LogicAtom<S>()
        atom.setDepProps(dep)
        atom.setAtomChangedCB(changed)
        atomList.add(atom)
    }

    /**
     * 当数据有更新时，通过此方法触发每个Atom进行更新
     * @param state 当前最新的State
     */
    fun update(state: S, ctx: ReduxContext<S>) {
        for (atom in atomList) {
            atom.doAtomChange(state, ctx)
        }
    }

    fun clear() {
        atomList.clear()
    }
}