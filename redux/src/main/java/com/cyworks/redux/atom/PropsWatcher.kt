package com.cyworks.redux.atom

import com.cyworks.redux.state.State
import com.cyworks.redux.types.DepProps
import com.cyworks.redux.ui.ComponentViewHolder

/**
 * 用于UI Updater中收集Atom，将UI更新颗粒化。
 */
class PropsWatcher<S : State> {
    /**
     * 原子化UI的更新列表
     */
    private val atomList = ArrayList<Atom<S>>()

    fun watch(dep: DepProps, changed: OnAtomChanged<S>) {
        val atom = Atom<S>()
        atom.setDep(dep)
        atom.setAtomChangedCB(changed)
        atomList.add(atom)
    }

    /**
     * 当数据有更新时，通过此方法触发每个Atom进行更新
     *
     * @param state 当前最新的State
     */
    fun update(state: S, holder: ComponentViewHolder?) {
        for (atom in atomList) {
            atom.doAtomChange(state, holder)
        }
    }

    fun clear() {
        atomList.clear()
    }
}