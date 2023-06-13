package com.yemesoft.permissions;

import android.Manifest;
import android.content.Context;

import com.yemesoft.permissions.handlers.common.CommonPermissionRequestHandler;
import com.yemesoft.permissions.handlers.floatwindow.FloatHandler;
import com.yemesoft.permissions.handlers.storage.StorageHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Permissions {
    private static final Map<String, Class<? extends PermissionRequestHandler>> handlerMap = new HashMap<>();

    static {
        handlerMap.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, StorageHandler.class);
        handlerMap.put(Manifest.permission.SYSTEM_ALERT_WINDOW, FloatHandler.class);
    }

    static List<PermissionRequestHandler> getHandlers(String... permissions) {
        try {
            Map<Class<? extends PermissionRequestHandler>, PermissionRequestHandler> map = new LinkedHashMap<>();
            for (String permission : permissions) {
                Class<? extends PermissionRequestHandler> handlerClass = handlerMap.get(permission);
                if (null == handlerClass) {
                    handlerClass = CommonPermissionRequestHandler.class;
                }
                PermissionRequestHandler handler = map.get(handlerClass);
                if (null == handler) {
                    handler = handlerClass.newInstance();
                    map.put(handlerClass, handler);
                }
                handler.addPermission(permission);
            }
            return new ArrayList<>(map.values());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static void request(Context context, PermissionCallback callback, String... permissions) {
        List<PermissionRequestHandler> handlers = getHandlers(permissions);
        processHandler(handlers, 0, context, new ArrayList<String>(), new ArrayList<String>(), callback);
    }

    private static void processHandler(final List<PermissionRequestHandler> handlers, final int index, final Context context, final List<String> granted, final List<String> rejected, final PermissionCallback callback) {
        if (index >= handlers.size()) {
            callback.onComplete(granted, rejected);
            return;
        }
        handlers.get(index).handle(context, new PermissionCallback() {
            @Override
            public void onComplete(List<String> g, List<String> r) {
                granted.addAll(g);
                rejected.addAll(r);
                processHandler(handlers, index + 1, context, granted, rejected, callback);
            }
        });
    }
}
