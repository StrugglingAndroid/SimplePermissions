package com.afterfinal.android.permissions;

import com.afterfinal.android.permissions.aspect.IProceedingJoinPoint;

import java.util.List;

public interface PermissionRequestCallback {
    void onPermissionRequestResult(Options options, List<String> granted, List<String> rejected, IProceedingJoinPoint joinPoint);
}
