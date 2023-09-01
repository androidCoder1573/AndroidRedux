package com.cyworks.redux.state

import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

class ReflectStateManager private constructor() {

    private var task: ReflectTask? = null

    fun putTask(t: ReflectTask) {
        if (task == null) {
            task = t
            task?.submit()
        } else {
            var temp = task
            while (temp?.next != null) {
                temp = temp.next
            }
            temp?.next = task
        }
    }

    fun tryRunNextTask(t: ReflectTask?, key: JvmType.Object) {
        if (t == null) {
            return
        }

        val finish = t.check(key)
        if (finish) {
            task = t.next
            if (task != null) {
                task!!.submit()
            }
        }
    }

    fun finish() {
        if (task == null) {
            return
        }

        task?.next = null
        task?.finish()
    }

    companion object {
        val instance = ReflectStateManager()

        /**
         * 执行State检测的线程池
         */
        val executor = ThreadPoolExecutor(0, 8,
            0, TimeUnit.SECONDS,
            LinkedBlockingQueue<Runnable>(), object : ThreadFactory{
                private var num = 0
                override fun newThread(r: Runnable?): Thread {
                    return Thread("state_reflect_${num++}")
                }
            })
    }
}

open class ReflectTask(private var num: Int) {
    var next: ReflectTask? = null

    private var hasCallSubmit = false

    val list: HashMap<JvmType.Object, Runnable> = HashMap()
    private val futureList = ArrayList<Future<*>>()

    fun finish() {
        hasCallSubmit = true
        futureList.forEach {
            if (!it.isDone) {
                it.cancel(true)
            }
        }
    }

    fun submit() {
        list.forEach {
            submitInner(it.value)
        }
    }

    private fun submitInner(r: Runnable) {
        val future = ReflectStateManager.executor.submit(r)
        futureList.add(future)
    }

    fun add(key: JvmType.Object, r: Runnable) {
        list[key] = r
        if (hasCallSubmit) {
            submitInner(r)
        }
    }

    fun check(key: JvmType.Object): Boolean {
        if (list.containsKey(key)) {
            list.remove(key)
            num--
        }

        return num < 1
    }
}
