package com.cyworks.redux.dependant;

import android.support.annotation.NonNull;
import com.tencent.redux.BaseComponentState;
import com.tencent.redux.BaseState;
import com.tencent.redux.Dependant;
import com.tencent.redux.reducer.SubReducer;
import java.util.HashMap;
import java.util.List;

/**
 * Desc: 如果某个组件需要依赖一些子组件，则需要使用本类来收集这些子组件。
 *
 * 本类作用就是收集当前组件依赖的子组件，还承担着合并子组件Reducer的作用。
 *
 * 针对列表Adapter的特殊处理：
 * 如果组件存在列表UI，由于Android的列表是比较特殊的存在：RecyclerView + Adapter;
 * 如果框架不对Adapter进行处理，那么必将会在ViewUpdater中操作Adapter，
 * 跟我们最初的设计相违背（ViewUpdater中只有UI，操作Adapter相当于对数据进行处理。）
 * 因此将Adapter变相设计成为一个LRLogic，让Adapter本身变成响应式的Redux Object，
 * 这样就能很好的融入整个框架，但是对Adapter做了限制：
 * 一个组件只能设置一个列表型UI，目的是让组件的粒度更加细致。
 *
 * @author randytu on 2020/8/2
 */
public class DependentCollect<PS extends BaseState> {
    /**
     * 组件依赖器集合
     */
    private HashMap<String, Dependant<? extends BaseComponentState, PS>> mDependants;

    /**
     * 当前组件持有的Adapter，一个组件只能持有一个Adapter，组件粒度按列表划分
     */
    private Dependant<BaseComponentState, PS> mRootAdapterDependant;

    public DependentCollect() {}

    /**
     * 给组件添加一个子组件依赖
     *
     * @param dependant 子组件dependant实例
     */
    public void addDependant(@NonNull Dependant<? extends BaseComponentState, PS> dependant) {
        if (mDependants == null) {
            mDependants = new HashMap<>();
        }

        String key = "" + dependant.hashCode();
        if (mDependants.containsKey(key)) {
            return;
        }

        mDependants.put(key, dependant);
    }

    /**
     * 合并子组件的Reducer，首先会将每个子组件的reducer合并成一个SubReducer(进行AOP操作)
     *
     * @param list 当前的子组件的Reducer列表
     */
    public void mergerDependantReducer(@NonNull List<SubReducer> list) {
        if (mDependants != null && !mDependants.isEmpty()) {
            for (Dependant<? extends BaseComponentState, PS> dependant : mDependants.values()) {
                if (dependant != null) {
                    dependant.createSubReducer(list);
                }
            }
        }

        // 将Adapter的Reducer合并
        if (mRootAdapterDependant != null) {
            mRootAdapterDependant.createSubReducer(list);
        }
    }

    public HashMap<String, Dependant<? extends BaseComponentState, PS>> getDependantMap() {
        return mDependants;
    }

    public void setAdapterDependant(Dependant dependant) {
        mRootAdapterDependant = dependant;
    }

    public Dependant<BaseComponentState, PS> getAdapterDependant() {
        return mRootAdapterDependant;
    }

    public void clear() {
        if (mDependants != null) {
            mDependants.clear();
        }
    }
}
