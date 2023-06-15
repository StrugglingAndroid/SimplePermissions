package com.yemesoft.permissions.handlers.floatwindow;

import android.content.Context;

import com.yemesoft.permissions.PermissionCallback;
import com.yemesoft.permissions.PermissionRequestHandler;

public class FloatHandler extends PermissionRequestHandler {
    @Override
    public void handle(Context context, PermissionCallback callback) {
        FloatPermissionActivity.request(context, callback, permissions.toArray(new String[0]));
    }

    @Override
    public boolean hasAllPermissions(Context context) {
        return false;
    }
}
