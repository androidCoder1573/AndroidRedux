package com.cyworks.redux.state

import com.cyworks.redux.ReduxManager
import com.cyworks.redux.prop.ReactiveProp
import com.cyworks.redux.util.ILogger
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

class DepHelper {
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
    private var targetState: State? = null

    /**
     * 在进行属性依赖期间，用于表示当前state的属性要依赖的父属性的key
     */
    internal var curDepPropKey: String = ""

    /**
     * 是否调用了detect函数进行key-value映射
     */
    private var calledDetect: Boolean = false

    /**
     * 标记State中哪些属性需要排除
     */
    private val excludePropMap = HashMap<String, Int>()

    private val keyList = ArrayList<String>()

    private var isCollectAtomKey = false

    init {
        excludePropMap["dataMap"] = 1
        excludePropMap["propertyMap"] = 1
        excludePropMap["depGlobalStateMap"] = 1
        excludePropMap["stateProxy"] = 1
        excludePropMap["depHelper"] = 1
        excludePropMap["stateType"] = 1
        excludePropMap["logger"] = 1
        excludePropMap["privatePropChanged"] = 1
        excludePropMap["publicPropChanged"] = 1
        excludePropMap["token"] = 1
    }

    fun detectField(list: Collection<KProperty1<out State, *>>, state: State) {
        if (calledDetect) {
            return
        }
        val time = System.currentTimeMillis()
        calledDetect = true

        list.forEach {
            if (!excludePropMap.containsKey(it.name) && !state.dataMap.containsKey(it.name) && !it.isAbstract) {
                try {
                    it.getter.call(state)
                } catch (e: Throwable) {
                    ReduxManager.instance.logger.w(ILogger.ERROR_TAG, "${e.cause}")
                }
            }
        }

        excludePropMap.clear()
        ReduxManager.instance.logger.d(ILogger.PERF_TAG,
            "detect state filed consume: ${System.currentTimeMillis() - time}ms, in State: ${state.javaClass.name}")
    }

    internal fun depProp(prop: ReactiveProp<Any>?) {
        if (prop == null) {
            return
        }

        if (!isMergingState || stateHasMerged || targetState == null) {
            ReduxManager.instance.logger.e("Dep Collect",
                "this step can not dep the prop from parent")
            return
        }

        val parentReactiveProp = targetState?.findProp()
        if (parentReactiveProp != null) {
            // 执行依赖
            if (parentReactiveProp.myStateIsGlobalState()) {
                prop.depGlobalProp(parentReactiveProp)
            } else {
                prop.depUpperComponentProp(parentReactiveProp)
            }
        }
    }

    fun canMergeState(): Boolean {
        return isMergingState && !stateHasMerged
    }

    fun canSetPropValue(prop: ReactiveProp<Any>?, value: Any): Boolean {
        return !calledDetect || prop?.canSet(value) == true
    }

    /**
     * 当子组件依赖父组件的属性时，通过此方法设置父组件的state，方便自组件进行属性依赖
     */
    internal fun setTargetState(target: State) {
        targetState = target
    }

    internal fun startMergeState() {
        isMergingState = true
    }

    internal fun endMergeState() {
        isMergingState = false
        stateHasMerged = true
        targetState = null
    }

    internal fun recordDepPropKey(key: String) {
        curDepPropKey = key
        if (isCollectAtomKey) {
            keyList.add(key)
        }
    }

    internal fun startCollectAtomKey() {
        isCollectAtomKey = true
    }

    internal fun endCollectAtomKey() {
        isCollectAtomKey = false
    }

    internal fun atomKeyList(): ArrayList<String> {
        isCollectAtomKey = false
        val list = ArrayList(keyList)
        keyList.clear()
        return list
    }
}