package com.afterfinal.android.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class SystemAlertPermission {
    static final String KEY_CALLBACK_ID = "KEY_CALLBACK_ID";
    static final Map<String, Callback> callbackMap = new HashMap<>();

    public static void request(Context context, Callback callback) {
        String callbackId = genId(callback);
        callbackMap.put(callbackId, callback);
        Intent intent = new Intent(context, SystemAlertPermissionActivity.class);
        intent.putExtra(KEY_CALLBACK_ID, callbackId);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    private static String genId(Callback callback) {
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis() + "-" + callback.hashCode();
    }

    static void applyPermission(Activity activity, SystemAlertPermission.Callback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getApplication().getPackageName()));
                activity.startActivityForResult(intent, SystemAlertPermissionActivity.REQUEST_FLOAT_WINDOW_PERMISSION);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(activity.getApplicationContext(), "无法打开悬浮窗权限设置，请到系统设置打开悬浮窗权限", Toast.LENGTH_SHORT).show();
                callback.onResult(false);
                activity.finish();
            }
        } else {
            Toast.makeText(activity.getApplicationContext(), "无法打开悬浮窗权限设置，请到系统设置打开悬浮窗权限", Toast.LENGTH_SHORT).show();
            callback.onResult(false);
            activity.finish();
        }
    }

    static boolean hasPermission(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return Settings.canDrawOverlays(context);
            } else {
                WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                } else {
                    params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                }
                TextView textView = new TextView(context.getApplicationContext());
                manager.addView(textView, params);
                manager.removeViewImmediate(textView);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public interface Callback {
        void onResult(boolean granted);
    }
}
