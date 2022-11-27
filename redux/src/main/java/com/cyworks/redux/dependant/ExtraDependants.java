package com.cyworks.redux.dependant;

import android.support.annotation.NonNull;
import com.tencent.redux.BaseComponentState;
import com.tencent.redux.BasePageState;
import com.tencent.redux.Dependant;
import java.util.HashMap;

/**
 * Desc: 存放额外的Feature, 目前存在这样的需求：
 * 如果开发者想要延迟安装一些组件，可以在某些时机发送
 * INSTALL_EXTRA_FEATURE_ACTION{@link com.tencent.redux.action.InnerActions}
 *
 * 组件通过此结构封装。
 */
public final class ExtraDependants<S extends BasePageState> {
    /**
     * 保存额外feature的map
     */
    public HashMap<String, Dependant<? extends BaseComponentState, S>> mExtraFeature;

    /**
     * 给page添加一个额外的feature
     * @param dependant Dependant实例
     */
    public void addExtDependant(@NonNull Dependant<? extends BaseComponentState, S> dependant) {
        if (mExtraFeature == null) {
            mExtraFeature = new HashMap<>();
        }

        String key = "" + dependant.hashCode();
        if (mExtraFeature.containsKey(key)) {
            return;
        }

        mExtraFeature.put(key, dependant);
    }
}
