package com.afterfinal.android.permissions;

import android.Manifest;
import android.content.Context;
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

import java.util.Collections;
import java.util.List;

public class PermissionActivity extends TransparentActivity {
    private static final int REQUEST_PERMISSIONS = 0x7038;
    private static final int REQUEST_PERMISSIONS_SETTINGS = 0x7347;
    private static final int REQUEST_STORAGE_MANAGER = 0x7653;

    private Options options;
    private List<String> permissions;
    private Permissions.Callback callback;

    private final List<String> granted = new UniqueArrayList();
    private final List<String> rejected = new UniqueArrayList();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        options = getIntent().getParcelableExtra(Permissions.KEY_OPTIONS);
        if (null == options) {
            options = new Options();
        }

        String[] ps = getIntent().getStringArrayExtra(Permissions.KEY_PERMISSIONS);
        permissions = new UniqueArrayList();
        if (null != ps) {
            Collections.addAll(permissions, ps);
        }

        callback = Permissions.callbackMap.remove(getIntent().getStringExtra(Permissions.KEY_CALLBACK_ID));

        requestPermissions();
    }

    private void requestPermissions() {
        check();

        if (0 == rejected.size()) {
            dispatchRequestFinish();
            return;
        }

        if (rejected.contains(Manifest.permission.SYSTEM_ALERT_WINDOW)) {
            SystemAlertPermission.request(PermissionActivity.this, new SystemAlertPermission.Callback() {
                @Override
                public void onResult(boolean isGranted) {
                    check();
                    if (isGranted) {
                        granted.add(Manifest.permission.SYSTEM_ALERT_WINDOW);
                        rejected.remove(Manifest.permission.SYSTEM_ALERT_WINDOW);
                        if (rejected.size() > 0) {
                            requestPermissions();
                        } else {
                            handlePermissionResult();
                        }
                    } else {
                        rejected.clear();
                        rejected.add(Manifest.permission.SYSTEM_ALERT_WINDOW);
                        handlePermissionResult();
                    }
                }
            });
        } else if (rejected.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage("缺少" + Permissions.getPermissionName(Manifest.permission.MANAGE_EXTERNAL_STORAGE) + "权限")
                    .setPositiveButton("去开启", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            intent.setData(Uri.parse("package:" + getApplication().getPackageName()));
                            startActivityForResult(intent, REQUEST_STORAGE_MANAGER);
                        }
                    })
                    .setNegativeButton("暂不开启", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            rejected.clear();
                            rejected.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                            handlePermissionResult();
                        }
                    }).show();
        } else {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        check();
        handlePermissionResult();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_PERMISSIONS_SETTINGS == requestCode) {
            check();
            handlePermissionResult();
        } else if (REQUEST_STORAGE_MANAGER == requestCode) {
            check();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    granted.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                    rejected.remove(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                    if (rejected.size() > 0) {
                        requestPermissions();
                    } else {
                        handlePermissionResult();
                    }
                } else {
                    rejected.clear();
                    rejected.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                    handlePermissionResult();
                }
            } else {
                rejected.remove(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                if (rejected.size() > 0) {
                    requestPermissions();
                } else {
                    handlePermissionResult();
                }
            }
        }
    }

    private void handlePermissionResult() {
        if (0 == rejected.size()) {
            dispatchRequestFinish();
        } else {
            switch (options.tipMode) {
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

    private static int checkSelfPermission(Context context, String permission) {
        if (Manifest.permission.SYSTEM_ALERT_WINDOW.equals(permission)) {
            return SystemAlertPermission.hasPermission(context) ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED;
        }
        if (Manifest.permission.MANAGE_EXTERNAL_STORAGE.equals(permission)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.R) {
                return Environment.isExternalStorageManager() ? PackageManager.PERMISSION_GRANTED : PackageManager.PERMISSION_DENIED;
            }
            return PackageManager.PERMISSION_GRANTED;
        }
        return ActivityCompat.checkSelfPermission(context, permission);
    }

    private void check(String... exludedPermissions) {
        granted.clear();
        rejected.clear();
        if (null != permissions && permissions.size() > 0) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || getApplicationInfo().targetSdkVersion < Build.VERSION_CODES.R) {
                permissions.remove(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
            }
            for (String permission : permissions) {
                if (PackageManager.PERMISSION_GRANTED == checkSelfPermission(this, permission)) {
                    granted.add(permission);
                } else {
                    boolean excluded = false;
                    for (String p : exludedPermissions) {
                        if (p.equals(permission)) {
                            excluded = true;
                            break;
                        }
                    }
                    if (!excluded) {
                        rejected.add(permission);
                    }
                }
            }
        }
    }

    private void dispatchRequestFinish() {
        if (null != callback) {
            callback.onResult(options, granted, rejected);
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
                .setNegativeButton(options.exitWhenRejected ? getString(R.string.sp_exit_app) : (options.execWhenRejected ? getString(R.string.sp_skip_setting) : getString(R.string.sp_close)), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (options.exitWhenRejected) {
                            System.exit(0);
                            Process.killProcess(Process.myPid());
                        } else {
                            dialogInterface.dismiss();
                            dispatchRequestFinish();
                        }
                    }
                }).show();
    }
}
