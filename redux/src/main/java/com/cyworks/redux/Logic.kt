package com.cyworks.redux

import android.os.Bundle
import com.cyworks.redux.action.InnerActionTypes
import com.cyworks.redux.logic.EffectCollect
import com.cyworks.redux.logic.LogicModule
import com.cyworks.redux.types.Effect
import com.cyworks.redux.util.ILogger

/**
 * Desc: Page以及Component的基类，封装组件和页面的公共逻辑，一个组件可能会有多个子组件。
 *
 * 关于一些逻辑对象的初始化问题：
 * 如果需要一些跟view相关的对象，比如：滚动事件监听等，我们可以在ViewBuilder中初始化View，然后
 * 在Effect更新。当然，这些对象最终会保持在state中，不过这些对象不能使用ReactiveProp来包裹。
 *
 * 针对复杂逻辑对象，内部可能有状态，建议使用ReduxObject来包裹。
 */
abstract class Logic<S : State>(b: Bundle) {
    /**
     * 组件的Effect
     */
    protected var effect: Effect<S>? = null

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
    protected var bundle: Bundle = b

    /**
     * Log 组件，组件内共享
     */
    protected val logger: ILogger = ReduxManager.instance.logger

    /**
     * 初始Effect以及一些依赖
     */
    init {
        initCollect()
    }

    private fun initCollect() {
        // 初始化Reducer
        var logicModule: LogicModule<S>? = getLogicModule()
        if (logicModule != null) {
            logicModule = object : LogicModule<S> {
                override fun addLocalEffects(collect: EffectCollect<S>) {}
            }
        }

        // 初始化Effect
        val effectCollect: EffectCollect<S> = EffectCollect()
        logicModule.addLocalEffects(effectCollect)
        // 检查Effect的注册, 并注入一些框架内部的Action
        checkEffect(effectCollect)
        effect = effectCollect.effect
    }

    protected fun initEffect() {
        // 初始化Reducer
        val logicModule: LogicModule<S>? = getLogicModule()
        this.logicModule = logicModule
        if (this.logicModule != null) {
            this.logicModule.addLocalEffects(this.effectCollect, this.context.state)
        }
        this.effectCollect.remove(InnerActionTypes.INTERCEPT_ACTION)
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
     * 获取实例类型
     */
    abstract fun getType(): LogicType

    /**
     * LogicModule，用户主动设置
     * @return LogicModule
     */
    abstract fun getLogicModule(): LogicModule<S>?

    /**
     * 获取依赖的子组件集合
     *
     * @return Map 子组件集合
     */
    abstract fun getChildrenDependant(): Map<String, Dependant<State, S>>?
}