package com.yemesoft.permissions;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public abstract class PermissionRequestHandler {
    protected final List<String> permissions = new ArrayList<>();

    public abstract void handle(Context context, PermissionCallback callback);

    public void addPermission(String permission) {
        permissions.add(permission);
    }
}
