package com.cyworks.redux

import android.content.res.Configuration
import com.cyworks.redux.util.ILogger
import java.lang.reflect.Field
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Desc: 状态基类，state中只有用[ReactiveProp]包裹的属性，才具有响应式。
 * 也即用ReactiveProp包装的属性变化时才能触发UI更新.
 * @author randytu
 */
abstract class State {
    /**
     * 用于存放组件所用的数据的Map, 方便框架进行修改数据，框架内部创建并使用，开发者无感知。
     * key: 某个属性对应的key, String；
     * value: PropValue, 某个属性对应的值；
     */
    val dataMap = ConcurrentHashMap<String, ReactiveProp<Any>>()

    /**
     * 用于加快查询设置的map，主要用于绑定全局store上
     * key: 依赖状态对应的类名；
     * value: 如果有则写入1；
     */
    private val mDependStateMap = HashMap<String, Int>()

    /**
     * state代理，主要用于当State中某些属性变化时记录这些属性的变化，方便后续处理
     */
    private var mStateProxy: StateProxy? = null

    /**
     * 防止多次调用
     */
    @Volatile
    private var isCheckedKey = false

    /**
     * Log 组件，组件内共享
     */
    protected val mLogger: ILogger = ReduxManager.getInstance().logger

   /**
    * 如果当前属性存在屏幕旋转，用此key来标注
    */
    val ORIENTATION_KEY = "current_orientation"

    /**
     * 页面内部默认的属性，表示当前的横竖屏状态
     */
    @Prop(selfKey = ORIENTATION_KEY)
    var mCurrentOrientation = ReactiveProp(
        Int::class.java,
        Configuration.ORIENTATION_PORTRAIT, this
    )

    /**
     * 组件内部默认的属性，表示当前是否需要懒加载，此属性定位为私有属性
     */
    @Prop(selfKey = "show_ui")
    var isShowUI = ReactiveProp(Boolean::class.java, true, this)

    /**
     * 页面内部默认的属性，表示当前的横竖屏状态
     */
    @Prop(selfKey = ORIENTATION_KEY)
    var mCurrentOrientation = ReactiveProp(
        Int::class.java,
        Configuration.ORIENTATION_PORTRAIT, this
    )

    /**
     * 对当前状态的属性进行检测，以保证后续的依赖收集以及精准更新可以正常运行
     */
    fun detectField() {
        if (isCheckedKey) {
            return
        }
        isCheckedKey = true
        val time = System.currentTimeMillis()
        val fields: MutableList<Field> = ArrayList()
        var tempClass: Class<*>? = this.javaClass

        // 当父类为null的时候说明到达了最上层的父类(Object类)
        while (tempClass != null) {
            fields.addAll(Arrays.asList(*tempClass.declaredFields))
            // 得到父类,然后赋给自己
            tempClass = tempClass.superclass
        }
        collectKeys(fields)
        mLogger.d(
            ILogger.PERF_TAG, "<" + this.javaClass.name
                    + ">" + "collect key consumer: "
                    + (System.currentTimeMillis() - time)
        )
    }

    private fun collectKeys(fields: List<Field>) {
        // 用于临时保存当前组件字段对应的key
        val tempKeys = ArrayList<String>()

        // 遍历一个对象字段数组
        for (field in fields) {
            if (field.type != ReactiveProp::class.java) {
                continue
            }
            if (!field.isAnnotationPresent(Prop::class.java)) {
                throw RuntimeException(
                    "ReactiveProp: <"
                            + field.name + "> must use @Prop annotation"
                )
            }
            val connectProp: Prop = field.getAnnotation(Prop::class.java) ?: continue
            val key: String = connectProp.selfKey()
            if (key == "") {
                throw RuntimeException(
                    "ReactiveProp: <"
                            + field.name + "> must has a real key"
                )
            }
            if (tempKeys.contains(key)) {
                throw RuntimeException(
                    "has same key: <" + key + ">"
                            + " in current state <" + this.javaClass.name + ">"
                )
            }
            tempKeys.add(key)
            initPropMap(key, field)
        }
        tempKeys.clear()
    }

    private fun initPropMap(key: String, field: Field) {
        try {
            val o = field[this]
                ?: throw RuntimeException(
                    "set Key <" + key
                            + "> value is null, please call ReactiveProp construct init it"
                )
            if (o is ReactiveProp<*>) {
                val prop = o as ReactiveProp<Any>
                prop.key = key
                setProp(key, prop)
            }
        } catch (e: IllegalAccessException) {
            mLogger.e(
                ILogger.ERROR_TAG, "map key-value exception, "
                        + e.message
            )
        }
    }

    /**
     * 当添加一个ReactiveProp时，需要在内部的HashMap中做key对应，本方法用于设置key - ReactiveProp对应，
     * 仅限框架内部使用。
     *
     * @param key 属性对应的key
     * @param value 对应[ReactiveProp]
     */
    fun setProp(key: String, value: ReactiveProp<Any>) {
        // 防止用户在外部多次创建ReactiveProp，多次创建会导致几个问题：
        // 1、开发者新建的ReactiveProp无法触发UI更新
        // 2、绕过了一个属性只能被一个组件一次创建的问题
        if (dataMap.containsKey(key)) {
            throw RuntimeException("can not create ReactiveProp again in state!")
        }
        dataMap[key] = value
    }

    /**
     * 设置属性，用于临时保存属性对应的value的值，只能在初始化阶段创建ReactiveProp
     */
    fun checkPropWhenCreate() {
        if (isCheckedKey) {
            throw RuntimeException("only create ReactiveProp in init State phase")
        }
    }

    fun addDependState(@NonNull token: String) {
        mDependStateMap[token] = DEPENDANT_STATE_FLAG
    }

    fun removeDependState(@NonNull token: String) {
        mDependStateMap.remove(token)
    }

    /**
     * 是否依赖了某个state，主要优化对全局store的依赖过程
     * @param token 全局store对应的类名
     * @return 是否依赖
     */
    fun isDependState(@NonNull token: String): Boolean {
        val result = mDependStateMap[token] ?: return false
        return result == DEPENDANT_STATE_FLAG
    }

    /**
     * 注入state代理，主要用于记录哪些状态变化了，仅限框架内部使用
     *
     * 为什么要注入一个代理对象，而不是通过state本身来处理？
     * 主要目的还是为了隔离：
     * 在reducer中，state具有更新能力，可以修改属性，
     * 而在其他场景，应该只能读取state中的属性，不能再对其进行修改了。
     *
     * @param stateProxy [StateProxy]
     */
    fun setStateProxy(stateProxy: StateProxy?) {
        mStateProxy = stateProxy
        for (prop in dataMap.values) {
            prop.setStateProxy(stateProxy)
        }
    }

    /**
     * 获取组件私有数据变化的情况, 在子组件的reducer执行完成后调用，仅限框架内部使用。
     * @return 私有数据变化列表 [ReactiveProp]
     */
    val privatePropChanged: List<ReactiveProp<Any>>?
        get() = if (mStateProxy != null) {
            mStateProxy!!.privatePropChanged
        } else null

    /**
     * 获取公开数据变化的情况，公开数据指的是依赖了父组件的数据，
     * 在子组件的reducer执行完成后调用，仅限框架内部使用。
     *
     * @return 公开数据变化列表 [ReactiveProp]
     */
    val publicPropChanged: List<ReactiveProp<Any>>?
        get() = if (mStateProxy != null) {
            mStateProxy!!.publicPropChanged
        } else null

    /**
     * 如果状态内存在公共状态，且公共状态发生变化，则认为当前属性属性有变化，
     * 此时框架需要通知关联了此数据的组件。
     *
     * @return 公共数据是否发生变化
     */
    val isChanged: Boolean
        get() = if (mStateProxy != null) {
            mStateProxy!!.isChanged
        } else false

    /**
     * 由于目前是主动依赖属性，所以退出时，需要将依赖删除
     */
    fun clear() {
        if (dataMap.isEmpty()) {
            return
        }
        for (prop in dataMap.values) {
            prop.detach()
        }
    }

    companion object {
        /**
         * 将依赖全局store的state的类名添加进来并用此标记
         */
        private const val DEPENDANT_STATE_FLAG = 1

        /**
         * 复制一个原始的state出来, 根据社区要求，Redux在State变更时必须复制一个新的state，
         * 由于java语言的限制，需要开发者手动实现Cloneable，目前先不做深拷贝
         *
         * @param state State
         * @return State
         */
        @JvmStatic
        fun <S : State?> copyState(state: S?): S? {
            if (state == null) {
                return null
            }
            state.setStateProxy(null)
            return state
        }
    }
}