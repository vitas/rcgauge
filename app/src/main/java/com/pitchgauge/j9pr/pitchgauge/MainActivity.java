package com.pitchgauge.j9pr.pitchgauge;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import com.google.android.material.navigation.NavigationView;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;
import android.webkit.WebView;


import java.util.ArrayList;

import static com.pitchgauge.j9pr.pitchgauge.BluetoothPipe.DEVICE_BT;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int REQ_THROW_ACTIVITY = 10010;
    private static final int REQ_DATA_ACTIVITY  = 10011;

    private static final String TAG = "MainActivity";
    public ArrayList<DeviceTag> devices;

    final String[] PERMISSIONS = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
    };
    private static boolean permissionsDenied = false;
    private ActivityResultContracts.RequestMultiplePermissions multiplePermissionsContract;
    private ActivityResultLauncher<String[]> multiplePermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } catch (IllegalStateException e) {
            // Only fullscreen activities can request orientation
        }

		// app permissions dialog
        multiplePermissionsContract = new ActivityResultContracts.RequestMultiplePermissions();
        multiplePermissionLauncher = registerForActivityResult(multiplePermissionsContract, isGranted -> {
            Log.d("PERMISSIONS", "Launcher result: " + isGranted.toString());
            if (isGranted.containsValue(false)) {
                Log.d("PERMISSIONS", "At least one of the permissions was not granted, launching again...");
                multiplePermissionLauncher.launch(PERMISSIONS);
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        WebView wv = (WebView) findViewById(R.id.throwmeterdoc);
        wv.loadUrl("file:///android_asset/Make-your-own-BlueTooth-RC-Throwmeter-V2.0.htm");

        // Use this check to determine whether Bluetooth classic is supported on the device.
        boolean bluetoothAvailable = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

		// ask user for app permissions
        askPermissions(multiplePermissionLauncher);

    }

	// permissions check
    private void askPermissions(ActivityResultLauncher<String[]> multiplePermissionLauncher) {
        if (!hasPermissions(PERMISSIONS)) {
            Log.d("PERMISSIONS", "Launching multiple contract permission launcher for ALL required permissions");
            multiplePermissionLauncher.launch(PERMISSIONS);
        } else {
            Log.d("PERMISSIONS", "All permissions are already granted");
        }
    }

    private boolean hasPermissions(String[] permissions) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    Log.d("PERMISSIONS", "Permission is not granted: " + permission);
                    return false;
                }
                Log.d("PERMISSIONS", "Permission already granted: " + permission);
            }
            return true;
        }
        return false;
    }



    @Override
	// permissions result callback (not used)
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], int[] grantResults) {

        for (int i = 0; i < permissions.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, permissions[i] + " permission denied");
                permissionsDenied = true;
            } else {
                Log.d(TAG, permissions[i] + " permission granted");
            }
        }
        return;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        Resources res = getResources();

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(res.getString(R.string.txt_closing) + " " + res.getString(R.string.app_name))
                    .setMessage(res.getString(R.string.txt_remind_sensor_power))
                    .setPositiveButton("OK", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            //super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        Resources res = getResources();

		// warning if user has denied permissions, android app can not ask again 
        if (!hasPermissions(PERMISSIONS)) {
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(res.getString(R.string.txt_missing_permission) + " " + res.getString(R.string.app_name))
                        .setMessage(res.getString(R.string.txt_notify_nearby_devices))
                        .setPositiveButton("Exit", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            drawer.closeDrawer(GravityCompat.START);
            return true;
        }

        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_throwmeter) { // Throw Meter
            Intent intent = new Intent(MainActivity.this, ThrowActivity.class);
            if (this.devices != null) {
                intent.putParcelableArrayListExtra(DEVICE_BT, this.devices );
                startActivityForResult(intent, REQ_THROW_ACTIVITY);
            } else {
                ArrayList<DeviceTag> savedTags = BluetoothPreferences.getKeyrings(getApplicationContext());
                if (!savedTags.isEmpty()) {
                    intent.putParcelableArrayListExtra(DEVICE_BT, savedTags );
                    startActivityForResult(intent, REQ_THROW_ACTIVITY);
                } else {
                    intent.setClass(MainActivity.this, DeviceListActivity.class);
                    startActivityForResult(intent, REQ_DATA_ACTIVITY);
                }
            }
        } else if (id == R.id.nav_BT) { // DeviceList / BT Sensors
            Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
            startActivityForResult(intent, REQ_DATA_ACTIVITY);
        } else if (id == R.id.nav_settings) { // Preferences/MainSettings
            Intent intent = new Intent(MainActivity.this, MainSettings.class);
            startActivityForResult(intent, REQ_DATA_ACTIVITY);
        }


        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_DATA_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        this.devices = data.getParcelableArrayListExtra(DEVICE_BT);
                        return;
                    }
                }
                return;
            default:
                return;
        }
    }
}
