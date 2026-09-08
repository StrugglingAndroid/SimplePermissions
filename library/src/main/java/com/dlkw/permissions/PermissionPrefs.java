package com.dlkw.permissions;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Tracks permissions that have been requested at least once,
 * used to distinguish first-time denial from "Don't ask again".
 */
final class PermissionPrefs {

    private static final String PREF_NAME = "sp_permission_requested";

    private PermissionPrefs() {
    }

    static void markRequested(Context context, String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return;
        }
        SharedPreferences.Editor editor = prefs(context).edit();
        for (String permission : permissions) {
            if (permission != null) {
                editor.putBoolean(permission, true);
            }
        }
        editor.apply();
    }

    static boolean wasRequested(Context context, String permission) {
        return prefs(context).getBoolean(permission, false);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
