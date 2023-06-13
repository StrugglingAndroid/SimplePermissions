package com.yemesoft.permissions.handlers.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.yemesoft.permissions.OnePixActivity;
import com.yemesoft.permissions.PermissionCallback;

public class PermissionsActivity extends OnePixActivity {

    public static void request(Context context, PermissionCallback callback, String... permissions) {
        Intent intent = new Intent(context, PermissionsActivity.class);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
