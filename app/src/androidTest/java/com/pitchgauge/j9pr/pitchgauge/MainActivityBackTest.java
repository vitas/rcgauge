package com.pitchgauge.j9pr.pitchgauge;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.SystemClock;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.Lifecycle;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

/**
 * MainActivityBackTest checks that back on the main screen closes an open drawer, and otherwise
 * shows the reminder to switch off the sensors, so the app is never left without the user's OK.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityBackTest {

    private static final long TIMEOUT_MS = 5000;

    // The nearby-devices permissions exist from Android 12 (API 31); granting them up front keeps
    // the permission dialog from covering the main screen.
    @Rule(order = 0)
    public final TestRule permissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ? GrantPermissionRule.grant(
                    Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            : RuleChain.emptyRuleChain();

    @Rule(order = 1)
    public final ActivityScenarioRule<MainActivity> rule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void back_withDrawerOpen_closesDrawerAndStays() {
        rule.getScenario().onActivity(a -> drawer(a).openDrawer(GravityCompat.START, false));

        pressBack();

        waitUntil(() -> !isDrawerOpen());
        assertFalse("drawer is still open after back", isDrawerOpen());
        assertEquals(Lifecycle.State.RESUMED, rule.getScenario().getState());
        onView(withText(R.string.txt_remind_sensor_power)).check(doesNotExist());
    }

    @Test
    public void back_onMainScreen_showsSensorPowerReminder() {
        pressBack();

        onView(withText(R.string.txt_remind_sensor_power)).inRoot(isDialog())
                .check(matches(isDisplayed()));
    }

    @Test
    public void reminderOk_finishesApp() {
        pressBack();
        onView(withText("OK")).inRoot(isDialog()).perform(click());

        waitUntil(() -> rule.getScenario().getState() == Lifecycle.State.DESTROYED);
        assertEquals(Lifecycle.State.DESTROYED, rule.getScenario().getState());
    }

    @Test
    public void reminderCancel_staysOnMainScreen() {
        pressBack();
        onView(withText("Cancel")).inRoot(isDialog()).perform(click());

        assertEquals(Lifecycle.State.RESUMED, rule.getScenario().getState());
        onView(withText(R.string.txt_remind_sensor_power)).check(doesNotExist());
    }

    @Test
    public void back_whileReminderShowing_dismissesReminderAndStays() {
        pressBack();
        onView(withText(R.string.txt_remind_sensor_power)).inRoot(isDialog())
                .check(matches(isDisplayed()));

        pressBack();

        assertEquals(Lifecycle.State.RESUMED, rule.getScenario().getState());
        onView(withText(R.string.txt_remind_sensor_power)).check(doesNotExist());
    }

    private boolean isDrawerOpen() {
        boolean[] open = new boolean[1];
        rule.getScenario().onActivity(a -> open[0] = drawer(a).isDrawerOpen(GravityCompat.START));
        return open[0];
    }

    private static DrawerLayout drawer(Activity a) {
        return a.findViewById(R.id.drawer_layout);
    }

    /** Condition is a check that waitUntil repeats until it holds. */
    private interface Condition {
        boolean holds();
    }

    // waitUntil polls the condition until it holds or TIMEOUT_MS passes; the caller asserts after.
    // The drawer's close animation and the activity's destruction finish after Espresso is idle.
    private static void waitUntil(Condition condition) {
        long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;
        while (!condition.holds() && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(50);
        }
    }
}
