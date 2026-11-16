package com.pitchgauge.j9pr.pitchgauge;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** ManifestPropertiesTest checks the properties the app declares in its manifest for the system. */
@RunWith(AndroidJUnit4.class)
public class ManifestPropertiesTest {

    // On large screens, Android 16 (API 36) ignores the app's portrait lock unless the app opts
    // out with this property. Manifest properties can be read back from Android 12 (API 31).
    @Test
    public void allowsRestrictedResizability_onLargeScreens()
            throws PackageManager.NameNotFoundException {
        assumeTrue("manifest properties can be read from Android 12",
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S);
        Context context = ApplicationProvider.getApplicationContext();
        assertTrue(context.getPackageManager()
                .getProperty("android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY",
                        context.getPackageName())
                .getBoolean());
    }
}
