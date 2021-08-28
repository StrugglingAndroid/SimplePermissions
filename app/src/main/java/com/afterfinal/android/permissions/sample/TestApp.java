package com.afterfinal.android.permissions.sample;

import android.app.Application;

import com.afterfinal.android.permissions.Permissions;

public class TestApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Permissions.init(this);
    }
}
