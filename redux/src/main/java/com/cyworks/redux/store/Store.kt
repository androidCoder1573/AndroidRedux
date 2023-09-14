package com.cyworks.redux.store

import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import com.cyworks.redux.ReduxManager
import com.cyworks.redux.prop.ReactiveProp
import com.cyworks.redux.state.State
import com.cyworks.redux.types.Dispose
import com.cyworks.redux.util.ILogger
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

/**
 * 定义一个android-redux Store，本质上是一个状态容器（有限状态，无限状态 [...items]）
 *
 * Redux三原则：
 * 1、单一数据源 --- 一个页面对应一个Store；
 * 2、State 是只读的 --- android-redux中没有遵循
 * 3、使用纯函数来执行修改 --- UI状态只能在Reducer中处理；
 *
 * 除了上述的一些改变，android-redux跟社区的redux还存在一些区别：
 * 1、是一个完全组件化的框架；
 * 2、数据存在公有以及私有；
 * 3、对UI进行精准更新；
 *
 * Redux存在着这两个问题：
 * 1、页面复杂时，state状态树过于庞大；
 * 2、无法知道哪些状态发生了更新；
 *
 * Android-Redux如何解决这两个问题呢？
 * 1、针对state过大，Android-redux通过状态分治将状态分散在各个组件中，
 * 而且提供了组件级别的私有状态(没有被以来的属性默认是私有的，不会影响其他组件)
 * 2、针对状态更新时无法确知哪些状态发生变化，Android-Redux提供了状态精准更新的能力；
 *
 * 为什么终端上的Redux需要精准更新界面？
 * 如果无法精准更新UI，意味着每次都要对State进行diff，Java对复杂的对象：比如列表，map等，不仅对开发者来说负担比较重，
 * 还会造成每次更新将有很多无用的cpu开销。
 *
 * 如何实现精准更新？
 * 为了实现精准更新，各个组件内部的数据默认是私有的，每个属性都有一个默认的key（依靠委托获取），
 * 在状态更新的时候，利用kotlin的委托属性，记录每个属性的赋值，通过收集这些变化的数据再对比每个组件依赖的数据，
 * 从而实现精准更新。
 *
 * 属性关联：
 * 需要共享的属性需要上升到父组件，子组件的属性通过关联父组件进行数据共享。
 *
 * store为什么不提供reset方法？
 * 将State重置，这种事情其实没必要放到框架里来做，每个组件内部有私有数据，放在框架里统一处理不是很好，
 * 建议的方式：
 * 页面做一个公有状态比如reset，所有组件订阅这个状态，做清理的操作。
 */
open class Store<S : State>  {
    /**
     * 获取state，为了防止外部缓存state，控制访问权限，外部只能通过监听的方式观察属性值
     */
    protected lateinit var state: S

    /**
     * 注册的观察者列表，用于分发store的变化
     */
    private var listeners: CopyOnWriteArrayList<StoreObserver>? = null

    private val tempChangedList: ArrayList<ReactiveProp<Any>> = ArrayList()

    /**
     * store是否销毁了, 销毁后不能再处理数据
     */
    protected var isDestroy = false

    protected val logger: ILogger = ReduxManager.instance.logger

    internal constructor()

    internal constructor(s: S) {
        state = s
    }

    fun copyState() : S {
        return State.copyState(state)
    }

    /**
     * 监听store中的属性，为了防止外部随意监听store变化，控制了仅限框架内部调用
     * @param observer [StoreObserver] 监听器
     */
    @MainThread
    internal fun observe(observer: StoreObserver?): Dispose? {
        if (observer == null) {
            return null
        }
        if (listeners == null) {
            listeners = CopyOnWriteArrayList()
        }
        listeners!!.add(observer)
        return { listeners!!.remove(observer) }
    }

    internal fun onStateChanged(state: State?) {
        if (state == null) {
            return
        }
        val changedPropList = state.publicPropChangedList ?: return
        update(changedPropList)
    }

    /**
     * 用于触发组件状态更新的接口
     * @param changedPropList 当前变化的属性列表
     */
    protected open fun update(changedPropList: List<ReactiveProp<Any>>) {
        // sub class impl
        fire(changedPropList)
    }

    /**
     * 通知组件状态变化
     * @param changeList 当前store中变化的数据
     */
    protected fun fire(changeList: List<ReactiveProp<Any>>) {
        if (isDestroy || changeList.isEmpty() || listeners == null) {
            return
        }

        // 因为一次可能会更新多个属性，这里牺牲一些性能，让每个组件可以一次性收到全部的状态变化
        val size = listeners!!.size
        if (size < 1) {
            return
        }

        for (i in 0 until size) {
            val listener = listeners!![i]
            val token = listener.token
            val tempList = checkChangeList(changeList, token)
            if (tempList.isNotEmpty()) {
                listener.onPropChanged(tempList)
            }
        }
    }

    /**
     * 寻找组件中哪些属性发生了变化
     *
     * @param changeList 当前发生变化的属性列表
     * @param token 组件state对应的类名
     * @return 组件变化的属性列表
     */
    private fun checkChangeList(changeList: List<ReactiveProp<Any>>, token: JvmType.Object): List<ReactiveProp<Any>> {
        tempChangedList.clear()

        val size = changeList.size
        for (i in 0 until size) {
            val item = changeList[i]
            // 如果组件的属性就在当前变化的列表里，直接加入
            if (item.stateToken() == token) {
                tempChangedList.add(item)
            } else {
                // 如果组件属性没在变化列表里，看当前属性的孩子是否具有此属性
                val child = item.getChild(token)
                if (child != null) {
                    // 首先要同步value
                    child.innerSetter(item.value())
                    tempChangedList.add(child)
                }
            }
        }

        return tempChangedList
    }

    /**
     * 页面退出时的清理操作
     */
    @CallSuper
    open fun clear() {
        isDestroy = true
        listeners?.clear()
    }
}