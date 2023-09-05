package com.cyworks.redux.dependant

import androidx.collection.ArrayMap
import com.cyworks.redux.state.State

/**
 * 如果某个组件需要依赖一些子组件，则需要使用本类来收集这些子组件。
 *
 * 针对列表Adapter的特殊处理：
 * 如果组件存在列表UI，由于Android的列表是比较特殊的存在：RecyclerView + Adapter;(本身就是数据驱动)
 * 如果框架不对Adapter进行处理，那么必将会在ViewUpdater中操作Adapter，
 * 跟我们最初的设计相违背（ViewUpdater中只有UI，操作Adapter相当于对数据进行处理。）
 * 因此将Adapter变相设计成为一个LRLogic，让Adapter本身变成响应式，
 * 这样就能很好的融入整个框架，但是对Adapter做了限制：一个组件只能设置一个列表型UI，目的是让组件的粒度更加细致。
 */
class DependentCollector<PS : State> {
    /**
     * 组件依赖器集合
     */
    private var dependants: ArrayMap<String, Dependant<out State, PS>>? = null

    /**
     * 当前组件持有的Adapter，一个组件只能持有一个Adapter，组件粒度按列表划分
     */
    // private var rootAdapterDependant: Dependant<State, PS>? = null

//    var adapterDependant: Dependant<State, PS>?
//        get() = rootAdapterDependant
//
//        set(dependant) {
//            rootAdapterDependant = dependant
//        }

    internal val dependantMap: ArrayMap<String, Dependant<out State, PS>>?
        get() = dependants

    /**
     * 给组件添加一个子组件依赖
     *
     * @param dependant [Dependant]
     */
    fun addDependant(dependant: Dependant<out State, PS>) {
        if (dependants == null) {
            dependants = ArrayMap()
        }
        val key = "" + dependant.hashCode()
        if (dependants!!.containsKey(key)) {
            return
        }
        dependants!![key] = dependant
    }

    fun clear() {
        if (dependants != null) {
            dependants!!.clear()
        }
    }
}