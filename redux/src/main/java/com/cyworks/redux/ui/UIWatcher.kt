package com.cyworks.redux.ui

import com.cyworks.redux.State
import com.cyworks.redux.types.DepProps
import java.util.ArrayList
import java.util.HashMap

/**
 * Desc: 用于UI Updater中收集Atom，将UI更新颗粒化。
 * @author randytu
 */
class UIWatcher<S : State> {
    /**
     * 保存OnAtomChanged跟ReactiveProp数组的对应关系。
     */
    private var atomMap: HashMap<DepProps, OnAtomChanged<S>>? = null

    /**
     * 原子化UI的更新列表
     */
    private val atomList = ArrayList<Atom<S>>()

    /**
     * 类似与前端的Computed函数，根据属性设置对应的UI更新函数
     * @param props 当前UI Atom依赖的属性数组
     * @param changed 属性变化对应的UI变化
     */
    fun watch(dep: DepProps, changed: OnAtomChanged<S>) {
        if (atomMap == null) {
            atomMap = HashMap<DepProps, OnAtomChanged<S>>()
        }
        atomMap!![dep] = changed
    }

    /**
     * 生成Atom
     * @param state 当前组件对应的State
     * @param module 当前组件对应的ViewModule
     */
    fun setAtom(state: S, module: ViewModule<S>?) {
        if (module == null) {
            return
        }
        module.mapPropToAtom(state, this)
        if (atomMap != null && atomMap!!.isNotEmpty()) {
            for ((key, value) in atomMap!!) {
                val atom = Atom<S>()
                atom.setDep(key)
                atom.setAtomChangedCB(value)
                atomList.add(atom)
            }
        }
    }

    /**
     * 当UI数据有更新时，通过此方法触发每个Atom进行更新
     *
     * @param state 当前最新的State
     * @param keys 当前更新的属性列表
     * @param holder 当前UI组件的View Holder
     */
    fun updateUI(state: S, holder: ComponentViewHolder?) {
        for (atom in atomList) {
            atom.doAtomChange(state, holder)
        }
    }
}