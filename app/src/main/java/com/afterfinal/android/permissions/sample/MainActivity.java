package com.afterfinal.android.permissions.sample;

import android.Manifest;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.yemesoft.permissions.PermissionCallback;
import com.yemesoft.permissions.Permissions;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Permissions.request(this, new PermissionCallback() {
            @Override
            public void onComplete(List<String> granted, List<String> rejected) {

            }
        }, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE, Manifest.permission.SYSTEM_ALERT_WINDOW, Manifest.permission.POST_NOTIFICATIONS);
    }
}