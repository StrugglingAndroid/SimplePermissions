package com.yemesoft.permissions;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

public abstract class PermissionRequestHandler {
    protected final List<String> permissions = new ArrayList<String>() {
        @Override
        public boolean add(String s) {
            if (contains(s)) {
                return false;
            }
            return super.add(s);
        }
    };

    public abstract void handle(Context context, PermissionCallback callback);

    /**
     * targetSdkVersion >= Build.VERSION.SDK_INT，否则无法运行
     *
     * @param context
     * @param permission
     */
    public void addPermission(Context context, String permission) {
        ApplicationInfo info = context.getApplicationInfo();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || info.targetSdkVersion < Build.VERSION_CODES.M) {//<6.0
            //系统版本或目标版本低于5.0，不需要动态权限申请
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getApplicationContext().getPackageName(), PackageManager.GET_PERMISSIONS);
                PermissionInfo[] permissionInfos = packageInfo.permissions;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {//6.0-10.0
            if (Manifest.permission.MANAGE_EXTERNAL_STORAGE.equals(permission)) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
                permissions.add(permission);
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {//10.0-13.0
            if (info.targetSdkVersion >= Build.VERSION_CODES.R) {
                if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) || permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    permission = Manifest.permission.MANAGE_EXTERNAL_STORAGE;
                }
            }
            permissions.add(permission);
        } else {//>13.0
            if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            } else {
                permissions.add(permission);
            }
        }
    }

    public abstract boolean hasAllPermissions(Context context);
}
