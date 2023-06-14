package com.yemesoft.permissions;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static boolean checkPermissions(Context context, String... permissions) {
        return checkPermissions(context, new ArrayList<String>(), new ArrayList<String>(), permissions);
    }

    public static boolean checkPermissions(Context context, List<String> rejected, String... permissions) {
        return checkPermissions(context, new ArrayList<String>(), rejected, permissions);
    }

    public static boolean checkPermissions(Context context, List<String> granted, List<String> rejected, String... permissions) {
        for (String permission : permissions) {
            if (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(context, permission)) {
                granted.add(permission);
            } else {
                rejected.add(permission);
            }
        }
        return 0 == rejected.size();
    }
}
