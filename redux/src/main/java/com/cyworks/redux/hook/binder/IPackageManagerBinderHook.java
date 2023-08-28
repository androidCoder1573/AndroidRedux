package com.cyworks.redux.hook.binder;

import android.content.Context;

import com.tencent.fortuneplat.hook.BaseHookHandle;

public class IPackageManagerBinderHook extends BinderHook {
    public IPackageManagerBinderHook(Context hostContext) {
        super(hostContext);
    }

    @Override
    protected BaseHookHandle createHookHandle() {
        return null;
    }

    @Override
    Object getOldObj() throws Exception {
        return null;
    }

    @Override
    public String getServiceName() {
        return null;
    }
}
