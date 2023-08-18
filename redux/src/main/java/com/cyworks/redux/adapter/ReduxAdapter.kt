package com.cyworks.redux.adapter

import android.view.View
import com.cyworks.redux.ReduxContext
import com.cyworks.redux.action.Action
import com.cyworks.redux.state.State
import com.cyworks.redux.types.DependantCreator
import com.cyworks.redux.util.Environment

class ReduxAdapter<PS: State> {
    /**
     * 父组件的Context
     */
    protected var context: ReduxContext<PS>? = null

    /**
     * 用于保存从父组件继承下来的属性
     */
    protected var env: Environment? = null

    private val dependantList: DependantList<PS>

    public constructor(depBuilder: DependantCreator<PS>) {
        dependantList = DependantList(depBuilder)
    }

    fun setEnv(context: ReduxContext<PS>, env: Environment) {
        this.context = context
        this.env = env
    }

    /**
     * 此方法还是要增加数据data，每个item的data并不需要关联父组件的list属性，直接再此处设置给对应的item组件，
     * item组件内部通过prop来判断是否变化，并进行刷新逻辑.
     *
     * 因为list view的list发生变化时，会自动回调renderRow方法，所以这里并不需要内部再关心list是否变化。
     * 所以不需要建立item跟list之间的数据联系
     *
     * 这里也可以根据可见索引，将其reducer/effect/interceptor的通道打断，这样可以保证性能开销最小
     *
     * @param type
     * @param index
     * @param data
     */
    fun buildItem(index: Int, type: String, data: Any): View {
        val depend = dependantList.buildDependant<State>(index, type, data)
        this.copyEnvToSub()?.let { depend.install(it) }
        val component = depend.logic
//        if (component !is AdapterItemComponent) {
//            throw RuntimeException("list item must be a AdapterItemComponent")
//        }
//        return component.createUI() ?: throw RuntimeException("list item must have a view")

        throw RuntimeException("list item must have a view")
    }

    @Suppress("UNCHECKED_CAST")
    fun doInterceptorAction(action: Action<Any>) {
        val interceptor = dependantList.getInterceptor()
        interceptor.doAction(action, this.context as ReduxContext<State>)
    }

    fun getListItemType(index: Int): String {
        return this.dependantList.getListItemType(index)
    }

    private fun copyEnvToSub(): Environment? {
        val env = this.env?.let { Environment.copy(it) }
        context?.state?.let { env?.parentState = it }
        context?.effectDispatch?.let { env?.parentDispatch = it }
        // 代表当前组件运行在列表中
        // env.setIsInAdapter(true)
        return env
    }
}