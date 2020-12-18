package com.afterfinal.android.permissions;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity/* implements PermissionRequestCallback*/ {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main);
    }

    public void requestUsePermissions(View view) {
        doRequestUsePermissions();
    }

    @RequestPermissions(value = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SYSTEM_ALERT_WINDOW
    }, tipMode = TipMode.Dialog, execWhenRejected = true)
    private void doRequestUsePermissions() {
        Toast.makeText(MainActivity.this, "doRequestUsePermissions", Toast.LENGTH_SHORT).show();
    }
//
//    @Override
//    public void onPermissionRequestResult(IProceedingJoinPoint joinPoint, boolean execWhenRejected, List<String> granted, List<String> rejected) {
//        try {
//            joinPoint.proceed();//此处相当于实际调用doRequestUsePermissions方法
//        } catch (Throwable throwable) {
//            throwable.printStackTrace();
//        }
//    }
}