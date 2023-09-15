package com.cyworks.redux.state

import android.util.ArrayMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

/**
 * 用于执行反射任务，具体思路：
 * 将page - component看成一棵树，每一层放到一个task中，当task中所有任务执行完成后，执行下一层的任务。
 * 这样做是因为，下一层的组件状态可能会依赖上一层的组件状态，所以要一层一层的执行，
 * 当每一层只有一个组件时，效率提升不明，但是占用主线程时常变短
 */
class ReflectTaskManager {
    /** 执行State检测的线程池 */
    val executor: ThreadPoolExecutor = ThreadPoolExecutor(
        CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_SECONDS,
        TimeUnit.SECONDS, LinkedBlockingQueue())

    private var root: ReflectTask? = null
    private var finish = false

    fun putTask(t: ReflectTask) {
        if (finish) {
            return
        }

        if (root == null) {
            root = t
            root?.submit()
        } else {
            var temp = root
            var pre = root
            while (temp != null) {
                pre = temp
                temp = temp.next
            }
            pre?.next = t
        }
    }

    fun tryRunNextTask(t: ReflectTask?, key: JvmType.Object) {
        if (finish || t == null) {
            return
        }

        val complete = t.checkComplete(key)
        if (complete) {
            root = t.next
            if (root != null) {
                root!!.submit()
            }
        }
    }

    fun finish() {
        finish = true
        if (root == null) {
            return
        }

        root?.next = null
        root?.finish()
    }

    companion object {
        private val CPU_COUNT = Runtime.getRuntime().availableProcessors()
        private const val CORE_POOL_SIZE = 0
        private val MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1
        private const val KEEP_ALIVE_SECONDS = 0L
    }
}

open class ReflectTask(private var num: Int, private val executor: ExecutorService) {
    var next: ReflectTask? = null

    private var hasCallSubmit = false
    private var finish = false

    val list: ArrayMap<JvmType.Object, Runnable> = ArrayMap()
    private val futureList = ArrayList<Future<*>>()

    fun finish() {
        finish = true
        val size = futureList.size
        for (i in 0 until size) {
            val future = futureList[i]
            if (!future.isDone) {
                future.cancel(true)
            }
        }
        list.clear()
        futureList.clear()
    }

    fun submit() {
        if (finish || hasCallSubmit) {
            return
        }
        hasCallSubmit = true
        val size = list.size
        for (i in 0 until size) {
            submitInner(list.valueAt(i))
        }
    }

    private fun submitInner(r: Runnable) {
        val future = executor.submit(r)
        futureList.add(future)
    }

    fun add(key: JvmType.Object, r: Runnable) {
        if (finish) {
            return
        }
        list[key] = r
        if (hasCallSubmit) {
            submitInner(r)
        }
    }

    fun checkComplete(key: JvmType.Object): Boolean {
        if (list.containsKey(key)) {
            list.remove(key)
            num--
        }

        return num < 1
    }
}
