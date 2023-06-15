package com.yemesoft.permissions.handlers.storage;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import com.yemesoft.permissions.PermissionCallback;
import com.yemesoft.permissions.PermissionRequestHandler;

public class StorageHandler extends PermissionRequestHandler {
    @Override
    public void handle(Context context, PermissionCallback callback) {
        ExternalStoragePermissionActivity.request(context, callback, permissions.toArray(new String[0]));
    }

    @Override
    public boolean hasAllPermissions(Context context) {
        return false;
    }
}
