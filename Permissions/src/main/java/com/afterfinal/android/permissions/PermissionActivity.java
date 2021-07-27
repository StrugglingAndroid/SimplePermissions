package com.afterfinal.android.permissions;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
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

    private boolean execWhenRejected = false;
    private boolean exitWhenRejected = false;
    private String[] permissions;
    private Permissions.Callback callback;

    private final List<String> granted = new ArrayList<>();
    private final List<String> rejected = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        permissions = getIntent().getStringArrayExtra(Permissions.KEY_PERMISSIONS);
        execWhenRejected = getIntent().getBooleanExtra(Permissions.KEY_EXEC_WHEN_REJECTED, false);
        exitWhenRejected = getIntent().getBooleanExtra(Permissions.KEY_EXIT_WHEN_REJECTED, false);
        tipMode = (TipMode) getIntent().getSerializableExtra(Permissions.KEY_TIP_MODE);
        callback = Permissions.callbackMap.remove(getIntent().getStringExtra(Permissions.KEY_CALLBACK_ID));

        requestPermissions();
    }

    private void requestPermissions() {
        check();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            rejected.remove(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        } else if (rejected.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE) && !rejected.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
            rejected.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
        }
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
        } else if (rejected.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE) && !Environment.isExternalStorageManager()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getApplication().getPackageName()));
            startActivityForResult(intent, REQUEST_PERMISSIONS);
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        handlePermissionResult();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_PERMISSIONS_SETTINGS == requestCode) {
            handlePermissionResult();
        }
    }

    private void handlePermissionResult() {
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

    private void check() {
        granted.clear();
        rejected.clear();
        if (null != permissions && permissions.length > 0) {
            for (String permission : permissions) {
                if (Manifest.permission.SYSTEM_ALERT_WINDOW.equals(permission)) {
                    if (SystemAlertPermission.hasPermission(this)) {
                        granted.add(permission);
                    } else {
                        rejected.add(permission);
                    }
                } else if (Manifest.permission.MANAGE_EXTERNAL_STORAGE.equals(permission)) {
                    if (Environment.isExternalStorageManager()) {
                        granted.add(permission);
                    } else {
                        rejected.add(permission);
                    }
                } else {
                    if (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, permission)) {
                        granted.add(permission);
                    } else {
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
                .setNegativeButton(exitWhenRejected ? getString(R.string.sp_exit_app) : (execWhenRejected ? getString(R.string.sp_skip_setting) : getString(R.string.sp_close)), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (execWhenRejected) {
                            dialogInterface.dismiss();
                            dispatchRequestFinish();
                        } else {
                            System.exit(0);
                            Process.killProcess(Process.myPid());
                        }
                    }
                }).show();
    }
}
