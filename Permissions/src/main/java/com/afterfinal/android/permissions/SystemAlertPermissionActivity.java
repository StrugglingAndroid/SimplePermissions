package com.afterfinal.android.permissions;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public class SystemAlertPermissionActivity extends TransparentActivity {
    static final int REQUEST_FLOAT_WINDOW_PERMISSION = 0x7003;
    private SystemAlertPermission.Callback callback;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        callback = SystemAlertPermission.callbackMap.remove(getIntent().getStringExtra(SystemAlertPermission.KEY_CALLBACK_ID));
        if (SystemAlertPermission.hasPermission(this)) {
            callback.onResult(true);
            finish();
        } else {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setMessage("缺少悬浮窗权限")
                    .setPositiveButton("去开启", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            SystemAlertPermission.applyPermission(SystemAlertPermissionActivity.this, callback);
                        }
                    })
                    .setNegativeButton("暂不开启", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            callback.onResult(false);
                            finish();
                        }
                    }).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_FLOAT_WINDOW_PERMISSION == requestCode) {
            callback.onResult(SystemAlertPermission.hasPermission(this));
            finish();
        }
    }
}
