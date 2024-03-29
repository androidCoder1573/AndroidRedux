package com.cyworks.redux.interceptor

import androidx.collection.ArrayMap
import com.cyworks.redux.action.Action
import com.cyworks.redux.action.ActionType
import com.cyworks.redux.adapter.ReduxAdapter
import com.cyworks.redux.state.State
import com.cyworks.redux.types.Dispose
import com.cyworks.redux.types.Interceptor

class InterceptorManager {
    /**
     * Interceptor 集合, map存储
     * key: [ActionType]
     */
    private var funcMap = ArrayMap<ActionType, ArrayList<InterceptorBean<State>>>()

    val adapters = ArrayList<ReduxAdapter<State>>()

    @Suppress("UNCHECKED_CAST")
    private val interceptor: Interceptor<State> = Interceptor { action, ctx ->
        doActionInner(action as Action<Any>)
    }

    fun addInterceptorEx(collector: InterceptorCollector<State>): ArrayList<Dispose>? {
        val map = collector.interceptorMap
        val size = map.size
        val disposeList = ArrayList<Dispose>()

        for (i in 0 until size) {
            val type = map.keyAt(i)
            val bean = map[type] ?: continue

            var list = this.funcMap[type]
            if (list == null) {
                list = ArrayList()
                this.funcMap[type] = list
            }

            list.add(bean)
            disposeList.add {
                val index: Int = list.indexOf(bean)
                if (index >= 0) {
                    list.removeAt(index)
                }
            }
        }

        return disposeList
    }

    fun addAdapter(adapter: ReduxAdapter<State>): Dispose {
        this.adapters.add(adapter)
        return {
            this.adapters.remove(adapter)
        }
    }

    fun getInterceptor(): Interceptor<State> {
        return this.interceptor
    }

    private fun doActionInner(action: Action<Any>) {
        if (this.funcMap.size < 1) {
            return
        }

        val payload = action.payload ?: return

        if (payload !is InterceptorPayload) {
            return
        }

        val array = this.funcMap[payload.realAction.type]
        if (array != null && array.size > 0) {
            array.forEach {
                val ctx = it.ctxProvider?.getCtx()
                if (ctx != null) {
                    it.interceptor?.doAction(payload.realAction, ctx)
                }
            }
        }

        this.adapters.forEach {
            it.doInterceptorAction(action)
        }
    }
}