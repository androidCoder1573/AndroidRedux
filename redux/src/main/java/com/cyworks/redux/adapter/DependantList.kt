package com.cyworks.redux.adapter

import com.cyworks.redux.component.LiveComponent
import com.cyworks.redux.dependant.Dependant
import com.cyworks.redux.interceptor.InterceptorManager
import com.cyworks.redux.state.State
import com.cyworks.redux.types.DependantCreator
import com.cyworks.redux.types.Interceptor

data class ListDependant<PS : State>(
    var data: Any?,
    var type: String,
    var depend: Dependant<State, PS>?,
    var isDetach: Boolean = false
)

class DependantList<PS : State> {
    // 用于保存adapter下的子组件的拦截器
    protected var interceptorManager: InterceptorManager = InterceptorManager()
        private set

    private var builder: DependantCreator<PS>? = null

    private var depMap = HashMap<Int, ListDependant<PS>>()

    constructor(builder: DependantCreator<PS>) {
        this.builder = builder
    }

    fun getInterceptor(): Interceptor<State> {
        return interceptorManager.getInterceptor()
    }

    fun getListItemType(index: Int): String {
        val listDep = depMap[index]
        if (listDep != null) {
            return listDep.type
        }
        return ""
    }

    /**
     * 此函数需具有记忆功能，如果实例没变，返回的dependant不能，
     * 这里分两级：
     * 1、查看对应的索引是否有值，如果没有则新建
     * 2、查看对应的索引对应的value是否有变化，如果有则新建
     * @param index
     * @param type
     * @param data
     */
    fun <CS : State> buildDependant(index: Int, type: String, data: Any): Dependant<CS, PS> {
        var dep: Dependant<CS, PS>
        var listDep = this.depMap[index]
        if (listDep != null) {
//            if (JSON.stringify(listDep.data) !== JSON.stringify(data)) {
//                if (listDep.type === type) {
//                    // 如果类型一致，则直接更新props
//                    listDep.data = data;
//                    const logic = listDep?.depend.getLogic()
//                    if (logic != null) {
//                        (logic as LiveComponent<State>).onPropsChanged(data)
//                    }
//                } else {
//                    // 如果类型不一致，则新建
//                    this.removeNotVisibleItem(index);
//                    listDep = this.makeDep(type, data);
//                    this.depMap[index] = listDep
//                }
//            }

            if (listDep.isDetach) {
                listDep.isDetach = false
                val logic = listDep.depend?.logic
                listDep.isDetach = true
                if (logic != null) {
                    (logic as LiveComponent<State>).attach()
                }
            }
            dep = listDep.depend!! as Dependant<CS, PS>
        } else {
            listDep = this.makeDep(type, data)
            dep = listDep.depend!! as Dependant<CS, PS>
            depMap[index] = listDep
        }

        return dep
    }

    fun removeNotVisibleItem(index: Int) {
        depMap.remove(index)
    }

    fun detachDep(index: Int): ListDependant<PS>? {
        val listDep = depMap[index]
        if (listDep?.isDetach == false) {
            val logic = listDep.depend?.logic
            listDep.isDetach = true
            if (logic != null) {
                (logic as LiveComponent<State>).detach()
            }
        }
        return listDep
    }

    private fun makeDep(type: String, data: Any): ListDependant<PS> {
        val dep = builder?.create(type, data)
        dep?.mergeInterceptor(interceptorManager)
        return ListDependant(data, type, dep)
    }
}



