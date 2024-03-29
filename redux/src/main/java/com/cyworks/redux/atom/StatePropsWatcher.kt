package com.cyworks.redux.atom

import com.cyworks.redux.ReduxContext
import com.cyworks.redux.state.State
import com.cyworks.redux.types.DepProps
import com.cyworks.redux.types.OnLogicAtomChanged

class StatePropsWatcher<S : State> : PropsWatcher<S, LogicAtom<S>>() {

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
    fun update(state: S, changedKeys: HashSet<String>?, ctx: ReduxContext<S>) {
        val size = atomList.size
        for (i in 0 until size) {
            val atom = atomList[i]
            atom.doAtomChange(state, changedKeys, ctx)
        }
    }
}