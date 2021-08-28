package com.afterfinal.android.permissions;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toast();
//        Permissions.request(this, new Options(true, false, TipMode.None), new Permissions.Callback() {
//            @Override
//            public void onResult(Options options, List<String> granted, List<String> rejected) {
//                toast();
//            }
//        }, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE);
    }

    @RequestPermissions({Manifest.permission.READ_CONTACTS, Manifest.permission.MANAGE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW, Manifest.permission.READ_PHONE_STATE})
    private void toast() {
        Toast.makeText(getApplicationContext(), "权限申请完毕", Toast.LENGTH_SHORT).show();
        BluetoothAdapter.getDefaultAdapter().enable();
    }
}