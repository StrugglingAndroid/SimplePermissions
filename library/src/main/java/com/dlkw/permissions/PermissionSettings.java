package com.dlkw.permissions;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Opens the app permission settings page with OEM-specific Intents when available.
 * Falls back to {@link Settings#ACTION_APPLICATION_DETAILS_SETTINGS}.
 */
public final class PermissionSettings {

    private PermissionSettings() {
    }

    /**
     * Opens app permission settings and returns via {@link Activity#onActivityResult}.
     * Do not add {@link Intent#FLAG_ACTIVITY_NEW_TASK} here — it breaks result delivery.
     *
     * @return true if an activity was started
     */
    public static boolean openAppPermissionSettings(Activity activity, int requestCode) {
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        for (Intent intent : buildCandidateIntents(activity)) {
            if (startForResult(activity, intent, requestCode)) {
                return true;
            }
        }
        try {
            activity.startActivityForResult(appDetailsIntent(activity), requestCode);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Opens the app notification settings page (API 26+ channel settings; older → app details).
     *
     * @return true if an activity was started
     */
    public static boolean openNotificationSettings(Activity activity, int requestCode) {
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        try {
            Intent intent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
            } else {
                intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", activity.getPackageName());
                intent.putExtra("app_uid", activity.getApplicationInfo().uid);
            }
            if (canHandle(activity, intent)) {
                activity.startActivityForResult(intent, requestCode);
                return true;
            }
        } catch (Exception ignored) {
        }
        return openAppPermissionSettings(activity, requestCode);
    }

    /**
     * Opens exact-alarm permission settings (API 31+).
     *
     * @return true if an activity was started
     */
    public static boolean openExactAlarmSettings(Activity activity, int requestCode) {
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return false;
        }
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            if (canHandle(activity, intent)) {
                activity.startActivityForResult(intent, requestCode);
                return true;
            }
        } catch (Exception ignored) {
        }
        return openAppPermissionSettings(activity, requestCode);
    }

    /**
     * Opens app permission settings without expecting a result (e.g. from a non-Activity context).
     */
    public static void openAppPermissionSettings(Context context) {
        for (Intent intent : buildCandidateIntents(context)) {
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            if (start(context, intent)) {
                return;
            }
        }
        Intent fallback = appDetailsIntent(context);
        if (!(context instanceof Activity)) {
            fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(fallback);
    }

    private static List<Intent> buildCandidateIntents(Context context) {
        String pkg = context.getPackageName();
        String brand = safeLower(Build.BRAND);
        String manufacturer = safeLower(Build.MANUFACTURER);
        List<Intent> intents = new ArrayList<>();

        if (isXiaomi(brand, manufacturer)) {
            Intent miui = new Intent("miui.intent.action.APP_PERM_EDITOR");
            miui.setClassName("com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity");
            miui.putExtra("extra_pkgname", pkg);
            intents.add(miui);

            Intent miuiAlt = new Intent("miui.intent.action.APP_PERM_EDITOR");
            miuiAlt.setClassName("com.miui.securitycenter",
                    "com.miui.permcenter.permissions.AppPermissionsEditorActivity");
            miuiAlt.putExtra("extra_pkgname", pkg);
            intents.add(miuiAlt);

            Intent miuiGeneric = new Intent("miui.intent.action.APP_PERM_EDITOR");
            miuiGeneric.putExtra("extra_pkgname", pkg);
            intents.add(miuiGeneric);
        } else if (isHuawei(brand, manufacturer)) {
            Intent hw = new Intent();
            hw.setComponent(new ComponentName("com.huawei.systemmanager",
                    "com.huawei.permissionmanager.ui.MainActivity"));
            intents.add(hw);

            Intent hwApp = new Intent();
            hwApp.setComponent(new ComponentName("com.huawei.systemmanager",
                    "com.huawei.permissionmanager.ui.SingleAppActivity"));
            hwApp.putExtra("packageName", pkg);
            intents.add(hwApp);
        } else if (isOppo(brand, manufacturer)) {
            Intent colorOs = new Intent();
            colorOs.setComponent(new ComponentName("com.coloros.safecenter",
                    "com.coloros.safecenter.permission.PermissionManagerActivity"));
            intents.add(colorOs);

            Intent colorOs2 = new Intent();
            colorOs2.setComponent(new ComponentName("com.coloros.safecenter",
                    "com.coloros.safecenter.permission.PermissionAppAllPermissionActivity"));
            colorOs2.putExtra("packageName", pkg);
            intents.add(colorOs2);

            Intent oppo = new Intent();
            oppo.setComponent(new ComponentName("com.oppo.safe",
                    "com.oppo.safe.permission.PermissionAppListActivity"));
            intents.add(oppo);
        } else if (isVivo(brand, manufacturer)) {
            Intent vivo = new Intent();
            vivo.setComponent(new ComponentName("com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.SoftPermissionDetailActivity"));
            vivo.putExtra("packagename", pkg);
            intents.add(vivo);

            Intent vivo2 = new Intent();
            vivo2.setComponent(new ComponentName("com.iqoo.secure",
                    "com.iqoo.secure.safeguard.SoftPermissionDetailActivity"));
            vivo2.putExtra("packagename", pkg);
            intents.add(vivo2);
        } else if (MeizuUtils.checkIsMeizuRom() || contains(brand, manufacturer, "meizu")) {
            Intent meizu = new Intent("com.meizu.safe.security.SHOW_APPSEC");
            meizu.putExtra("packageName", pkg);
            intents.add(meizu);
        }
        // Samsung / Google / AOSP: no reliable dedicated page — fall through to app details.

        intents.add(appDetailsIntent(context));
        return intents;
    }

    private static Intent appDetailsIntent(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        return intent;
    }

    private static boolean startForResult(Activity activity, Intent intent, int requestCode) {
        try {
            if (!canHandle(activity, intent)) {
                return false;
            }
            activity.startActivityForResult(intent, requestCode);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean start(Context context, Intent intent) {
        try {
            if (!canHandle(context, intent)) {
                return false;
            }
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean canHandle(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        return intent.resolveActivity(pm) != null;
    }

    private static boolean isXiaomi(String brand, String manufacturer) {
        return contains(brand, manufacturer, "xiaomi", "redmi", "poco", "blackshark");
    }

    private static boolean isHuawei(String brand, String manufacturer) {
        return contains(brand, manufacturer, "huawei", "honor");
    }

    private static boolean isOppo(String brand, String manufacturer) {
        return contains(brand, manufacturer, "oppo", "realme", "oneplus");
    }

    private static boolean isVivo(String brand, String manufacturer) {
        return contains(brand, manufacturer, "vivo", "iqoo");
    }

    private static boolean contains(String brand, String manufacturer, String... keys) {
        for (String key : keys) {
            if ((!TextUtils.isEmpty(brand) && brand.contains(key))
                    || (!TextUtils.isEmpty(manufacturer) && manufacturer.contains(key))) {
                return true;
            }
        }
        return false;
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US);
    }
}
