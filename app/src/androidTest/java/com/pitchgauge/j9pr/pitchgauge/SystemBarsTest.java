package com.pitchgauge.j9pr.pitchgauge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

/**
 * SystemBarsTest checks that every screen keeps its top bar just below the status bar, its content
 * just above the navigation bar, and a teal status bar strip with white icons. Android 15 and 16
 * draw apps edge to edge, under both bars, so this is where they would differ.
 */
@RunWith(AndroidJUnit4.class)
public class SystemBarsTest {

    private static final long BLUETOOTH_TIMEOUT_MS = 5000;

    // The nearby-devices permissions exist from Android 12 (API 31); granting them up front keeps
    // the permission dialog from covering the screens.
    @Rule
    public final TestRule permissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ? GrantPermissionRule.grant(
                    Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            : RuleChain.emptyRuleChain();

    private static BluetoothAdapter adapter;

    // enableBluetooth switches Bluetooth on: with it off, the device list and throw screens ask to
    // turn it on, and that system dialog would cover them.
    @BeforeClass
    public static void enableBluetooth() throws IOException {
        Context context = ApplicationProvider.getApplicationContext();
        BluetoothManager manager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager == null ? null : manager.getAdapter();
        if (adapter == null) {
            return;
        }
        ParcelFileDescriptor output = InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().executeShellCommand("svc bluetooth enable");
        try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(output)) {
            byte[] buffer = new byte[256];
            while (in.read(buffer) != -1) {
                // Drain the output so the command has finished before waiting on the adapter.
            }
        }
        long deadline = SystemClock.uptimeMillis() + BLUETOOTH_TIMEOUT_MS;
        while (!adapter.isEnabled() && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(100);
        }
    }

    @Test
    public void deviceList_staysClearOfSystemBars() {
        assumeTrue("this device has no Bluetooth", adapter != null);
        try (ActivityScenario<DeviceListActivity> scenario =
                ActivityScenario.launch(DeviceListActivity.class)) {
            scenario.onActivity(SystemBarsTest::assertActionBarScreenClearOfSystemBars);
        }
    }

    @Test
    public void settings_staysClearOfSystemBars() {
        try (ActivityScenario<MainSettings> scenario =
                ActivityScenario.launch(MainSettings.class)) {
            scenario.onActivity(SystemBarsTest::assertActionBarScreenClearOfSystemBars);
        }
    }

    @Test
    public void throwScreen_staysClearOfSystemBars() {
        assumeTrue("this device has no Bluetooth", adapter != null);
        DeviceTag tag = new DeviceTag();
        tag.setAddress("00:11:22:AA:BB:CC");
        tag.setName("Test sensor");
        tag.setPos(0);
        ArrayList<DeviceTag> tags = new ArrayList<>();
        tags.add(tag);
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ThrowActivity.class)
                .putParcelableArrayListExtra(BluetoothPipe.DEVICE_BT, tags);
        try (ActivityScenario<ThrowActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(SystemBarsTest::assertActionBarScreenClearOfSystemBars);
        }
    }

    @Test
    public void mainScreen_staysClearOfSystemBars() {
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> assertClearOfSystemBars(a,
                    a.findViewById(R.id.toolbar), a.findViewById(R.id.throwmeterdocscroll)));
        }
    }

    // assertActionBarScreenClearOfSystemBars checks a screen whose top bar is the theme's action
    // bar and whose content is the screen's own layout.
    private static void assertActionBarScreenClearOfSystemBars(Activity a) {
        ViewGroup content = a.findViewById(android.R.id.content);
        assertClearOfSystemBars(a, a.findViewById(androidx.appcompat.R.id.action_bar_container),
                content.getChildAt(0));
    }

    // assertClearOfSystemBars runs on the main thread. Positions are measured from the top of the
    // window, which on a phone is the top of the screen.
    private static void assertClearOfSystemBars(Activity a, View topBar, View content) {
        View decor = a.getWindow().getDecorView();
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decor);
        assertNotNull("window insets", insets);
        int statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
        int navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
        assertTrue("the screen has a status bar", statusTop > 0);

        int barTop = topOf(topBar) - topOf(decor);
        int contentTop = topOf(content) - topOf(decor);
        assertEquals("top bar starts right below the status bar", statusTop, barTop);
        assertEquals("content starts right below the top bar",
                barTop + topBar.getHeight(), contentTop);
        assertEquals("content ends right above the navigation bar",
                decor.getHeight() - navBottom, contentTop + content.getHeight());

        Bitmap strip = Bitmap.createBitmap(decor.getWidth(), statusTop, Bitmap.Config.ARGB_8888);
        decor.draw(new Canvas(strip));
        int stripColor = strip.getPixel(decor.getWidth() / 2, statusTop / 2);
        strip.recycle();
        assertEquals("status bar strip color",
                hex(ContextCompat.getColor(a, R.color.colorPrimaryDark)), hex(stripColor));

        assertFalse("status bar icons are dark instead of white",
                WindowCompat.getInsetsController(a.getWindow(), decor)
                        .isAppearanceLightStatusBars());
    }

    private static int topOf(View v) {
        int[] location = new int[2];
        v.getLocationOnScreen(location);
        return location[1];
    }

    private static String hex(int color) {
        return String.format("#%08X", color);
    }
}
