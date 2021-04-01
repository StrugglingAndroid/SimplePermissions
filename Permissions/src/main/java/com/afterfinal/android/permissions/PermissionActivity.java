package com.afterfinal.android.permissions;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.afterfinal.android.permissions.api.R;

import java.util.ArrayList;
import java.util.List;

public class PermissionActivity extends TransparentActivity {
    private static final int REQUEST_PERMISSIONS = 0x7038;
    private static final int REQUEST_PERMISSIONS_SETTINGS = 0x7347;

    private TipMode tipMode = TipMode.Dialog;

    private String[] permissions;
    private Permissions.Callback callback;

    private boolean shouldShowRequestPermissionRationale = false;
    private final List<String> granted = new ArrayList<>();
    private final List<String> rejected = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        permissions = getIntent().getStringArrayExtra(Permissions.KEY_PERMISSIONS);
        tipMode = (TipMode) getIntent().getSerializableExtra(Permissions.KEY_TIP_MODE);
        callback = Permissions.callbackMap.remove(getIntent().getStringExtra(Permissions.KEY_CALLBACK_ID));

        requestPermissions();
    }

    private void requestPermissions() {
        check();
        if (rejected.contains(Manifest.permission.SYSTEM_ALERT_WINDOW) && !SystemAlertPermission.hasPermission(this)) {
            SystemAlertPermission.request(this, new SystemAlertPermission.Callback() {
                @Override
                public void onResult(boolean isGranted) {
                    if (isGranted) {
                        granted.add(Manifest.permission.SYSTEM_ALERT_WINDOW);
                        rejected.remove(Manifest.permission.SYSTEM_ALERT_WINDOW);
                    }
                    ActivityCompat.requestPermissions(PermissionActivity.this, permissions, REQUEST_PERMISSIONS);
                }
            });
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        check();
        if (0 == rejected.size()) {
            dispatchRequestFinish();
        } else {
            switch (tipMode) {
                case None:
                    dispatchRequestFinish();
                    break;
                case Toast:
                    Toast.makeText(getApplicationContext(), getString(R.string.sp_permissions_lacked) + "\n" + Permissions.getPermissionNames(rejected, "\n"), Toast.LENGTH_SHORT).show();
                    dispatchRequestFinish();
                    break;
                case Dialog:
                    showRequestPermissionSettingDialog();
                    break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_PERMISSIONS_SETTINGS == requestCode) {
            check();
            if (shouldShowRequestPermissionRationale && rejected.size() > 0) {
                showRequestPermissionSettingDialog();
            } else {
                dispatchRequestFinish();
            }
        }
    }

    private void check() {
        granted.clear();
        rejected.clear();
        if (null != permissions && permissions.length > 0) {
            for (String permission : permissions) {
                if (Manifest.permission.SYSTEM_ALERT_WINDOW.equals(permission)) {
                    if (SystemAlertPermission.hasPermission(this)) {
                        granted.add(permission);
                    } else {
                        shouldShowRequestPermissionRationale = true;
                        rejected.add(permission);
                    }
                } else {
                    if (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, permission)) {
                        granted.add(permission);
                    } else {
                        shouldShowRequestPermissionRationale |= ActivityCompat.shouldShowRequestPermissionRationale(this, permission);
                        rejected.add(permission);
                    }
                }
            }
        }
    }

    private void dispatchRequestFinish() {
        if (null != callback) {
            callback.onResult(granted, rejected);
        }
        finish();
    }

    private void showRequestPermissionSettingDialog() {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(getString(R.string.sp_permissions_lacked))
                .setMessage(Permissions.getPermissionNames(rejected, "\n"))
                .setPositiveButton(getString(R.string.sp_go_setting), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        try {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getApplication().getPackageName(), null);
                            intent.setData(uri);
                            startActivityForResult(intent, REQUEST_PERMISSIONS_SETTINGS);
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), getString(R.string.sp_cannot_open_settings), Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(getString(R.string.sp_go_close), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        dispatchRequestFinish();
                    }
                }).show();
    }
}
