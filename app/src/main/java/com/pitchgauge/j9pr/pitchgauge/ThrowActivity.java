package com.pitchgauge.j9pr.pitchgauge;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.arch.lifecycle.Observer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.pitchgauge.j9pr.pitchgauge.databinding.ThrowActivityBinding;

import java.util.ArrayList;

import static com.pitchgauge.j9pr.pitchgauge.BluetoothPipe.REQUEST_CONNECT_DEVICE;
import static com.pitchgauge.j9pr.pitchgauge.BluetoothPipe.REQUEST_ENABLE_BT;

public class ThrowActivity extends BluetoothBaseActivity {


    private ThrowGaugeViewModel mGaugeViewModel;
    private final Handler mSendSensor = new SendSensor();
    int RunMode = 0;
    private short sOffsetAccX;
    private short sOffsetAccY;
    private short sOffsetAccZ;
    int iCurrentGroup = 3;
    private int ar = 16;
    int arithmetic = 0;
    private int av = 2000;
    public byte[] writeBuffer;
    public byte[] readBuffer;
    private int type;
    private boolean isOpen;
    private EditText input;

    private enum dialogType {
        T_LIMIT, T_CHORD, T_CALIBRATE
    }

    ArrayList<DeviceTag> devicePrefs = new ArrayList<DeviceTag>();

    private boolean busyReset = false;
    private boolean busyCalibration = false;

    private btStatusWatcherClass btWatcher = new btStatusWatcherClass();

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CONNECT_DEVICE) {
            if(resultCode == Activity.RESULT_OK) {
                //TODO
                // mBluetoothPipe.connect(data);
            }
        } else if(requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_OK) {
                mBluetoothPipe.setupService(mBluetoothService, mHandler);
            } else {
                Toast.makeText(getApplicationContext()
                        , getApplication().getString(R.string.warning_bluetooth_not_enabled)
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    class DataHandler extends Handler {

        public void handleMessage(Message msg) {
            switch (msg.what) {

                case BluetoothState.MESSAGE_READ:
                    try {
                        float[] fData = msg.getData().getFloatArray("Data");
                        int pos = msg.getData().getInt("Pos");
                        switch (ThrowActivity.this.RunMode) {
                            case 0:
                                switch (ThrowActivity.this.iCurrentGroup) {
                                    case 0:
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum1)).setText(msg.getData().getString("Date"));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum2)).setText(msg.getData().getString("Time"));
                                        return;
                                    case 1:
                                    case 2:
                                    case 3:
                                        fData[0] = fData[0] * ((float) ThrowActivity.this.ar);
                                        fData[1] = fData[1] * ((float) ThrowActivity.this.ar);
                                        fData[2] = fData[2] * ((float) ThrowActivity.this.ar);
                                        ThrowActivity.this.mGaugeViewModel.setAccelerations(pos, Float.valueOf(fData[0]), Float.valueOf(fData[1]), Float.valueOf(fData[2]));
                                        fData[3] = fData[3] * ((float) ThrowActivity.this.av);
                                        fData[4] = fData[4] * ((float) ThrowActivity.this.av);
                                        fData[5] = fData[5] * ((float) ThrowActivity.this.av);
                                        ThrowActivity.this.mGaugeViewModel. setVelocities(pos, Float.valueOf(fData[3]), Float.valueOf(fData[4]), Float.valueOf(fData[5]));
                                        // Roll Pitch Yaw
                                        ThrowActivity.this.mGaugeViewModel.setAngles(pos, Float.valueOf(fData[6]), Float.valueOf(fData[7]), Float.valueOf(fData[8]));
                                        return;
                                    default:
                                        return;
                                }
                            case 1:
                                //((TextView) DataMonitor.this.findViewById(C0242R.id.tvAccX)).setText(String.format("% 10.2fg", new Object[]{Float.valueOf(fData[0])}));
                                ThrowActivity.this.sOffsetAccX = (short) ((int) ((fData[0] / 16.0f) * 32768.0f));
                                //((TextView) DataMonitor.this.findViewById(C0242R.id.tvAccY)).setText(String.format("% 10.2fg", new Object[]{Float.valueOf(fData[1])}));
                                ThrowActivity.this.sOffsetAccY = (short) ((int) ((fData[1] / 16.0f) * 32768.0f));
                                //((TextView) DataMonitor.this.findViewById(C0242R.id.tvAccZ)).setText(String.format("% 10.2fg", new Object[]{Float.valueOf(fData[2])}));
                                ThrowActivity.this.sOffsetAccZ = (short) ((int) (((fData[2] - 1.0f) / 16.0f) * 32768.0f));
                                return;
                            case 2:
                                //((TextView) DataMonitor.this.findViewById(C0242R.id.tvGyroX)).setText(String.format("% 10.2f°/s", new Object[]{Float.valueOf(fData[3])}));
                                //((TextView) DataMonitor.this.findViewById(C0242R.id.tvGyroY)).setText(String.format("% 10.2f°/s", new Object[]{Float.valueOf(fData[4])}));
                                //((TextView) DataMonitor.this.findViewById(C0242R.id.tvGyroZ)).setText(String.format("% 10.2f°/s", new Object[]{Float.valueOf(fData[5])}));
                                return;
                            case 3:
                                //((TextView) DataMonitor.this.findViewById(C0242R.id.tvMagX)).setText(String.format("% 10.0f", new Object[]{Float.valueOf(fData[9])}));
                                //((TextView) DataMonitor.this.findViewById(C0242R.id.tvMagY)).setText(String.format("% 10.0f", new Object[]{Float.valueOf(fData[10])}));
                                //((TextView) DataMonitor.this.findViewById(C0242R.id.tvMagZ)).setText(String.format("% 10.0f", new Object[]{Float.valueOf(fData[11])}));
                                return;
                            default:
                                return;
                        }
                    } catch (Exception e) {
                        return;
                    }

                default:
                    return;
            }
        }
    }

    class SendSensor extends Handler {
        SendSensor() {
        }

        public void handleMessage(Message msg) {

            switch (msg.what) {
                case BluetoothState.MESSAGE_STATE_CHANGE:
                    if (ThrowActivity.this.mBluetoothPipe.isServiceAvailable()) {
                        Bundle command = msg.getData();
                        if (command.getString("Reset sensor") == "New neutral") {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    ThrowActivity.this.busyReset = true;
                                    byte[] ResetZaxis = {(byte) 0xFF, (byte) 0xAA, (byte) 0x52};
                                    ThrowActivity.this.mBluetoothService.Send(ResetZaxis);
                                    try { Thread.sleep(1000); } catch(InterruptedException e) { }
                                    ThrowActivity.this.resetSensor();
                                    while (!ThrowActivity.this.hasResumed()) ;
                                    ThrowActivity.this.resetNeutral();
                                    ThrowActivity.this.busyReset = false;
                                }
                            }).start();

                        }
                        if (command.getString("Reset sensor") == "Calibrate") {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    ThrowActivity.this.busyCalibration = true;
                                    byte[] CalibrationCmd = {(byte) 0xFF, (byte) 0xAA, (byte) 0x67};
                                    ThrowActivity.this.mBluetoothService.Send(CalibrationCmd);
                                    try { Thread.sleep(10000); } catch(InterruptedException e) { }
                                    ThrowActivity.this.resetSensor();
                                    try { Thread.sleep(1000); } catch(InterruptedException e) { }
                                    ThrowActivity.this.resetSensor();
                                    while (!ThrowActivity.this.hasResumed()) ;
                                    ThrowActivity.this.resetNeutral();
                                    ThrowActivity.this.busyCalibration = false;
                                }
                            }).start();
                        }
                    }
                    break;
                default:
                    return;
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        autoStart = true;

        ThrowActivityBinding binding = DataBindingUtil.setContentView(this, R.layout.throw_activity);

        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } catch (IllegalStateException e) {
            // Only fullscreen activities can request orientation
        }

        mGaugeViewModel = ViewModelProviders.of(this).get(ThrowGaugeViewModel.class);
        mGaugeViewModel.SetSendSensorHandler(this.mSendSensor);
        binding.setCommandthrowViewModel(mGaugeViewModel);
        binding.setLifecycleOwner(this);

        final Button minAlert = (Button)findViewById(R.id.buttonSetMinTravel);
        final Button maxAlert = (Button)findViewById(R.id.buttonSetMaxTravel);

        minAlert.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                onOpenDialogThresholdAlert(dialogType.T_LIMIT,0);
            }
        });

         maxAlert.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                onOpenDialogThresholdAlert(dialogType.T_LIMIT,1);
            }
        });

        // chord button
        final Button chordButton = findViewById(R.id.inChordButton);
        chordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onOpenDialogThresholdAlert(dialogType.T_CHORD,0);
            }
        });

        // calibration confirm box
        final Button calibrateButton = (Button)findViewById(R.id.buttonCalibrate);
        calibrateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onOpenDialogThresholdAlert(dialogType.T_CALIBRATE,0);
            }
        });


        mGaugeViewModel.getThrowGauge().observe(this, new Observer<ThrowGauge>() {
            @Override
            public void onChanged(@Nullable ThrowGauge user) {
                if (user.GetAngle() < -90 || user.GetAngle() > 90)
                    Toast.makeText(getApplicationContext(), "Angle must be -90 < Angle < 90", Toast.LENGTH_SHORT).show();
            }
        });

        mGaugeViewModel.getThrowGauge2().observe(this, new Observer<ThrowGauge>() {
            @Override
            public void onChanged(@Nullable ThrowGauge user) {
                if (user.GetAngle() < -90 || user.GetAngle() > 90)
                    Toast.makeText(getApplicationContext(), "Angle must be -90 < Angle < 90", Toast.LENGTH_SHORT).show();
            }
        });

        mGaugeViewModel.setMultiDevice(mDeviceTags.size()>1);

        this.RunMode = 0;

        mHandler = new DataHandler();

        // read preference
        devicePrefs = BluetoothPreferences.getKeyrings(getApplicationContext());
        for (int i = 0; i < devicePrefs.size(); i++) {
            DeviceTag tag = devicePrefs.get(i);
            if (tag.getChord() != "0.0") {
                mGaugeViewModel.setChordValueDia(tag.getChord());
                mGaugeViewModel.setMaxTravelDia(tag.getTravelMax());
                mGaugeViewModel.setMaxTravelDia2(tag.getTravelMax());
                mGaugeViewModel.setMinTravelDia(tag.getTravelMin());
                mGaugeViewModel.setMinTravelDia2(tag.getTravelMin());
            }
        }
        // watch BT activity and display status line
        btWatcher.start();
    }

    public  void resetNeutral(){
        mGaugeViewModel.resetNeutral();
    }

    public void resetSensor(){
        mGaugeViewModel.resetSensorPosition();
    }

    public boolean hasResumed(){
        return mGaugeViewModel.HasResumed();
    }

    // dialog for MaxTravel, MinTravel and Chord
    private void onOpenDialogThresholdAlert(final dialogType t, final int lohi) {

        String strTitle = "";
        String strValue = "";
        switch (t) {
            case T_LIMIT:
                if (lohi == 0) {
                    strTitle = "Min travel limit";
                    strValue = mGaugeViewModel.getMinTravelSetNum();

                } else {
                    strTitle = "Max travel limit";
                    strValue = mGaugeViewModel.getMaxTravelSetNum();
                }
                break;
            case T_CHORD:
                strTitle = "Chord length";
                strValue = mGaugeViewModel.getChordValueNum();
                break;
            case T_CALIBRATE:
                strTitle = "Keep sensor horizontal and do not move";
                strValue = "";
                break;
            default:
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(strTitle);

        // Set up the input
        input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        if (strValue != "") {
            input.setText(strValue); // show actual value
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            builder.setView(input);
        }
        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (t) {
                    case T_LIMIT:
                        if (lohi == 0) {
                            mGaugeViewModel.setMinTravelDia(input.getText().toString());
                            mGaugeViewModel.setMinTravelDia2(input.getText().toString());
                            mGaugeViewModel.notifyPropertyChanged(BR.minTravelSet);
                        } else {
                            mGaugeViewModel.setMaxTravelDia(input.getText().toString());
                            mGaugeViewModel.setMaxTravelDia2(input.getText().toString());
                            mGaugeViewModel.notifyPropertyChanged(BR.maxTravelSet);
                        }
                        break;
                    case T_CHORD:
                        mGaugeViewModel.setChordValueDia(input.getText().toString());
                        break;
                    case T_CALIBRATE:
                        mGaugeViewModel.onCalibrateClicked();
                        break;
                    default:
                }
            }
        });
        builder.setNegativeButton(getApplication().getString(R.string.dlg_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregisterReceiver(this.mReceiver);

        if (mBluetoothPipe != null) {
            mBluetoothPipe.cancelDiscovery();
            mBluetoothPipe.stopService();
        }
        
        // save the preference to each registered device
        for (int i = 0; i < devicePrefs.size(); i++) {
            DeviceTag tag = devicePrefs.get(i);
            tag.setChord(mGaugeViewModel.getChordValue());
            tag.setTravelMax(mGaugeViewModel.getMaxTravelSetNum());
            tag.setTravelMin(mGaugeViewModel.getMinTravelSetNum());
        }
        BluetoothPreferences.setKeyrings(getApplicationContext(), devicePrefs);

        // get rid of remaining thread
        btWatcher.cancel();
    }

    class btStatusWatcherClass extends Thread
    {
        private volatile boolean exit = false;
        private String btText[] = new String[2];
        private String btTextAlt[] = new String[2];
        private Drawable btColor[] = new Drawable[2];
        private boolean btShowAlt[] = new boolean[2];

        private boolean buttonResetEnabled[] = new boolean[2];
        private boolean buttonCalEnabled[] = new boolean[2];

        private int tick = 0;

        public void run()
        {
            setName("btStatusWatcher");
            if (mDeviceTags.size() > 0) {
                btText[0] = mDeviceTags.get(0).name + " (" + mDeviceTags.get(0).address + ")";
            }
            if (mDeviceTags.size() > 1) {
                btText[1] = mDeviceTags.get(1).name + " (" + mDeviceTags.get(1).address + ")";
            }

            while (!exit) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }

                setTextAndColor(0);
                setTextAndColor(1);

                // enable/disable buttons
                if (mGaugeViewModel.getMultiDevice()) {
                    mGaugeViewModel.setButtonResetAngleEnable(
                            !(busyReset || busyCalibration) && (buttonResetEnabled[0] && buttonResetEnabled[1]));
                    mGaugeViewModel.notifyPropertyChanged(BR.buttonResetAngleEnable);
                    mGaugeViewModel.setButtonCalibrateEnable(
                            !(busyReset || busyCalibration) && (buttonCalEnabled[0] && buttonCalEnabled[1]));
                    mGaugeViewModel.notifyPropertyChanged(BR.buttonCalibrateEnable);
                } else {
                    mGaugeViewModel.setButtonResetAngleEnable(
                            !(busyReset || busyCalibration) && buttonResetEnabled[0]);
                    mGaugeViewModel.notifyPropertyChanged(BR.buttonResetAngleEnable);
                    mGaugeViewModel.setButtonCalibrateEnable(
                            !(busyReset || busyCalibration) && buttonCalEnabled[0]);
                    mGaugeViewModel.notifyPropertyChanged(BR.buttonCalibrateEnable);
                }

                switch (tick) {
                    case 0:
                        mGaugeViewModel.setBtStatus(btText[0]);
                        mGaugeViewModel.notifyPropertyChanged(BR.btStatus);
                        mGaugeViewModel.setBtStatusColor(btColor[0]);
                        mGaugeViewModel.notifyPropertyChanged(BR.btStatusColor);

                        mGaugeViewModel.setBtStatus2(btText[1]);
                        mGaugeViewModel.notifyPropertyChanged(BR.btStatus2);
                        mGaugeViewModel.setBtStatusColor2(btColor[1]);
                        mGaugeViewModel.notifyPropertyChanged(BR.btStatusColor2);

                        if (btShowAlt[0]) {
                            mGaugeViewModel.setBtStatus(btTextAlt[0]);
                            mGaugeViewModel.notifyPropertyChanged(BR.btStatus);
                        }
                        if (btShowAlt[1]) {
                            mGaugeViewModel.setBtStatus2(btTextAlt[1]);
                            mGaugeViewModel.notifyPropertyChanged(BR.btStatus2);
                        }
                        break;
                    case 10:
                        mGaugeViewModel.setBtStatus(btText[0]);
                        mGaugeViewModel.notifyPropertyChanged(BR.btStatus);
                        mGaugeViewModel.setBtStatusColor(btColor[0]);
                        mGaugeViewModel.notifyPropertyChanged(BR.btStatusColor);

                        mGaugeViewModel.setBtStatus2(btText[1]);
                        mGaugeViewModel.notifyPropertyChanged(BR.btStatus2);
                        mGaugeViewModel.setBtStatusColor2(btColor[1]);
                        mGaugeViewModel.notifyPropertyChanged(BR.btStatusColor2);
                        break;
                    default:
                        break;
                }
                tick = (tick >= 20) ? 0 : tick + 1;
            }
        }

        public void cancel() {
            exit = true;
        }

        // TODO AHa: not used
//        private Drawable getWitColor(int channel) {
//            Drawable color = ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_red, null);
//            switch (mGaugeViewModel.getWitLinkStatus(channel)) {
//                case BluetoothState.WIT_IDLE:
//                    color = ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_red, null);
//                    break;
//                case BluetoothState.WIT_DATA_ARRIVING:
//                    color = ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_blue, null);
//                    break;
//                default:
//            }
//            mGaugeViewModel.setWitLinkStatus(channel, BluetoothState.WIT_IDLE);
//            return color;
//        }

        private void setTextAndColor (int channel) {
            int linkState;
            int witState;

            // mBluetoothService.getState() might be called before method is available
            try {
                linkState = mBluetoothService.getState();
            } catch  (Exception e) {
                linkState = BluetoothState.STATE_NONE;
            }
            // witmotion data status
            witState = mGaugeViewModel.getWitLinkStatus(channel);
            // TODO AHa: states should not combined for both sensors
            switch (linkState) {
                case BluetoothState.STATE_CONNECTED:
                    if (witState == BluetoothState.WIT_DATA_ARRIVING) {
                        btColor[channel] = ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_blue, null);
                        btTextAlt[channel] = "connected";
                        btShowAlt[channel] = false;
                        buttonResetEnabled[channel] = true;
                        buttonCalEnabled[channel] = true;
                    } else {
                        btColor[channel] = ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_red, null);
                        btTextAlt[channel] = "connected wait";
                        btShowAlt[channel] = true;
                        buttonResetEnabled[channel] = false;
                        buttonCalEnabled[channel] = false;
                    }
                    break;
                case BluetoothState.STATE_CONNECTING:
                    btColor[channel] = ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_red, null);
                    btTextAlt[channel] = "connecting";
                    btShowAlt[channel] = true;
                    buttonResetEnabled[channel] = false;
                    buttonCalEnabled[channel] = false;
                    break;
                case BluetoothState.STATE_LISTEN:
                    btColor[channel] = ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_red, null);
                    btTextAlt[channel] = "listening";
                    btShowAlt[channel] = true;
                    buttonResetEnabled[channel] = false;
                    buttonCalEnabled[channel] = false;
                    break;

                 default:
                    btColor[channel] = ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_red, null);
                    btTextAlt[channel] = "opening";
                    btShowAlt[channel] = true;
                    buttonResetEnabled[channel] = false;
                    buttonCalEnabled[channel] = false;
                    break;
            }
        }
    }
}
