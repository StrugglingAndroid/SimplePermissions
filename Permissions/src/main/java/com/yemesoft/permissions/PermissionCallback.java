package com.yemesoft.permissions;

import java.util.List;

public interface PermissionCallback {
    void onComplete(List<String> granted, List<String> rejected);
}
