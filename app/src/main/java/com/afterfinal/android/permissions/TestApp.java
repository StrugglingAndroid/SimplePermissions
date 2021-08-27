package com.afterfinal.android.permissions;

import android.app.Application;

public class TestApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Permissions.init(this);
    }
}
