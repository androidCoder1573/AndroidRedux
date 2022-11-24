package com.cyworks.redux

import android.support.annotation.CallSuper
import java.util.ArrayList
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Desc: 定义一个live-redux Store，本质上是一个状态容器（有限状态，无限状态 [...items]）
 *
 * Redux跟flux的区别：
 * Flux拥有多个Store和一个中央调度程序，通过中央调度程序与这些store的通信,
 * 由于是多个store，导致管理成本比较高，组件间交互也比较复杂。
 * Redux只有一个Store，并鼓励使用简单的reducer函数与Store中的各种属性进行交互，
 * 这种策略使开发者可以轻松地推断应用程序的整体状态。
 *
 * Redux三原则：
 * 1、单一数据源 --- 一个页面对应一个Store；
 * 2、State 是只读的 --- 在live-redux中没有遵循，但也保证了只能在Reducer中修改UI数据[ReactiveProp]；
 * 3、使用纯函数来执行修改 --- UI状态只能在Reducer中处理；
 *
 * 除了上述的一些改变，live-redux跟社区的redux还存在一些区别：
 * 1、是一个完全组件化的框架；
 * 2、数据存在公有以及私有；
 * 3、对UI进行精准更新；
 *
 * Redux存在着这两个问题：
 * 1、页面复杂时，state状态树过于庞大；
 * 2、无法知道哪些状态发生了更新；
 *
 * Live-Redux如何解决这两个问题呢？
 * 1、针对state过大，live-redux通过状态分治将状态分散在各个组件中，而且提供了组件级别的私有状态；
 * 2、针对状态更新时无法确知哪些状态发生变化，live-redux提供了状态精准更新的能力；
 *
 * 为什么终端上的Redux需要精准更新界面？
 * 原始的Redux并没有实现状态精准更新通知，站在android的角度，如果无法精准更新UI，
 * 意味着每次都要对State进行diff，Java对复杂的对象：比如列表，map等，diff性能比较差，
 * 如果不能做到精准更新，也意味着每次更新将浪费很多无用的cpu开销。
 *
 * 如何实现精准更新？
 * V1：为state中每个属性创建getter/setter，通过setter通知框架属性变更，实际操作起来对开发者不友好；
 * V2：state不再是用户自定义的类，提供HashMap给开发这保存自定义数据，但是HashMap导致数据类型丢失，使用繁琐；
 * V3：UI属性使用ReactiveProp进行包装，框架内部自行判断属性是否更新，兼顾了便利性以及属性类型；
 *
 * 精准更新在端上是强需求，为了实现精准更新，各个组件内部的数据默认是私有的，每个属性都有一个默认的key，
 * 在创建属性过程中通过注解给属性创建key。
 * 在reducer执行的时候，通过State来记录当前变化的属性，通过收集这些变化的数据再对比每个组件依赖的数据，
 * 从而实现精准更新。
 *
 * 属性关联：
 * 需要共享的属性需要上升到父组件，子组件的属性通过关联父组件进行数据共享。
 *
 * Live-Redux通过改变社区redux对数据的更新方式，解决了端上数据更新存在的性能问题。
 *
 * store为什么不提供reset方法？
 * 将State重置，这种事情其实没必要放到框架里来做，每个组件内部有私有数据，放在框架里统一处理不是很好，
 * 建议的方式是，页面做一个公有状态比如reset，所有组件订阅这个状态，做清理的操作。
 *
 * @author randytu on 2020/7/18
 */
open class Store<S : State?> {
    /**
     * 获取state，为了防止外部缓存state，控制访问权限，外部只能通过监听的方式观察属性值
     *
     * @return S [BasePageState]
     */
    /**
     * 将状态包装成可观察的对象
     */
    var state: S? = null

    /**
     * 经过合并的Reducer, 因为是经过合并的，所以这里不给此reducer增加泛型类型
     */
    @JvmField
    var mReducer: Reducer? = null

    /**
     * 分发Reducer的dispatch, 需要在page中使用，扩展dispatch能力
     */
    @JvmField
    var mDispatch: Dispatch? = null

    /**
     * 注册的观察者列表，用于分发store的变化
     */
    protected var mListeners: CopyOnWriteArrayList<StoreObserver>? = null

    /**
     * store是否销毁了
     */
    @JvmField
    protected var isDestroy = false

    /**
     * Log 组件
     */
    @JvmField
    protected val mLogger: ILogger = ReduxManager.instance.logger

    internal constructor() {}

    /**
     * 单参数构造器，初始化store的reducer以及dispatch
     * @param reducer Reducer
     */
    internal constructor(@NonNull reducer: Reducer?) {
        mReducer = reducer

        // 初始化store dispatch
        initDispatch()
    }

    /**
     * 两参数构造器
     * @param reducer Reducer，Page Store中的Reducer是一个聚合Reducer，无法确定具体类型
     * @param state 专指页面的State
     */
    internal constructor(@NonNull reducer: Reducer?, @NonNull state: S) : this(reducer) {
        this.state = state
    }

    private fun initDispatch() {
        val stateGetter: StateGetter<S> = StateGetter<S> {
            val state: S?
            state = state
            state!!.setStateProxy(StateProxy())
            state
        }

        // 如果存在中间件，Store中的Dispatch将在最后执行
        mDispatch = label@ Dispatch { action, payload ->
            if (isDestroy) {
                return@label
            }
            onDispatch(action, payload, stateGetter)
        }
    }

    /**
     * store调用dispatch后，通过此方法检查此次store更新的状态，并触发一次更新
     *
     * @param action 当前要处理的action
     * @param payload 当前action带的参数
     * @param stateGetter 当前组件的state getter
     */
    @CallSuper
    protected open fun onDispatch(action: Action?, payload: Any?, stateGetter: StateGetter<S>?) {
        val state: State = mReducer.doAction(stateGetter, action, payload)
        onStateChanged(state)
    }

    protected fun onStateChanged(state: State?) {
        if (state == null) {
            return
        }
        val changedPropList = state.publicPropChanged
            ?: return
        update(changedPropList)
    }

    /**
     * 用于触发组件状态更新的接口
     * @param changedPropList 当前变化的属性列表
     */
    protected open fun update(changedPropList: List<ReactiveProp<Any?>?>?) {
        // sub class impl
    }

    /**
     * 通知组件状态变化
     *
     * @param changeList 当前store中变化的数据
     */
    fun notifySubs(changeList: List<ReactiveProp<Any?>>?) {
        if (isDestroy || changeList == null || changeList.isEmpty()
            || mListeners == null || mListeners!!.isEmpty()
        ) {
            return
        }

        // 因为一次可能会更新多个属性，这里牺牲一些性能，让每个组件可以一次性收到全部的状态变化
        for (observer in mListeners!!) {
            val token = observer.token
            val tempList = checkChangeList(changeList, token)
            if (tempList.isEmpty()) {
                continue
            }
            observer.onPropChanged(tempList)
        }
    }

    /**
     * 寻找组件中哪些属性发生了变化
     *
     * @param changeList 当前发生变化的属性列表
     * @param token 组件state对应的类名
     * @return 组件变化的属性列表
     */
    private fun checkChangeList(
        changeList: List<ReactiveProp<Any?>>,
        token: String
    ): List<ReactiveProp<Any?>?> {
        val tempList: MutableList<ReactiveProp<Any?>?> = ArrayList()
        for (prop in changeList) {
            // 如果组件的属性就在当前变化的列表里，直接加入
            if (prop.token == token) {
                tempList.add(prop)
                continue
            }

            // 如果组件属性没在变化列表里，看当前属性的孩子是否具有此属性
            val child = prop.getChild(token)
            if (child != null) {
                // 首先要同步value
                child.innerSetter(prop.value())
                tempList.add(child)
            }
        }
        return tempList
    }

    /**
     * 监听store中的属性,
     * 为了防止外部随意监听store变化，控制了仅限框架内部调用
     *
     * @param storeWatcher 监听器
     * @return IDispose 返回一个解除器，方便组件detach的时候进行删除
     */
    @MainThread
    fun observe(storeWatcher: StoreObserver?): IDispose? {
        if (storeWatcher == null) {
            return null
        }
        if (mListeners == null) {
            mListeners = CopyOnWriteArrayList()
        }
        mListeners!!.add(storeWatcher)
        return IDispose { mListeners!!.remove(storeWatcher) }
    }

    /**
     * 分发Action，这里只会交给Reducer处理
     * @param action Action
     */
    fun dispatch(action: Action, payload: Any) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            dispatchInner(action, payload)
            return
        }
        ReduxManager.instance.submitInMainThread { dispatchInner(action, payload) }
    }

    private fun dispatchInner(action: Action, payload: Any) {
        if (isDestroy) {
            return
        }
        mDispatch.dispatch(action, payload)
    }

    /**
     * 页面退出时的清理操作
     */
    @CallSuper
    protected open fun clear() {
        isDestroy = true
        if (mListeners != null) {
            mListeners!!.clear()
        }
    }
}