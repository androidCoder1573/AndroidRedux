package com.cyworks.redux.atom

import com.cyworks.redux.state.State
import com.cyworks.redux.types.DepProps
import com.cyworks.redux.types.OnUIAtomChanged
import com.cyworks.redux.ui.ComponentViewHolder

/**
 * 用于UI Updater中收集Atom，将UI更新颗粒化。
 */
class UIPropsWatcher<S : State> : PropsWatcher<S, UIAtom<S>>() {
    fun watch(dep: DepProps, changed: OnUIAtomChanged<S>) {
        val atom = UIAtom<S>()
        atom.setDepProps(dep)
        atom.setAtomChangedCB(changed)
        atomList.add(atom)
    }

    /**
     * 当数据有更新时，通过此方法触发每个Atom进行更新
     * @param state 当前最新的State
     */
    internal fun update(state: S, changedKeys: HashSet<String>?, holder: ComponentViewHolder?) {
        for (i in 0 until atomList.size) {
            val atom = atomList[i]
            atom.doAtomChange(state, changedKeys, holder)
        }
//        for (atom in atomList) {
//            atom.doAtomChange(state, changedKeys, holder)
//        }
    }
}