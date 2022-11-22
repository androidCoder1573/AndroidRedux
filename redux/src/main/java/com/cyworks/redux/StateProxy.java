package com.cyworks.redux;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Desc: 主要用于记录State的变化，具有几个功能：
 * 1、记录本次修改的私有属性
 * 2、记录本次修改的公共属性
 * 3、检查组件是否可以修改某个值
 *
 * 本类的实例会在执行Reducer期间注入到具体的State对象中 {@link State},
 * 这样做的主要目的是防止用户在非Reducer中更新UI属性，导致框架无法捕获属性变更。
 *
 * @author randytu on 2021/1/10
 */
final class StateProxy {
    /**
     * 每次执行reducer并不一定只更新一个属性， 用一个表来记录哪些数据发生了变化，
     * 当store更新界面的时候会统一提取这个表中的数据，进行统一更新
     */
    private final List<ReactiveProp<Object>> mChangeQueue;

    StateProxy() {
        mChangeQueue = new ArrayList<>();
    }

    /**
     * 记录变化的属性，用于store通知时进行判断。

     * @param prop 属性值 {@link ReactiveProp}
     */
    void recordChangedProp(ReactiveProp prop) {
        mChangeQueue.add(prop);
    }

    /**
     * 获取组件私有数据变化的情况。
     * @return ChangedProp列表
     */
    List<ReactiveProp<Object>> getPrivatePropChanged() {
        if (mChangeQueue.isEmpty()) {
            return null;
        }

        List<ReactiveProp<Object>> list = new ArrayList<>();
        Iterator<ReactiveProp<Object>> it = mChangeQueue.iterator();

        while (it.hasNext()) {
            ReactiveProp<Object> changedProp = it.next();
            if (changedProp.isPrivateProp()) {
                list.add(changedProp);
                it.remove();
            }
        }

        return list.isEmpty() ? null : list;
    }

    /**
     * 获取reducer执行完成后数据变化的情况。
     * @return ChangedProp列表
     */
    List<ReactiveProp<Object>> getPublicPropChanged() {
        if (mChangeQueue.isEmpty()) {
            return null;
        }

        List<ReactiveProp<Object>> list = new ArrayList<>();

        for (ReactiveProp<Object> changedProp : mChangeQueue) {
            if (!changedProp.isPrivateProp()) {
                list.add(changedProp);
            }
        }

        mChangeQueue.clear();

        return list.isEmpty() ? null : list;
    }

    /**
     * 如果状态内存在公共状态，且公共状态发生变化，则认为当前属性属性有变化，需要通知关注的组件。
     * @return 是否发生变化
     */
    boolean isChanged() {
        if (mChangeQueue.isEmpty()) {
            return false;
        }

        for (ReactiveProp<Object> prop : mChangeQueue) {
            if (!prop.isPrivateProp()) {
                return true;
            }
        }

        return false;
    }
}
