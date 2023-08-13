package com.cyworks.redux.interceptor

import com.cyworks.redux.ReduxContext
import com.cyworks.redux.state.State
import com.cyworks.redux.action.Action
import com.cyworks.redux.action.ActionType
import com.cyworks.redux.types.Dispose
import com.cyworks.redux.types.Interceptor

class InterceptorManager {
    /**
     * Interceptor 集合, map存储
     * key: [ActionType]
     */
    private var funcMap = HashMap<ActionType, ArrayList<InterceptorBean<State>>>()

    val adapters = ArrayList<Adapter<State>>()

    val interceptor: Interceptor<State> = object : Interceptor<State> {
        override fun doAction(action: Action<Any>, ctx: ReduxContext<State>?) {
            doActionEx(action)
        }
    }

    open fun addInterceptorEx(collector: InterceptorCollect<State>): ArrayList<Dispose>? {
        val map = collector.interceptorMap
        if (collector.isEmpty) {
            return null
        }

        val disposeList = ArrayList<Dispose>()
        for (action in map.keys) {
            val bean = map[action] ?: continue

            var list = this.funcMap[action.type]
            if (list == null) {
                list = ArrayList()
                this.funcMap[action.type] = list
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

    open fun addAdapter(adapter: Adapter<State>): Dispose? {
        this.adapters.push(adapter);
        return {
            val index = this.adapters.indexOf(adapter);
            if (index >= 0) {
                this.adapters.splice(index, 1)
            }
        }
    }

    private fun doActionEx(action: Action<Any>) {
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
                val ctx = it.provider?.provider()
                if (ctx != null) {
                    it.interceptor?.doAction(payload.realAction, ctx)
                }
            }
        }

        this.adapters.forEach {
            it.doInterceptorAction(action);
        }
    }
}