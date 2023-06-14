package com.yemesoft.permissions.handlers.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.yemesoft.permissions.OnePixActivity;
import com.yemesoft.permissions.PermissionCallback;
import com.yemesoft.permissions.Utils;

import java.util.ArrayList;
import java.util.List;

public class PermissionsActivity extends OnePixActivity {
    private static PermissionCallback permissionCallback;
    private static String[] permissions;

    public static void request(Context context, PermissionCallback callback, String... permissions) {
        List<String> granted = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        if (Utils.checkPermissions(context, granted, rejected, permissions)) {
            callback.onComplete(granted, rejected);
            return;
        }
        PermissionsActivity.permissionCallback = callback;
        PermissionsActivity.permissions = permissions;
        Intent intent = new Intent(context, PermissionsActivity.class);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(this, permissions, 0x7003);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        List<String> granted = new ArrayList<>();
        List<String> rejected = new ArrayList<>();
        Utils.checkPermissions(this, granted, rejected, permissions);
        permissionCallback.onComplete(granted, rejected);
        finish();
    }
}
