package com.cyworks.redux

import android.content.res.Configuration
import com.cyworks.redux.State.Reactive
import com.cyworks.redux.util.ILogger
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

enum class StateType {
    PageType,
    ComponentType,
    GlobalType,
}

typealias PropertySet<T> = (value: T) -> Unit

/**
 * Desc: 状态基类，state中只有用[Reactive]委托的属性才有响应式
 */
abstract class State {
    /**
     * 用于存放组件所用的数据的Map, 方便框架进行修改数据，框架内部创建并使用，开发者无感知。
     * key: 某个属性对应的key
     * value: PropValue, 某个属性对应的值；
     */
    private val dataMap = ConcurrentHashMap<String, ReactiveProp<Any>>()

    private val propertyMap = HashMap<String, PropertySet<Any>>()

    /**
     * 用于加快查询设置的map，主要用于绑定全局store上
     * key: 依赖状态对应的类名；
     * value: 如果有则写入1；
     */
    private val depGlobalStateMap = HashMap<String, Int>()

    /**
     * state代理，主要用于当State中某些属性变化时记录这些属性的变化，方便后续处理
     */
    private var stateProxy: StateProxy? = null

    /**
     * 是否已经进行了merge state操作，此操作每个对象仅能进行一次
     */
    private var hasMergeState = false

    /**
     * 是否正在执行state merge操作
     */
    private var isMerging = false

    /**
     * 在进行属性依赖期间，用于表示当前state要依赖的父state
     */
    private var depState: State? = null

    /**
     * 在进行属性依赖期间，用于表示当前state的属性要依赖的父属性的key
     */
    private var curDepPropKey: String? = null

    /**
     * 当前state的类型，用于后续属性依赖的来源
     */
    var stateType: StateType? = null

    /**
     * Log 组件，组件内共享
     */
    private val logger: ILogger = ReduxManager.instance.logger

    /**
     * 页面内部默认的属性，表示当前的横竖屏状态
     */
    var currentOrientation: Int by Reactive(Configuration.ORIENTATION_PORTRAIT)

    /**
     * 组件内部默认的属性，表示当前是否需要懒加载，此属性定位为私有属性
     */
    var isShowUI: Boolean by Reactive(true)

    /**
     * 获取组件私有数据变化的情况, 在子组件的reducer执行完成后调用，仅限框架内部使用。
     */
    internal val privatePropChanged: List<ReactiveProp<Any>>?
        get() = if (stateProxy != null) {
            stateProxy!!.privatePropChanged
        } else null

    /**
     * 获取公开数据变化的情况，公开数据指的是依赖了父组件的数据，
     * 在子组件的reducer执行完成后调用，仅限框架内部使用。
     */
    internal val publicPropChanged: List<ReactiveProp<Any>>?
        get() = if (stateProxy != null) {
            stateProxy!!.publicPropChanged
        } else null

    /**
     * 如果状态内存在公共状态，且公共状态发生变化，则认为当前属性属性有变化，
     * 此时框架需要通知关联了此数据的组件。
     *
     * @return 公共数据是否发生变化
     */
    internal val isChanged: Boolean
        get() = if (stateProxy != null) {
            stateProxy!!.isChanged
        } else false

    /**
     * 当子组件依赖父组件的属性时，通过此方法设置父组件的state，方便自组件进行属性依赖
     */
    internal fun setWillMergedState(willMergeState: State) {
        depState = willMergeState
    }

    internal fun startMerging() {
        isMerging = true
    }

    internal fun endMerging() {
        isMerging = false
        hasMergeState = true
    }

    /**
     * 设置当前State的类型，会在组件的onCreateState之后设置，框架内调用
     */
    internal fun setType(type: StateType) {
        this.stateType = type
    }

    /**
     * 用于记录哪些状态变化了，仅限框架内部使用
     *
     * 为什么要注入一个代理对象，而不是通过state本身来处理？主要目的还是为了隔离：
     * 在reducer中，state具有更新能力，可以修改属性，
     * 而在其他场景，应该只能读取state中的属性，不能再对其进行修改了。
     *
     * @param stateProxy [StateProxy]
     */
    internal fun setStateProxy(stateProxy: StateProxy?) {
        this.stateProxy = stateProxy
        for (prop in dataMap.values) {
            prop.setStateProxy(stateProxy)
        }
    }

    /**
     * 当某个属性对外有依赖时，依赖的父属性发生更新时，此时需要更新当前属性，
     * 但是此时不能触发更新收集
     */
    internal fun innerSetProp(key: String, value: Any) {
        propertyMap[key]?.let { it(value) }
    }

    internal fun addDepGlobalState(token: String) {
        depGlobalStateMap[token] = DEPENDANT_STATE_FLAG
    }

    internal fun removeDepGlobalState(token: String) {
        depGlobalStateMap.remove(token)
    }

    /**
     * 是否依赖了某个state，主要优化对全局store的依赖过程
     * @param token 全局store对应的类名
     * @return 是否依赖
     */
    internal fun isDependGlobalState(token: String): Boolean {
        val result = depGlobalStateMap[token] ?: return false
        return result == DEPENDANT_STATE_FLAG
    }

    internal fun attach() {
        for (key in dataMap.keys()) {
            val value = dataMap[key]
            value?.attach()
        }
    }

    /**
     * detach 时清除依赖的属性
     */
    internal fun detach() {
        if (dataMap.isEmpty()) {
            return
        }

        for (key in dataMap.keys()) {
            val value = dataMap[key]
            value?.detach()
        }
    }

    /**
     * 由于目前是主动依赖属性，所以退出时，需要将依赖删除
     */
    fun clear() {
        detach()
        dataMap.clear()
        propertyMap.clear()
        depGlobalStateMap.clear()
    }

    private fun depProp(curProp: ReactiveProp<Any>) {
        if (!isMerging || hasMergeState || depState == null) {
            return
        }

        val parentReactiveProp = depState!!.findProp()
        if (parentReactiveProp != null) {
            // 执行依赖
            if (parentReactiveProp.myStateIsGlobalState()) {
                curProp.depGlobalProp(parentReactiveProp)
            } else {
                curProp.depUpperComponentProp(parentReactiveProp)
            }
        } else {
            logger.w("", "can not dep prop, because can not find the prop,"
                    + " please check is use the whole prop to dep")
        }
    }

     private fun findProp(): ReactiveProp<Any>? {
        val key = curDepPropKey
        return dataMap[key]
    }

    inner class Reactive<V : Any?>(initialValue: V) : ReadWriteProperty<Any?, V> {
        private var value = initialValue

        private fun checkDataMap(key: String, value: Any, set: PropertySet<V>) {
            if (dataMap[key] == null) {
                propertyMap[key] = set as PropertySet<Any>
                val prop = ReactiveProp(value, this@State);
                prop.key = key
                dataMap[key] = prop
            }
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): V {
            val key = property.name
            curDepPropKey = key
            checkDataMap(key, value as Any) {
                value = it
            }
            return value
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, v: V) {
            val name = property.name
            checkDataMap(name, value as Any) {
                value = it
            }

            val prop = dataMap[name]
            if (stateType === StateType.ComponentType && isMerging && !hasMergeState) {
                if (prop != null) {
                    depProp(prop)
                }

                this.value = v
                return
            }

            if (prop?.canSet(v as Any, stateProxy) == true) {
                this.value = v
            }
        }

    }

    companion object {
        /**
         * 将依赖全局store的state的类名添加进来并用此标记
         */
        private const val DEPENDANT_STATE_FLAG = 1

        @JvmStatic
        fun <S : State?> copyState(state: S?): S? {
            if (state == null) {
                return null
            }
            state.setStateProxy(null)
            return state
        }
    }
}