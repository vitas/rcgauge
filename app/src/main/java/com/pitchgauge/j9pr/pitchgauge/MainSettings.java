package com.pitchgauge.j9pr.pitchgauge;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

import java.security.cert.CertPathChecker;

public class MainSettings extends AppCompatActivity {

    MainPrefs prefs = new MainPrefs();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_settings);

        // read preferences
        prefs = BluetoothPreferences.getMainPrefs(getApplicationContext());

        RadioButton rButton1 = (RadioButton) findViewById(R.id.prefsSensorNoZ);
        RadioButton rButton2 = (RadioButton) findViewById(R.id.prefsSensorAll);

        if (prefs.getzMode() == MainPrefs.zmodeT.IGNORE) {
            rButton1.setChecked(true);
        } else if (prefs.getzMode() == MainPrefs.zmodeT.FULL) {
            rButton2.setChecked(true);
        }

    }

    public void onRadioButtonClicked1(View view) {
        prefs.setzMode(MainPrefs.zmodeT.IGNORE);
        BluetoothPreferences.setMainPrefs(getApplicationContext(), prefs);
    }

    public void onRadioButtonClicked2(View view) {
        prefs.setzMode(MainPrefs.zmodeT.FULL);
        BluetoothPreferences.setMainPrefs(getApplicationContext(), prefs);
    }
}