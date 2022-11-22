package com.cyworks.redux;

import android.support.annotation.NonNull;

import com.tencent.redux.action.Action;
import com.tencent.redux.adapter.RootAdapter;
import com.tencent.redux.interceptor.Interceptor;
import com.tencent.redux.reducer.SubReducer;
import com.tencent.redux.state.StateGetter;

import java.util.HashMap;
import java.util.List;

/**
 * Desc: 表示一个具体的依赖，一个组件对应唯一一个依赖。
 * 将子组件以及对应的Connector组合成一个Dependant，用于在父组件中表示依赖的子组件
 *
 * PS: 父组件的State，CS：当前组件的State
 *
 * @author randytu on 2020/7/21
 */
public class Dependant<CS extends BaseComponentState, PS extends State> {
    /**
     * 组件实例
     */
    private final Logic<CS> mLogic;

    /**
     * 组件对应的连接器
     */
    private LRConnector<CS, State> mConnector;

    /**
     * 构造器，创建一个组件依赖
     *
     * @param logic 对应的组件
     * @param connector 组件连接器
     */
    @SuppressWarnings("unchecked")
    public Dependant(@NonNull Logic<CS> logic, LRConnector<CS, PS> connector) {
        mLogic = logic;

        initConnector(connector);
    }

    private LRConnector<CS, PS> createDefaultConnector() {
        return new LRConnector<CS, PS>() {
            @Override
            public void parentStateCollector(CS childState, PS parentState) { }

            @Override
            public void globalStateCollector(GlobalStoreWatcher<CS> watcher) { }

            @Override
            public HashMap<Action, Interceptor<CS>> interceptorCollector() {
                return null;
            }
        };
    }

    private void initConnector(LRConnector<CS, PS> connector) {
        mConnector = (LRConnector<CS, State>) connector;
        if (connector == null) {
            mConnector = (LRConnector<CS, State>) createDefaultConnector();
        }
        // 注入子组件State的获取接口
        StateGetter<CS> stateGetter = () -> {
            ReduxContext<CS> context = mLogic.getContext();
            if (context == null) {
                return null;
            }

            CS state = context.getState();
            if (state != null) {
                state.setStateProxy(new StateProxy());
            }

            return state;
        };

        // 注入StateGetter
        mConnector.injectGetter(stateGetter);

        // 注入子组件私有属性变化时的监听器
        mConnector.injectLocalStateChangeCb((props) -> {
            ReduxContext<CS> context = mLogic.getContext();
            context.onStateChange(props);
        });
    }

    /**
     * 将每个子组件的Reducer合成一个SubReducer，SubReducer目的是为组件的Reducer增加一些功能
     *
     * @param list 外部传入一个SubReducer列表，用于收集子组件的SubReducer
     */
    public void createSubReducer(@NonNull List<SubReducer> list) {
        mLogic.mergeReducer(list, mConnector);
    }

    protected final void install(@NonNull Environment env) {
        initComponent(env);
        initAdapter(env);
    }

    /**
     * 对当前组件的子组件进行初始化操作
     * @param env 父组件的一些信息
     */
    protected final void initComponent(@NonNull Environment env) {
        if (!(mLogic instanceof BaseComponent || mLogic instanceof LogicTestComponent)) {
            return;
        }

        mConnector.setParentState(env.getParentState());

        ((LogicComponent<CS>)mLogic).install(env, mConnector);
    }

    /**
     * 如果组件中存在列表，调用此方法初始化Adapter
     * @param env 父组件的一些信息
     */
    protected final void initAdapter(@NonNull Environment env) {
        if (!(mLogic instanceof RootAdapter)) {
            return;
        }

        mConnector.setParentState(env.getParentState());

        ((RootAdapter<CS>)mLogic).install(env, mConnector);
    }

    /**
     * 显示组件UI
     */
    protected final void show() {
        if (mLogic instanceof BaseComponent) {
            ((BaseComponent<BaseComponentState>)mLogic).show();
        }
    }

    /**
     * 隐藏组件UI
     */
    protected final void hide() {
        if (mLogic instanceof BaseComponent) {
            ((BaseComponent<BaseComponentState>)mLogic).hide();
        }
    }

    /**
     * 绑定组件UI
     */
    protected final void attach() {
        if (mLogic instanceof BaseComponent) {
            ((BaseComponent<BaseComponentState>)mLogic).attach();
            return;
        }

        if (mLogic instanceof RootAdapter) {
            ((RootAdapter<BaseComponentState>)mLogic).attach();
        }
    }

    /**
     * 卸载组件UI
     */
    protected final void detach() {
        if (mLogic instanceof BaseComponent) {
            ((BaseComponent<BaseComponentState>)mLogic).detach();
            return;
        }

        if (mLogic instanceof RootAdapter) {
            ((RootAdapter<BaseComponentState>)mLogic).detach();
        }
    }

    /**
     * 如果界面存在列表，通过这个接口获取RootAdapter
     * @return RootAdapter
     */
    protected final RootAdapter<CS> getAdapter() {
        if (mLogic instanceof RootAdapter) {
            return (RootAdapter<CS>)mLogic;
        }

        return null;
    }

    /**
     * 获取当前组件对应Connector
     * @return 返回Connector {@link LRConnector}
     */
    @SuppressWarnings("unchecked")
    public LRConnector<CS, PS> getConnector() {
        return (LRConnector<CS, PS>) mConnector;
    }

    protected final Logic<? extends BaseComponentState> getLogic() {
        return mLogic;
    }

    /**
     * 当前组件是否已经安装到父组件中
     * @return 是否已安装
     */
    protected final boolean isInstalled() {
        if (mLogic instanceof BaseComponent) {
           return ((BaseComponent<BaseComponentState>)mLogic).isInstalled();
        }

        return true;
    }

}
