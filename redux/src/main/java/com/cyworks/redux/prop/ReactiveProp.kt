package com.cyworks.redux.prop

import androidx.collection.ArrayMap
import com.cyworks.redux.ReduxManager
import com.cyworks.redux.state.State
import com.cyworks.redux.state.StateType
import com.cyworks.redux.types.Dispose
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

enum class PropFromType {
    FROM_UPPER_COMPONENT, // 当前依赖的属性来自父组件
    FROM_GLOBAL_STORE, // 当前依赖的属性来自全局store
}

/**
 * 将属性封装成一个ReactiveProp，包括属性值，属性类型，是否是私有数据等。
 *
 * 私有数据的作用？
 * 对于一个组件来说，绝大多数的数据都是私有数据，如果所有数据都是公开的，会导致page state变得特别庞大，
 * 处理组件任意数据的更新也将变得繁琐。
 * 所以这里将只属于某个组件的私有数据设置成私有属性，发生变化后只在组件内部更新。
 *
 * 因此：
 * 在reducer执行成功之后对更新的状态进行分类，如果状态变化的是私有数据，则直接在组件内部循环，
 * 如果更新的公开数据，则通过store在全局更新。
 *
 * 为什么不直接在ReactiveProp中通知每个关联属性更新?
 * 如果类似LiveData的方式更新，将导致UI频繁刷新，
 * 假设一次Reducer更新了10个数据，将会调用UI更新callback 10次，性能上会有一定的损耗。
 */
class ReactiveProp<T>(
    propValue: T,
    s: State,
    val isUIProp: Boolean,
    val isUpdateWithInitValue: Boolean = true, // 首次渲染组件时，是否通知UI更新
) {
    /**
     * 属性对应的Key，用于State中进行属性key-value关联
     */
    internal var key: String? = null

    /**
     * 当前属性的真实value
     */
    private var value: T? = propValue

    private var version: Int = 0

    /**
     * 是否是私有属性, 默认是私有属性
     */
    private var isPrivate = true

    /**
     * 对当前属性的依赖集合
     */
    private var depMap: ArrayMap<JvmType.Object, ReactiveProp<T>>? = null

    /**
     * 当前属性依赖的父属性
     */
    private var parentProp: ReactivePropParent<T>? = null

    /**
     * 当前属性所在的State
     */
    private var state: State = s

    private var dispose: Dispose? = null

    /**
     * 用于检查当前属性是否是组件的私有属性:
     * 组件本身是私有属性;
     * 组件的无父属性;
     * 当前属性无依赖;
     * @return boolean 是否是私有属性
     */
    internal val isPrivateProp: Boolean
        get() = isPrivate && parentProp == null && (depMap == null || depMap!!.isEmpty)

    internal val isDepGlobalState: Boolean
        get() = parentProp?.fromType == PropFromType.FROM_GLOBAL_STORE

    /**
     * 每个属性上的parent目前都是直接赋值到root，目前写了一些兼容逻辑
     * @return 当前属性依赖的根属性
     */
    internal val rootProp: ReactiveProp<T>
        get() {
            var tempParent = this
            var propParent = parentProp
            while (propParent != null) {
                val temp = propParent.prop?.parentProp ?: break
                propParent = temp
            }
            if (propParent != null) {
                tempParent = propParent.prop!!
            }
            return tempParent
        }

    /**
     * 框架内部设置value，不触发变更收集
     * @param value 待设置的属性的value
     */
    internal fun innerSetter(value: T?) {
        this.value = value
        this.state.innerSetProp(key ?: "", value as Any)
    }

    internal fun canSet(value: T): Boolean {
        // 防止开发者在非Reducer中更新UI属性
        val stateProxy = state.grabStateProxy()
        if (stateProxy == null) {
            ReduxManager.instance.logger.e("ReactiveProp",
                "set prop: ${this.key} can not use = operator, when not call updateState func, state: ${state.javaClass.name}")
            return false
        }

        // 组件在修改state prop过程中, 不能改全局store的属性
        if (parentProp != null && parentProp!!.fromType == PropFromType.FROM_GLOBAL_STORE) {
            ReduxManager.instance.logger.e("ReactiveProp", "component can not change global state prop!!!")
            return false
        }

        // 记录哪些数据变更了
        @Suppress("UNCHECKED_CAST")
        stateProxy.recordChangedProp(this as ReactiveProp<Any>)
        this.value = value

        return true
    }

    /**
     * 依赖父组件的prop
     * @param prop 要依赖的属性
     */
    internal fun depUpperComponentProp(prop: ReactiveProp<T>) {
        if (this.parentProp != null) {
            return
        }

        val rootParent = prop.rootProp

        // 检查父属性是否是全局store
        val propParent = prop.parentProp
        if (propParent !== null && propParent.fromType == PropFromType.FROM_GLOBAL_STORE) {
            depGlobalProp(rootParent)
            return
        }

        // 这里直接使用最终的parent，中间parent就不需要管了
        setParent(rootParent, PropFromType.FROM_UPPER_COMPONENT)
    }

    /**
     * 依赖全局store的prop
     *
     * @param parent 全局store对应的State的属性
     */
    internal fun depGlobalProp(parent: ReactiveProp<T>) {
        setParent(parent, PropFromType.FROM_GLOBAL_STORE)
    }

    internal fun myStateIsGlobalState(): Boolean {
        return this.state.stateType == StateType.GLOBAL_TYPE
    }

    internal fun value(): T? {
        return value
    }

    /**
     * 更新某个value，仅能在Reducer中调用，会触发更新收集。
     *
     * @param value Object
     * @throws RuntimeException 当参数为空或者参数类型不一致时抛出异常
     */
    internal fun set(value: T) {
        // 防止在非Reducer中更新UI属性
        val stateProxy = state.grabStateProxy()
            ?: throw RuntimeException("can't set prop value when StateProxy null!!!")

        // 组件在修改state prop过程中, 不能改全局store的属性
        if (parentProp != null && parentProp!!.fromType == PropFromType.FROM_GLOBAL_STORE) {
            throw RuntimeException("component can't change global state prop!!!")
        }

        innerSetter(value)
        // 记录哪些数据变更了
        @Suppress("UNCHECKED_CAST")
        stateProxy.recordChangedProp(this as ReactiveProp<Any>)
    }

    internal fun attach() {
        if (parentProp?.prop != null) {
            dispose = parentProp?.prop?.addChild(this)
            parentProp?.prop?.value?.let { innerSetter(it) }
        }
    }

    /**
     * 当属性关联了父组件的属性，组件detach时，需要解除这种关联状态。
     * 目前主要在全局store上使用。
     */
    internal fun detach() {
        dispose?.let { it() }
    }

    private fun setParent(prop: ReactiveProp<T>, type: PropFromType) {
        dispose = prop.addChild(this)

        // 如果当前依赖的是全局store，则将当前state的token写入全局store中，方便后续快速查找
        if (type == PropFromType.FROM_GLOBAL_STORE) {
            prop.state.addTheStateToGlobalState(stateToken())
        }

        parentProp = ReactivePropParent(prop)
        parentProp!!.fromType = type
        value = prop.value
    }

    internal fun stateToken(): JvmType.Object {
        return state.token
    }

    private fun addChild(child: ReactiveProp<T>): Dispose {
        if (depMap == null) {
            depMap = ArrayMap()
        }

        val key = child.stateToken()
        depMap!![key] = child
        return { depMap!!.remove(key) }
    }

    internal fun getChild(token: JvmType.Object): ReactiveProp<T>? {
        return if (depMap == null) {
            null
        } else depMap!![token]
    }

    /**
     * 针对依赖的属性，使用此类来封装
     */
    inner class ReactivePropParent<TP> constructor(prop: ReactiveProp<TP>) {
        /**
         * 包装类对应的真实属性
         */
        var prop: ReactiveProp<TP>? = prop

        /**
         * 当前依赖属性的依赖来源
         */
        var fromType = PropFromType.FROM_UPPER_COMPONENT
    }
}