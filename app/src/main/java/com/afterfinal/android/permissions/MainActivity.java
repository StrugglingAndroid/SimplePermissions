package com.afterfinal.android.permissions;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toast();
    }

    @RequestPermissions({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW, Manifest.permission.READ_PHONE_STATE})
    private void toast() {
        Toast.makeText(getApplicationContext(), "权限申请完毕", Toast.LENGTH_SHORT).show();
    }
}