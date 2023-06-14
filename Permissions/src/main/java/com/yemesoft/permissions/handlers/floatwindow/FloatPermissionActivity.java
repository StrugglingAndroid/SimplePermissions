package com.yemesoft.permissions.handlers.floatwindow;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.yemesoft.permissions.OnePixActivity;
import com.yemesoft.permissions.PermissionCallback;

import java.util.ArrayList;
import java.util.List;

public class FloatPermissionActivity extends OnePixActivity {
    private static PermissionCallback permissionCallback;

    public static void request(Context context, PermissionCallback callback, String... permissions) {
        List<String> granted = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        if (PermissionUtil.hasPermission(context)) {
            granted.add(Manifest.permission.SYSTEM_ALERT_WINDOW);
            callback.onComplete(granted, rejected);
            return;
        }
        FloatPermissionActivity.permissionCallback = callback;
        Intent intent = new Intent(context, FloatPermissionActivity.class);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestOverlayPermission();
        } else {
            dispatchResult();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, 0x7003);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        dispatchResult();
    }

    private void dispatchResult() {
        List<String> granted = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        if (PermissionUtil.hasPermission(this)) {
            granted.add(Manifest.permission.SYSTEM_ALERT_WINDOW);
        } else {
            rejected.add(Manifest.permission.SYSTEM_ALERT_WINDOW);
        }
        permissionCallback.onComplete(granted, rejected);
        finish();
    }
}
