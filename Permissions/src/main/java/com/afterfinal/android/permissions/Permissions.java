package com.afterfinal.android.permissions;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import com.afterfinal.android.permissions.api.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Permissions {
    private static Application sApplication;

    static final String KEY_CALLBACK_ID = "KEY_CALLBACK_ID";
    static final String KEY_PERMISSIONS = "KEY_PERMISSIONS";
    static final String KEY_EXEC_WHEN_REJECTED = "KEY_EXEC_WHEN_REJECTED";
    static final String KEY_TIP_MODE = "KEY_SHOW_TIPS_WHEN_REJECTED";

    private static final Map<String, String[]> permissionNamesMap = new HashMap<>();

    static final Map<String, Callback> callbackMap = new HashMap<>();

    public static void init(Application app) {
        sApplication = app;
        String[] permissions = app.getResources().getStringArray(R.array.com_afterfinal_permissions);
        String[] permissionNames = app.getResources().getStringArray(R.array.com_afterfinal_permission_names);
        String[] permissionDescriptions = app.getResources().getStringArray(R.array.com_afterfinal_permission_descriptions);
        for (int i = 0; i < permissions.length; i++) {
            if (i < permissionNames.length && i < permissionDescriptions.length) {
                permissionNamesMap.put(permissions[i], new String[]{permissionNames[i], permissionDescriptions[i]});
            }
        }
    }

    public static Application getApplication() {
        return sApplication;
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
        request(sApplication, callback, permissions);
    }

    public static void request(Context context, Callback callback, String... permissions) {
        request(context, TipMode.Dialog, callback, permissions);
    }

    public static void request(Context context, TipMode tipMode, Callback callback, String... permissions) {
        request(context, true, tipMode, callback, permissions);
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
        intent.putExtra(KEY_PERMISSIONS, permissions);
        intent.putExtra(KEY_EXEC_WHEN_REJECTED, execWhenRejected);
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


}