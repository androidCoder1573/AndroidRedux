package com.cyworks.redux.state

import android.content.res.Configuration
import android.util.ArrayMap
import com.cyworks.redux.ReduxManager
import com.cyworks.redux.prop.ReactiveProp
import com.cyworks.redux.types.PropertySet
import com.cyworks.redux.util.ILogger
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

enum class StateType {
    PAGE_TYPE,
    COMPONENT_TYPE,
    GLOBAL_TYPE,
}

/**
 * 状态基类，state中只有委托的属性才有响应式
 */
abstract class State {
    /**
     * 当前state的token，用于标记一个state
     */
    internal val token: JvmType.Object
            = JvmType.Object("${this.javaClass.name}_${System.currentTimeMillis()}")

    /**
     * 用于存放组件响应式数据的Map, 框架内部创建并使用，开发者无感知
     * key: 某个属性对应的key
     * value: ReactiveProp, 属性对应的值
     */
    internal val dataMap = ArrayMap<String, ReactiveProp<Any>>()

    /**
     * 当框架内部修改响应式数据的时候，为了不触发依赖收集，将设置函数保存下来
     * key: 某个属性对应的key
     * value: 修改属性的函数
     */
    private val propertyMap = ArrayMap<String, PropertySet<Any>>()

    /**
     * 用于加快查询设置的map，主要用于绑定全局store上
     * key: 依赖状态对应的类名
     * value: 如果有则写入1
     */
    private val depGlobalStateMap = ArrayMap<JvmType.Object, String>()

    /**
     * 当State中某些属性变化时记录这些属性的变化，方便后续处理
     */
    private var stateProxy: StateProxy? = null

    private val depHelper = DepHelper()

    /**
     * 当前state的类型，用于后续属性依赖的来源
     */
    internal var stateType: StateType? = null

    private val logger: ILogger = ReduxManager.instance.logger

    /**
     * 获取组件私有数据变化的情况, 在子组件的reducer执行完成后调用，仅限框架内部使用。
     */
    internal val privatePropChanged: List<ReactiveProp<Any>>?
        get() = if (stateProxy != null) {
            stateProxy!!.changedPrivateProps
        } else null

    /**
     * 获取公开数据变化的情况，公开数据指的是依赖了父组件的数据，
     * 在子组件的reducer执行完成后调用，仅限框架内部使用。
     */
    internal val publicPropChanged: List<ReactiveProp<Any>>?
        get() = if (stateProxy != null) {
            stateProxy!!.changedPublicProps
        } else null

    /**
     * 页面内部默认的属性，表示当前的横竖屏状态
     */
    var currentOrientation: Int by ReactUIData(Configuration.ORIENTATION_PORTRAIT)

    /**
     * 组件内部默认的属性，表示当前是否需要懒加载，此属性定位为私有属性
     */
    var isShowUI: Boolean by ReactUIData(true)

    internal fun setTargetState(target: State) {
        depHelper.setTargetState(target)
    }

    internal fun startMergeState() {
        depHelper.startMergeState()
    }

    internal fun endMergeState() {
        depHelper.endMergeState()
    }

    internal fun startCollectAtomKey() {
        depHelper.startCollectAtomKey()
    }

    internal fun endCollectAtomKey() {
        depHelper.endCollectAtomKey()
    }

    internal fun atomKeyList(): ArrayList<String> {
        return depHelper.atomKeyList()
    }

    /**
     * 用于记录哪些状态变化了，仅限框架内部使用
     *
     * 为什么要注入一个代理对象，而不是通过state本身来处理？主要目的还是为了隔离：
     * 在reducer中，state具有更新能力，可以修改属性，
     * 而在其他场景，应该只能读取state中的属性，不能再对其进行修改了。
     *
     * @param proxy [StateProxy]
     */
    internal fun setStateProxy(proxy: StateProxy?) {
        if (proxy == null && stateProxy == null) {
            return
        }

        stateProxy = proxy
        for (prop in dataMap.values) {
            prop.setStateProxy(proxy)
        }
    }

    internal fun detectField(list: Collection<KProperty1<out State, *>>) {
        depHelper.detectField(list, this)
    }

    /**
     * 当某个属性对外有依赖时，依赖的父属性发生更新时，此时需要更新当前属性，但是此时不能触发更新收集
     */
    internal fun innerSetProp(key: String, value: Any) {
        propertyMap[key]?.let { it(value) }
    }

    internal fun addTheStateToGlobalState(token: JvmType.Object) {
        if (stateType == StateType.GLOBAL_TYPE) {
            depGlobalStateMap[token] = DEPENDANT_STATE_FLAG
        }
    }

    internal fun removeTheStateFromGlobalState(token: JvmType.Object) {
        if (stateType == StateType.GLOBAL_TYPE) {
            depGlobalStateMap.remove(token)
        }
    }

    /**
     * 是否依赖了某个state，主要优化对全局store的依赖过程
     * @param token 全局store对应的token
     */
    internal fun isTheStateDependGlobalState(token: JvmType.Object): Boolean {
        if (stateType != StateType.GLOBAL_TYPE) {
            return false
        }
        val result = depGlobalStateMap[token] ?: return false
        return result == DEPENDANT_STATE_FLAG
    }

    internal fun findProp(): ReactiveProp<Any>? {
        return dataMap[depHelper.curDepPropKey]
    }

    internal fun attach() {
        for (key in dataMap.keys) {
            val value = dataMap[key]
            value?.attach()
        }
    }

    /**
     * detach 时清除依赖的属性
     */
    private fun detach() {
        if (dataMap.isEmpty()) {
            return
        }

        for (key in dataMap.keys) {
            val value = dataMap[key]
            value?.detach()
        }
    }

    /**
     * 由于目前是主动依赖属性，所以退出时，需要将依赖删除
     */
    open fun clear() {
        detach()
        dataMap.clear()
        propertyMap.clear()
        depGlobalStateMap.clear()
    }

    /**
     * 内部类，用于实现UI字段响应式属性的委托
     */
    inner class ReactUIData<V : Any?>(initialValue: V, private val updateValueByInit: Boolean = true) : ReadWriteProperty<Any?, V> {
        private var value = initialValue

        @Suppress("UNCHECKED_CAST")
        private fun checkDataMap(key: String, value: V, set: PropertySet<V>) {
            if (stateType == StateType.GLOBAL_TYPE
                && (key == CURRENT_ORIENTATION_NAME || key == IS_SHOW_UI_NAME)) {
                return
            }

            logger.d("State",
                "create ui ReactiveProp $key, state: ${this@State.javaClass.name}")

            propertyMap[key] = set as PropertySet<Any>
            val prop = ReactiveProp(value, this@State, true, updateValueByInit)
            prop.key = key
            dataMap[key] = prop as ReactiveProp<Any>
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): V {
            val name = property.name

            if (stateType == StateType.GLOBAL_TYPE
                && (name == CURRENT_ORIENTATION_NAME || name == IS_SHOW_UI_NAME)) {
                return this.value
            }

            depHelper.recordDepPropKey(name)

            if (propertyMap[name] == null) {
                checkDataMap(name, value) {
                    value = it
                }
            }

            return value
        }

        /**
         * 设置属性的值, 首次设置的时候要设置依赖关系
         */
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
            val name = property.name

            if (stateType == StateType.GLOBAL_TYPE
                && (name == CURRENT_ORIENTATION_NAME || name == IS_SHOW_UI_NAME)) {
                this.value = value
                return
            }

            if (propertyMap[name] == null) {
                checkDataMap(name, value) {
                    this.value  = it
                }
            }

            val prop = dataMap[name]
            if ((stateType == StateType.COMPONENT_TYPE) && depHelper.canMergeState()) {
                depHelper.depProp(prop)
                this.value = value
                return
            }

            if (depHelper.canSetPropValue(prop, value as Any)) {
                this.value = value
            }
        }
    }

    /**
     * 内部类，用于实现逻辑字段响应式属性的委托
     */
    open inner class ReactLogicData<V : Any?>(initialValue: V, private val updateValueByInit: Boolean = true) : ReadWriteProperty<Any?, V> {
        private var value = initialValue

        @Suppress("UNCHECKED_CAST")
        private fun checkDataMap(key: String, value: V, set: PropertySet<V>) {
            propertyMap[key] = set as PropertySet<Any>
            val prop = ReactiveProp(value, this@State, false, updateValueByInit)
            prop.key = key
            dataMap[key] = prop as ReactiveProp<Any>
        }

        /**
         * 获取属性的值
         */
        override fun getValue(thisRef: Any?, property: KProperty<*>): V {
            val key = property.name
            depHelper.recordDepPropKey(key)
            if (propertyMap[key] == null) {
                checkDataMap(key, value) {
                    this.value  = it
                }
            }
            return value
        }

        /**
         * 设置属性的值，首次设置的时候要设置依赖关系
         */
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
            val name = property.name
            if (propertyMap[name] == null) {
                checkDataMap(name, value) {
                    this.value  = it
                }
            }

            val prop = dataMap[name]
            if ((stateType == StateType.COMPONENT_TYPE) && depHelper.canMergeState()) {
                depHelper.depProp(prop)
                this.value = value
                return
            }

            if (depHelper.canSetPropValue(prop, value as Any)) {
                this.value = value
            }
        }
    }

    companion object {
        /**
         * 将依赖全局store的state的类名添加进来并用此标记
         */
        private const val DEPENDANT_STATE_FLAG = "1"

        const val CURRENT_ORIENTATION_NAME = "currentOrientation"
        const val IS_SHOW_UI_NAME = "isShowUI"

        @JvmStatic
        fun <S : State> copyState(state: S): S {
            state.setStateProxy(null)
            return state
        }
    }
}