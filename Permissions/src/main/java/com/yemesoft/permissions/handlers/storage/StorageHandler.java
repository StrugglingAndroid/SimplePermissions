package com.yemesoft.permissions.handlers.storage;

import android.content.Context;

import com.yemesoft.permissions.PermissionCallback;
import com.yemesoft.permissions.PermissionRequestHandler;

public class StorageHandler extends PermissionRequestHandler {
    @Override
    public void handle(Context context, PermissionCallback callback) {
        ExternalStoragePermissionActivity.request(context, callback, permissions.toArray(new String[0]));
    }
}
