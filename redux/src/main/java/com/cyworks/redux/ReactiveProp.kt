package com.cyworks.redux

import androidx.annotation.CallSuper
import com.cyworks.redux.types.Dispose
import java.lang.RuntimeException
import java.util.HashMap

/**
 * Desc: 将属性封装成一个ReactiveProp，包括属性值，属性类型，是否是私有数据等。
 * 只有属性用此对象来包裹的时候，才会更新UI。
 *
 * 第二版: 通过编译期注解来生成state，将state中的属性声明为：Key-Value，这种方式的
 * 劣势显而易见，Key-Value会导致类型缺失，各种强转也导致了开发低效。
 *
 * 第三版: 使用ReactiveProp包裹具体的属性，通过这种方式保留了属性的类型，
 * 同时也方便的区分了UI所需的属性以及普通属性。
 *
 * 私有数据的作用？
 * 对于一个组件来说，绝大多数的数据都是私有数据，如果所有数据都是公开的，会导致page state变得特别庞大，
 * 处理组件任意数据的更新也将变得繁琐。
 * 所以这里将只属于某个组件的私有数据设置成私有属性，发生变化后只在组件内部更新。
 * 因此：
 * 对page的reducer来说，因为都是处理公共数据，所以会将变化直接通知到store中，
 * 而针对子组件，通过SubReducer这个概念对子组件的Reducer进行包装(类似aop)，
 * 在reducer执行成功之后对更新的状态进行分类，如果状态变化的是私有数据，则直接在组件内部循环，
 * 如果更新的公开数据，则通过store在全局更新。
 *
 * 为什么不直接在ReactiveProp中通知每个关联属性更新?
 * 如果类似live data的方式更新，将导致UI频繁刷新，因为一个数据对应一块UI，假设一次Reducer更新了10个数据，
 * 将会调用UI更新callback 10次，性能上会有一定的损耗。
 *
 * @author randytu
 */
class ReactiveProp<T> @JvmOverloads constructor(
    /**
     * 当前属性的value对应class type
     */
    private val mType: Class<*>,
    /**
     * 当前属性的真实value
     */
    private var mValue: T,
    /**
     * 当前属性所在的State
     */
    private val mState: State,
    /**
     * 首次渲染组件时，是否通知UI更新
     */
    val isUpdateWithInitValue: Boolean = false
) {
    /**
     * 主要用于状态变化时记录状态的改变情况
     */
    private var mStateProxy: StateProxy? = null

    /**
     * 属性对应的Key，用于State中进行属性key-value关联
     */
    var key: String? = null

    /**
     * 当前属性依赖的父属性
     */
    private var mParent: ReactivePropParent<T>? = null

    /**
     * 对当前属性的依赖集合
     */
    private var mDepMap: HashMap<String, ReactiveProp<T>>? = null

    /**
     * 是否是私有属性, 默认是私有属性
     */
    private var isPrivate = true
    /**
     * 这个判断只针对首次渲染界面时有效，用于判断首次首次渲染组件时，是否通知对应的UI进行更新。
     *
     * @return 是否需要在首次渲染时触发UI更新
     */

    /**
     * 属性detach的接口
     */
    private var mPropDispose: Dispose? = null

    /**
     * 当前属性所在的State的类名
     */
    val token: String

    /**
     * 依赖父组件的prop
     *
     * @param parent 要依赖的属性
     */
    fun dependantProp(parent: ReactiveProp<T>?) {
        if (mParent != null || parent == null) {
            return
        }
        val rootParent = parent.rootProp

        // 检查父属性是否是全局store
        val propParent = parent.mParent
        if (propParent != null
            && propParent.mFromType == ReactivePropParent.FROM_GLOBAL_STORE
        ) {
            dependantGlobalProp(propParent.mGlobalStore, rootParent)
            return
        }

        // 这里直接使用最终的parent，中间parent就不需要管了
        setParent(rootParent, ReactivePropParent.FROM_UPPER_COMPONENT, null)
    }

    /**
     * 其实每个属性上的parent目前都是直接赋值到root，目前写了一些兼容逻辑
     *
     * @return 当前属性依赖的根属性
     */
    val rootProp: ReactiveProp<T>?
        get() {
            var tempParent = this
            var propParent = mParent
            while (propParent != null) {
                val temp = propParent.mProp!!.mParent ?: break
                propParent = temp
            }
            if (propParent != null) {
                tempParent = propParent.mProp!!
            }
            return tempParent
        }

    /**
     * 依赖全局store的prop
     *
     * @param parent 全局store对应的State的属性
     */
    fun dependantGlobalProp(store: BaseGlobalStore<*>?, parent: ReactiveProp<T>?) {
        setParent(parent, ReactivePropParent.FROM_GLOBAL_STORE, store)
    }

    private fun setParent(parent: ReactiveProp<T>?, type: Int, store: BaseGlobalStore<*>?) {
        mPropDispose = parent!!.addChild(this)
        parent.mState.addDependState(token)
        mParent = ReactivePropParent()
        mParent!!.mProp = parent
        mParent!!.mFromType = type
        mParent!!.mGlobalStore = store
        mValue = parent.mValue
    }

    private fun addChild(prop: ReactiveProp<T>?): Dispose? {
        if (prop == null) {
            return null
        }
        if (mDepMap == null) {
            mDepMap = HashMap()
        }
        mDepMap!![prop.token] = prop
        return { mDepMap!!.remove(prop.token) }
    }

    fun getChild(token: String): ReactiveProp<T>? {
        return if (mDepMap == null) {
            null
        } else mDepMap!![token]
    }

    /**
     * 注入state代理，主要用于监听状态变化并记录状态
     *
     * 为什么要注入一个代理对象，主要还是为了隔离，在reducer中state具有更新能力，
     * 在其他场景，state应该只能读取不能再进行设置了
     *
     * @param stateProxy [StateProxy]
     */
    fun setStateProxy(stateProxy: StateProxy?) {
        mStateProxy = stateProxy
    }

    /**
     * 更新某个value，仅能在Reducer中调用，会触发更新收集。
     * note：原始类型一定要一致，最好使用拷贝来进行赋值。
     *
     * @param value Object
     * @throws RuntimeException 当参数为空或者参数类型不一致时抛出异常
     */
    fun set(value: T) {
        // 防止开发者在非Reducer中更新UI属性
        if (mStateProxy == null) {
            throw RuntimeException("can't set prop value when StateProxy null!")
        }

        // 组件在修改state prop过程中, 不能改全局store的属性
        if (mParent != null && mParent!!.mFromType == ReactivePropParent.FROM_GLOBAL_STORE) {
            throw RuntimeException("component can't change global state prop!")
        }
        innerSetter(value)

        // 记录哪些数据变更了
        mStateProxy!!.recordChangedProp(this)
    }

    /**
     * 框架内部设置value，不触发变更收集
     *
     * @param value 待设置的属性的value
     */
    fun innerSetter(value: T) {
        if (value.javaClass != mType) {
            throw RuntimeException(
                "can't set value when class type not same!"
                        + " real type is " + mType.simpleName
            )
        }
        mValue = value
    }

    fun value(): T {
        return mValue
    }

    /**
     * 用于检查当前属性是否是组件的私有属性:
     * 1、组件本身是私有属性
     * 2、组件的无父属性
     * 3、当前属性无依赖
     *
     * @return boolean 是否是私有属性
     */
    val isPrivateProp: Boolean
        get() = isPrivate && mParent == null && (mDepMap == null || mDepMap!!.isEmpty())

    /**
     * 当属性关联了父组件的属性，组件detach时，需要解除这种关联状态。
     * 目前主要在全局store上使用。
     */
    @CallSuper
    protected fun detach() {
        if (mPropDispose != null) {
            mPropDispose?.let { it() }
            mState.removeDependState(token)
        }
    }

    /**
     * Desc: 针对依赖的属性，这里使用此类来封装
     *
     * @author randytu on 2021/4/14
     */
    class ReactivePropParent<TP> {
        /**
         * 包装类对应的真实属性
         */
        var mProp: ReactiveProp<TP>? = null

        /**
         * 当前依赖属性的依赖来源
         */
        var mFromType = FROM_UPPER_COMPONENT

        /**
         * 如果依赖的属性来自全局store，这里需要标明全局store
         */
        var mGlobalStore: BaseGlobalStore<*>? = null

        companion object {
            /**
             * 当前依赖的属性来自父组件
             */
            const val FROM_UPPER_COMPONENT = 1

            /**
             * 当前依赖的属性来自全局store
             */
            const val FROM_GLOBAL_STORE = 2
        }
    }

    /**
     * 创建一个ReactiveProp
     *
     * @param mType 当前属性的类型
     * @param mValue 当前属性的真实值
     * @param mState 当前持有该属性的state
     * @param isUpdateWithInitValue 是否要触发首次更新，比如某些属性需要等网路请求的数据，所以增加此变量
     */
    init {
        isPrivate = mState !is BasePageState
        mState.checkPropWhenCreate()
        token = mState.javaClass.name
    }
}