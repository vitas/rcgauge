package com.pitchgauge.j9pr.pitchgauge;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;

public class MainSettings extends AppCompatActivity {

    MainPrefs prefs = new MainPrefs();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_settings);

        // read preferences
        prefs = BluetoothPreferences.getMainPrefs(getApplicationContext());

        // Sensor Axes
        RadioButton rButtonAxes1 = (RadioButton) findViewById(R.id.prefsSensorNoZ);
        RadioButton rButtonAxes2 = (RadioButton) findViewById(R.id.prefsSensorAll);
        switch (prefs.getzMode()) {
            case IGNORE:
                rButtonAxes1.setChecked(true);
                break;
            case FULL:
                rButtonAxes2.setChecked(true);
                break;
        }

        // Throw Calculation Method
        RadioButton rButtonCalcMethod1 = (RadioButton) findViewById(R.id.prefsThrowCalcOrtho);
        RadioButton rButtonCalcMethod2 = (RadioButton) findViewById(R.id.prefsThrowCalcChord);
        switch (prefs.getThrowCalcMethod()) {
            case ORTHO:
                rButtonCalcMethod1.setChecked(true);
                break;
            case CHORD:
                rButtonCalcMethod2.setChecked(true);
                break;
        }

        // Sensor Type selection
        RadioButton rButtonSensor1 = (RadioButton) findViewById(R.id.prefsSensorAUTO);
        RadioButton rButtonSensor2 = (RadioButton) findViewById(R.id.prefsSensorBWT61CL);
        RadioButton rButtonSensor3 = (RadioButton) findViewById(R.id.prefsSensorBWT901CL);
        switch (prefs.getWitModel()) {
            case AUTO:
                rButtonSensor1.setChecked(true);
                break;
            case BWT61CL:
                rButtonSensor2.setChecked(true);
                break;
            case BWT901CL:
                rButtonSensor3.setChecked(true);
                break;
        }

        // Sensor Configuration
//        RadioButton rButtonSensorConfig1 = (RadioButton) findViewById(R.id.prefsSensorConfigAUTO);
//        RadioButton rButtonSensorConfig2 = (RadioButton) findViewById(R.id.prefsSensorConfigMANUAL);
//        switch (prefs.getSensorConfigMode()) {
//            case AUTO:
//                rButtonSensorConfig1.setChecked(true);
//                break;
//            case MANUAL:
//                rButtonSensorConfig2.setChecked(true);
//                break;
//        }

    }

    // Sensor Axes
    public void onSensorAxesRadioButtonClicked1(View view) {
        prefs.setzMode(MainPrefs.zmodeT.IGNORE);
        BluetoothPreferences.setMainPrefs(getApplicationContext(), prefs);
    }
    public void onSensorAxesRadioButtonClicked2(View view) {
        prefs.setzMode(MainPrefs.zmodeT.FULL);
        BluetoothPreferences.setMainPrefs(getApplicationContext(), prefs);
    }

    // Throw Calc Method
    public void onThrowCalcRadioButtonClicked1(View view) {
        prefs.setThrowCalcMethod(MainPrefs.throwCalcMethodT.ORTHO);
        BluetoothPreferences.setMainPrefs(getApplicationContext(), prefs);
    }
    public void onThrowCalcRadioButtonClicked2(View view) {
        prefs.setThrowCalcMethod(MainPrefs.throwCalcMethodT.CHORD);
        BluetoothPreferences.setMainPrefs(getApplicationContext(), prefs);
    }

    // Sensor Model
    public void onSensorModelRadioButtonClicked1(View view) {
        prefs.setWitModel(MainPrefs.witModelT.AUTO);
        BluetoothPreferences.setMainPrefs(getApplicationContext(), prefs);
    }

    public void onSensorModelRadioButtonClicked2(View view) {
        prefs.setWitModel(MainPrefs.witModelT.BWT61CL);
        BluetoothPreferences.setMainPrefs(getApplicationContext(), prefs);
    }

    public void onSensorModelRadioButtonClicked3(View view) {
        prefs.setWitModel(MainPrefs.witModelT.BWT901CL);
        BluetoothPreferences.setMainPrefs(getApplicationContext(), prefs);
    }

    // Sensor Configuration Mode
    public void onSensorConfigRadioButtonClicked1(View view) {
        prefs.setSensorConfigMode(MainPrefs.sensorConfigModeT.AUTO);
        BluetoothPreferences.setMainPrefs(getApplicationContext(), prefs);
    }

    public void onSensorConfigRadioButtonClicked2(View view) {
        prefs.setSensorConfigMode(MainPrefs.sensorConfigModeT.MANUAL);
        BluetoothPreferences.setMainPrefs(getApplicationContext(), prefs);
    }

}