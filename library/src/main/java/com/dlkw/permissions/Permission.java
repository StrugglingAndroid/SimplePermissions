package com.dlkw.permissions;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * callBack 方式获取权限
 */
public class Permission {

    public interface Callback<T> {
        void onResult(List<String> granted, List<String> rejected, T extras);

        default void onShowConfirmDialog(Activity context, String message, OnConfirmListener listener) {
            new AlertDialog.Builder(context)
                    .setCancelable(false)
                    .setTitle("权限申请提示")
                    .setMessage(message)
                    .setPositiveButton("现在去开启", (dialog, which) -> {
                        listener.onConfirm();
                        dialog.dismiss();
                    }).setNegativeButton("暂不开启", (dialog, which) -> {
                        listener.onCancel();
                        dialog.dismiss();
                    })
                    .show();
        }
    }

    static final String KEY_CALLBACK_ID = "KEY_CALLBACK_ID";
    static final String KEY_TITLE = "KEY_TITLE";
    static final String KEY_DESCRIPTION = "KEY_DESCRIPTION";
    static final Map<String, Callback<?>> callbackMap = new HashMap<>();
    static final Map<String, String[]> permissionMap = new HashMap<>();
    static final Map<String, Object> extraMap = new HashMap<>();

    public static <T> void request(Context context, String description, Callback<T> callback, String... permissions) {
        request(context, description, null, callback, permissions);
    }

    /**
     * "请同意相关权限后才能正常使用该功能"
     *
     * @param context     上下文
     * @param description 权限使用说明
     * @param extras      回调参数
     * @param callback    回调
     * @param permissions 权限
     */
    public static <T> void request(Context context, String description, T extras, Callback<T> callback, String... permissions) {
        try {
            final Set<String> tempPermissionSet = new LinkedHashSet<>();
            Collections.addAll(tempPermissionSet, permissions);
            adaptPermissions(context, tempPermissionSet);
            permissions = tempPermissionSet.toArray(new String[0]);

            if (hasAllPermissions(context, permissions)) {
                List<String> granted = new ArrayList<>();
                List<String> rejected = new ArrayList<>();
                Collections.addAll(granted, permissions);
                callback.onResult(granted, rejected, extras);
                return;
            }

            String id = System.currentTimeMillis() + "-" + UUID.randomUUID().toString();
            permissionMap.put(id, permissions);
            callbackMap.put(id, callback);
            extraMap.put(id, extras);

            Intent intent = new Intent(context, PermissionActivity.class);
            intent.putExtra(KEY_CALLBACK_ID, id);
            intent.putExtra(KEY_TITLE, "权限使用说明");
            intent.putExtra(KEY_DESCRIPTION, description);
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (Exception e) {
            List<String> rejected = new ArrayList<>();
            Collections.addAll(rejected, permissions);
            callback.onResult(Collections.emptyList(), rejected, extras);
        }

    }

    static boolean hasFloatingWindowPermission(Context context) {
        return FloatWindowUtil.checkPermission(context);
    }

    static boolean hasPermission(Context context, String permission) {
        if (Manifest.permission.MANAGE_EXTERNAL_STORAGE.equals(permission)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return Environment.isExternalStorageManager();
            } else {
                return hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        } else if (Manifest.permission.SYSTEM_ALERT_WINDOW.equals(permission)) {
            return hasFloatingWindowPermission(context);
        } else {
            return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
        }
    }

    @SuppressLint("NewApi")
    public static boolean hasAllPermissions(Context context, String... permissions) {
        Set<String> tempPermissionSet = new LinkedHashSet<>();
        Collections.addAll(tempPermissionSet, permissions);
        adaptPermissions(context, tempPermissionSet);
        for (String permission : tempPermissionSet) {
            if (!hasPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }

    static void adaptPermissions(Context context, Collection<String> permissionSet) {
        int targetSdkVersion = context.getApplicationInfo().targetSdkVersion;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && targetSdkVersion >= Build.VERSION_CODES.R) {
            if (permissionSet.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
                permissionSet.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                permissionSet.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (permissionSet.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE) || permissionSet.contains(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                permissionSet.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
            }
        } else {
            permissionSet.remove(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        }
        if (permissionSet.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE) || permissionSet.contains(Manifest.permission.READ_EXTERNAL_STORAGE)
                || permissionSet.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
            permissionSet.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissionSet.add(Manifest.permission.READ_MEDIA_AUDIO);
            permissionSet.add(Manifest.permission.READ_MEDIA_VIDEO);
        }
        if (permissionSet.contains(Manifest.permission.BLUETOOTH) || permissionSet.contains(Manifest.permission.BLUETOOTH_ADMIN)) {
            permissionSet.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            permissionSet.add(Manifest.permission.ACCESS_FINE_LOCATION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && targetSdkVersion >= Build.VERSION_CODES.S) {
                permissionSet.add(Manifest.permission.BLUETOOTH_SCAN);
                permissionSet.add(Manifest.permission.BLUETOOTH_CONNECT);
                permissionSet.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            } else {
                permissionSet.remove(Manifest.permission.BLUETOOTH_SCAN);
                permissionSet.remove(Manifest.permission.BLUETOOTH_CONNECT);
                permissionSet.remove(Manifest.permission.BLUETOOTH_ADVERTISE);
            }
        }
    }
}
