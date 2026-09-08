package com.dlkw.permissions;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Fully custom permission-explain UI. Return any View hierarchy — no library view ids required.
 * Return {@code null} to fall back to the next strategy (layout res / library default).
 */
public interface PermissionExplainUi {

    /**
     * Creates the explain panel shown while the system permission dialog is visible.
     *
     * @param activity    host {@link PermissionActivity}
     * @param title       explain title (may be empty)
     * @param description usage description from {@link Permission#request}
     * @return custom content view, or {@code null} to use default UI
     */
    @Nullable
    View onCreateView(@NonNull Activity activity, @Nullable String title, @Nullable String description);

    /** Optional cleanup when the permission Activity is finishing. */
    default void onDestroy(@NonNull Activity activity) {
    }
}
