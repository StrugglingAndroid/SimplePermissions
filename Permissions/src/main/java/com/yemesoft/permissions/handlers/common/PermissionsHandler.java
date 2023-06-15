package com.yemesoft.permissions.handlers.common;

import android.content.Context;

import com.yemesoft.permissions.PermissionCallback;
import com.yemesoft.permissions.PermissionRequestHandler;

public class PermissionsHandler extends PermissionRequestHandler {
    @Override
    public void handle(Context context, PermissionCallback callback) {
        PermissionsActivity.request(context, callback, permissions.toArray(new String[0]));
    }

    @Override
    public boolean hasAllPermissions(Context context) {
        return false;
    }
}
