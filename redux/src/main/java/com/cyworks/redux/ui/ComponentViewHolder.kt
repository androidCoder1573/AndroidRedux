package com.cyworks.redux.ui

import android.util.SparseArray
import android.view.View
import androidx.annotation.IdRes

/**
 * 缓存当前组件的View实例
 */
class ComponentViewHolder(private val rootView: View) {
    /**
     * 根View下挂载的view集合
     */
    private val mViewMap: SparseArray<View> = SparseArray<View>()

    /**
     * 根据View的id来返回view实例
     * @param id View对应的id
     * @return View实例
     */
    fun <T : View> getView(@IdRes id: Int): T {
        var view: View? = mViewMap.get(id)
        if (view == null) {
            view = rootView.findViewById(id)
            mViewMap.put(id, view)
        }
        return view as T
    }

    fun dispose() {
        mViewMap.clear()
    }
}