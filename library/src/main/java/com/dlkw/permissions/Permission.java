package com.dlkw.permissions;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Callback-style runtime / special permission helper.
 */
public class Permission {

    public interface Callback<T> {
        void onResult(List<String> granted, List<String> rejected, T extras);

        /**
         * Extended result. Default merges {@code permanentlyDenied} into {@code rejected}
         * so existing three-arg implementations keep working.
         */
        default void onResult(List<String> granted, List<String> rejected,
                              List<String> permanentlyDenied, T extras) {
            List<String> allRejected = new ArrayList<>(rejected);
            if (permanentlyDenied != null && !permanentlyDenied.isEmpty()) {
                allRejected.addAll(permanentlyDenied);
            }
            onResult(granted, allRejected, extras);
        }

        default void onShowConfirmDialog(Activity context, String message, OnConfirmListener listener) {
            if (context == null || context.isFinishing() || context.isDestroyed()) {
                if (listener != null) {
                    listener.onCancel();
                }
                return;
            }
            try {
                new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setTitle("权限申请提示")
                        .setMessage(message)
                        .setPositiveButton("现在去开启", (dialog, which) -> {
                            if (listener != null) {
                                listener.onConfirm();
                            }
                            dialog.dismiss();
                        }).setNegativeButton("暂不开启", (dialog, which) -> {
                            if (listener != null) {
                                listener.onCancel();
                            }
                            dialog.dismiss();
                        })
                        .show();
            } catch (Exception e) {
                if (listener != null) {
                    listener.onCancel();
                }
            }
        }
    }

    static final String KEY_CALLBACK_ID = "KEY_CALLBACK_ID";
    static final String KEY_TITLE = "KEY_TITLE";
    static final String KEY_DESCRIPTION = "KEY_DESCRIPTION";
    static final Map<String, Callback<?>> callbackMap = new ConcurrentHashMap<>();
    static final Map<String, String[]> permissionMap = new ConcurrentHashMap<>();
    static final Map<String, Object> extraMap = new ConcurrentHashMap<>();

    private static volatile boolean autoGuideOnPermanentDenied = true;

    /**
     * When true (default), after "Don't ask again" the library shows a confirm dialog
     * and guides the user to the app permission settings page.
     */
    public static void setAutoGuideOnPermanentDenied(boolean enable) {
        autoGuideOnPermanentDenied = enable;
    }

    public static boolean isAutoGuideOnPermanentDenied() {
        return autoGuideOnPermanentDenied;
    }

    /** Opens the app permission settings page (OEM-aware). */
    public static void openAppPermissionSettings(Context context) {
        if (context == null) {
            return;
        }
        PermissionSettings.openAppPermissionSettings(context);
    }

    /**
     * Returns true if the permission was denied and the system will no longer show the dialog
     * ("Don't ask again"), based on rationale + prior request history.
     */
    public static boolean isPermanentlyDenied(Activity activity, String permission) {
        if (activity == null || permission == null) {
            return false;
        }
        if (hasPermission(activity, permission)) {
            return false;
        }
        if (isSpecialSettingsPermission(permission)) {
            return false;
        }
        return PermissionPrefs.wasRequested(activity, permission)
                && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }

    public static <T> void request(Context context, String description, Callback<T> callback, String... permissions) {
        request(context, description, null, callback, permissions);
    }

    /**
     * @param context     context
     * @param description permission usage description shown above the system dialog
     * @param extras      opaque value returned in the callback
     * @param callback    result callback
     * @param permissions permissions to request
     */
    public static <T> void request(Context context, String description, T extras, Callback<T> callback, String... permissions) {
        if (callback == null) {
            return;
        }
        if (context == null) {
            callback.onResult(Collections.emptyList(),
                    permissions == null ? Collections.emptyList() : java.util.Arrays.asList(permissions),
                    Collections.emptyList(), extras);
            return;
        }
        if (permissions == null || permissions.length == 0) {
            callback.onResult(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), extras);
            return;
        }
        try {
            final Set<String> tempPermissionSet = new LinkedHashSet<>();
            Collections.addAll(tempPermissionSet, permissions);
            adaptPermissions(context, tempPermissionSet);
            String[] adapted = tempPermissionSet.toArray(new String[0]);

            if (adapted.length == 0 || hasAllPermissions(context, adapted)) {
                List<String> granted = new ArrayList<>();
                List<String> rejected = new ArrayList<>();
                Collections.addAll(granted, adapted);
                callback.onResult(granted, rejected, Collections.emptyList(), extras);
                return;
            }

            String id = System.currentTimeMillis() + "-" + UUID.randomUUID().toString();
            permissionMap.put(id, adapted);
            callbackMap.put(id, callback);
            // ConcurrentHashMap disallows null values — omit entry when extras is null.
            if (extras != null) {
                extraMap.put(id, extras);
            }

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
            callback.onResult(Collections.emptyList(), rejected, Collections.emptyList(), extras);
        }
    }

    static boolean hasFloatingWindowPermission(Context context) {
        return FloatWindowUtil.checkPermission(context);
    }

    static boolean isSpecialSettingsPermission(String permission) {
        return Manifest.permission.MANAGE_EXTERNAL_STORAGE.equals(permission)
                || Manifest.permission.SYSTEM_ALERT_WINDOW.equals(permission)
                || Manifest.permission.REQUEST_INSTALL_PACKAGES.equals(permission);
    }

    static boolean hasPermission(Context context, String permission) {
        if (context == null || permission == null) {
            return false;
        }
        if (Manifest.permission.MANAGE_EXTERNAL_STORAGE.equals(permission)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return Environment.isExternalStorageManager();
            }
            return hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (Manifest.permission.SYSTEM_ALERT_WINDOW.equals(permission)) {
            return hasFloatingWindowPermission(context);
        }
        if (Manifest.permission.REQUEST_INSTALL_PACKAGES.equals(permission)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return context.getPackageManager().canRequestPackageInstalls();
            }
            return true;
        }
        if (Manifest.permission.POST_NOTIFICATIONS.equals(permission)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return true;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (Manifest.permission.READ_MEDIA_IMAGES.equals(permission)
                    || Manifest.permission.READ_MEDIA_VIDEO.equals(permission)) {
                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
                // Partial photo/video access counts as granted for media read.
                return ContextCompat.checkSelfPermission(context,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED;
            }
        }
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("NewApi")
    public static boolean hasAllPermissions(Context context, String... permissions) {
        if (context == null || permissions == null || permissions.length == 0) {
            return true;
        }
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

    /**
     * Normalizes the permission set for the current device / targetSdk.
     * Does not force {@link Manifest.permission#MANAGE_EXTERNAL_STORAGE} onto ordinary storage requests.
     */
    static void adaptPermissions(Context context, Collection<String> permissionSet) {
        if (context == null || permissionSet == null || permissionSet.isEmpty()) {
            return;
        }
        int sdk = Build.VERSION.SDK_INT;
        int targetSdkVersion = context.getApplicationInfo().targetSdkVersion;

        // All-files access: keep only on R+ with target R+; otherwise degrade to legacy write.
        if (permissionSet.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
            if (sdk >= Build.VERSION_CODES.R && targetSdkVersion >= Build.VERSION_CODES.R) {
                // keep as-is
            } else {
                permissionSet.remove(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                permissionSet.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                permissionSet.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        // Media permissions only on API 33+; map legacy storage when appropriate.
        if (sdk >= Build.VERSION_CODES.TIRAMISU) {
            boolean wantsLegacyRead = permissionSet.contains(Manifest.permission.READ_EXTERNAL_STORAGE)
                    || permissionSet.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            boolean wantsManage = permissionSet.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
            if (wantsLegacyRead && !wantsManage) {
                permissionSet.add(Manifest.permission.READ_MEDIA_IMAGES);
                permissionSet.add(Manifest.permission.READ_MEDIA_VIDEO);
                permissionSet.add(Manifest.permission.READ_MEDIA_AUDIO);
                if (targetSdkVersion >= Build.VERSION_CODES.TIRAMISU) {
                    permissionSet.remove(Manifest.permission.READ_EXTERNAL_STORAGE);
                    permissionSet.remove(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
        } else {
            permissionSet.remove(Manifest.permission.READ_MEDIA_IMAGES);
            permissionSet.remove(Manifest.permission.READ_MEDIA_VIDEO);
            permissionSet.remove(Manifest.permission.READ_MEDIA_AUDIO);
            if (sdk < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                permissionSet.remove(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
            }
        }

        if (sdk < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissionSet.remove(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
        }

        // Notifications: only runtime on API 33+.
        if (sdk < Build.VERSION_CODES.TIRAMISU) {
            permissionSet.remove(Manifest.permission.POST_NOTIFICATIONS);
        }

        // Install unknown apps: settings path from O; below treat as granted / drop.
        if (sdk < Build.VERSION_CODES.O) {
            permissionSet.remove(Manifest.permission.REQUEST_INSTALL_PACKAGES);
        }

        // Bluetooth adaptation.
        if (permissionSet.contains(Manifest.permission.BLUETOOTH)
                || permissionSet.contains(Manifest.permission.BLUETOOTH_ADMIN)
                || permissionSet.contains(Manifest.permission.BLUETOOTH_SCAN)
                || permissionSet.contains(Manifest.permission.BLUETOOTH_CONNECT)
                || permissionSet.contains(Manifest.permission.BLUETOOTH_ADVERTISE)) {
            if (sdk >= Build.VERSION_CODES.S && targetSdkVersion >= Build.VERSION_CODES.S) {
                if (permissionSet.contains(Manifest.permission.BLUETOOTH)
                        || permissionSet.contains(Manifest.permission.BLUETOOTH_ADMIN)) {
                    permissionSet.add(Manifest.permission.BLUETOOTH_SCAN);
                    permissionSet.add(Manifest.permission.BLUETOOTH_CONNECT);
                    permissionSet.add(Manifest.permission.BLUETOOTH_ADVERTISE);
                }
            } else {
                permissionSet.remove(Manifest.permission.BLUETOOTH_SCAN);
                permissionSet.remove(Manifest.permission.BLUETOOTH_CONNECT);
                permissionSet.remove(Manifest.permission.BLUETOOTH_ADVERTISE);
                if (permissionSet.contains(Manifest.permission.BLUETOOTH)
                        || permissionSet.contains(Manifest.permission.BLUETOOTH_ADMIN)) {
                    permissionSet.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                    permissionSet.add(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }
        }

        // Background location is invalid below Q.
        if (sdk < Build.VERSION_CODES.Q) {
            permissionSet.remove(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        // Drop unknown empty entries.
        Iterator<String> it = permissionSet.iterator();
        while (it.hasNext()) {
            if (it.next() == null) {
                it.remove();
            }
        }
    }
}
