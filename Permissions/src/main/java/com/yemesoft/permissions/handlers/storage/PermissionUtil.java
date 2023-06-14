package com.yemesoft.permissions.handlers.storage;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

class PermissionUtil {
    private static final int PERMISSION_SETTING_FOR_RESULT = 0x7003;

    public void GoToSetting(Activity context) {
        goIntentSetting(context);
    }

    private void goIntentSetting(Activity context) {
        try {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            context.startActivityForResult(intent, PERMISSION_SETTING_FOR_RESULT);
        } catch (Exception e) {
            e.printStackTrace();
            String name = Build.MANUFACTURER;
            switch (name) {
                case "HUAWEI":
                    goHuaWeiMainager(context);
                    break;
                case "vivo":
                    goVivoMainager(context);
                    break;
                case "OPPO":
                    goOppoMainager(context);
                    break;
                case "Coolpad":
                    goCoolpadMainager(context);
                    break;
                case "Meizu":
                    goMeizuMainager(context);
                    break;
                case "Xiaomi":
                    goXiaoMiMainager(context);
                    break;
                case "Sony":
                    goSonyMainager(context);
                    break;
                case "LG":
                    goLGMainager(context);
                    break;
                default:
                    systemConfig(context);
                    break;
            }
        }

    }

    private void goLGMainager(Activity context) {
        try {
            Intent intent = new Intent(context.getApplicationContext().getPackageName());
            ComponentName comp = new ComponentName("com.android.settings", "com.android.settings.Settings$AccessLockSummaryActivity");
            intent.setComponent(comp);
            if (context != null) {
                context.startActivityForResult(intent, PERMISSION_SETTING_FOR_RESULT);
            }
        } catch (Exception e) {
            e.printStackTrace();
            systemConfig(context);
        }

    }

    private void goSonyMainager(Activity context) {
        try {
            Intent intent = new Intent(context.getApplicationContext().getPackageName());
            ComponentName comp = new ComponentName("com.sonymobile.cta", "com.sonymobile.cta.SomcCTAMainActivity");
            intent.setComponent(comp);
            if (context != null) {
                context.startActivityForResult(intent, PERMISSION_SETTING_FOR_RESULT);
            }
        } catch (Exception e) {
            e.printStackTrace();
            systemConfig(context);
        }

    }

    private void goHuaWeiMainager(Activity context) {
        try {
            Intent intent = new Intent(context.getApplicationContext().getPackageName());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ComponentName comp = new ComponentName("com.huawei.systemmanager", "com.huawei.permissionmanager.ui.MainActivity");
            intent.setComponent(comp);
            if (context != null) {
                context.startActivityForResult(intent, PERMISSION_SETTING_FOR_RESULT);
            }
        } catch (Exception e) {
            e.printStackTrace();
            systemConfig(context);
        }

    }

    private static String getMiuiVersion() {
        String propName = "ro.miui.ui.version.name";
        String line;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return line;

    }

    private void goXiaoMiMainager(Activity context) {
        String rom = getMiuiVersion();
        Intent intent = new Intent();
        if ("V6".equals(rom) || "V7".equals(rom)) {
            intent.setAction("miui.intent.action.APP_PERM_EDITOR");
            intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity");
            intent.putExtra("extra_pkgname", context.getApplicationContext().getPackageName());
            if (context != null) {
                context.startActivityForResult(intent, PERMISSION_SETTING_FOR_RESULT);
            }
        } else if ("V8".equals(rom) || "V9".equals(rom)) {
            intent.setAction("miui.intent.action.APP_PERM_EDITOR");
            intent.setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity");
            intent.putExtra("extra_pkgname", context.getApplicationContext().getPackageName());
            if (context != null) {
                context.startActivityForResult(intent, PERMISSION_SETTING_FOR_RESULT);
            }
        } else {
            systemConfig(context);
        }
    }

    private void goMeizuMainager(Activity context) {
        try {
            Intent intent = new Intent("com.meizu.safe.security.SHOW_APPSEC");
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.putExtra("packageName", context.getApplicationContext().getPackageName());
            if (context != null) {
                context.startActivityForResult(intent, PERMISSION_SETTING_FOR_RESULT);
            }
        } catch (ActivityNotFoundException localActivityNotFoundException) {
            localActivityNotFoundException.printStackTrace();
            systemConfig(context);
        }

    }


    /**
     * 系统设置界面
     */
    private void systemConfig(Activity context) {
        Intent intent = new Intent(Settings.ACTION_SETTINGS);
        if (context != null) {
            context.startActivityForResult(intent, PERMISSION_SETTING_FOR_RESULT);
        }
    }

    private void goOppoMainager(Activity context) {
        doStartApplicationWithPackageName(context, "com.coloros.safecenter");
    }

    /**
     * doStartApplicationWithPackageName("com.yulong.android.security:remote")
     * <p>
     * 和Intent open = getPackageManager().getLaunchIntentForPackage("com.yulong.android.security:remote");
     * <p>
     * startActivity(open);
     * <p>
     * 本质上没有什么区别，通过Intent open...打开比调用doStartApplicationWithPackageName方法更快，也是android本身提供的方法
     */
    private void goCoolpadMainager(Activity context) {
        doStartApplicationWithPackageName(context, "com.yulong.android.security:remote");
    }

    private void goVivoMainager(Activity context) {
        doStartApplicationWithPackageName(context, "com.bairenkeji.icaller");
    }

    private void doStartApplicationWithPackageName(Activity context, String packagename) {
        // 通过包名获取此APP详细信息，包括Activities、services、versioncode、name等等
        PackageInfo packageinfo = null;
        try {
            packageinfo = context.getPackageManager().getPackageInfo(packagename, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageinfo == null) {
            return;
        }
        // 创建一个类别为CATEGORY_LAUNCHER的该包名的Intent
        Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        resolveIntent.setPackage(packageinfo.packageName);
        // 通过getPackageManager()的queryIntentActivities方法遍历
        List<ResolveInfo> resolveinfoList = context.getPackageManager().queryIntentActivities(resolveIntent, 0);

        ResolveInfo resolveinfo = resolveinfoList.iterator().next();
        if (resolveinfo != null) {
            // packageName参数2 = 参数 packname
            String packageName = resolveinfo.activityInfo.packageName;
            // 这个就是我们要找的该APP的LAUNCHER的Activity[组织形式：packageName参数2.mainActivityname]
            String className = resolveinfo.activityInfo.name;
            // LAUNCHER Intent
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            // 设置ComponentName参数1:packageName参数2:MainActivity路径
            ComponentName cn = new ComponentName(packageName, className);
            intent.setComponent(cn);
            try {
                context.startActivityForResult(intent, PERMISSION_SETTING_FOR_RESULT);
            } catch (Exception e) {
                systemConfig(context);
                e.printStackTrace();
            }
        }
    }
}
