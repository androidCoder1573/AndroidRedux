package com.cyworks.demo.userstore

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.cyworks.redux.store.GlobalStore
import com.cyworks.redux.types.CreateGlobalState

class UserStore private constructor() {
    private val userStore: GlobalStore<UserState>

    init {
        val creator: CreateGlobalState<UserState> = object : CreateGlobalState<UserState> {
            override fun create(): UserState {
                return userStateInit()
            }

        }
        userStore = object : GlobalStore<UserState>(creator) {}
    }

    val store: GlobalStore<UserState>
        get() = userStore

    fun modifyUserName(name: String) {
        instance.userStore.updateState { state ->
            state.name = name
            state
        }
    }

    fun modifyUserNameAsync(name: String) {
        object : Thread() {
            override fun run() {
                super.run()
                try {
                    sleep(2000L)
                } catch (ie: InterruptedException) {
                    Log.e("InterruptedException: ", ie.message!!)
                    currentThread().interrupt()
                }

                Handler(Looper.getMainLooper()).post { modifyUserName(name) }
            }
        }.start()
    }

    companion object {
        val instance = UserStore()
    }
}