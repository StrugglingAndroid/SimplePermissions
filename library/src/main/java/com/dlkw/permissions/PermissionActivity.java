package com.dlkw.permissions;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("all")
public class PermissionActivity extends Activity {

    private static final int REQ_NORMAL_PERMISSIONS = 0x7003;
    private static final int REQ_MANAGE_OVERLAY_PERMISSION = 0x7036;
    private static final int REQ_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION = 0x7035;
    private static final int REQ_MANAGE_UNKNOWN_APP_SOURCES = 0x7037;
    private static final int REQ_APP_PERMISSION_SETTINGS = 0x7038;
    private static final int REQ_BACKGROUND_LOCATION = 0x7039;

    private static final String STATE_CALLBACK_ID = "sp_state_callback_id";
    private static final String STATE_RESULT_DISPATCHED = "sp_state_result_dispatched";
    private static final String STATE_GUIDED_PERMANENT = "sp_state_guided_permanent";
    private static final String STATE_WAITING_SETTINGS = "sp_state_waiting_settings";
    private static final String STATE_PENDING = "sp_state_pending";
    private static final String STATE_GRANTED = "sp_state_granted";
    private static final String STATE_REJECTED = "sp_state_rejected";
    private static final String STATE_PERMANENT = "sp_state_permanent";

    private Permission.Callback callback;
    private Object extras;
    private String callbackId;
    private final ArrayList<String> granted = new ArrayList<>();
    private final ArrayList<String> rejected = new ArrayList<>();
    private final ArrayList<String> permanentlyDenied = new ArrayList<>();
    private final Set<String> tempPermissionSet = new LinkedHashSet<>();
    private boolean resultDispatched;
    private boolean guidedPermanentDenied;
    /** True while waiting for Settings / special-permission activity result. */
    private boolean waitingForSettingsResult;
    /** Request code of the settings page we last opened (for onResume fallback). */
    private int pendingSettingsRequestCode = -1;
    /** True only while classifying results from {@link #onRequestPermissionsResult}. */
    private boolean classifyingFromSystemResult;
    /** True when explain UI came from Callback / PermissionExplainUi (no id binding). */
    private boolean usingCustomExplainUi;
    private PermissionExplainUi activeExplainUi;
    private static final String STATE_PENDING_SETTINGS_CODE = "sp_state_pending_settings_code";
    private final Runnable settingsReturnRunnable = this::consumeSettingsReturn;
    private Runnable delayedSettingsHandleRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            if (savedInstanceState != null) {
                callbackId = savedInstanceState.getString(STATE_CALLBACK_ID);
                resultDispatched = savedInstanceState.getBoolean(STATE_RESULT_DISPATCHED, false);
                guidedPermanentDenied = savedInstanceState.getBoolean(STATE_GUIDED_PERMANENT, false);
                waitingForSettingsResult = savedInstanceState.getBoolean(STATE_WAITING_SETTINGS, false);
                pendingSettingsRequestCode = savedInstanceState.getInt(STATE_PENDING_SETTINGS_CODE, -1);
            } else {
                callbackId = getIntent().getStringExtra(Permission.KEY_CALLBACK_ID);
            }

            if (callbackId != null) {
                callback = Permission.callbackMap.get(callbackId);
                extras = Permission.extraMap.get(callbackId);
                String[] permissions = Permission.permissionMap.get(callbackId);
                if (savedInstanceState != null) {
                    restoreList(savedInstanceState.getStringArrayList(STATE_PENDING), tempPermissionSet);
                    restoreList(savedInstanceState.getStringArrayList(STATE_GRANTED), granted);
                    restoreList(savedInstanceState.getStringArrayList(STATE_REJECTED), rejected);
                    restoreList(savedInstanceState.getStringArrayList(STATE_PERMANENT), permanentlyDenied);
                } else if (permissions != null) {
                    java.util.Collections.addAll(tempPermissionSet, permissions);
                    Permission.adaptPermissions(this, tempPermissionSet);
                }
            }

            if (null == callbackId || null == callback) {
                // Still need a content view before finish on some devices.
                setContentView(new View(this));
                finish();
                return;
            }

            if (resultDispatched) {
                setContentView(new View(this));
                finish();
                return;
            }

            String title = getIntent().getStringExtra(Permission.KEY_TITLE);
            String description = getIntent().getStringExtra(Permission.KEY_DESCRIPTION);
            installExplainUi(title, description);

            if (savedInstanceState == null) {
                requestPermissions();
            } else if (!waitingForSettingsResult) {
                // Config change drops AlertDialogs. Re-show guide if it was on screen.
                if (guidedPermanentDenied && tempPermissionSet.isEmpty() && !permanentlyDenied.isEmpty()) {
                    guidedPermanentDenied = false;
                }
                if (!tempPermissionSet.isEmpty()) {
                    requestPermissions();
                } else {
                    dispatchResult();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (!tempPermissionSet.isEmpty()) {
                rejected.addAll(tempPermissionSet);
                tempPermissionSet.clear();
            }
            if (callback != null && !resultDispatched) {
                if (findViewById(android.R.id.content) == null) {
                    setContentView(new View(this));
                }
                requestPermissions();
            } else {
                finish();
            }
        }
    }

    /**
     * Priority: Callback custom View → global {@link PermissionExplainUi} → layout res / default XML.
     * Custom View paths do not require any library view ids.
     */
    private void installExplainUi(String title, String description) {
        usingCustomExplainUi = false;
        activeExplainUi = null;

        View custom = null;
        try {
            custom = callback.onCreatePermissionExplainView(this, title, description);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (custom != null) {
            setContentView(custom);
            // Do not force insets on custom roots — callers own their layout.
            usingCustomExplainUi = true;
            return;
        }

        PermissionExplainUi globalUi = Permission.getPermissionExplainUi();
        if (globalUi != null) {
            try {
                custom = globalUi.onCreateView(this, title, description);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (custom != null) {
                setContentView(custom);
                usingCustomExplainUi = true;
                activeExplainUi = globalUi;
                return;
            }
        }

        setContentView(Permission.resolvePermissionExplainLayout());
        applyWindowInsets(findViewById(R.id.sp_main));
        bindDefaultDescriptionUi(title, description);
    }

    private void applyWindowInsets(View root) {
        if (root == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void bindDefaultDescriptionUi(String title, String description) {
        TextView titleView = findViewById(R.id.permission_title);
        TextView descriptionView = findViewById(R.id.permission_description);
        View panelView = findViewById(R.id.permission_declare_panel);

        if (titleView != null) {
            if (TextUtils.isEmpty(title)) {
                titleView.setVisibility(View.GONE);
            } else {
                titleView.setText(title);
                titleView.setVisibility(View.VISIBLE);
            }
        }
        if (descriptionView != null) {
            descriptionView.setText(description == null ? "" : description);
        }
        if (panelView != null) {
            panelView.setVisibility(View.VISIBLE);
        }
    }

    private void destroyExplainUi() {
        if (!usingCustomExplainUi) {
            return;
        }
        // Only notify the provider that actually supplied the View.
        if (activeExplainUi != null) {
            try {
                activeExplainUi.onDestroy(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (callback != null) {
            try {
                callback.onDestroyPermissionExplainView(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        activeExplainUi = null;
        usingCustomExplainUi = false;
    }

    private static void restoreList(ArrayList<String> source, List<String> target) {
        target.clear();
        if (source != null) {
            target.addAll(source);
        }
    }

    private static void restoreList(ArrayList<String> source, Set<String> target) {
        target.clear();
        if (source != null) {
            target.addAll(source);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CALLBACK_ID, callbackId);
        outState.putBoolean(STATE_RESULT_DISPATCHED, resultDispatched);
        outState.putBoolean(STATE_GUIDED_PERMANENT, guidedPermanentDenied);
        outState.putBoolean(STATE_WAITING_SETTINGS, waitingForSettingsResult);
        outState.putInt(STATE_PENDING_SETTINGS_CODE, pendingSettingsRequestCode);
        outState.putStringArrayList(STATE_PENDING, new ArrayList<>(tempPermissionSet));
        outState.putStringArrayList(STATE_GRANTED, new ArrayList<>(granted));
        outState.putStringArrayList(STATE_REJECTED, new ArrayList<>(rejected));
        outState.putStringArrayList(STATE_PERMANENT, new ArrayList<>(permanentlyDenied));
    }

    @Override
    public void onBackPressed() {
        if (resultDispatched) {
            super.onBackPressed();
            return;
        }
        // Cancel remaining work and deliver whatever we have.
        for (String permission : new ArrayList<>(tempPermissionSet)) {
            addRejected(permission);
        }
        tempPermissionSet.clear();
        waitingForSettingsResult = false;
        pendingSettingsRequestCode = -1;
        dispatchResultInternal();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Some OEMs return from Settings without delivering onActivityResult, or deliver it
        // before PackageManager has refreshed. Defer so onActivityResult can run first.
        if (waitingForSettingsResult && !resultDispatched && pendingSettingsRequestCode != -1) {
            View decor = getWindow() != null ? getWindow().getDecorView() : null;
            if (decor != null) {
                decor.removeCallbacks(settingsReturnRunnable);
                decor.post(settingsReturnRunnable);
            } else {
                consumeSettingsReturn();
            }
        }
    }

    @Override
    protected void onDestroy() {
        View decor = getWindow() != null ? getWindow().getDecorView() : null;
        if (decor != null) {
            decor.removeCallbacks(settingsReturnRunnable);
            if (delayedSettingsHandleRunnable != null) {
                decor.removeCallbacks(delayedSettingsHandleRunnable);
                delayedSettingsHandleRunnable = null;
            }
        }
        // Only finalize when finishing. Do not callback on process death / non-finishing destroy
        // (Settings navigation or config change) — static maps still hold the callback in-process.
        if (isFinishing() && !resultDispatched && callback != null) {
            if (!tempPermissionSet.isEmpty()) {
                for (String permission : new ArrayList<>(tempPermissionSet)) {
                    addRejected(permission);
                }
                tempPermissionSet.clear();
            }
            dispatchResultInternal();
        }
        super.onDestroy();
    }

    /** Marks that we opened a settings page and expect to resume / get a result. */
    private void markWaitingForSettings(int requestCode) {
        waitingForSettingsResult = true;
        pendingSettingsRequestCode = requestCode;
    }

    /**
     * Handles return from any settings page. Safe to call from onActivityResult and onResume;
     * only the first call wins.
     */
    private void consumeSettingsReturn() {
        if (resultDispatched || !waitingForSettingsResult || pendingSettingsRequestCode == -1) {
            return;
        }
        final int code = pendingSettingsRequestCode;
        waitingForSettingsResult = false;
        pendingSettingsRequestCode = -1;
        // Brief delay: PackageManager / AppOps often lag right when Settings closes.
        View decor = getWindow() != null ? getWindow().getDecorView() : null;
        if (delayedSettingsHandleRunnable != null && decor != null) {
            decor.removeCallbacks(delayedSettingsHandleRunnable);
        }
        delayedSettingsHandleRunnable = () -> {
            delayedSettingsHandleRunnable = null;
            if (!resultDispatched) {
                handleSettingsReturn(code);
            }
        };
        if (decor != null) {
            decor.postDelayed(delayedSettingsHandleRunnable, 250);
        } else {
            delayedSettingsHandleRunnable.run();
        }
    }

    private void handleSettingsReturn(int requestCode) {
        if (REQ_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION == requestCode) {
            tempPermissionSet.remove(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                addGranted(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
            } else {
                addRejected(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
            }
            requestPermissions();
        } else if (REQ_MANAGE_OVERLAY_PERMISSION == requestCode) {
            tempPermissionSet.remove(Manifest.permission.SYSTEM_ALERT_WINDOW);
            if (Permission.hasFloatingWindowPermission(this)) {
                addGranted(Manifest.permission.SYSTEM_ALERT_WINDOW);
            } else {
                addRejected(Manifest.permission.SYSTEM_ALERT_WINDOW);
            }
            requestPermissions();
        } else if (REQ_MANAGE_UNKNOWN_APP_SOURCES == requestCode) {
            tempPermissionSet.remove(Manifest.permission.REQUEST_INSTALL_PACKAGES);
            if (Permission.hasPermission(this, Manifest.permission.REQUEST_INSTALL_PACKAGES)) {
                addGranted(Manifest.permission.REQUEST_INSTALL_PACKAGES);
            } else {
                addRejected(Manifest.permission.REQUEST_INSTALL_PACKAGES);
            }
            requestPermissions();
        } else if (REQ_APP_PERMISSION_SETTINGS == requestCode) {
            recheckAfterSettings();
            // Re-ask the system for anything still missing. After the user grants in
            // Settings, checkSelfPermission can lag on some OEMs; requestPermissions
            // returns GRANTED immediately once the grant is visible to the framework.
            if (!permanentlyDenied.isEmpty() || hasUngrantedRejected()) {
                ArrayList<String> retry = new ArrayList<>();
                retry.addAll(permanentlyDenied);
                for (String permission : new ArrayList<>(rejected)) {
                    if (!retry.contains(permission) && !Permission.hasPermission(this, permission)) {
                        retry.add(permission);
                    }
                }
                permanentlyDenied.clear();
                for (String permission : retry) {
                    rejected.remove(permission);
                    if (!Permission.isSpecialSettingsPermission(permission)) {
                        tempPermissionSet.add(permission);
                    } else if (!tempPermissionSet.contains(permission)) {
                        tempPermissionSet.add(permission);
                    }
                }
                // Keep guidedPermanentDenied=true so we deliver results instead of
                // looping another settings dialog in this same session.
                requestPermissions();
            } else {
                dispatchResultInternal();
            }
        }
    }

    private boolean hasUngrantedRejected() {
        for (String permission : rejected) {
            if (!Permission.hasPermission(this, permission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        try {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (REQ_NORMAL_PERMISSIONS != requestCode && REQ_BACKGROUND_LOCATION != requestCode) {
                if (!resultDispatched) {
                    dispatchResultInternal();
                } else {
                    finish();
                }
                return;
            }
            classifyingFromSystemResult = true;
            try {
                // Mark only after a real system callback so process-death mid-dialog
                // cannot falsely trip isPermanentlyDenied on the next launch.
                PermissionPrefs.markRequested(this, permissions);
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    tempPermissionSet.remove(permission);
                    boolean grantedNow = i < grantResults.length
                            && PackageManager.PERMISSION_GRANTED == grantResults[i];
                    // Android 14+ partial photo/video access: images/video may report DENIED
                    // while READ_MEDIA_VISUAL_USER_SELECTED is granted.
                    if (!grantedNow && Permission.hasPermission(this, permission)) {
                        grantedNow = true;
                    }
                    if (grantedNow) {
                        addGranted(permission);
                    } else {
                        classifyDenied(permission);
                    }
                }
            } finally {
                classifyingFromSystemResult = false;
            }
            requestPermissions();
        } catch (Exception e) {
            e.printStackTrace();
            if (!resultDispatched) {
                dispatchResultInternal();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQ_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                    || requestCode == REQ_MANAGE_OVERLAY_PERMISSION
                    || requestCode == REQ_MANAGE_UNKNOWN_APP_SOURCES
                    || requestCode == REQ_APP_PERMISSION_SETTINGS) {
                if (waitingForSettingsResult) {
                    pendingSettingsRequestCode = requestCode;
                    consumeSettingsReturn();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            waitingForSettingsResult = false;
            pendingSettingsRequestCode = -1;
            if (!resultDispatched) {
                dispatchResultInternal();
            }
        }
    }

    private void recheckAfterSettings() {
        List<String> stillDenied = new ArrayList<>(permanentlyDenied);
        permanentlyDenied.clear();
        for (String permission : stillDenied) {
            if (Permission.hasPermission(this, permission)) {
                rejected.remove(permission);
                addGranted(permission);
            } else {
                // Still missing after Settings — keep as permanent for this session result.
                // Do not use isPermanentlyDenied() here: after a Settings revoke/re-grant cycle
                // its prefs+rationale heuristic can be stale.
                if (!permanentlyDenied.contains(permission)) {
                    permanentlyDenied.add(permission);
                }
            }
        }
        List<String> soft = new ArrayList<>(rejected);
        for (String permission : soft) {
            if (Permission.hasPermission(this, permission)) {
                rejected.remove(permission);
                addGranted(permission);
            }
        }
    }

    private void dispatchResult() {
        if (resultDispatched) {
            return;
        }
        if (!guidedPermanentDenied
                && Permission.isAutoGuideOnPermanentDenied()
                && !permanentlyDenied.isEmpty()
                && callback != null) {
            guidedPermanentDenied = true;
            showConfirmDialog("部分权限已被禁止且不再询问，请到系统设置中开启后再试",
                    new OnConfirmListener() {
                        @Override
                        public void onConfirm() {
                            markWaitingForSettings(REQ_APP_PERMISSION_SETTINGS);
                            if (!PermissionSettings.openAppPermissionSettings(
                                    PermissionActivity.this, REQ_APP_PERMISSION_SETTINGS)) {
                                waitingForSettingsResult = false;
                                pendingSettingsRequestCode = -1;
                                dispatchResultInternal();
                            }
                        }

                        @Override
                        public void onCancel() {
                            dispatchResultInternal();
                        }
                    });
            return;
        }
        dispatchResultInternal();
    }

    private void dispatchResultInternal() {
        if (resultDispatched) {
            return;
        }
        resultDispatched = true;
        waitingForSettingsResult = false;
        destroyExplainUi();
        try {
            if (callback != null) {
                callback.onResult(granted, rejected, permanentlyDenied, extras);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            clearCallbackMaps();
            callback = null;
            if (!isFinishing()) {
                finish();
            }
        }
    }

    private void clearCallbackMaps() {
        if (callbackId != null) {
            Permission.callbackMap.remove(callbackId);
            Permission.permissionMap.remove(callbackId);
            Permission.extraMap.remove(callbackId);
        }
    }

    private void requestPermissions() {
        if (resultDispatched || isFinishing()) {
            return;
        }
        if (tempPermissionSet.isEmpty()) {
            dispatchResult();
            return;
        }

        ArrayList<String> already = new ArrayList<>();
        for (String permission : tempPermissionSet) {
            if (Permission.hasPermission(this, permission)) {
                already.add(permission);
            }
        }
        for (String permission : already) {
            tempPermissionSet.remove(permission);
            addGranted(permission);
        }
        if (tempPermissionSet.isEmpty()) {
            dispatchResult();
            return;
        }

        if (tempPermissionSet.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
            handleManageExternalStorage();
            return;
        }
        if (tempPermissionSet.contains(Manifest.permission.SYSTEM_ALERT_WINDOW)) {
            handleOverlayPermission();
            return;
        }
        if (tempPermissionSet.contains(Manifest.permission.REQUEST_INSTALL_PACKAGES)) {
            handleInstallPackages();
            return;
        }

        if (tempPermissionSet.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            Set<String> others = new LinkedHashSet<>(tempPermissionSet);
            others.remove(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            if (!others.isEmpty()) {
                requestNormalPermissions(others, REQ_NORMAL_PERMISSIONS);
                return;
            }
            handleBackgroundLocation();
            return;
        }

        // Always go through the system API for remaining runtime permissions.
        // Do NOT peel "permanently denied" via prefs alone — after the user grants in
        // Settings, skipping requestPermissions causes a stuck "go to settings" loop.
        requestNormalPermissions(new LinkedHashSet<>(tempPermissionSet), REQ_NORMAL_PERMISSIONS);
    }

    private void handleManageExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            addGranted(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
            tempPermissionSet.remove(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
            requestPermissions();
            return;
        }
        showConfirmDialog("您的手机没有授予文件管理权限，请开启后再试", new OnConfirmListener() {
            @Override
            public void onConfirm() {
                markWaitingForSettings(REQ_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                if (!startManageAllFilesAccess()) {
                    waitingForSettingsResult = false;
                    pendingSettingsRequestCode = -1;
                    addRejected(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                    tempPermissionSet.remove(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                    requestPermissions();
                }
            }

            @Override
            public void onCancel() {
                addRejected(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                tempPermissionSet.remove(Manifest.permission.MANAGE_EXTERNAL_STORAGE);
                requestPermissions();
            }
        });
    }

    private boolean startManageAllFilesAccess() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQ_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            return true;
        } catch (Exception first) {
            try {
                Intent fallback = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(fallback, REQ_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                return true;
            } catch (Exception second) {
                return PermissionSettings.openAppPermissionSettings(this, REQ_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            }
        }
    }

    private void handleOverlayPermission() {
        if (Permission.hasFloatingWindowPermission(this)) {
            addGranted(Manifest.permission.SYSTEM_ALERT_WINDOW);
            tempPermissionSet.remove(Manifest.permission.SYSTEM_ALERT_WINDOW);
            requestPermissions();
            return;
        }
        showConfirmDialog("您的手机没有授予悬浮窗权限，请开启后再试", new OnConfirmListener() {
            @Override
            public void onConfirm() {
                markWaitingForSettings(REQ_MANAGE_OVERLAY_PERMISSION);
                if (!FloatWindowUtil.applyPermission(PermissionActivity.this, REQ_MANAGE_OVERLAY_PERMISSION)) {
                    waitingForSettingsResult = false;
                    pendingSettingsRequestCode = -1;
                    addRejected(Manifest.permission.SYSTEM_ALERT_WINDOW);
                    tempPermissionSet.remove(Manifest.permission.SYSTEM_ALERT_WINDOW);
                    requestPermissions();
                }
            }

            @Override
            public void onCancel() {
                addRejected(Manifest.permission.SYSTEM_ALERT_WINDOW);
                tempPermissionSet.remove(Manifest.permission.SYSTEM_ALERT_WINDOW);
                requestPermissions();
            }
        });
    }

    private void handleInstallPackages() {
        if (Permission.hasPermission(this, Manifest.permission.REQUEST_INSTALL_PACKAGES)) {
            addGranted(Manifest.permission.REQUEST_INSTALL_PACKAGES);
            tempPermissionSet.remove(Manifest.permission.REQUEST_INSTALL_PACKAGES);
            requestPermissions();
            return;
        }
        showConfirmDialog("您的手机没有授予安装应用权限，请开启后再试", new OnConfirmListener() {
            @Override
            public void onConfirm() {
                markWaitingForSettings(REQ_MANAGE_UNKNOWN_APP_SOURCES);
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQ_MANAGE_UNKNOWN_APP_SOURCES);
                } catch (Exception e) {
                    if (!PermissionSettings.openAppPermissionSettings(
                            PermissionActivity.this, REQ_MANAGE_UNKNOWN_APP_SOURCES)) {
                        waitingForSettingsResult = false;
                        pendingSettingsRequestCode = -1;
                        addRejected(Manifest.permission.REQUEST_INSTALL_PACKAGES);
                        tempPermissionSet.remove(Manifest.permission.REQUEST_INSTALL_PACKAGES);
                        requestPermissions();
                    }
                }
            }

            @Override
            public void onCancel() {
                addRejected(Manifest.permission.REQUEST_INSTALL_PACKAGES);
                tempPermissionSet.remove(Manifest.permission.REQUEST_INSTALL_PACKAGES);
                requestPermissions();
            }
        });
    }

    private void handleBackgroundLocation() {
        boolean hasForeground = Permission.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                || Permission.hasPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                || granted.contains(Manifest.permission.ACCESS_FINE_LOCATION)
                || granted.contains(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (!hasForeground) {
            // Cannot request background location without foreground — soft reject (not permanent).
            addRejected(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            tempPermissionSet.remove(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            requestPermissions();
            return;
        }
        Set<String> only = new LinkedHashSet<>();
        only.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        requestNormalPermissions(only, REQ_BACKGROUND_LOCATION);
    }

    private void requestNormalPermissions(Set<String> permissions, int requestCode) {
        String[] array = permissions.toArray(new String[0]);
        if (array.length == 0) {
            requestPermissions();
            return;
        }
        // Do NOT markRequested here — if the process dies before the system result,
        // a premature mark would make isPermanentlyDenied() true and skip the dialog forever.
        try {
            ActivityCompat.requestPermissions(this, array, requestCode);
        } catch (Exception e) {
            e.printStackTrace();
            for (String permission : array) {
                tempPermissionSet.remove(permission);
                addRejected(permission);
            }
            requestPermissions();
        }
    }

    /**
     * After a real system permission callback:
     * - rationale == true  → temporary deny
     * - rationale == false → don't ask again / silent deny (including first-time checkbox)
     * Non-result paths never mark permanent.
     */
    private void classifyDenied(String permission) {
        rejected.remove(permission);
        permanentlyDenied.remove(permission);
        granted.remove(permission);
        boolean permanent = classifyingFromSystemResult
                && !Permission.isSpecialSettingsPermission(permission)
                && !ActivityCompat.shouldShowRequestPermissionRationale(this, permission);
        if (permanent) {
            if (!permanentlyDenied.contains(permission)) {
                permanentlyDenied.add(permission);
            }
        } else {
            addRejected(permission);
        }
    }

    private void showConfirmDialog(String message, OnConfirmListener listener) {
        if (callback == null || isFinishing()) {
            listener.onCancel();
            return;
        }
        try {
            callback.onShowConfirmDialog(this, message, listener);
        } catch (Exception e) {
            e.printStackTrace();
            listener.onCancel();
        }
    }

    private void addGranted(String permission) {
        rejected.remove(permission);
        permanentlyDenied.remove(permission);
        if (!granted.contains(permission)) {
            granted.add(permission);
        }
        // Permission was granted (system dialog or Settings) — reset request history so a
        // later revoke + re-request is not falsely treated as "Don't ask again".
        PermissionPrefs.clearRequested(this, permission);
    }

    private void addRejected(String permission) {
        granted.remove(permission);
        permanentlyDenied.remove(permission);
        if (!rejected.contains(permission)) {
            rejected.add(permission);
        }
    }
}
