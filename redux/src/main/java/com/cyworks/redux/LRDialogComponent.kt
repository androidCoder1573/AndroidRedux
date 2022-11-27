package com.cyworks.redux

import android.support.annotation.CallSuper
import android.view.View
import java.lang.Exception
import java.util.ArrayList

/**
 * Desc: 对话框组件基类，扩展了打开对话框的功能，控制了初始化的一些特殊操作。
 *
 * note: 如果对话框本身功能比较复杂，还是建议使用Page来实现,
 * 防止单一组件功能过多。
 */
abstract class LRDialogComponent<S : BaseComponentState?> : BaseComponent<S>(true) {
    /**
     * 当前展示的对话框实例,
     * 通过这种方式，框架不需要关心对话框的具体形式(Dialog 或者 FragmentDialog，androidX等)
     */
    private var mDialog: ILRDialog? = null

    /**
     * 框架注入的对话框控制接口
     */
    private var mDialogInterface: IDialogController? = object : IDialogController() {
        val view: View
            get() = mUIMixin.mCurrentView

        fun onDialogDismiss() {
            detach()
            // UI detach之后再清理，保证安全
            this@LRDialogComponent.onDialogDismiss(context)
            mDialog = null
        }
    }

    /**
     * UI 数据变化时的回调
     *
     * @param stateCompare ChangedState
     */
    private fun onDataChangedCB(stateCompare: ChangedState<S>) {
        if (mUIMixin.canNotUpdateUI() || environment == null) {
            return
        }
        val props: List<ReactiveProp<Any>> = stateCompare.mChangedProps
        // 检查属性是否合法
        if (props == null || props.isEmpty()) {
            return
        }

        // 将变化的属性的key抽离到一个列表中
        val propKeys: MutableList<String?> = ArrayList()
        for (prop in props) {
            propKeys.add(prop.key)
        }

        // 如果组件UI不可见
        if (!mUIMixin.isShow) {
            // 更新一次旋转方向
            mUIMixin.mLastOrientation = context!!.state.mCurrentOrientation.value()
            return
        }

        // 屏幕旋转事件
        if (needHandleOrientation(propKeys)) {
            return
        }

        // 最后更新UI
        mUIMixin.callUIUpdate(stateCompare.mState, propKeys, mUIMixin.mViewHolder)
    }

    /**
     * 检查是否可以处理屏幕旋转
     * @param propKeys 当前状态变化的属性对应key
     * @return 是否可以处理屏幕旋转
     */
    private fun needHandleOrientation(propKeys: MutableList<String?>): Boolean {
        val orientationKey: String = ORIENTATION_KEY
        if (!propKeys.contains(orientationKey)) {
            return false
        }

        // 开发者不需要关心这个状态
        propKeys.remove(orientationKey)

        // 读取最新的屏幕方向
        val nowOrientation: Int = context!!.state.mCurrentOrientation.value()

        // 防重入
        if (mUIMixin.mLastOrientation == nowOrientation) {
            return false
        }

        // 方向不一致，执行切换
        mUIMixin.mLastOrientation = nowOrientation
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
        mDialog = dialog
        attach()
        mDialog.setIDialog(mDialogInterface)
        val platform: IPlatform = context!!.platform
        if (platform != null) {
            mDialog.showDialog(platform.getActivity())
        }
    }

    private fun closeDialog() {
        if (mDialog != null) {
            try {
                mDialog.closeDialog()
            } catch (e: Exception) {
                // 关闭对话框可能存在多种异常，比如空指针，bad token等
                mLogger.printStackTrace(
                    ILogger.ERROR_TAG,
                    "close component dialog exception ", e
                )
            }
            mDialog = null
        }
    }

    @CallSuper
    override fun clear() {
        super.clear()
        closeDialog()
        mDialogInterface = null
    }

    /**
     * 当组件的对话框销毁的时候，给组件一个机会去做一些清理操作
     */
    protected fun onDialogDismiss(context: ReduxContext<S?>?) {
        // sub class impl
    }

    init {
        mObserver = Observer<ChangedState<S>> { stateCompare: ChangedState<S> ->
            onDataChangedCB(stateCompare)
        }
    }
}