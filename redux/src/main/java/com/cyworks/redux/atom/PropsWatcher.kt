package com.cyworks.redux.atom

import com.cyworks.redux.state.State

abstract class PropsWatcher<S : State, A : Atom<S>> {
    protected val atomList = ArrayList<A>()

    internal fun generateKeyList(state: S) {
        val size = atomList.size
        for (i in 0 until size) {
            val atom = atomList[i]
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