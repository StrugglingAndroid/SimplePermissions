package com.dlkw.permissions;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.dlkw.permissions.library.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@SuppressWarnings("all")
public class PermissionActivity extends Activity {

    private static final int REQ_REUQEST_NORMAL_PERMISSIONS = 0x7003;
    private static final int REQ_MANAGE_OVERLAY_PERMISSION = 0x7036;
    private static final int REQ_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION = 0x7035;

    private Permission.Callback callback;
    private Object extras;
    private final ArrayList<String> granted = new ArrayList<>();
    private final ArrayList<String> rejected = new ArrayList<>();
    private final Set<String> tempPermissionSet = new LinkedHashSet<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission1);

        try {
            String callbackId = getIntent().getStringExtra(Permission.KEY_CALLBACK_ID);
            callback = Permission.callbackMap.remove(callbackId);
            extras = Permission.extraMap.remove(callbackId);
            String[] permissions = Permission.permissionMap.remove(callbackId);
            if (null == callbackId || null == callback || null == permissions) {
                finish();
            }

            Collections.addAll(tempPermissionSet, permissions);
            Permission.adaptPermissions(this, tempPermissionSet);

            TextView titleView = findViewById(R.id.permission_title);
            TextView descriptionView = findViewById(R.id.permission_description);
            View panelView = findViewById(R.id.permission_declare_panel);

            String title = getIntent().getStringExtra(Permission.KEY_TITLE);
            if (TextUtils.isEmpty(title)) {
                titleView.setVisibility(View.GONE);
            } else {
                titleView.setText(title);
                titleView.setVisibility(View.VISIBLE);
            }

            String description = getIntent().getStringExtra(Permission.KEY_DESCRIPTION);
            descriptionView.setText(description);

            panelView.setVisibility(View.VISIBLE);

            requestPermissions();
        } catch (Exception e) {
            e.printStackTrace();
            rejected.addAll(tempPermissionSet);
            tempPermissionSet.clear();
            requestPermissions();
        }
    }

    @Override
    protected void onDestroy() {
        if (null != callback && !tempPermissionSet.isEmpty()) {
            rejected.addAll(tempPermissionSet);
            tempPermissionSet.clear();
            requestPermissions();
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        try {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (REQ_REUQEST_NORMAL_PERMISSIONS == requestCode) {
                for (int i = 0; i < grantResults.length; i++) {
                    tempPermissionSet.remove(permissions[i]);
                    if (PackageManager.PERMISSION_GRANTED == grantResults[i]) {
                        granted.add(permissions[i]);
                    } else {
                        rejected.add(permissions[i]);
                    }
                }
                requestPermissions();
            } else {
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);
            if (REQ_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION == requestCode) {
                tempPermissionSet.remove(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                if (Environment.isExternalStorageManager()) {
                    granted.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                } else {
                    rejected.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                }
                requestPermissions();
            } else if (REQ_MANAGE_OVERLAY_PERMISSION == requestCode) {
                tempPermissionSet.remove(Manifest.permission.SYSTEM_ALERT_WINDOW);
                if (Permission.hasFloatingWindowPermission(this)) {
                    granted.add(Manifest.permission.SYSTEM_ALERT_WINDOW);
                } else {
                    rejected.add(Manifest.permission.SYSTEM_ALERT_WINDOW);
                }
                requestPermissions();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void dispatchResult() {
        try {
            if (callback != null) {
                callback.onResult(granted, rejected, extras);
            }
            callback = null;
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void requestPermissions() {
        if (tempPermissionSet.isEmpty()) {
            dispatchResult();
            return;
        }
        if (tempPermissionSet.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
            if (Environment.isExternalStorageManager()) {
                granted.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                tempPermissionSet.remove(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                requestPermissions();
            } else {
                callback.onShowConfirmDialog(this, "您的手机没有授予文件管理权限，请开启后再试", new OnConfirmListener() {

                    @Override
                    public void onConfirm() {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, REQ_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    }

                    @Override
                    public void onCancel() {
                        rejected.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                        tempPermissionSet.remove(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                        requestPermissions();
                    }
                });
            }
        } else if (tempPermissionSet.contains(Manifest.permission.SYSTEM_ALERT_WINDOW)) {
            if (Settings.canDrawOverlays(getApplicationContext())) {
                granted.add(Manifest.permission.SYSTEM_ALERT_WINDOW);
                tempPermissionSet.remove(Manifest.permission.SYSTEM_ALERT_WINDOW);
                requestPermissions();
            } else {
                callback.onShowConfirmDialog(this, "您的手机没有授予悬浮窗权限，请开启后再试", new OnConfirmListener() {

                    @Override
                    public void onConfirm() {
                        FloatWindowUtil.applyPermission(PermissionActivity.this, REQ_MANAGE_OVERLAY_PERMISSION);
                    }

                    @Override
                    public void onCancel() {
                        rejected.add(Manifest.permission.SYSTEM_ALERT_WINDOW);
                        tempPermissionSet.remove(Manifest.permission.SYSTEM_ALERT_WINDOW);
                        requestPermissions();
                    }
                });
            }
        } else {
            ActivityCompat.requestPermissions(this, tempPermissionSet.toArray(new String[0]), REQ_REUQEST_NORMAL_PERMISSIONS);
        }
    }
}