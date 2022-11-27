package com.cyworks.redux.types

import androidx.annotation.MainThread
import com.cyworks.redux.ReduxContext
import com.cyworks.redux.State
import com.cyworks.redux.action.Action
import com.cyworks.redux.prop.ReactiveProp

/**
 * 组件自己的State的获取接口
 */
fun interface StateGetter<S : State> {
    /**
     * 复制并返回当前组件的State
     * @return 组件的State
     */
    fun copy(): S
}

/**
 * 当当前State发生变化时，通知给UI的接口
 */
fun interface StateChangeForUI<S : State> {
    /**
     * 组件state变化的回调
     * @param state 上一次的PureState
     * @param changedProps 组件变化的属性列表
     */
    fun onChange(state: S, changedProps: List<ReactiveProp<Any>>)
}

/**
 * 通过此接口来更新state，不需要发送action
 */
fun interface Reducer<S : State> {
    /**
     * 执行更新Store的操作，只能在主线程运行。
     * @param state S, 上一次的state
     */
    @MainThread
    fun update(state: S)
}

/**
 * Redux体系中，不仅存在Reducer这种处理状态变换的函数，
 * 也需要处理一些具有副作用的操作，比如跳转，网络请求，这些逻辑操作在框架中用Effect来封装。
 */
fun interface Effect<S: State> {
    /**
     * 处理Effect
     * @param action Action
     * @param ctx 当前Feature的context
     */
    fun doAction(action: Action<Any>, ctx: ReduxContext<S>?)
}

/**
 * 对State变化更新UI，做了刷新对齐，通过vsync信号统一进行刷新
 */
interface UIUpdater {
    /**
     * 框架内部实现这个方法，用于接收vsync信号
     */
    fun onNewFrameCome()
}

/**
 * 这里跟Effect概念分开，虽然是跟Effect相同的逻辑，但是希望在概念上是负责处理拦截后的Action的作用
 */
interface Interceptor<S : State> : Effect<S>

/**
 * 用于获取组件对应的ReduxContext
 */
interface ContextProvider<S: State> {
    /**
     * 返回组件对应的ReduxContext
     * @return ReduxContext
     */
    fun provider(): ReduxContext<S>?
}

/**
 * 此函数用于解注册
 */
typealias Dispose = () -> Unit

/**
 * Action 分发器接口
 */
interface Dispatch {
    fun dispatch(action: Action<Any>);
}

/**
 * Bus接口, 主要用于处理Page内部的 Effect以及全局Effect
 */
interface IBus : Dispatch {
    /**
     * 将当前Bus绑定到上一级bus上
     * @param parent DispatchBus 上一级Bus
     */
    fun attach(parent: IBus?)

    /**
     * 将当前Bus从上一级里删除
     */
    fun detach()

    /**
     * 用当前Bus发送广播
     * @param action AbsAction
     * @param payload 携带的参数
     */
    fun broadcast(action: Action<Any>)

    /**
     * 向当前bus中注册一个Dispatch
     * @param dispatch 每个Bus会持有多个Dispatch，用于处理当前Page下的Effect
     * @return 返回一个反注册器
     */
    fun registerReceiver(dispatch: Dispatch): Dispose?
}

/**
 * 用于主动订阅组件内数据对的观察者
 */
fun interface IPropChanged<S : State, T> {
    /**
     * 通知状态变化, 请不要在这里做一些耗时操作，防止出现阻塞UI线程的问题
     *
     * @param prop 当前变化的属性集合[ReactiveProp]
     */
    fun onPropChanged(prop: ReactiveProp<T>, ctx: ReduxContext<S>)
}

/**
 * 当前公共状态变化监听器
 */
typealias PublicPropsChangedCallback = (props: List<ReactiveProp<Any>>) -> Unit

/**
 * 组件私有属性发生变化时，通过此接口通知给当前变化的组件
 */
typealias PrivatePropsChangedCallback = (props: List<ReactiveProp<Any>>) -> Unit

/**
 * Adapter 统一接口，方便进行类型转换
 */
interface IAdapter {
    /**
     * 触发Action操作
     * @param action Action
     * @param payload 参数
     */
    fun dispatchReducer(action: Action<Any>)

    /**
     * 触发Action操作
     * @param action Action
     * @param payload 参数
     */
    fun dispatchEffect(action: Action<Any>)
}

/**
 * Desc: watch 属性时，通过此方法获取属性list
 */
typealias DepProps = () -> Array<Any>