package com.cyworks.redux.component

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.collection.ArrayMap
import com.cyworks.redux.IController
import com.cyworks.redux.ReduxContext
import com.cyworks.redux.ReduxManager
import com.cyworks.redux.atom.StatePropsWatcher
import com.cyworks.redux.dependant.Dependant
import com.cyworks.redux.interceptor.InterceptorBean
import com.cyworks.redux.interceptor.InterceptorManager
import com.cyworks.redux.logic.EffectCollector
import com.cyworks.redux.logic.LogicModule
import com.cyworks.redux.state.State
import com.cyworks.redux.types.Dispose
import com.cyworks.redux.types.Effect
import com.cyworks.redux.util.Environment
import com.cyworks.redux.util.ILogger

/**
 * Page以及Component的基类，封装组件和页面的公共逻辑，一个组件可能会有多个子组件。
 *
 * 关于一些逻辑对象的初始化问题：
 * 如果需要一些跟view相关的对象，比如：滚动事件监听等，我们可以在ViewBuilder中初始化View，然后
 * 在Effect更新。当然，这些对象最终会保持在state中，不过这些对象不能使用ReactiveProp来包裹。
 *
 * 针对复杂逻辑对象，内部可能有状态，建议使用ReduxObject来包裹。
 */
abstract class Logic<S : State>(p: Bundle?) {
    /**
     * 组件的Effect
     */
    internal var effect: Effect<S>? = null

    /**
     * 组件的Context
     */
    lateinit var context: ReduxContext<S>
        protected set

    /**
     * 用于保存从父组件继承下来的属性
     */
    internal lateinit var environment: Environment

    /**
     * 创建页面时，携带的Bundle参数
     */
    protected var props: Bundle? = p

    protected val logger: ILogger = ReduxManager.instance.logger

    /**
     * 每个组件对应的拦截器解注册
     */
    protected var interceptorDispose: ArrayList<Dispose>? = null

    /**
     * 用于监听本组件的属性变化
     */
    internal val propsWatcher: StatePropsWatcher<S> = StatePropsWatcher()

    protected var logicModule: LogicModule<S> = object : LogicModule<S> {
        override fun addLocalEffects(collect: EffectCollector<S>) {}
        override fun subscribeProps(
            state: S,
            watcher: StatePropsWatcher<S>
        ) {}
        override fun <C : IController> createController(): C? {
            return null
        }
    }

    /**
     * 获取依赖的子组件集合
     */
    internal abstract val childrenDepMap: ArrayMap<String, Dependant<out State, S>>?

    /**
     * 初始Effect以及一些依赖
     */
    init {
        initCollect()
    }

    private fun initCollect() {
        // 初始化Reducer
        val module: LogicModule<S>? = createLogicModule()
        if (module != null) {
            logicModule = module
        }

        // 初始化Effect
        val effectCollector: EffectCollector<S> = EffectCollector()
        logicModule.addLocalEffects(effectCollector)
        // 检查Effect的注册, 并注入一些框架内部的Action
        checkEffect(effectCollector)
        effect = effectCollector.effect
    }

    open fun mergeInterceptor(manager: InterceptorManager, selfDep: Dependant<S, State>) {
        // sub class impl
    }

    @CallSuper
    open fun destroy() {
        if (interceptorDispose != null && interceptorDispose!!.size > 0) {
            interceptorDispose?.forEach {
                it()
            }
        }
    }

    /**
     * 检查当前组件注册的Effect，主要做几件事情：
     * 1、检查组件有没有注册过框架内部的Action，这些外部注册需要无效化
     * 2、重新注册框架内部的Action
     *
     * @param effectCollector EffectCollect
     */
    protected open fun checkEffect(effectCollector: EffectCollector<S>?) {
        // sub class impl
    }

    /**
     * LogicModule，用户主动设置
     */
    abstract fun createLogicModule(): LogicModule<S>?
}