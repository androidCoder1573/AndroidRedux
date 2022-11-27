package com.cyworks.redux.adapter

import androidx.recyclerview.widget.RecyclerView
import com.cyworks.redux.action.Action
import com.cyworks.redux.types.IAdapter

/**
 * Desc: 如果RecyclerView存在多层嵌套，那么这里其实还是需要子Adapter，子Adapter必须存在一个父亲
 */
abstract class SubAdapter(parentAdapter: IAdapter) : IAdapter {
    /**
     * 当前RecyclerView对应的Adapter
     */
    protected var mAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>? = null

    /**
     * 如果存在父亲，则所有的交互都依赖RootAdapter, 否则自己是Root
     */
    protected var mParentAdapter: IAdapter

    /**
     * 获取根Adapter, 主要用于二级或者更多级列表发送Action
     * @return RootAdapter
     */
    private val rootAdapter: RootAdapter<Any>?
        private get() {
            var adapter: IAdapter = mParentAdapter ?: return null
            if (mParentAdapter is RootAdapter<*>) {
                return mParentAdapter
            }
            while (adapter != null) {
                if (adapter is RootAdapter<*>) {
                    return adapter
                } else if (adapter is SubAdapter) {
                    adapter = (adapter as SubAdapter).mParentAdapter
                }
            }
            return null
        }

    fun dispatchReducer(action: Action<Any>) {
        val adapter: RootAdapter<*>? = rootAdapter
        adapter?.dispatchReducer(action)
    }

    fun dispatchEffect(action: Action<Any>) {
        val adapter: RootAdapter<*>? = rootAdapter
        adapter?.dispatchEffect(action)
    }

    /**
     * 构造器，SubAdapter构造时需要传递parent。
     * @param parentAdapter BaseAdapter
     */
    init {
        mParentAdapter = parentAdapter
    }
}