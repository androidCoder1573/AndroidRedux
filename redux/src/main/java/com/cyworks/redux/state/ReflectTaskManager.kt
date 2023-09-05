package com.cyworks.redux.state

import android.util.ArrayMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

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
        futureList.forEach {
            if (!it.isDone) {
                it.cancel(true)
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
        list.forEach {
            submitInner(it.value)
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
