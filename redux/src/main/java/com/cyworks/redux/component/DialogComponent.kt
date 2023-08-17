package com.cyworks.redux.component

import android.view.View
import androidx.annotation.CallSuper
import androidx.lifecycle.Observer
import com.cyworks.redux.ReduxContext
import com.cyworks.redux.dialog.IDialogController
import com.cyworks.redux.dialog.ILRDialog
import com.cyworks.redux.prop.ChangedState
import com.cyworks.redux.prop.ReactiveProp
import com.cyworks.redux.state.State
import com.cyworks.redux.util.ILogger
import com.cyworks.redux.util.IPlatform

/**
 * 对话框组件基类，扩展了打开对话框的功能，控制了初始化的一些特殊操作。
 *
 * @note: 如果对话框本身功能比较复杂，还是建议使用Page来实现, 防止单一组件功能过多。
 */
abstract class DialogComponent<S : State> : BaseComponent<S>(true) {
    /**
     * 当前展示的对话框实例,
     * 通过这种方式，框架不需要关心对话框的具体形式(Dialog 或者 FragmentDialog，androidX等)
     */
    private var dialogInstance: ILRDialog? = null

    /**
     * 框架注入的对话框控制接口
     */
    private var dialogInterface: IDialogController? = object : IDialogController {
        override fun getView(): View? {
            return uiController.currentView
        }

        override fun onDialogDismiss() {
            this@DialogComponent.uiController.detach()
            // UI detach之后再清理，保证安全
            this@DialogComponent.onDialogDismiss(this@DialogComponent.context)
            dialogInstance = null
        }
    }

    init {
        observer = Observer<ChangedState<S>> { stateCompare: ChangedState<S> ->
            onDataChangedCB(stateCompare)
        }
    }

    /**
     * UI 数据变化时的回调
     *
     * @param stateCompare ChangedState
     */
    private fun onDataChangedCB(stateCompare: ChangedState<S>) {
        if (environment == null) {
            return
        }

        val props: List<ReactiveProp<Any>> = stateCompare.changedProps
        // 检查属性是否合法
        if (props.isEmpty()) {
            return
        }

        // 将变化的属性的key抽离到一个列表中
        val propKeys: MutableList<String?> = ArrayList()
        for (prop in props) {
            propKeys.add(prop.getKey())
        }

        // 如果组件UI不可见
        if (!uiController.isShow) {
            // 更新一次旋转方向
            uiController.lastOrientation = context.state.currentOrientation
            return
        }

        // 屏幕旋转事件
        if (needHandleOrientation(propKeys)) {
            return
        }

        // 最后更新UI
        uiController.callUIUpdate(stateCompare.lastState, uiController.viewHolder)
    }

    /**
     * 检查是否可以处理屏幕旋转
     * @param propKeys 当前状态变化的属性对应key
     * @return 是否可以处理屏幕旋转
     */
    private fun needHandleOrientation(propKeys: MutableList<String?>): Boolean {
        val orientationKey = "currentOrientation"
        if (!propKeys.contains(orientationKey)) {
            return false
        }

        // 开发者不需要关心这个状态
        propKeys.remove(orientationKey)

        // 读取最新的屏幕方向
        val nowOrientation: Int = context.state.currentOrientation

        // 防重入
        if (uiController.lastOrientation == nowOrientation) {
            return false
        }

        // 方向不一致，执行切换
        uiController.lastOrientation = nowOrientation
        closeDialog()
        return true
    }

    /**
     * 展示对话框，框架内部调用，不需要外部介入
     */
    fun showDialog(dialog: ILRDialog?) {
        if (dialog == null) {
            return
        }
        closeDialog()

        dialogInstance = dialog
        this@DialogComponent.uiController.attach()

        dialogInterface?.let { dialogInstance?.setIDialog(it) }

        val platform: IPlatform = context.platform
        platform.activity?.let { dialogInstance?.showDialog(it) }
    }

    private fun closeDialog() {
        if (dialogInstance != null) {
            try {
                dialogInstance?.closeDialog()
            } catch (e: Exception) {
                logger.printStackTrace(
                    ILogger.ERROR_TAG, "close component dialog exception ", e)
            }
            dialogInstance = null
        }
    }

    @CallSuper
    override fun clear() {
        super.clear()
        closeDialog()
        dialogInterface = null
    }

    /**
     * 当组件的对话框销毁的时候，给组件一个机会去做一些清理操作
     */
    protected fun onDialogDismiss(context: ReduxContext<S>?) {
        // sub class impl
    }
}