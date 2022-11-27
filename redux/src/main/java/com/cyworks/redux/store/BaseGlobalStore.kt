package com.cyworks.redux.store

import com.cyworks.redux.State
import com.cyworks.redux.StateProxy
import com.cyworks.redux.action.Action
import com.cyworks.redux.logic.EffectCollect
import com.cyworks.redux.prop.ReactiveProp
import com.cyworks.redux.types.Effect
import com.cyworks.redux.types.Reducer
import com.cyworks.redux.util.ILogger
import com.cyworks.redux.util.ThreadUtil

/**
 * Desc: 带Effect的Store，为全局Store优化的产物，增加Effect处理能力，方便全局store处理异步请求。
 *
 * 主要是因为store不具备ReduxContext，导致无法发送处理Effect的Action。
 *
 * 给开发这一个选择，可以使用action来驱动全局store，
 * 也可以不使用，直接通过面向对象的方式操作
 */
abstract class BaseGlobalStore<S : State> : Store<S> {
    /**
     * 全局store扩展的内部effect
     */
    private var effect: Effect<S>? = null

    /**
     * 构造器，用于初始化State/Effect
     */
    constructor() : super() {
        val effectCollect: EffectCollect<S> = EffectCollect()
        addEffects(effectCollect)
        effect = effectCollect.effect
        state = onCreateState()
        state!!.detectField()
    }

    /**
     * 创建State
     * @return S [BasePageState]
     */
    abstract fun onCreateState(): S

    /**
     * 注入Effect，不强制实现，如果开发者使用action驱动，需要实现此方法
     */
    protected fun addEffects(effectCollect: EffectCollect<S>?) {
        // sub class maybe impl
    }

    /**
     * 扩展分发Effect Action的能力，全局Store本身可以通过单例获取
     * @param action Action
     * @param payload 参数
     */
    fun dispatchEffect(action: Action<Any>) {
        effect?.doAction(action, null)
    }

    /**
     * 全局store的更新操作只能在全局store发生
     *
     * @param changedPropList 当前变化的属性列表
     */
    override fun update(changedPropList: List<ReactiveProp<Any>>) {
        // 通知更新
        if (changedPropList.isEmpty()) {
            return
        }

        val time = System.currentTimeMillis()
        // 通知组件进行状态更新
        notifySubs(changedPropList)
        logger.d(
            ILogger.PERF_TAG, "global store update consumer: "
                    + (System.currentTimeMillis() - time)
        )
    }

    /**
     * 更新状态
     */
    fun updateState(reducer: Reducer<S>) {
        ThreadUtil.checkMainThread("update state must be called in main thread!")
        state!!.setStateProxy(StateProxy())
        val newState: S = (reducer.update(state!!)) as S
        onStateChanged(newState)
    }
}