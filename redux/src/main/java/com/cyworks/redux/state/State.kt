package com.cyworks.redux.state

import android.content.res.Configuration
import android.util.Log
import com.cyworks.redux.ReduxManager
import com.cyworks.redux.prop.ReactiveProp
import com.cyworks.redux.types.PropertySet
import com.cyworks.redux.util.ILogger
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties
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
     * 用于存放组件响应式数据的Map, 框架内部创建并使用，开发者无感知。
     * key: 某个属性对应的key；
     * value: PropValue, 某个属性对应的值；
     */
    internal val dataMap = HashMap<String, ReactiveProp<Any>>()

    /**
     * 用于存放组件响应式数据的Map, 方便框架进行修改数据，开发者无感知。
     * key: 某个属性对应的key；
     * value: 修改属性的函数；
     */
    private val propertyMap = HashMap<String, PropertySet<Any>>()

    /**
     * 用于加快查询设置的map，主要用于绑定全局store上
     * key: 依赖状态对应的类名；
     * value: 如果有则写入1；
     */
    private val depGlobalStateMap = HashMap<JvmType.Object, Int>()

    /**
     * state代理，主要用于当State中某些属性变化时记录这些属性的变化，方便后续处理
     */
    private var stateProxy: StateProxy? = null

    /**
     * 是否已经进行了merge state操作，此操作每个对象仅能进行一次
     */
    private var stateHasMerged = false

    /**
     * 是否正在执行state merge操作
     */
    private var isMergingState = false

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
    internal var stateType: StateType? = null

    /**
     * Log 组件，组件内共享
     */
    private val logger: ILogger = ReduxManager.instance.logger

    /**
     * 页面内部默认的属性，表示当前的横竖屏状态
     */
    var currentOrientation: Int by ReactUIData(Configuration.ORIENTATION_PORTRAIT)

    /**
     * 组件内部默认的属性，表示当前是否需要懒加载，此属性定位为私有属性
     */
    var isShowUI: Boolean by ReactUIData(true)

    /**
     * 是否调用了detect函数进行key-value映射
     */
    private var calledDetect: Boolean = false

    /**
     * 标记哪些属性需要排除
     */
    private val excludePropMap = HashMap<String, Int>()

    /**
     * 当前state的token，用于标记一个state
     */
    internal val token: JvmType.Object = JvmType.Object("${System.currentTimeMillis()}")

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

    init {
        excludePropMap["excludePropMap"] = 1
        excludePropMap["dataMap"] = 1
        excludePropMap["propertyMap"] = 1
        excludePropMap["depGlobalStateMap"] = 1
        excludePropMap["stateProxy"] = 1
        excludePropMap["stateHasMerged"] = 1
        excludePropMap["isMergingState"] = 1
        excludePropMap["depState"] = 1
        excludePropMap["curDepPropKey"] = 1
        excludePropMap["stateType"] = 1
        excludePropMap["logger"] = 1
        excludePropMap["calledDetect"] = 1
        excludePropMap["privatePropChanged"] = 1
        excludePropMap["publicPropChanged"] = 1
        excludePropMap["token"] = 1
    }

    /**
     * 当子组件依赖父组件的属性时，通过此方法设置父组件的state，方便自组件进行属性依赖
     */
    internal fun setParentState(parentState: State) {
        depState = parentState
    }

    internal fun startMergeState() {
        isMergingState = true
    }

    internal fun endMergeState() {
        isMergingState = false
        stateHasMerged = true
        depState = null
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
        stateProxy = proxy
        for (prop in dataMap.values) {
            prop.setStateProxy(proxy)
        }
    }

    internal fun detectField() {
        if (calledDetect) {
            return
        }
        calledDetect = true

        val kClass = this.javaClass.kotlin
        kClass.memberProperties.forEach {
            if (!excludePropMap.containsKey(it.name) && !dataMap.containsKey(it.name) && !it.isAbstract) {
                // it.isAccessible = true
                try {
                    it.getter.call(this@State)
                } catch (e: Throwable) {
                    logger.w("state detect", "${e.cause}")
                }
                // it.isAccessible = true
            }
        }
        excludePropMap.clear()
    }

    /**
     * 当某个属性对外有依赖时，依赖的父属性发生更新时，此时需要更新当前属性，但是此时不能触发更新收集
     */
    internal fun innerSetProp(key: String, value: Any) {
        logger.i("State", "innerSetProp: key: $key, value: $value")
        propertyMap[key]?.let { it(value) }
    }

    internal fun addTheStateToGlobalState(token: JvmType.Object) {
        depGlobalStateMap[token] = DEPENDANT_STATE_FLAG
    }

    internal fun removeTheStateFromGlobalState(token: JvmType.Object) {
        depGlobalStateMap.remove(token)
    }

    /**
     * 是否依赖了某个state，主要优化对全局store的依赖过程
     * @param token 全局store对应的token
     */
    internal fun isTheStateDependGlobalState(token: JvmType.Object): Boolean {
        val result = depGlobalStateMap[token] ?: return false
        return result == DEPENDANT_STATE_FLAG
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
    internal fun detach() {
        if (dataMap.isEmpty()) {
            return
        }

        for (key in dataMap.keys) {
            val value = dataMap[key]
            value?.detach()
        }
    }

    private fun depProp(curProp: ReactiveProp<Any>) {
        if (!isMergingState || stateHasMerged || depState == null) {
            logger.e("State", "this step can not dep the prop from parent")
            return
        }

        val parentReactiveProp = depState!!.findProp()
        if (parentReactiveProp != null) {
            // 执行依赖
            if (parentReactiveProp.myStateIsGlobalState()) {
                logger.i("State", "dep global prop")
                curProp.depGlobalProp(parentReactiveProp)
            } else {
                logger.i("State", "dep parent prop")
                curProp.depUpperComponentProp(parentReactiveProp)
            }
        } else {
            logger.w("State", "can not find the prop from parent,"
                    + " please check to dep global prop")
        }
    }

     private fun findProp(): ReactiveProp<Any>? {
        return dataMap[curDepPropKey]
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

    inner class ReactUIData<V : Any?>(initialValue: V, private val updateValueByInit: Boolean = true) : ReadWriteProperty<Any?, V> {
        private var value = initialValue

        @Suppress("UNCHECKED_CAST")
        private fun checkDataMap(key: String, value: V, set: PropertySet<V>) {
            if (propertyMap[key] == null) {
                propertyMap[key] = set as PropertySet<Any>
            }
            if (dataMap[key] == null) {
                logger.d("State", "create ui ReactiveProp $key, state: ${this@State.javaClass.name}")
                val prop = ReactiveProp(value, this@State, true, updateValueByInit)
                prop.key = key
                dataMap[key] = prop as ReactiveProp<Any>
            }
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): V {
            val key = property.name
            curDepPropKey = key
            checkDataMap(key, value) {
                value = it
            }
            return value
        }

        /**
         * 设置属性的值，，首次设置的时候要设置依赖关系
         */
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
            val name = property.name
            Log.d("state", "call start prop setValue: $name, state: ${this@State.javaClass.name}")

            checkDataMap(name, this.value) {
                this.value = it
            }

            val prop = dataMap[name]
            if ((stateType == StateType.COMPONENT_TYPE) && isMergingState && !stateHasMerged) {
                if (prop != null) {
                    Log.d("state", "start dep ui prop: $name")
                    depProp(prop)
                }

                this.value = value
                return
            }

            if (!calledDetect || prop?.canSet(value as Any) == true) {
                this.value = value
            }
        }
    }

    /**
     * 内部类，用于实现响应式属性的委托
     */
    open inner class ReactLogicData<V : Any?>(initialValue: V, private val updateValueByInit: Boolean = true) : ReadWriteProperty<Any?, V> {
        private var value = initialValue

        @Suppress("UNCHECKED_CAST")
        private fun checkDataMap(key: String, value: V, set: PropertySet<V>) {
            if (propertyMap[key] == null) {
                propertyMap[key] = set as PropertySet<Any>
            }
            if (dataMap[key] == null) {
                val prop = ReactiveProp(value, this@State, false, updateValueByInit)
                prop.key = key
                dataMap[key] = prop as ReactiveProp<Any>
            }
        }

        /**
         * 获取属性的值
         */
        override fun getValue(thisRef: Any?, property: KProperty<*>): V {
            val key = property.name
            curDepPropKey = key
            checkDataMap(key, value) {
                value = it
            }
            return value
        }

        /**
         * 设置属性的值，，首次设置的时候要设置依赖关系
         */
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
            val name = property.name
            checkDataMap(name, this.value) {
                this.value = it
            }

            val prop = dataMap[name]
            if ((stateType == StateType.COMPONENT_TYPE) && isMergingState && !stateHasMerged) {
                if (prop != null) {
                    depProp(prop)
                }

                this.value = value
                return
            }

            if (!calledDetect || prop?.canSet(value as Any) == true) {
                this.value = value
            }
        }
    }

    companion object {
        /**
         * 将依赖全局store的state的类名添加进来并用此标记
         */
        private const val DEPENDANT_STATE_FLAG = 1

        @JvmStatic
        fun <S : State> copyState(state: S): S {
            state.setStateProxy(null)
            return state
        }
    }
}