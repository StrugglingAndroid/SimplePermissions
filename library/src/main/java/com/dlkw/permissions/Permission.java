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
import android.view.View;

import androidx.core.app.ActivityCompat;

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

        /**
         * Provide a fully custom explain View (any layout / ids).
         * Return {@code null} to use global {@link PermissionExplainUi} or the library default.
         */
        default View onCreatePermissionExplainView(Activity activity, String title, String description) {
            return null;
        }

        /** Called when the permission Activity is finishing, if you created a custom explain View. */
        default void onDestroyPermissionExplainView(Activity activity) {
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
    /** Original permissions as passed by the caller (before adaptation). */
    static final Map<String, String[]> originalPermissionMap = new ConcurrentHashMap<>();
    static final Map<String, Object> extraMap = new ConcurrentHashMap<>();

    private static volatile boolean autoGuideOnPermanentDenied = true;
    /**
     * When true (default), legacy {@code READ/WRITE_EXTERNAL_STORAGE} is upgraded to
     * {@link Manifest.permission#MANAGE_EXTERNAL_STORAGE} on Android 11+ (API 30+ / target 30+).
     * When false, they map to {@code READ_MEDIA_*} on API 33+ instead.
     */
    private static volatile boolean autoUpgradeStorageToManage = true;
    /** 0 = use library default {@code R.layout.sp_activity_permission}. */
    private static volatile int permissionExplainLayoutRes = 0;
    private static volatile PermissionExplainUi permissionExplainUi;

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

    /**
     * Controls whether legacy storage ({@code READ/WRITE_EXTERNAL_STORAGE}) auto-upgrades
     * to {@code MANAGE_EXTERNAL_STORAGE} on Android 11+.
     * <ul>
     *   <li>{@code true} (default): R+ → all-files settings page; below R → keep legacy.</li>
     *   <li>{@code false}: API 33+ → {@code READ_MEDIA_*}; older → legacy READ/WRITE.
     *       Prefer this for Play Store / least-privilege media apps.</li>
     * </ul>
     * Explicit {@code MANAGE_EXTERNAL_STORAGE} / {@code READ_MEDIA_*} in the request
     * are unaffected by this switch (except MANAGE still supersedes legacy in the same batch).
     */
    public static void setAutoUpgradeStorageToManage(boolean enable) {
        autoUpgradeStorageToManage = enable;
    }

    public static boolean isAutoUpgradeStorageToManage() {
        return autoUpgradeStorageToManage;
    }

    /**
     * App-wide custom explain UI. Highest priority after per-request
     * {@link Callback#onCreatePermissionExplainView}. Pass {@code null} to clear.
     * Custom UI does <b>not</b> need to use library view ids.
     */
    public static void setPermissionExplainUi(PermissionExplainUi explainUi) {
        permissionExplainUi = explainUi;
    }

    public static PermissionExplainUi getPermissionExplainUi() {
        return permissionExplainUi;
    }

    /**
     * Optional: inflate a layout that still uses the library's default id binding.
     * Prefer {@link #setPermissionExplainUi} or {@link Callback#onCreatePermissionExplainView}
     * if you want a fully free layout without id constraints.
     * Pass {@code 0} to restore the library default layout file.
     */
    public static void setPermissionExplainLayout(int layoutResId) {
        permissionExplainLayoutRes = layoutResId;
    }

    public static int getPermissionExplainLayout() {
        return permissionExplainLayoutRes;
    }

    /** Resolves the default-path layout to inflate; falls back to library default. */
    static int resolvePermissionExplainLayout() {
        int custom = permissionExplainLayoutRes;
        return custom != 0 ? custom : R.layout.sp_activity_permission;
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
     * <p>
     * Do not use this alone to skip {@code requestPermissions}: after the user changes the
     * grant in system Settings the heuristic can be stale — always ask the system again.
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
            // Keep caller-facing names for checks / callbacks; adapt a copy for the system.
            final String[] originals = permissions.clone();
            final Set<String> tempPermissionSet = new LinkedHashSet<>();
            Collections.addAll(tempPermissionSet, originals);
            adaptPermissions(context, tempPermissionSet);
            String[] adapted = tempPermissionSet.toArray(new String[0]);

            if (adapted.length == 0 || hasAllPermissions(context, originals)) {
                List<String> granted = new ArrayList<>();
                Collections.addAll(granted, originals);
                callback.onResult(granted, Collections.emptyList(), Collections.emptyList(), extras);
                return;
            }

            String id = System.currentTimeMillis() + "-" + UUID.randomUUID().toString();
            permissionMap.put(id, adapted);
            originalPermissionMap.put(id, originals);
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
        if (permission == null) {
            return false;
        }
        if (Manifest.permission.MANAGE_EXTERNAL_STORAGE.equals(permission)
                || Manifest.permission.SYSTEM_ALERT_WINDOW.equals(permission)
                || Manifest.permission.REQUEST_INSTALL_PACKAGES.equals(permission)) {
            return true;
        }
        // Below API 33 there is no runtime notification permission — guide to notification settings.
        if (Manifest.permission.POST_NOTIFICATIONS.equals(permission)
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        if (Manifest.permission.SCHEDULE_EXACT_ALARM.equals(permission)
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return true;
        }
        return false;
    }

    static boolean hasPermission(Context context, String permission) {
        if (context == null || permission == null) {
            return false;
        }
        // --- Special / settings permissions ---
        if (Manifest.permission.MANAGE_EXTERNAL_STORAGE.equals(permission)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return Environment.isExternalStorageManager();
            }
            return rawGranted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
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
            return hasNotificationPermission(context);
        }
        if (Manifest.permission.SCHEDULE_EXACT_ALARM.equals(permission)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return true;
            }
            try {
                android.app.AlarmManager am =
                        (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                return am == null || am.canScheduleExactAlarms();
            } catch (Exception e) {
                return false;
            }
        }

        // --- Storage family (caller may use any alias) ---
        if (isLegacyStoragePermission(permission) || isMediaStoragePermission(permission)) {
            return hasStorageCapability(context, permission);
        }

        if (Manifest.permission.ACCESS_BACKGROUND_LOCATION.equals(permission)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        || hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            return rawGranted(context, permission);
        }

        if (Manifest.permission.ACTIVITY_RECOGNITION.equals(permission)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return true;
            }
            return rawGranted(context, permission);
        }

        if (Manifest.permission.NEARBY_WIFI_DEVICES.equals(permission)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                // Pre-13 nearby Wi-Fi typically relied on location.
                return hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        || hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            return rawGranted(context, permission);
        }

        if (Manifest.permission.BLUETOOTH.equals(permission)
                || Manifest.permission.BLUETOOTH_ADMIN.equals(permission)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.S) {
                return rawGranted(context, Manifest.permission.BLUETOOTH_CONNECT)
                        && rawGranted(context, Manifest.permission.BLUETOOTH_SCAN);
            }
            return rawGranted(context, permission);
        }

        if (Manifest.permission.BLUETOOTH_SCAN.equals(permission)
                || Manifest.permission.BLUETOOTH_CONNECT.equals(permission)
                || Manifest.permission.BLUETOOTH_ADVERTISE.equals(permission)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                    || context.getApplicationInfo().targetSdkVersion < Build.VERSION_CODES.S) {
                return rawGranted(context, Manifest.permission.BLUETOOTH)
                        || rawGranted(context, Manifest.permission.ACCESS_FINE_LOCATION)
                        || rawGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            return rawGranted(context, permission);
        }

        if (Manifest.permission.READ_PHONE_NUMBERS.equals(permission)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return rawGranted(context, Manifest.permission.READ_PHONE_STATE);
            }
            return rawGranted(context, permission);
        }

        return rawGranted(context, permission);
    }

    static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return rawGranted(context, Manifest.permission.POST_NOTIFICATIONS);
        }
        try {
            return androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled();
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean rawGranted(Context context, String permission) {
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean isLegacyStoragePermission(String permission) {
        return Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission)
                || Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission);
    }

    private static boolean isMediaStoragePermission(String permission) {
        return Manifest.permission.READ_MEDIA_IMAGES.equals(permission)
                || Manifest.permission.READ_MEDIA_VIDEO.equals(permission)
                || Manifest.permission.READ_MEDIA_AUDIO.equals(permission)
                || Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED.equals(permission);
    }

    /**
     * Storage capability check with cross-version equivalence:
     * MANAGE covers legacy/media; on old APIs legacy covers media aliases; on API 33+
     * media covers legacy write/read requests.
     */
    private static boolean hasStorageCapability(Context context, String permission) {
        int sdk = Build.VERSION.SDK_INT;
        // All-files implies any storage-related capability.
        if (sdk >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            return true;
        }

        if (Manifest.permission.READ_MEDIA_IMAGES.equals(permission)
                || Manifest.permission.READ_MEDIA_VIDEO.equals(permission)) {
            if (sdk >= Build.VERSION_CODES.TIRAMISU) {
                if (rawGranted(context, permission)) {
                    return true;
                }
                return sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                        && rawGranted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
            }
            return rawGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    || rawGranted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (Manifest.permission.READ_MEDIA_AUDIO.equals(permission)) {
            if (sdk >= Build.VERSION_CODES.TIRAMISU) {
                return rawGranted(context, permission);
            }
            return rawGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    || rawGranted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED.equals(permission)) {
            if (sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                return rawGranted(context, permission)
                        || rawGranted(context, Manifest.permission.READ_MEDIA_IMAGES)
                        || rawGranted(context, Manifest.permission.READ_MEDIA_VIDEO);
            }
            return hasStorageCapability(context, Manifest.permission.READ_MEDIA_IMAGES);
        }
        if (isLegacyStoragePermission(permission)) {
            if (sdk >= Build.VERSION_CODES.TIRAMISU
                    && context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.TIRAMISU) {
                // Legacy storage request satisfied by full media grant (or manage, already checked).
                return rawGranted(context, Manifest.permission.READ_MEDIA_IMAGES)
                        && rawGranted(context, Manifest.permission.READ_MEDIA_VIDEO)
                        && rawGranted(context, Manifest.permission.READ_MEDIA_AUDIO);
            }
            return rawGranted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    || rawGranted(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        return rawGranted(context, permission);
    }

    /**
     * Returns true when every permission in {@code permissions} is granted,
     * using cross-version equivalence (callers may pass legacy or modern names).
     */
    @SuppressLint("NewApi")
    public static boolean hasAllPermissions(Context context, String... permissions) {
        if (context == null || permissions == null || permissions.length == 0) {
            return true;
        }
        for (String permission : permissions) {
            if (!hasPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Rewrites {@code permissionSet} to what must actually be requested on this device.
     * Callers may freely mix legacy storage, {@code READ_MEDIA_*}, {@code MANAGE_EXTERNAL_STORAGE},
     * bluetooth, notifications, etc. — this method performs the compatibility mapping.
     *
     * <p>Policy highlights:
     * <ul>
     *   <li>Explicit {@code MANAGE_EXTERNAL_STORAGE}: settings page on R+; legacy WRITE/READ below.</li>
     *   <li>Legacy READ/WRITE: when {@link #isAutoUpgradeStorageToManage()} is true (default),
     *       upgrade to MANAGE on R+; when false, map to {@code READ_MEDIA_*} on API 33+.</li>
     *   <li>Explicit {@code READ_MEDIA_*} alone: never upgraded to MANAGE; mapped to legacy below 33.</li>
     *   <li>{@code POST_NOTIFICATIONS}: runtime on 33+; below 33 keep as settings-guide permission.</li>
     * </ul>
     */
    static void adaptPermissions(Context context, Collection<String> permissionSet) {
        if (context == null || permissionSet == null || permissionSet.isEmpty()) {
            return;
        }
        int sdk = Build.VERSION.SDK_INT;
        int targetSdkVersion = context.getApplicationInfo().targetSdkVersion;
        boolean upgradeLegacyToManage = autoUpgradeStorageToManage;

        boolean wantManage = permissionSet.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        boolean wantLegacyRead = permissionSet.contains(Manifest.permission.READ_EXTERNAL_STORAGE);
        boolean wantLegacyWrite = permissionSet.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        boolean wantImages = permissionSet.contains(Manifest.permission.READ_MEDIA_IMAGES);
        boolean wantVideo = permissionSet.contains(Manifest.permission.READ_MEDIA_VIDEO);
        boolean wantAudio = permissionSet.contains(Manifest.permission.READ_MEDIA_AUDIO);
        boolean wantVisualSelected =
                permissionSet.contains(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
        boolean wantAnyMedia = wantImages || wantVideo || wantAudio || wantVisualSelected;
        boolean wantAnyLegacy = wantLegacyRead || wantLegacyWrite;

        // Strip storage family; re-emit the right set for this device.
        permissionSet.remove(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        permissionSet.remove(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissionSet.remove(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionSet.remove(Manifest.permission.READ_MEDIA_IMAGES);
        permissionSet.remove(Manifest.permission.READ_MEDIA_VIDEO);
        permissionSet.remove(Manifest.permission.READ_MEDIA_AUDIO);
        permissionSet.remove(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);

        boolean useManage = wantManage
                || (wantAnyLegacy && upgradeLegacyToManage
                && sdk >= Build.VERSION_CODES.R && targetSdkVersion >= Build.VERSION_CODES.R);

        if (useManage) {
            if (sdk >= Build.VERSION_CODES.R && targetSdkVersion >= Build.VERSION_CODES.R) {
                permissionSet.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                // MANAGE supersedes legacy/media in the same request batch.
            } else {
                permissionSet.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                permissionSet.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        } else if (wantAnyLegacy || wantAnyMedia) {
            if (sdk >= Build.VERSION_CODES.TIRAMISU
                    && targetSdkVersion >= Build.VERSION_CODES.TIRAMISU) {
                if (wantAnyLegacy) {
                    // Least-privilege path: broad legacy → all media types.
                    permissionSet.add(Manifest.permission.READ_MEDIA_IMAGES);
                    permissionSet.add(Manifest.permission.READ_MEDIA_VIDEO);
                    permissionSet.add(Manifest.permission.READ_MEDIA_AUDIO);
                } else {
                    if (wantImages || wantVisualSelected) {
                        permissionSet.add(Manifest.permission.READ_MEDIA_IMAGES);
                    }
                    if (wantVideo || wantVisualSelected) {
                        permissionSet.add(Manifest.permission.READ_MEDIA_VIDEO);
                    }
                    if (wantAudio) {
                        permissionSet.add(Manifest.permission.READ_MEDIA_AUDIO);
                    }
                }
                if (wantVisualSelected && sdk >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    permissionSet.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
                }
            } else {
                // API < 33 (or target < 33): media aliases → legacy storage.
                permissionSet.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                permissionSet.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        // Notifications: keep POST_NOTIFICATIONS on all APIs.
        // API 33+ → runtime dialog; below → settings page (see PermissionActivity).
        // Do not remove below TIRAMISU — that used to fake "always granted".

        // Exact alarm: settings path on S+; below treat as granted.
        if (sdk < Build.VERSION_CODES.S) {
            permissionSet.remove(Manifest.permission.SCHEDULE_EXACT_ALARM);
        }

        // Install unknown apps: settings path from O; below treat as granted / drop.
        if (sdk < Build.VERSION_CODES.O) {
            permissionSet.remove(Manifest.permission.REQUEST_INSTALL_PACKAGES);
        }

        // Activity recognition: runtime from Q; below treat as granted.
        if (sdk < Build.VERSION_CODES.Q) {
            permissionSet.remove(Manifest.permission.ACTIVITY_RECOGNITION);
            permissionSet.remove(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        // Nearby Wi-Fi devices (API 33+); below map to location.
        if (permissionSet.contains(Manifest.permission.NEARBY_WIFI_DEVICES)) {
            if (sdk < Build.VERSION_CODES.TIRAMISU
                    || targetSdkVersion < Build.VERSION_CODES.TIRAMISU) {
                permissionSet.remove(Manifest.permission.NEARBY_WIFI_DEVICES);
                permissionSet.add(Manifest.permission.ACCESS_FINE_LOCATION);
                permissionSet.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }

        // READ_PHONE_NUMBERS (O+); below map to READ_PHONE_STATE.
        if (permissionSet.contains(Manifest.permission.READ_PHONE_NUMBERS)) {
            if (sdk < Build.VERSION_CODES.O) {
                permissionSet.remove(Manifest.permission.READ_PHONE_NUMBERS);
                permissionSet.add(Manifest.permission.READ_PHONE_STATE);
            }
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
                permissionSet.remove(Manifest.permission.BLUETOOTH);
                permissionSet.remove(Manifest.permission.BLUETOOTH_ADMIN);
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

        Iterator<String> it = permissionSet.iterator();
        while (it.hasNext()) {
            if (it.next() == null) {
                it.remove();
            }
        }
    }

    /**
     * Maps device-level grant/deny results back to the caller's original permission names.
     */
    static void mapResultsToOriginals(Context context,
                                      String[] originals,
                                      List<String> deviceGranted,
                                      List<String> deviceRejected,
                                      List<String> devicePermanentlyDenied,
                                      List<String> outGranted,
                                      List<String> outRejected,
                                      List<String> outPermanentlyDenied) {
        outGranted.clear();
        outRejected.clear();
        outPermanentlyDenied.clear();
        if (originals == null) {
            outGranted.addAll(deviceGranted);
            outRejected.addAll(deviceRejected);
            outPermanentlyDenied.addAll(devicePermanentlyDenied);
            return;
        }
        for (String original : originals) {
            if (hasPermission(context, original)) {
                outGranted.add(original);
                continue;
            }
            Set<String> related = new LinkedHashSet<>();
            Collections.addAll(related, original);
            Set<String> adapted = new LinkedHashSet<>();
            adapted.add(original);
            adaptPermissions(context, adapted);
            related.addAll(adapted);

            boolean permanent = false;
            boolean rejected = false;
            for (String r : related) {
                if (devicePermanentlyDenied.contains(r)) {
                    permanent = true;
                    break;
                }
                if (deviceRejected.contains(r)) {
                    rejected = true;
                }
            }
            // Also respect system permanent state for adapted runtime perms.
            if (!permanent) {
                for (String r : adapted) {
                    if (context instanceof Activity
                            && !isSpecialSettingsPermission(r)
                            && isPermanentlyDenied((Activity) context, r)) {
                        permanent = true;
                        break;
                    }
                }
            }
            if (permanent) {
                outPermanentlyDenied.add(original);
            } else if (rejected || !adapted.isEmpty()) {
                outRejected.add(original);
            } else {
                outRejected.add(original);
            }
        }
    }

    /** Human-readable label for permanent-deny tips. */
    static String permissionLabel(String permission) {
        if (permission == null) {
            return "";
        }
        switch (permission) {
            case Manifest.permission.CAMERA:
                return "相机";
            case Manifest.permission.RECORD_AUDIO:
                return "麦克风";
            case Manifest.permission.READ_EXTERNAL_STORAGE:
            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                return "存储";
            case Manifest.permission.MANAGE_EXTERNAL_STORAGE:
                return "所有文件访问";
            case Manifest.permission.READ_MEDIA_IMAGES:
                return "照片";
            case Manifest.permission.READ_MEDIA_VIDEO:
                return "视频";
            case Manifest.permission.READ_MEDIA_AUDIO:
                return "音频";
            case Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED:
                return "所选照片和视频";
            case Manifest.permission.ACCESS_FINE_LOCATION:
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                return "位置";
            case Manifest.permission.ACCESS_BACKGROUND_LOCATION:
                return "后台位置";
            case Manifest.permission.POST_NOTIFICATIONS:
                return "通知";
            case Manifest.permission.SYSTEM_ALERT_WINDOW:
                return "悬浮窗";
            case Manifest.permission.REQUEST_INSTALL_PACKAGES:
                return "安装应用";
            case Manifest.permission.SCHEDULE_EXACT_ALARM:
                return "精确闹钟";
            case Manifest.permission.ACTIVITY_RECOGNITION:
                return "身体活动";
            case Manifest.permission.NEARBY_WIFI_DEVICES:
                return "附近的设备";
            case Manifest.permission.READ_PHONE_STATE:
            case Manifest.permission.READ_PHONE_NUMBERS:
                return "电话状态";
            case Manifest.permission.CALL_PHONE:
                return "拨打电话";
            case Manifest.permission.BLUETOOTH:
            case Manifest.permission.BLUETOOTH_ADMIN:
            case Manifest.permission.BLUETOOTH_CONNECT:
            case Manifest.permission.BLUETOOTH_SCAN:
            case Manifest.permission.BLUETOOTH_ADVERTISE:
                return "蓝牙";
            default:
                int dot = permission.lastIndexOf('.');
                return dot >= 0 && dot < permission.length() - 1
                        ? permission.substring(dot + 1)
                        : permission;
        }
    }
}
