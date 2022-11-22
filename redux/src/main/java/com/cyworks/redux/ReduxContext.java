package com.cyworks.redux;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

import com.tencent.redux.action.Action;
import com.tencent.redux.action.InnerActions;
import com.tencent.redux.adapter.RootAdapter;
import com.tencent.redux.dialog.ILRDialog;
import com.tencent.redux.dispatcher.Dispatch;
import com.tencent.redux.dispose.IDispose;
import com.tencent.redux.interceptor.InterceptorPayload;
import com.tencent.redux.lifecycle.LifeCycleAction;
import com.tencent.redux.prop.IPropsChanged;
import com.tencent.redux.reducer.PureReducer;
import com.tencent.redux.state.ComponentStateGetter;
import com.tencent.redux.state.StateChangeForUI;
import com.tencent.redux.util.ILogger;
import com.tencent.redux.util.IPlatform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Desc: Redux Context, context是开发者主要关心的类，主要用来做一些界面交互，发送Action等.
 *
 * 为什么将对store的操作封装成context对象？
 * context里主要做了这几件事情：
 * 1、创建Dispatch：包括Effect以及Reducer。
 * 2、初始化store观察者，并注入到Store中
 * 3、初始化属性观察者，方便开发者观察组件内某些属性的变化
 * 4、维护一个本组件State的副本，方便进行界面更新
 * 5、维护了平台相关的一些操作，比如权限控制，启动activity等。
 *
 * <p></p>
 * 通过Context封装的方式，让组件对store的操作更加内聚。
 *
 * @author randytu on 2020/7/18
 */
public final class ReduxContext<S extends State> {

    /**
     * ReduxContext对应的组件实例
     */
    private Logic<S> mLogic;

    /**
     * 组件对应的State
     */
    private final S mState;

    /**
     * 当状态发生变化时，通过此接口分发给UI
     */
    private final StateChangeForUI<S> mStateChangeListener;

    /**
     * 保存对store观察的dispose，用于清理操作
     */
    private IDispose mStateGetterDispose;

    /**
     * 保存对dispatch的dispose，用于清理操作
     */
    private IDispose mDispatchDispose;

    /**
     * 保存对UI更新listener的dispose，用于清理操作
     */
    private IDispose mUIUpdaterDispose;

    /**
     * 保存对store观察的dispose，用于清理操作
     */
    private final IDispose mStoreObserverDispose;

    /**
     * 存放当前组件已更新的属性，在下一次vsync信号过来时，用于UI更新
     */
    private HashMap<String, ReactiveProp<Object>> mPendingChangedProps;

    /**
     * 可分发Reducer的Action的dispatch
     */
    private Dispatch mReducerDispatch;

    /**
     * 可分发effect的dispatch
     */
    private Dispatch mEffectDispatch;

    /**
     * 保存一些父组件相关的数据
     */
    private Environment mEnvironment;

    /**
     * 平台操作相关
     */
    private final IPlatform mPlatform;

    /**
     * log组件
     */
    private final ILogger mLogger = ReduxManager.getInstance().getLogger();

    /**
     * 是否销毁了
     */
    private boolean isDestroy;

    /**
     * 是否检测状态完成
     */
    private volatile boolean isStateReady;

    /**
     * 存放因为异步操作而被挂起的action
     */
    private ArrayList<Action> mPendingLifeCycleActionList;

    /**
     * 如果开发这不想使用Action驱动，可以通过传统的方式书写逻辑代码，需继承BaseController
     */
    private BaseController<S> mController;

    /**
     * 构造器，通过builder创建
     * @param builder ReduxContextBuilder
     */
    ReduxContext(@NonNull ReduxContextBuilder<S> builder) {
        mLogic = builder.getLogic();
        mEnvironment = mLogic.getEnvironment();
        mPlatform = builder.getPlatform();

        // 初始化Dispatch
        initDispatch();

        // 获取组件的初始state
        mState = builder.getState();

        // 初始化State Getter，用注册到Store中，获取当前组件对应的State
        initStateGetter();

        // 监听Store抛出来的变化
        final IPropsChanged propsChanged = ReduxContext.this::onStateChange;
        StoreObserver observer =
                new StoreObserver(propsChanged, mState.getClass().getName());
        mStoreObserverDispose = mEnvironment.getStore().observe(observer);

        // UI观察者，当state变化时，需要触发ui刷新
        mStateChangeListener = builder.getStateChangeListener();

        // 注册Vsync同步信号，统一时机刷新UI
        registerUIUpdater();
    }

    protected void setController(BaseController<S> controller) {
        mController = controller;
        if (mController == null) {
            mController = new BaseController<>();
        }
        mController.setReduxContext(this);

        initDispatch();
    }

    public BaseController<S> getController() {
        return mController;
    }

    private void initDispatch() {
        mReducerDispatch = (action, payload) -> {
            mLogger.d(ILogger.ACTION_TAG, "reducer action is <"
                    + action.getName() + ">, in <"
                    + mLogic.getClass().getSimpleName() + ">");
            if (mEnvironment != null) {
                Store<? extends State> store = mEnvironment.getStore();
                if (store != null) {
                    store.dispatch(action, payload);
                }
            }
        };

        // 创建负责分发Effect Action的Dispatch
        mEffectDispatch = (action, payload) -> {
            mLogger.d(ILogger.ACTION_TAG, "effect action is <"
                    + action.getName() + ">, in <"
                    + mLogic.getClass().getSimpleName() + ">");
            mLogic.mEffect.doAction(action, this, payload);
            // 如果是非严格模式，这个Effect的action还会发送到其他组件中

            // 如果不是私有action，则拦截此action，并交给感兴趣的组件处理
            // 比如: 考虑这个场景：礼物模块发送了一个礼物，其他模块要同时进行一些响应。
            if (!action.isPrivate()) {
                mLogger.d(ILogger.ACTION_TAG, "action is <" + action.getName()
                        + "> is public action, will send to any component in page");
                // Interceptor只能由Page来拦截, 拦截时排除自己
                dispatchToPage(InnerActions.INTERCEPT_ACTION,
                        new InterceptorPayload(action, payload, mEffectDispatch));
            }
        };

        DispatchBus bus = mEnvironment.getDispatchBus();
        // 注册effect dispatch, 用于组件间交互
        mDispatchDispose = bus.registerReceiver(mEffectDispatch);

        if (mLogic instanceof LogicPage) {
            // 为了防止组件发广播时，其他组件也可以接收此广播，导致组件间通信通过广播来进行。
            // 规定只有page才能接收广播，因此在此设置整个page的Effect分发状态。
            bus.setPageEffectDispatch(mEffectDispatch);
        }
    }

    protected Dispatch getEffectDispatch() {
        return mEffectDispatch;
    }

    @SuppressWarnings("unchecked")
    private void initStateGetter() {
        // 主要给store来用
        final ComponentStateGetter<S> getter = ReduxContext.this::getState;

        Store<? extends State> store = mEnvironment.getStore();
        if (store instanceof PageStore) {
            mStateGetterDispose =
                    ((PageStore<? extends State>)store)
                            .setStateGetter((ComponentStateGetter<State>) getter);
        }
    }

    private void registerUIUpdater() {
        Store<? extends State> store = mEnvironment.getStore();
        if (mStateChangeListener == null || !(store instanceof PageStore)) {
            return;
        }

        // 接收Vsync信号，优化刷新性能
        mUIUpdaterDispose = ((PageStore)store).addUIUpdater(() -> {
            if (mPendingChangedProps != null && !mPendingChangedProps.isEmpty()) {
                List<ReactiveProp<Object>> props =
                        new ArrayList<>(mPendingChangedProps.values());
                mPendingChangedProps.clear();
                mStateChangeListener.onChange(mState, props);
            }
        });
    }

    /**
     * 当开发者不使用action的时候，本方法用于更新State
     */
    protected void updateState(final PureReducer<S> reducer) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            innerUpdateState(reducer);
            return;
        }

        ReduxManager.getInstance().submitInMainThread(() -> {
            innerUpdateState(reducer);
        });
    }

    private void innerUpdateState(PureReducer<S> reducer) {
        S state = getState();
        if (state != null) {
            state.setStateProxy(new StateProxy());
        }

        final S newState = reducer.update(state);
        if (newState == null) {
            return;
        }

        // 获取私有属性变化，并本地更新
        List<ReactiveProp<Object>> privateProps = newState.getPrivatePropChanged();
        if (privateProps != null) {
            onStateChange(privateProps);
        }

        mEnvironment.getStore().onStateChanged(newState);
    }

    /**
     * 当状态发生变化的时候，通过这个接口更新UI
     * @param props 变化的属性列表 {@link ReactiveProp}
     */
    protected void onStateChange(List<ReactiveProp<Object>> props) {
        if (mLogic == null) {
            return;
        }

        for (ReactiveProp<Object> prop : props) {
            String key = prop.getKey();

            putChangedProp(key, prop);
            mLogger.d(ILogger.ACTION_TAG, "current changed prop is <"
                    + key + "> in <" + mLogic.getClass().getSimpleName() + ">");
        }

        markNeedUpdate();

        // 通知属性订阅者，状态发生了变化
        mLogic.getPropWatcher().notifyPropChanged(props, this);
    }

    /**
     * 进行一次全量更新：
     * 主要用组件隐藏之后再显示的操作，或者横竖屏切换时的操作
     */
    protected void runFullUpdate() {
        ConcurrentHashMap<String, ReactiveProp<Object>> map = mState.getDataMap();

        for (String key : map.keySet()) {
            ReactiveProp<Object> prop = map.get(key);

            if (prop != null) {
                putChangedProp(key, prop);
            }
        }

        markNeedUpdate();
    }

    /**
     * 首次创建UI时，根据是否更新初始值来展示UI
     */
    protected void firstUpdate() {
        ConcurrentHashMap<String, ReactiveProp<Object>> map = mState.getDataMap();

        for (String key : map.keySet()) {
            ReactiveProp<Object> reactiveProp = map.get(key);

            if (reactiveProp != null && reactiveProp.isUpdateWithInitValue()) {
                putChangedProp(key, reactiveProp);
            }
        }

        markNeedUpdate();
    }

    private void putChangedProp(String key, ReactiveProp<Object> reactiveProp) {
        if (mPendingChangedProps == null) {
            mPendingChangedProps = new HashMap<>();
        }
        mPendingChangedProps.put(key, reactiveProp);
    }

    private void markNeedUpdate() {
        boolean need = mPendingChangedProps != null && !mPendingChangedProps.isEmpty();
        if (mEnvironment.getStore() instanceof PageStore && need) {
            ((PageStore) mEnvironment.getStore()).markNeedUpdate();
        }
    }

    public boolean isSameEffectDispatch(Dispatch dispatch) {
        return dispatch == mEffectDispatch;
    }

    /**
     * 获取当前组件的State, 这里返回的State不具备修改通知能力
     *
     * @return 当前组件的State
     */
    public S getState() {
        return State.copyState(mState);
    }

    /**
     * 分发Reducer Action
     *
     * @param action 携带的Action
     * @param payload 携带的参数
     */
    public void dispatchReducer(Action action, Object payload) {
        if (isDestroy || !isStateReady) {
            return;
        }

        mReducerDispatch.dispatch(action, payload);
    }

    /**
     * 分发 Effect Action
     *
     * @param action 携带的Action
     * @param payload 携带的参数
     */
    public void dispatchEffect(Action action, Object payload) {
        if (isDestroy || !isStateReady) {
            return;
        }

        mEffectDispatch.dispatch(action, payload);
    }

    /**
     * 子组件直接给Page发送action, 只能Effect来接收
     *
     * @param action Action
     * @param payload 携带的参数
     */
    public final void dispatchToPage(Action action, Object payload) {
        if (isDestroy || !isStateReady || mEnvironment == null) {
            return;
        }

        DispatchBus bus = mEnvironment.getDispatchBus();
        bus.getPageDispatch().dispatch(action, payload);
    }

    /**
     * 子组件发action给父组件，只能使用Effect来接收
     *
     * @param action Action
     * @param payload 携带的参数
     */
    public final void dispatchToParent(Action action, Object payload) {
        if (isDestroy || !isStateReady || mEnvironment == null) {
            return;
        }

        if (!action.isPrivate()) {
            mLogger.w(ILogger.ERROR_TAG, "only private action can interact parent & child");
            return;
        }

        // 交给父组件的dispatch
        Dispatch parentDispatch = mEnvironment.getParentDispatch();
        if (parentDispatch != null) {
            parentDispatch.dispatch(action, payload);
        }
    }

    /**
     * 父组件发action给子组件，只能使用Effect来接收
     *
     * @param action Action
     * @param payload 携带的参数
     */
    public final void dispatchToChildren(Action action, Object payload) {
        if (isDestroy || !isStateReady || mEnvironment == null) {
            return;
        }

        if (!action.isPrivate()) {
            mLogger.w(ILogger.ERROR_TAG, "only private action can interact parent & child");
            return;
        }

        LogicComponent<BaseComponentState> component = (LogicComponent<BaseComponentState>) mLogic;
        dispatchToSubComponent(component, action, payload);
        dispatchToAdapter(component, action, payload);
    }

    private void dispatchToSubComponent(LogicComponent<BaseComponentState> component,
                                        Action action, Object payload) {
        // 发给组件依赖的子组件
        HashMap<String, Dependant<? extends BaseComponentState, State>> maps =
                component.getChildrenDependant();
        if (maps != null) {
            for (Dependant<? extends BaseComponentState, State> dependant : maps.values()) {
                Logic<? extends BaseComponentState> logic = dependant.getLogic();
                logic.mContext.mEffectDispatch.dispatch(action, payload);
            }
        }
    }

    private void dispatchToAdapter(LogicComponent<BaseComponentState> component,
                                   Action action, Object payload) {
        Dependant<? extends BaseComponentState, State> dependant =
                component.getAdapterDependant();
        if (dependant != null) {
            Logic<? extends BaseComponentState> logic = dependant.getLogic();
            logic.mContext.mEffectDispatch.dispatch(action, payload);
        }
    }

    /**
     * 发送全局广播，本方法在App级别是全局的, 只有page下的Effect才可以处理
     *
     * @param action 要分发的Action
     * @param payload 携带的参数
     */
    public void broadcast(Action action, Object payload) {
        if (isDestroy || !isStateReady || mEnvironment == null) {
            return;
        }

        mEnvironment.getDispatchBus().broadcast(action, payload);
    }

    /**
     * 发送页面内广播，仅Page可以发送页面内广播
     *
     * @param action Action
     * @param payload 参数
     */
    public final void broadcastInPage(Action action, Object payload) {
        if (mEnvironment == null || !stateReady() || !(mLogic instanceof LogicPage)) {
            return;
        }

        mEnvironment.getDispatchBus().dispatch(action, payload);
    }

    /**
     * 用于组件生命周期响应的方法
     *
     * @param action Action
     */
    void onLifecycle(Action action) {
        if (isDestroy || !LifeCycleAction.isLifeCycle(action)) {
            return;
        }

        if (!isStateReady) {
            if (mPendingLifeCycleActionList == null) {
                mPendingLifeCycleActionList = new ArrayList<>();
            }
            mPendingLifeCycleActionList.add(action);
            return;
        }

        dispatchEffect(action, null);
    }

    /**
     * 如果当前组件存在列表型UI，则可以通过组件实例获取到当前组件列表对应的Adapter
     *
     * @return 组件列表对应的实际的Adapter
     */
    public RecyclerView.Adapter<RecyclerView.ViewHolder> getRootAdapter() {
        if (mLogic == null || mLogic instanceof LogicPage) {
            return null;
        }

        Dependant<? extends BaseComponentState, State> dependant =
                ((LogicComponent<? extends BaseComponentState>)mLogic).getAdapterDependant();

        RootAdapter<? extends BaseComponentState> adapter = dependant != null
                ? dependant.getAdapter()
                : null;

        return adapter == null ? null : adapter.getRealAdapter();
    }

    public IPlatform getPlatform() {
        return mPlatform;
    }

    /**
     * 当State 检测以及merge ready之后，调用此方法，然后会检查异步操作之后挂起的life cycle action
     */
    void setStateReady() {
        isStateReady = true;

        if (mPendingLifeCycleActionList == null) {
            return;
        }

        final int size = mPendingLifeCycleActionList.size();
        if (size > 0) {
            List<Action> copy = new ArrayList<>(mPendingLifeCycleActionList);
            for (int i = 0; i < size; i++) {
                onLifecycle(copy.get(i));
            }
            mPendingLifeCycleActionList.clear();
        }
    }

    /**
     * 是否状态ready
     * @return boolean
     */
    boolean stateReady() {
        return isStateReady;
    }

    /**
     * 展示一个对话框组件
     */
    public void showComponentDialog(ILRDialog dialog) {
        if (mLogic instanceof LRDialogComponent) {
            ((LRDialogComponent<? extends BaseComponentState>) mLogic).showDialog(dialog);
        }
    }

    /**
     * 清理接口，做一些清理的事情
     */
    public void destroy() {
        isDestroy = true;
        mState.clear();

        if (mDispatchDispose != null) {
            mDispatchDispose.dispose();
        }

        if (mStoreObserverDispose != null) {
            mStoreObserverDispose.dispose();
        }

        if (mUIUpdaterDispose != null) {
            mUIUpdaterDispose.dispose();
        }

        if (mStateGetterDispose != null) {
            mStateGetterDispose.dispose();
        }

        mLogic = null;
        mEnvironment = null;
    }

}
