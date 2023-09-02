package com.cyworks.redux.atom

import com.cyworks.redux.state.State

abstract class PropsWatcher<S : State, A : Atom<S>> {
    protected val atomList = ArrayList<A>()

    internal fun generateKeyList(state: S) {
        for (atom in atomList) {
            val dep = atom.dep
            state.startCollectAtomKey()
            dep?.let { it() }
            state.endCollectAtomKey()
            val list: ArrayList<String> = state.atomKeyList()
            atom.addKeyList(list)
        }
    }

    fun clear() {
        atomList.clear()
    }
}