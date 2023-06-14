package com.yemesoft.permissions.handlers.storage;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.yemesoft.permissions.OnePixActivity;
import com.yemesoft.permissions.PermissionCallback;
import com.yemesoft.permissions.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("all")
public class ExternalStoragePermissionActivity extends OnePixActivity {
    private static PermissionCallback permissionCallback;
    private static String[] permissions;

    public static void request(Context context, PermissionCallback callback, String... permissions) {
        List<String> granted = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        if (Utils.checkPermissions(context, granted, rejected, permissions)) {
            callback.onComplete(granted, rejected);
            return;
        }
        ExternalStoragePermissionActivity.permissionCallback = callback;
        ExternalStoragePermissionActivity.permissions = permissions;
        Intent intent = new Intent(context, ExternalStoragePermissionActivity.class);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            List<String> granted = new ArrayList<>();
            List<String> rejected = new ArrayList<>();
            Collections.addAll(granted, permissions);
            permissionCallback.onComplete(granted, rejected);
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            ActivityCompat.requestPermissions(this, permissions, 0x7001);
        } else {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getApplication().getPackageName()));
                startActivityForResult(intent, 0x7002);
            } else {
                requestNormalPermissions();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void requestNormalPermissions() {
        Set<String> permissionSet = new HashSet<>();
        Collections.addAll(permissionSet, permissions);
        permissionSet.remove(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (permissionSet.contains(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                permissionSet.add(Manifest.permission.READ_MEDIA_VIDEO);
                permissionSet.add(Manifest.permission.READ_MEDIA_AUDIO);
                permissionSet.add(Manifest.permission.READ_MEDIA_IMAGES);
                permissionSet.remove(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        ActivityCompat.requestPermissions(this, permissionSet.toArray(new String[0]), 0x7003);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (0x7001 == requestCode || 0x7003 == requestCode) {
            List<String> granted = new ArrayList<>();
            List<String> rejected = new ArrayList<>();
            Utils.checkPermissions(this, granted, rejected, permissions);
            permissionCallback.onComplete(granted, rejected);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (0x7002 == requestCode) {
            List<String> granted = new ArrayList<>();
            List<String> rejected = new ArrayList<>();
            if (!Environment.isExternalStorageManager()) {
                Collections.addAll(rejected, Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                permissionCallback.onComplete(granted, rejected);
                return;
            }
            requestNormalPermissions();
        }
    }
}
