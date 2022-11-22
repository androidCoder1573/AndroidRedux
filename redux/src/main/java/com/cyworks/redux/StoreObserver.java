package com.cyworks.redux;

import android.support.annotation.NonNull;

import com.tencent.redux.prop.IPropsChanged;

import java.util.List;

/**
 * Desc: 用于监听Store的变化, 通过ReduxContext注入到Store中{@link ReduxContext},
 * 框架内部负责创建StoreObserver，外部不可见。
 *
 * @author randytu on 2021/4/29
 */
final class StoreObserver {
    /**
     * 通知属性变化的callback
     */
    private final IPropsChanged mCB;

    /**
     * 当前组件对应的State的类名
     */
    private final String mToken;

    /**
     * 创建一个StoreObserver
     *
     * @param cb 属性变化监听器
     * @param token 当前StoreObserver对应状态的状态的class name
     */
    StoreObserver(@NonNull IPropsChanged cb, String token) {
        mCB = cb;
        mToken = token;
    }

    /**
     * 当store内部数据发生变化时，通知关心的组件状态变化了
     *
     * @param props 变化的属性
     */
    public void onPropChanged(List<ReactiveProp<Object>> props) {
        if (props == null || props.isEmpty()) {
            return;
        }

        mCB.onPropChanged(props);
    }

    public String getToken() {
        return mToken;
    }
}
