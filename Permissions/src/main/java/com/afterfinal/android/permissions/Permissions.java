package com.afterfinal.android.permissions;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import com.afterfinal.android.permissions.api.R;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("all")
public class Permissions {
    private static Context sContext;

    static final String KEY_CALLBACK_ID = "KEY_CALLBACK_ID";
    static final String KEY_EXEC_WHEN_REJECTED = "KEY_EXEC_WHEN_REJECTED";
    static final String KEY_PERMISSIONS = "KEY_PERMISSIONS";
    static final String KEY_TIP_MODE = "KEY_SHOW_TIPS_WHEN_REJECTED";

    private static final Map<String, String[]> permissionNamesMap = new HashMap<>();

    static final Map<String, Callback> callbackMap = new HashMap<>();

    public static void init(Context context) {
        sContext = context;
        String[] permissions = context.getResources().getStringArray(R.array.com_afterfinal_permissions);
        String[] permissionNames = context.getResources().getStringArray(R.array.com_afterfinal_permission_names);
        String[] permissionDescriptions = context.getResources().getStringArray(R.array.com_afterfinal_permission_descriptions);
        for (int i = 0; i < permissions.length; i++) {
            if (i < permissionNames.length && i < permissionDescriptions.length) {
                permissionNamesMap.put(permissions[i], new String[]{permissionNames[i], permissionDescriptions[i]});
            }
        }
    }

    public static Context getContext() {
        if (null == sContext) {
            sContext = reflectApplication();
        }
        return sContext;
    }

    public static String getPermissionName(String permission) {
        String[] v = permissionNamesMap.get(permission);
        return null == v ? permission : v[0];
    }

    public static String getPermissionDescription(String permission) {
        String[] v = permissionNamesMap.get(permission);
        return null == v ? permission : v[1];
    }

    public static void request(Callback callback, String... permissions) {
        request(sContext, callback, permissions);
    }

    public static void request(Context context, Callback callback, String... permissions) {
        request(context, TipMode.Dialog, callback, permissions);
    }

    public static void request(Context context, TipMode tipMode, Callback callback, String... permissions) {
        request(context, false, tipMode, callback, permissions);
    }

    public static void request(Context context, boolean execWhenRejected, TipMode tipMode, Callback callback, String... permissions) {
        boolean granted = true;
        for (String permission : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(context, permission)) {
                granted = false;
                break;
            }
        }
        if (granted) {
            callback.onResult(Arrays.asList(permissions), new ArrayList<String>());
            return;
        }
        String callbackId = genId(callback);
        callbackMap.put(callbackId, callback);
        Intent intent = new Intent(context, PermissionActivity.class);
        intent.putExtra(KEY_CALLBACK_ID, callbackId);
        intent.putExtra(KEY_EXEC_WHEN_REJECTED, execWhenRejected);
        intent.putExtra(KEY_PERMISSIONS, permissions);
        intent.putExtra(KEY_TIP_MODE, tipMode);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    private static String genId(Callback callback) {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis() + "-" + callback.hashCode();
    }

    public static String getPermissionNames(List<String> permissions, String concat) {
        if (null == permissions || 0 == permissions.size()) {
            return "";
        }
        StringBuilder result = new StringBuilder(getPermissionName(permissions.get(0)));
        for (int i = 1; i < permissions.size(); i++) {
            result.append(concat).append(getPermissionName(permissions.get(i)));
        }
        return result.toString();
    }

    public interface Callback {

        void onResult(List<String> granted, List<String> rejected);
    }

    /**
     * 获取当前应用的Application
     * 先使用ActivityThread里获取Application的方法，如果没有获取到，
     * 再使用AppGlobals里面的获取Application的方法
     *
     * @return
     */
    @SuppressWarnings("all")
    private static Application reflectApplication() {
        Application application = null;
        try {
            Class atClass = Class.forName("android.app.ActivityThread");
            Method currentApplicationMethod = atClass.getDeclaredMethod("currentApplication");
            currentApplicationMethod.setAccessible(true);
            application = (Application) currentApplicationMethod.invoke(null);
        } catch (Exception e) {

        }

        if (application != null) {
            return application;
        }

        try {
            Class atClass = Class.forName("android.app.AppGlobals");
            Method currentApplicationMethod = atClass.getDeclaredMethod("getInitialApplication");
            currentApplicationMethod.setAccessible(true);
            application = (Application) currentApplicationMethod.invoke(null);
        } catch (Exception e) {
        }

        return application;
    }

}