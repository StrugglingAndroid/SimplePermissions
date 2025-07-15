package com.dlkw.simple_permissions;

import android.Manifest;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dlkw.permissions.library.Permission;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.main).setOnClickListener(v -> {
            Permission.request(this, "请授予权限", new Permission.Callback<>() {
                        @Override
                        public void onResult(List<String> granted, List<String> rejected, Object extras) {
                            System.out.print("granted:" + String.join(",", granted));
                            System.out.println(",rejected:" + String.join(",", granted));
                        }
                    }, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW
                    , Manifest.permission.WRITE_SETTINGS, Manifest.permission.BLUETOOTH, Manifest.permission.RECORD_AUDIO);
        });
    }
}