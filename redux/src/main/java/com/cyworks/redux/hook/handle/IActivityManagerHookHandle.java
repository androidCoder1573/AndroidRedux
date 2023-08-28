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
import android.util.Log;

import com.tencent.fortuneplat.hook.BaseHookHandle;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Created by Andy Zhang(zhangyong232@gmail.com) on 2015/2/28.
 */
public class IActivityManagerHookHandle extends BaseHookHandle {

    public IActivityManagerHookHandle(Context hostContext) {
        super(hostContext);
    }

    @Override
    protected void init() {
        sHookedMethodHandlers.put("getCallingPackage", new getCallingPackage(mHostContext));
        sHookedMethodHandlers.put("getCallingActivity", new getCallingActivity(mHostContext));
        sHookedMethodHandlers.put("getProcessesInErrorState", new getProcessesInErrorState(mHostContext));
        sHookedMethodHandlers.put("killApplicationProcess", new killApplicationProcess(mHostContext));
        sHookedMethodHandlers.put("getPackageForToken", new getPackageForToken(mHostContext));
        sHookedMethodHandlers.put("getIntentSender", new getIntentSender(mHostContext));
        // sHookedMethodHandlers.put("getRunningAppProcesses", new getRunningAppProcesses(mHostContext));
        // sHookedMethodHandlers.put("getRunningExternalApplications", new getRunningExternalApplications(mHostContext));
    }

    private static class getCallingPackage extends ReplaceCallingPackageHookedMethodHandler {

        public getCallingPackage(Context hostContext) {
            super(hostContext);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            Exception e = new Exception("aaaaaa");
            Log.e("aaaaaa", "call getCallingPackage, " + Arrays.toString(e.getStackTrace()));
            return super.beforeInvoke(receiver, method, args);
        }
    }

    private static class getCallingActivity extends ReplaceCallingPackageHookedMethodHandler {

        public getCallingActivity(Context hostContext) {
            super(hostContext);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            Exception e = new Exception("aaaaaa");
            Log.e("aaaaaa", "call getCallingActivity, " + Arrays.toString(e.getStackTrace()));
            return super.beforeInvoke(receiver, method, args);
        }
    }

    private static class getProcessesInErrorState extends ReplaceCallingPackageHookedMethodHandler {

        public getProcessesInErrorState(Context hostContext) {
            super(hostContext);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            Exception e = new Exception("aaaaaa");
            Log.e("aaaaaa", "call getProcessesInErrorState, " + Arrays.toString(e.getStackTrace()));
            return super.beforeInvoke(receiver, method, args);
        }
    }





    private static class killApplicationProcess extends ReplaceCallingPackageHookedMethodHandler {

        public killApplicationProcess(Context hostContext) {
            super(hostContext);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            Exception e = new Exception("aaaaaa");
            Log.e("aaaaaa", "call killApplicationProcess, " + Arrays.toString(e.getStackTrace()));
            return super.beforeInvoke(receiver, method, args);
        }
    }


    private static class getActivityClassForToken extends ReplaceCallingPackageHookedMethodHandler {

        public getActivityClassForToken(Context hostContext) {
            super(hostContext);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            Exception e = new Exception("aaaaaa");
            Log.e("aaaaaa", "call getActivityClassForToken, " + Arrays.toString(e.getStackTrace()));
            return super.beforeInvoke(receiver, method, args);
        }
    }

    private static class getPackageForToken extends ReplaceCallingPackageHookedMethodHandler {

        public getPackageForToken(Context hostContext) {
            super(hostContext);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            Exception e = new Exception("aaaaaa");
            Log.e("aaaaaa", "call getPackageForToken, " + Arrays.toString(e.getStackTrace()));
            return super.beforeInvoke(receiver, method, args);
        }
    }

    public static class getIntentSender extends ReplaceCallingPackageHookedMethodHandler {

        public getIntentSender(Context hostContext) {
            super(hostContext);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            Exception e = new Exception("aaaaaa");
            Log.e("aaaaaa", "call getIntentSender, " + Arrays.toString(e.getStackTrace()));
            return super.beforeInvoke(receiver, method, args);
        }
    }

    private static class getRunningAppProcesses extends ReplaceCallingPackageHookedMethodHandler {

        public getRunningAppProcesses(Context hostContext) {
            super(hostContext);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            Exception e = new Exception("aaaaaa");
            Log.e("aaaaaa", "call getRunningAppProcesses, " + Arrays.toString(e.getStackTrace()));
            return super.beforeInvoke(receiver, method, args);
        }
    }

    private static class getRunningExternalApplications extends ReplaceCallingPackageHookedMethodHandler {

        public getRunningExternalApplications(Context hostContext) {
            super(hostContext);
        }

        @Override
        protected boolean beforeInvoke(Object receiver, Method method, Object[] args) throws Throwable {
            Exception e = new Exception("aaaaaa");
            Log.e("aaaaaa", "call getRunningExternalApplications, " + Arrays.toString(e.getStackTrace()));
            return super.beforeInvoke(receiver, method, args);
        }
    }
}
