package com.cyworks.redux;

import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.view.View;

import com.tencent.redux.beans.UIChangedBean;
import com.tencent.redux.lifecycle.LifeCycleAction;
import com.tencent.redux.ui.ComponentViewHolder;
import com.tencent.redux.ui.UIWatcher;
import com.tencent.redux.ui.ViewModule;
import com.tencent.redux.util.ILogger;

import java.util.HashMap;
import java.util.List;

/**
 * Desc: 对组件的UI操作抽取出来放到UI处理类中:
 * 1、初始化UI;
 * 2、设置Atom，用于UI的局部更新;
 * 3、设置UI组件的View的显示/隐藏以及绑定/删除操作;
 *
 * todo: 可以考虑将此类让开发者实现，更加定制化的操作UI
 *
 * @author randytu on 2021/5/25
 */
final class ComponentUIMixin<S extends BaseComponentState> {
    /**
     * 组件是否bind到父组件上
     */
    boolean isBind;

    /**
     * 当前组件的UI界面是否显示
     */
    boolean isShow;

    /**
     * Component自己的View，可以被detach，横屏UI
     */
    private View mLandscapeView;

    /**
     * Component自己的View，可以被detach, 竖屏UI
     */
    private View mPortraitView;

    /**
     * Component当前显示的View
     */
    View mCurrentView;

    /**
     * 保存当前组件View的辅助类，方便在更新接口中获取view
     */
    ComponentViewHolder mViewHolder;

    /**
     * 当前组件对应的UI的更新接口
     */
    private final ViewModule<S> mViewModule;

    /**
     * UI 监听状态变化，触发UI更新
     */
    private UIWatcher<S> mUIWatcher;

    /**
     * 上次的屏幕方向
     */
    int mLastOrientation = Configuration.ORIENTATION_PORTRAIT;

    /**
     * 组件实例
     */
    private final BaseComponent<S> mComponent;

    /**
     * Log 组件
     */
    private final ILogger mLogger = ReduxManager.getInstance().getLogger();

    private boolean isRunFirstUpdate;

    ComponentUIMixin(BaseComponent<S> component, boolean lazyBindUI) {
        mComponent = component;
        mViewModule = mComponent.getViewModule();

        // 如果是延迟加载，表明初始状态时不加载UI
        isShow = !lazyBindUI;
    }

    /**
     * 为当前组件创建UI片段观察者
     */
    void makeUIWatcher(@NonNull S state) {
        if (mViewModule == null) {
            return;
        }

        // 创建UI更新器
        mUIWatcher = new UIWatcher<>();
        mUIWatcher.setAtom(state, mViewModule);
    }

    /**
     * 重置View Holder
     */
    void resetViewHolder() {
        if (mViewHolder != null) {
            mViewHolder.dispose();
        }
        mViewHolder = new ComponentViewHolder(mCurrentView);
    }

    /**
     * 组件UI展示
     *
     * @param needVisible 只有调用过程中的第一个View才需要显示
     */
    void show(boolean needVisible) {
        // 如果已经绑定了，则直接走构建UI的逻辑
        if (mLastOrientation == Configuration.ORIENTATION_PORTRAIT && mPortraitView == null
                || mLastOrientation == Configuration.ORIENTATION_LANDSCAPE && mLandscapeView == null) {
            initUI();
        }

        // 根组件进行展示
        if (needVisible) {
            if (mCurrentView != null) {
                mCurrentView.setVisibility(View.VISIBLE);
            }
        }

        // 更新一次UI
        fullUpdate();

        // 通知组件当前组件UI发生变化了，给用户一个机会做一些善后处理
        // 首次初始化不需要这些
        sendUIChangedAction(UIChangedBean.TYPE_VISIBILITY_CHANGE);

        showChildren();
    }

    private void showChildren() {
        if (mComponent == null) {
            return;
        }

        attachAdapter();

        // 处理子组件
        HashMap<String, Dependant<? extends BaseComponentState, State>> map =
                mComponent.getChildrenDependant();
        if (map == null || map.isEmpty()) {
            return;
        }

        for (Dependant<? extends BaseComponentState, State> dependant : map.values()) {
            if (dependant == null) {
                continue;
            }

            if (!dependant.isInstalled()) {
                installComponent(dependant);
                continue;
            }

            // 如果绑定了，则走show的逻辑
            dependant.show();
        }
    }

    /**
     * 组件UI隐藏，跟detach还是有区别的，detach了之后只有像ViewPager/Dialog这类的组件，
     * 才能够重新安装，否则ViewStub被清除，导致重新加载布局时出现异常。
     *
     * @param needGone 只有调用过程中的第一个View才需要隐藏
     */
    void hide(boolean needGone) {
        if (needGone) {
            if (mCurrentView != null) {
                mCurrentView.setVisibility(View.GONE);
            }
        }

        hideChildren();

        setShow(false);

        // 通知组件当前组件UI发生变化了，给用户一个机会做一些善后处理
        sendUIChangedAction(UIChangedBean.TYPE_VISIBILITY_CHANGE);
    }

    private void hideChildren() {
        if (mComponent == null) {
            return;
        }

        detachAdapter();

        // 处理子组件
        HashMap<String, Dependant<? extends BaseComponentState, State>> map =
                mComponent.getChildrenDependant();
        if (map != null && !map.isEmpty()) {
            for (Dependant<? extends BaseComponentState, State> dependant : map.values()) {
                if (dependant != null) {
                    dependant.hide();
                }
            }
        }
    }

    /**
     * 主要是执行View的attach操作，已经安装的组件不执行attach操作，
     * detach/attach 主要针对UI彻底销毁的场景。
     *
     * 绑定/删除操作针对普通View会出现detach之后，无法重新绑定的问题，不建议在普通场景调用。
     */
    void attach() {
        // 如果已经绑定了，则直接走构建UI的逻辑
        initUI();

        // 更新一次UI
        fullUpdate();

        attachChildren();
    }

    private void attachChildren() {
        if (mComponent == null) {
            return;
        }

        attachAdapter();

        // 处理子组件
        HashMap<String, Dependant<? extends BaseComponentState, State>> map =
                mComponent.getChildrenDependant();
        if (map == null || map.isEmpty()) {
            return;
        }

        for (Dependant<? extends BaseComponentState, State> dependant : map.values()) {
            if (dependant == null) {
                continue;
            }

            // 走未绑定的逻辑
            if (!dependant.isInstalled()) {
                installComponent(dependant);
                continue;
            }

            // 如果已经绑定了，走attach流程
            dependant.attach();
        }
    }

    /**
     * 主要是执行View的detach操作，已经安装的组件不执行detach操作，
     * detach/attach 主要针对UI彻底销毁的场景。
     *
     * 绑定/删除操作针对普通View会出现detach之后，无法重新绑定的问题，不建议在普通场景调用。
     */
    void detach() {
        detachChildren();

        // 设置不可见
        setShow(false);

        setViewNUll();
    }

    private void setViewNUll() {
        // 清理一些View
        mCurrentView = null;
        mLandscapeView = null;
        mPortraitView = null;

        if (mViewHolder != null) {
            mViewHolder.dispose();
            mViewHolder = null;
        }
    }

    private void detachChildren() {
        if (mComponent == null) {
            return;
        }

        detachAdapter();

        // 处理子组件
        HashMap<String, Dependant<? extends BaseComponentState, State>> map =
                mComponent.getChildrenDependant();
        if (map != null && !map.isEmpty()) {
            for (Dependant<? extends BaseComponentState, State> dependant : map.values()) {
                if (dependant != null) {
                    dependant.detach();
                }
            }
        }
    }

    private void installComponent(Dependant<? extends BaseComponentState, State> dependant) {
        ReduxContext<S> context = mComponent.getContext();
        Environment env = Environment.copy(mComponent.getEnvironment());
        env.setParentState(context.getState())
                .setParentDispatch(context.getEffectDispatch());
        dependant.initComponent(env);
    }

    private void attachAdapter() {
        // 子组件也包含adapter, Adapter要单独处理，adapter是否可复用
        Dependant<? extends BaseComponentState, State> adapter =
                mComponent.getAdapterDependant();
        if (adapter != null) {
            adapter.attach();
        }
    }

    private void detachAdapter() {
        // 子组件也包含adapter, Adapter要单独处理
        Dependant<? extends BaseComponentState, State> adapter =
                mComponent.getAdapterDependant();
        if (adapter != null) {
            adapter.detach();
        }
    }

    /**
     * 初始化组件UI，主要是创建UI实例，初始化Adapter，发起发起首次UI更新动作
     */
    void initUI() {
        // 绑定View
        if (mViewModule == null || mComponent == null) {
            return;
        }

        // Adapter 要先于View初始化，因为创建View的过程中会初始化RecyclerView
        mComponent.initAdapter();

        // 根据屏幕方向，进行View的初始化操作
        changeView(mLastOrientation);

        // 创建View Holder
        if (mViewHolder != null) {
            mViewHolder.dispose();
            mViewHolder = null;
        }
        mViewHolder = new ComponentViewHolder(mCurrentView);

        // 设置显示状态
        setShow(true);
    }

    /**
     * 首次渲染时，对UI更新一次数据
     */
    void firstUpdate() {
        if (mCurrentView == null || isRunFirstUpdate) {
            return;
        }

        ReduxContext<S> context = mComponent.getContext();
        if (context != null && context.stateReady()) {
            isRunFirstUpdate = true;
            context.firstUpdate();
        }
    }

    /**
     * 触发一次全量更新
     */
    void fullUpdate() {
        ReduxContext<S> context = mComponent.getContext();
        if (context != null) {
            context.runFullUpdate();
        }
    }

    private void setShow(boolean show) {
        isShow = show;
        ReduxContext<S> context = mComponent.getContext();
        if (context != null) {
            context.getState().isShowUI.innerSetter(isShow);
        }
    }

    /**
     * 当UI发生可见性/方向变化的时候，发送Action告知用户，进行一些清理操作
     *
     * @param type UI变化的类型
     */
    void sendUIChangedAction(int type) {
        UIChangedBean uiChangedBean = new UIChangedBean();
        uiChangedBean.orientation = mLastOrientation;
        uiChangedBean.isShow = isShow;
        uiChangedBean.mPartialView = mPortraitView;
        uiChangedBean.mLandscapeView = mLandscapeView;
        uiChangedBean.mType = type;

        ReduxContext<S> context = mComponent.getContext();
        if (context != null) {
            context.dispatchEffect(LifeCycleAction.ACTION_ON_UI_CHANGED, uiChangedBean);
        }
    }

    /**
     * 屏幕旋转时，进行View切换，内部进行一些安全检查，并在异常时进行捕获并继续抛出异常
     *
     * @param orientation 当前屏幕的旋转方向
     */
    void changeView(int orientation) {
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            setPortraitView();
            return;
        }

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setLandscapeView();
        }
    }

    private void setPortraitView() {
        if (mPortraitView == null) {
            mPortraitView = callViewBuilder();
        }

        // 如果用户没有设置竖屏UI，则屏幕旋转的时候直接使用横屏UI
        if (mPortraitView == null) {
            mPortraitView = mLandscapeView;
        }

        if (mPortraitView == null) {
            throw new RuntimeException("create view failed in portrait, component is: "
                    + mComponent.getClass().getName());
        }

        mCurrentView = mPortraitView;
    }

    private void setLandscapeView() {
        if (mLandscapeView == null) {
            mLandscapeView = callViewBuilder();
        }

        // 如果用户没有设置横屏UI，则屏幕旋转的时候直接使用竖屏UI
        if (mLandscapeView == null) {
            mLandscapeView = mPortraitView;
        }

        if (mLandscapeView == null) {
            throw new RuntimeException("create view failed in landscape, component is: "
                    + mComponent.getClass().getName());
        }

        mCurrentView = mLandscapeView;
    }

    /**
     * 获取UI组件的View实例
     * @return 组件View实例
     */
    private View callViewBuilder() {
        ReduxContext<S> context = mComponent.getContext();
        if (mViewModule == null || context == null) {
            return null;
        }

        try {
            return mViewModule.getView(context);
        } catch (Exception e) {
            // 这里可能会产生多种异常，比如空指针，重复添加等
            mLogger.printStackTrace(ILogger.ERROR_TAG, "call view builder fail: ", e);
            return null;
        }
    }

    /**
     * 针对UI组件来说，需要检查是否可以更新UI
     * @return 当mViewUpdater不为null时，此时说明当前组件存在UI，可以更新UI
     */
    public boolean canNotUpdateUI() {
        return mViewModule == null;
    }

    /**
     * 当UI数据有更新时，通过此方法触发每个Atom进行更新
     *
     * @param state 当前最新的State
     * @param keys 当前更新的属性列表
     * @param holder 当前UI组件的View Holder
     */
    public void callUIUpdate(S state, List<String> keys, ComponentViewHolder holder) {
        mUIWatcher.updateUI(state, keys, holder);
    }

    void clear() {
        setViewNUll();
    }
}
