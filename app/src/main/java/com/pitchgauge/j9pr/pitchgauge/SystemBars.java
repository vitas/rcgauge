package com.pitchgauge.j9pr.pitchgauge;

import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * SystemBars keeps a screen that uses the theme's action bar looking as it did before Android 15.
 *
 * <p>Android 15 and 16 draw apps edge to edge, under transparent system bars. There, applyTo paints
 * a colorPrimaryDark strip behind the status bar, keeps the status bar icons white, and pads the
 * screen's content so it starts below the action bar and ends above the navigation bar. On older
 * versions it does nothing.
 */
final class SystemBars {

    private SystemBars() {
    }

    // applyTo must be called after setContentView.
    static void applyTo(AppCompatActivity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            return;
        }
        Window window = activity.getWindow();
        StatusBarStrip background = new StatusBarStrip(themeWindowBackground(activity),
                ContextCompat.getColor(activity, R.color.colorPrimaryDark));
        window.setBackgroundDrawable(background);
        WindowCompat.getInsetsController(window, window.getDecorView())
                .setAppearanceLightStatusBars(false);

        ViewCompat.setOnApplyWindowInsetsListener(activity.findViewById(android.R.id.content),
                (content, insets) -> {
                    // AppCompat adds the action bar's height to the top inset it passes here.
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
                    content.setPadding(bars.left, bars.top, bars.right, bars.bottom);

                    // The strip follows the real status bar, so it is read from the window's
                    // insets rather than the ones AppCompat changed.
                    WindowInsetsCompat rootInsets = ViewCompat.getRootWindowInsets(content);
                    background.setHeight(rootInsets == null
                            ? 0 : rootInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top);
                    return insets;
                });
    }

    private static Drawable themeWindowBackground(AppCompatActivity activity) {
        TypedArray a = activity.obtainStyledAttributes(new int[] {android.R.attr.windowBackground});
        try {
            Drawable d = a.getDrawable(0);
            return d != null ? d : new ColorDrawable(Color.TRANSPARENT);
        } finally {
            a.recycle();
        }
    }

    /** StatusBarStrip draws the window background with a strip of one color across its top. */
    private static final class StatusBarStrip extends Drawable {

        private final Drawable windowBackground;
        private final Paint strip = new Paint();
        private int height;

        StatusBarStrip(Drawable windowBackground, int color) {
            this.windowBackground = windowBackground;
            strip.setColor(color);
        }

        void setHeight(int height) {
            if (this.height != height) {
                this.height = height;
                invalidateSelf();
            }
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect bounds = getBounds();
            windowBackground.setBounds(bounds);
            windowBackground.draw(canvas);
            canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.top + height, strip);
        }

        @Override
        public void setAlpha(int alpha) {
            // The window background is always drawn fully opaque.
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            // The window background is never filtered.
        }

        // getOpacity is deprecated but still abstract; the window uses it to decide whether the
        // app's window is opaque.
        @SuppressWarnings("deprecation")
        @Override
        public int getOpacity() {
            return windowBackground.getOpacity();
        }
    }
}
