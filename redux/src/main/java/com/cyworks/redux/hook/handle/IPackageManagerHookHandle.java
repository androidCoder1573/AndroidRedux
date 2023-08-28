/*
**        DroidPlugin Project
**
** Copyright(c) 2015 Andy Zhang <zhangyong232@gmail.com>
**
** This file is part of DroidPlugin.
**
** DroidPlugin is free software: you can redistribute it and/or
** modify it under the terms of the GNU Lesser General Public
** License as published by the Free Software Foundation, either
** version 3 of the License, or (at your option) any later version.
**
** DroidPlugin is distributed in the hope that it will be useful,
** but WITHOUT ANY WARRANTY; without even the implied warranty of
** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
** Lesser General Public License for more details.
**
** You should have received a copy of the GNU Lesser General Public
** License along with DroidPlugin.  If not, see <http://www.gnu.org/licenses/lgpl.txt>
**
**/

package com.cyworks.redux.hook.handle;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import com.tencent.fortuneplat.hook.BaseHookHandle;
import com.tencent.fortuneplat.hook.HookedMethodHandler;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Andy Zhang(zhangyong232@gmail.com) on 2015/2/28.
 */
public class IPackageManagerHookHandle extends BaseHookHandle {

    public IPackageManagerHookHandle(Context hostContext) {
        super(hostContext);
    }

    @Override
    protected void init() {
        PackageManager packageManager;
        sHookedMethodHandlers.put("getPackageInfo", new getPackageInfo(mHostContext));
        sHookedMethodHandlers.put("queryIntentActivities", new queryIntentActivities(mHostContext));
        sHookedMethodHandlers.put("getLaunchIntentForPackage", new getLaunchIntentForPackage(mHostContext));
        sHookedMethodHandlers.put("getInstalledPackages", new getInstalledPackages(mHostContext));
        sHookedMethodHandlers.put("getInstalledApplications", new getInstalledApplications(mHostContext));
        sHookedMethodHandlers.put("getPersistentApplications", new getPersistentApplications(mHostContext));
    }

    private class getInstalledPackages extends HookedMethodHandler {
        public getInstalledPackages(Context context) {
            super(context);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            Exception e = new Exception("aaaaaa");
            Log.e("aaaaaa", "call getInstalledPackages, " + Arrays.toString(e.getStackTrace()));
            return super.beforeInvoke(receiver, method, args);
        }

        @Override
        protected void afterInvoke(Object receiver, Method method, Object[] args, Object invokeResult) throws Throwable {

            super.afterInvoke(receiver, method, args, invokeResult);
        }
    }

    private class getInstalledApplications extends HookedMethodHandler {
        public getInstalledApplications(Context context) {
            super(context);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            Exception e = new Exception("aaaaaa");
            Log.e("aaaaaa", "call getInstalledApplications, " + Arrays.toString(e.getStackTrace()));
            return super.beforeInvoke(receiver, method, args);
        }
    }

    private class getPersistentApplications extends HookedMethodHandler {
        public getPersistentApplications(Context context) {
            super(context);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            Log.e("aaaaaa", "call getPersistentApplications");
            return super.beforeInvoke(receiver, method, args);
        }
    }

    private class getPackageInfo extends HookedMethodHandler {
        public getPackageInfo(Context context) {
            super(context);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            Exception e = new Exception("aaaaaa");
            Log.e("aaaaaa", "call getPackageInfo, args: " + Arrays.toString(args));
            Log.e("aaaaaa", "call getPackageInfo, " + Arrays.toString(e.getStackTrace()));
            return super.beforeInvoke(receiver, method, args);
        }
    }

    private class queryIntentActivities extends HookedMethodHandler {
        public queryIntentActivities(Context context) {
            super(context);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            Exception e = new Exception("aaaaaa");
            Log.e("aaaaaa", "call queryIntentActivities, " + Arrays.toString(e.getStackTrace()));
            return super.beforeInvoke(receiver, method, args);
        }
    }

    private class getLaunchIntentForPackage extends HookedMethodHandler {

        public getLaunchIntentForPackage(Context hostContext) {
            super(hostContext);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            Exception e = new Exception("aaaaaa");
            Log.e("aaaaaa", "call getLaunchIntentForPackage, " + Arrays.toString(e.getStackTrace()));
            return super.beforeInvoke(receiver, method, args);
        }
    }
}
