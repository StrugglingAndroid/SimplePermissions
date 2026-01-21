/*
 * Copyright (C) 2016 Facishare Technology Co., Ltd. All Rights Reserved.
 */
package com.dlkw.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

class FloatWindowUtil {
    private static final String TAG = "FloatWindowManager";

    public static boolean checkPermission(Context context) {
        if (MeizuUtils.checkIsMeizuRom()) {
            return MeizuUtils.checkFloatWindowPermission(context);
        } else {
            if (Settings.canDrawOverlays(context)) {
                return true;
            }
            try {
                WindowManager wm = (WindowManager) context.getSystemService(Activity.WINDOW_SERVICE);
                View view = new View(context);
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.width = 1;
                lp.height = 1;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
                } else {
                    lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                }
                wm.addView(view, lp);
                wm.removeView(view);
                return true;
            } catch (Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
                return false;
            }
        }
    }

    public static void applyPermission(Activity activity, int requestCode) {
        if (MeizuUtils.checkIsMeizuRom()) {
            //这里也一样，魅族系统需要单独适配
            try {
                Intent intent = new Intent("com.meizu.safe.security.SHOW_APPSEC");
                intent.putExtra("packageName", activity.getPackageName());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivityForResult(intent, requestCode);
            } catch (Exception e) {
                commonROMPermissionApplyInternal(activity, requestCode);
            }
        } else {
            commonROMPermissionApplyInternal(activity, requestCode);
        }
    }

    public static void commonROMPermissionApplyInternal(Activity activity, int requestCode) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        activity.startActivityForResult(intent, requestCode);
    }
}
