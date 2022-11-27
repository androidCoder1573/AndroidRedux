package com.cyworks.redux.adapter

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.cyworks.redux.LogicComponent
import com.cyworks.redux.State
import com.cyworks.redux.action.Action
import com.cyworks.redux.prop.ChangedState
import com.cyworks.redux.types.IAdapter
import com.cyworks.redux.types.StateChangeForUI

/**
 * Desc: 组件的根Adapter，一个组件仅能有一个根Adapter。
 *
 *
 *
 * 如果Adapter是一个逻辑组件，作为每个组件的依附，每个组件仅有一个Adapter类型的逻辑组件。
 * 逻辑组件可通过context获得，从逻辑组件中可以取得实际的Adapter。
 * adapter内部会安装ViewHolder，如果每个ViewHolder被封装成一个真正的Feature，那么实现起来太重。
 *
 *
 *
 * 思路：只有RootAdapter具有操作State的能力。
 * 如果当前Adapter具有子Adapter，需要将父Adapter设置进去，触发Reducer的操作最终都会去更新根Adapter。
 *
 * 框架不再提供具体的Adapter实现，将具体的Adapter实现跟框架分离开来，
 * 开发者可以注入自己的列表Adapter实现，对开发者来说，实现起来更灵活，框架的外部依赖也会减少。
 */
abstract class RootAdapter<S : State>(adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>) :
    LogicComponent<S>(null), IAdapter {
    /**
     * 当前RecyclerView对应的Adapter
     */
    protected var adapter: RecyclerView.Adapter<RecyclerView.ViewHolder>

    /**
     * LiveData包装ChangedState，用于通知界面更新
     */
    private var liveData: MutableLiveData<ChangedState<S>>? = null

    /**
     * 是否已经bind到父组件上
     */
    private var isBind = false

    /**
     * 当前UI是否展示
     */
    private var isUIShow = false

    /**
     * 用于观察LiveData
     */
    private var observer: Observer<ChangedState<S>>? = null

    init {
        this.adapter = adapter
    }

    private fun initLiveData() {
        liveData = MutableLiveData()
        val owner: LifecycleOwner? = environment.lifeCycleProxy?.lifecycleOwner
        observer =
            Observer<ChangedState<S>> { changedState: ChangedState<S>? -> onListChanged(changedState) }
        liveData!!.observe(owner, observer)
    }

    protected fun makeUIListener(): StateChangeForUI<S> {
        return StateChangeForUI<S> { state, changedProps ->
            if (!isUIShow) {
                return
            }
            val stateCompare: ChangedState<S> = ChangedState()
            stateCompare.mState = state
            stateCompare.mChangedProps = changedProps
            mLiveData.setValue(stateCompare)
        }
    }

    fun dispatchReducer(action: Action<Any>, payload: Any?) {
        mContext.dispatchReducer(action, payload)
    }

    fun dispatchEffect(action: Action<Any>) {
        mContext.dispatchEffect(action, payload)
    }

    fun attach() {
        isUIShow = true
    }

    fun detach() {
        isUIShow = false
    }

    fun install(
        @NonNull environment: Environment,
        @NonNull connector: LRConnector<S, BaseState?>
    ) {
        if (isBind) {
            return
        }
        isBind = true
        mEnvironment = environment
        mBundle = mEnvironment.getLifeCycleProxy().getBundle()
        mConnector = connector
        initLiveData()
        createContext()
        isUIShow = true
    }

    val reduxContext: ReduxContext<S>
        get() = mContext

    /**
     * 返回当前列表真正的Adapter，RootAdapter只是对真实的Adapter进行了一次包装
     */
    val realAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>
        get() = mAdapter

    /**
     * 当列表的数据发生改变时，通过此接口通知给开发者。
     * 目的：将数据改变的逻辑放在外部，让开发者自己决定列表的diff方式。
     * @param changedState 当前改变的属性 [ChangedState]
     */
    abstract fun onListChanged(changedState: ChangedState<S>?)
    @CallSuper
    fun clear() {
        super.clear()
        mLiveData.removeObserver(mObserver)
        mObserver = null
        if (mContext != null) {
            mContext.destroy()
        }
        mEnvironment = null
    }
}