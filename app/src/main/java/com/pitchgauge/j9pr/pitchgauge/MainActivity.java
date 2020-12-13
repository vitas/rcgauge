package com.pitchgauge.j9pr.pitchgauge;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import java.util.ArrayList;

import static com.pitchgauge.j9pr.pitchgauge.BluetoothPipe.DEVICE_BT;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int REQ_THROW_ACTIVITY = 10010;
    private static final int REQ_DATA_ACTIVITY  = 10011;

    public ArrayList<DeviceTag> devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } catch (IllegalStateException e) {
            // Only fullscreen activities can request orientation
        }

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

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_throwmeter) {
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
        } else if (id == R.id.nav_BT) {
            Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
            startActivityForResult(intent, REQ_DATA_ACTIVITY);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
