package com.cyworks.redux

import android.os.Bundle
import com.tencent.redux.effect.Effect

/**
 * Desc: Page以及Component的基类，封装组件和页面的公共逻辑，一个组件可能会有多个子组件。
 *
 * 关于一些逻辑对象的初始化问题：
 * 如果需要一些跟view相关的对象，比如：滚动事件监听等，我们可以在ViewBuilder中初始化View，然后
 * 在Effect更新。当然，这些对象最终会保持在state中，不过这些对象不能使用ReactiveProp来包裹。
 *
 * 针对复杂逻辑对象，内部可能有状态，建议使用ReduxObject来包裹。
 */
abstract class Logic<S : State?>(bundle: Bundle) : ILogic<S> {
    /**
     * 组件的Reducer
     */
    @JvmField
    protected var mReducer: Reducer<S>? = null

    /**
     * 组件的Effect
     */
    protected var mEffect: Effect<S>? = null

    /**
     * 组件的Context
     */
    var context: ReduxContext<S>? = null
        protected set

    /**
     * 用于保存从父组件继承下来的属性
     */
    var environment: Environment? = null
        protected set

    /**
     * 创建页面时，携带的Bundle参数
     */
    @JvmField
    protected var mBundle: Bundle

    /**
     * 用于监听本组件的属性变化
     */
    private var mWatcher: PropWatcher<S>? = null

    /**
     * Log 组件，组件内共享
     */
    @JvmField
    protected val mLogger: ILogger = ReduxManager.instance.logger
    private fun initCollect() {
        // 初始化Reducer
        var logicModule: LogicModule<S> = getLogicModule()
        if (logicModule == null) {
            logicModule = object : LogicModule<S>() {
                fun registerReducer(@NonNull collect: ReducerCollect<S>?) {}
                fun registerEffect(@NonNull collect: EffectCollect<S>?) {}
            }
        }

        // 初始化Reducer
        val reducerCollect: ReducerCollect<S> = ReducerCollect()
        logicModule.registerReducer(reducerCollect)
        // 检查reducer的注册情况，防止覆盖内部action
        checkReducer(reducerCollect)
        mReducer = reducerCollect.getReducer()

        // 初始化Effect
        val effectCollect: EffectCollect<S> = EffectCollect()
        logicModule.registerEffect(effectCollect)
        // 检查Effect的注册, 并注入一些框架内部的Action
        checkEffect(effectCollect)
        mEffect = effectCollect.getEffect()

        // 收集订阅的属性
        mWatcher = PropWatcher()
        subscribeProps(mWatcher)
    }

    val propWatcher: PropWatcher<S>?
        get() = mWatcher

    /**
     * 合并当前组件下的reducer为一个大Reducer
     *
     * @param list 用于存放组件的Reducer
     * @param connector 组件连接器
     */
    open fun mergeReducer(@NonNull list: MutableList<SubReducer?>, connector: LRConnector<*, *>?) {
        if (connector != null) {
            list.add(connector.subReducer(mReducer))
        }
    }

    /**
     * 检查当前组件注册的Reducer，主要做几件事情：
     * 1、检查组件有没有注册过框架内部的Action，这些外部注册需要无效化
     * 2、重新注册框架内部的Action
     *
     * @param reducerCollect ReducerCollect
     */
    protected open fun checkReducer(reducerCollect: ReducerCollect<S>?) {
        // sub class impl
    }

    /**
     * 检查当前组件注册的Effect，主要做几件事情：
     * 1、检查组件有没有注册过框架内部的Action，这些外部注册需要无效化
     * 2、重新注册框架内部的Action
     *
     * @param effectCollect EffectCollect
     */
    protected open fun checkEffect(effectCollect: EffectCollect<S>?) {
        // sub class impl
    }

    /**
     * 通过这个接口来订阅自己组件下的属性变化，这里需要调用watchProp注入
     *
     * @param watcher 属性订阅器
     */
    protected fun subscribeProps(watcher: PropWatcher<S>?) {
        // sub class impl
    }

    /**
     * 初始Reducer/Effect以及一些依赖
     * @param bundle 页面带来的参数
     */
    init {
        mBundle = bundle
        initCollect()
    }
}