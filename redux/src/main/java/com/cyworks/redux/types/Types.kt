package com.cyworks.redux.types

import androidx.annotation.MainThread
import com.cyworks.redux.ReduxContext
import com.cyworks.redux.action.Action
import com.cyworks.redux.dependant.Dependant
import com.cyworks.redux.prop.ReactiveProp
import com.cyworks.redux.state.State
import com.cyworks.redux.ui.ComponentViewHolder

/**
 * 使用这个赋值函数不会造成变化收集
 */
typealias PropertySet<T> = (value: T) -> Unit

/**
 * watch 属性时，通过此方法获取当前关注的属性list
 */
typealias DepProps = () -> Array<Any>

/**
 * 此函数用于解注册
 */
typealias Dispose = () -> Unit

/**
 * 创建一个Dependant
 */
fun interface DependantCreator<PS : State> {
    fun create(type: String, data: Any): Dependant<State, PS>
}

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
 * 通过此接口来更新state，不需要发送action
 */
fun interface Reducer<S : State> {
    /**
     * 执行更新Store的操作，只能在主线程运行。
     * @param state S, 上一次的state
     */
    @MainThread
    fun update(state: S): S
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
    fun doAction(action: Action<out Any>, ctx: ReduxContext<S>?)
}

/**
 * 这里跟Effect概念分开，虽然是跟Effect相同的逻辑，但是希望在概念上是负责处理拦截后的Action的作用
 */
fun interface Interceptor<S : State> : Effect<S>

/**
 * State变化更新UI，做了刷新对齐，通过vsync信号统一进行刷新
 */
fun interface UIFrameUpdater {
    /**
     * 框架内部实现这个方法，用于接收vsync信号
     */
    fun onNewFrameCome()
}

/**
 * 用于获取组件对应的ReduxContext
 */
interface ComponentContextWrapper<S: State> {

    /**
     * 返回组件对应的ReduxContext
     * @return ReduxContext
     */
    fun getCtx(): ReduxContext<State>
}

/**
 * 多个公开属性变化的回调
 */
fun interface IPropsChanged {
    fun onPropsChanged(props: List<ReactiveProp<Any>>?)
}

/**
 * 当当前State发生变化时，通知给UI的接口
 */
fun interface IStateChange<S : State> {
    /**
     * 组件state变化的回调
     * @param changedProps 组件变化的属性列表
     */
    fun onChange(changedProps: List<ReactiveProp<Any>>)
}

/**
 * 当UI Atom对应的dep发生变化时，触发对应UI的更新方法
 */
fun interface OnUIAtomChanged<S : State> {
    /**
     * 更新UI，这里抽象更彻底，某一个组件内部的UI，只能绑定
     */
    fun update(state: S, oldDeps: Array<Any>?, holder: ComponentViewHolder?)
}

/**
 * 当Logic Atom对应的dep发生变化时，触发对应的更新方法
 */
fun interface OnLogicAtomChanged<S : State> {
    /**
     * 更新UI，这里抽象更彻底，某一个组件内部的UI，只能绑定
     */
    fun update(state: S, oldDeps: Array<Any>?, ctx: ReduxContext<S>?)
}

/**
 * 创建全局store的state接口
 */
interface CreateGlobalState<S: State> {
    fun create(): S
}

/**
 * Action 分发器接口
 */
fun interface Dispatch {
    fun dispatch(action: Action<out Any>)
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
    fun register(dispatch: Dispatch?): Dispose?
}

/**
 * Action分发器
 */
interface Dispatcher {
    /**
     * 发送私有action
     */
    fun dispatch(action: Action<out Any>)

    /**
     * 发送全局Action，任意组件都可以拦截此action
     */
    fun dispatchToInterceptor(action: Action<out Any>)

    /**
     * 发送action给自己的父组件
     */
    fun dispatchToParent(action: Action<out Any>)

    /**
     * 发送action给处在Adapter中的item的所有孩子，adapter的顶层item的任意组件都可以发送
     */
    fun dispatchToAdapterItemComponents(action: Action<out Any>)

    /**
     * 发送action给当前组件的子组件，当然也包括自己, 仅限adapter 中的顶层item发送
     */
    fun dispatchToSubComponents(action: Action<out Any>)
}

/**
 * Adapter 统一接口，方便进行类型转换
 */
interface IAdapter {

    /**
     * 触发Action操作
     * @param action Action
     * @param payload 参数
     */
    fun dispatchEffect(action: Action<Any>)
}