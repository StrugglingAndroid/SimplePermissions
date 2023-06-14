package com.yemesoft.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class DescriptionActivity extends OnePixActivity {
    public static final String EXTRA_DESCRIPTIONS = "EXTRA_DESCRIPTIONS";

    private static PermissionCallback permissionCallback;
    private static String[] permissions;

    public static void start(Context context, String[] descriptions, PermissionCallback callback, String... permissions) {
        DescriptionActivity.permissionCallback = callback;
        DescriptionActivity.permissions = permissions;
        Intent intent = new Intent(context, DescriptionActivity.class);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.putExtra(EXTRA_DESCRIPTIONS, descriptions);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        if (null != getActionBar()) {
            getActionBar().hide();
        }

        String[] descriptions = getIntent().getStringArrayExtra(EXTRA_DESCRIPTIONS);
        if (null != descriptions) {
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            setContentView(layout);
            for (String description : descriptions) {
                TextView textView = new TextView(this);
                textView.setText(description);
                layout.addView(textView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
        }

//        Permissions.request(this, permissionCallback, permissions);

        startActivity(new Intent(this, OnePixActivity.class));
    }
}
