package com.yemesoft.permissions;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;

public class OnePixActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = 1;
        lp.height = 1;
        lp.x = -1;
        lp.y = -1;
        getWindow().setAttributes(lp);

        if (null != getActionBar()) {
            getActionBar().hide();
        }
    }
}
