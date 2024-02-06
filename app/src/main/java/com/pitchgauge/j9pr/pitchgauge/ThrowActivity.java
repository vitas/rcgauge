package com.pitchgauge.j9pr.pitchgauge;

import android.app.Activity;
import androidx.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import androidx.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.lifecycle.Observer;
import android.os.Handler;
import android.os.Message;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.Nullable;
import android.text.InputType;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.pitchgauge.j9pr.pitchgauge.databinding.ThrowActivityBinding;
import android.view.WindowManager.LayoutParams;

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
        T_LIMIT, T_CHORD, T_CALIBRATE, T_BT_SETTING
    }

    ArrayList<DeviceTag> devicePrefs = new ArrayList<DeviceTag>();

    private boolean busyReset = false;
    private boolean busyCalibration = false;
    private boolean busyConfigure = false;
    private boolean btConnectedAll = false;
    private boolean witKnown = false;

    // detection of wit model
    private enum bwtModelT {
        UNKNOWN, BWT61CL, BWT901CL
    }
    private bwtModelT bwtModel = bwtModelT.UNKNOWN;

    private boolean sendSensorConfigStartup = false;

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
                        , getApplication().getString(R.string.txt_warning_bluetooth_not_enabled)
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    class DataHandler extends Handler {

        private long[] lLastTimeDetect = new long[] {System.currentTimeMillis(), System.currentTimeMillis()};
        private long[] lLastTimeKeepAlive = new long[] {System.currentTimeMillis(), System.currentTimeMillis()};

        private boolean firstMessage = true;
        private long[] lLastTimeFirstMessage = new long[] {System.currentTimeMillis(), System.currentTimeMillis()};
        private boolean[] configSent = new boolean[] {false, false};

        public void handleMessage(Message msg) {

            long lTimeNow = System.currentTimeMillis();
            long delta = 0;

            switch (msg.what) {

                case BluetoothState.MESSAGE_READ:
                    try {
                        float[] fData = msg.getData().getFloatArray("Data");
                        int pos = msg.getData().getInt("Pos");

                        // common message stream, checks to be done per sensor instance
                        if (this.firstMessage) {
                            // first message detected
                            this.lLastTimeFirstMessage[pos] = lTimeNow;
                            this.firstMessage = false;
                        } else {

                            // send sensor configuration at startup once
                            // TODO: this is not fully functional and not used, as the function could be called to early in dual sensor mode
                            if (sendSensorConfigStartup) {
                                delta = lTimeNow - this.lLastTimeFirstMessage[pos];
                                if (!configSent[pos] && (delta > 100)) {
                                    mGaugeViewModel.sendConfigMessage(pos);
                                    configSent[pos] = true;
                                }
                            }

                            // try to detect a BWT901CL
                            if (bwtModel == bwtModelT.UNKNOWN) {
                                delta = lTimeNow - this.lLastTimeDetect[pos];
                                if (delta > 500) {
                                    this.lLastTimeDetect[pos] = lTimeNow;
                                    mGaugeViewModel.sendConfigReadMessage(pos);
                                }
                            }

                            // send a regular keep alive message, fixes the delayed instream issue with witmotion HC-02
                            delta = lTimeNow - this.lLastTimeKeepAlive[pos];
                            if (delta > 2000) {
                                this.lLastTimeKeepAlive[pos] = lTimeNow;
                                mGaugeViewModel.sendAliveMessage(pos);
                            }
                        }
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
                                        // angle values (Roll,Pitch,Yaw)
                                        ThrowActivity.this.mGaugeViewModel.setAngles(pos, Float.valueOf(fData[6]), Float.valueOf(fData[7]), Float.valueOf(fData[8]));

                                        // if config data is received, we assume it is a BWT901CL
                                        if ((Float.valueOf(fData[32]) != 0)
                                                || (Float.valueOf(fData[33]) != 0)
                                                || (Float.valueOf(fData[34]) != 0)
                                                || (Float.valueOf(fData[35]) != 0)) {
                                            bwtModel = bwtModelT.BWT901CL;
                                            fData[32] = 0;
                                            fData[33] = 0;
                                            fData[34] = 0;
                                            fData[35] = 0;
                                        }
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

        private boolean txBusy = false;
        private long cmdLeaveLastTime = System.currentTimeMillis();

        private int messageCount = 0;
        private int configCount = 0;


        public void handleMessage(Message msg) {

            long lTimeNow = System.currentTimeMillis();

            messageCount++;

            // do not send before all connected
            if (!btConnectedAll) {
                return;
            }

            if (bwtModel == bwtModelT.UNKNOWN) {
                if (configCount > 2 ) { // assume sensor being a BWT61 after some failing read commands
                    bwtModel = bwtModel.BWT61CL;
                }
            }

            if  (!(txBusy)) {
                switch (msg.what) {
                        case BluetoothState.MESSAGE_STATE_CHANGE:
                            if (ThrowActivity.this.mBluetoothPipe.isServiceAvailable()) {
                                Bundle command = msg.getData();

                                // reset button
                                if (command.getString("Reset sensor") == "New neutral") {
                                    ThrowActivity.this.busyReset = true;
                                    txBusy = true;
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // ensure guard time to keep-alive message
                                            long deltaT = lTimeNow - cmdLeaveLastTime;
                                            if (deltaT < 400) {
                                                try { Thread.sleep(deltaT); } catch(InterruptedException e) {};
                                            }
                                            switch (bwtModel) {
                                                case BWT901CL:
                                                    byte[] cmdStart = {(byte)0xFF, (byte)0xAA, (byte)0x69, (byte)0x88, (byte)0xB5};
                                                    byte[] cmdResetZ = {(byte)0xFF, (byte)0xAA, (byte)0x01, (byte)0x04, (byte)0x00};
                                                    byte[] cmdSave = {(byte)0xFF, (byte)0xAA, (byte)0x00, (byte)0x00, (byte)0x00};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdStart);
                                                    try { Thread.sleep(250); } catch(InterruptedException e) {};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdResetZ);
                                                    try { Thread.sleep(300); } catch(InterruptedException e) {};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdSave);
                                                    break;
                                                case BWT61CL:
                                                    byte[] ResetZaxis = {(byte) 0xFF, (byte) 0xAA, (byte) 0x52};
                                                    ThrowActivity.this.mBluetoothService.Send(ResetZaxis);
                                                    try { Thread.sleep(300); } catch(InterruptedException e) {};
                                                    break;
                                                default:
                                            }
                                            ThrowActivity.this.resetSensor();
                                            try { Thread.sleep(400); } catch(InterruptedException e) {};
                                            ThrowActivity.this.resetNeutral();
                                            ThrowActivity.this.busyReset = false;
                                            cmdLeaveLastTime = System.currentTimeMillis();
                                            txBusy = false;
                                        }
                                    }).start();
                                }

                                // calibrate button
                                if (command.getString("Reset sensor") == "Calibrate") {
                                    ThrowActivity.this.busyCalibration = true;
                                    txBusy = true;
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // ensure guard time to keep-alive message
                                            long deltaT = lTimeNow - cmdLeaveLastTime;
                                            if (deltaT < 400) {
                                                try { Thread.sleep(deltaT); } catch(InterruptedException e) {};
                                            }
                                            switch (bwtModel) {
                                                case BWT901CL:
                                                    byte[] cmdStart = {(byte)0xFF, (byte)0xAA, (byte)0x69, (byte)0x88, (byte)0xB5};
                                                    byte[] cmdCalibrate = {(byte)0xFF, (byte)0xAA, (byte)0x01, (byte)0x01, (byte)0x00};
                                                    byte[] cmdResetZ = {(byte)0xFF, (byte)0xAA, (byte)0x01, (byte)0x04, (byte)0x00};
                                                    byte[] cmdSave = {(byte)0xFF, (byte)0xAA, (byte)0x00, (byte)0x00, (byte)0x00};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdStart);
                                                    try { Thread.sleep(250); } catch(InterruptedException e) {};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdCalibrate);
                                                    try { Thread.sleep(5000); } catch(InterruptedException e) {};
                                                    ThrowActivity.this.resetSensor();
                                                    ThrowActivity.this.mBluetoothService.Send(cmdStart);
                                                    try { Thread.sleep(250); } catch(InterruptedException e) {};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdResetZ);
                                                    try { Thread.sleep(250); } catch(InterruptedException e) {};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdSave);
                                                    break;
                                                case BWT61CL:
                                                    byte[] CalibrationCmd = {(byte) 0xFF, (byte) 0xAA, (byte) 0x67};
                                                    ThrowActivity.this.mBluetoothService.Send(CalibrationCmd);
                                                    try { Thread.sleep(10000); } catch(InterruptedException e) {};
                                                    ThrowActivity.this.resetSensor();
                                                    byte[] ResetZaxis = {(byte) 0xFF, (byte) 0xAA, (byte) 0x52};
                                                    ThrowActivity.this.mBluetoothService.Send(ResetZaxis);
                                                    break;
                                                default:
                                            }
                                            try { Thread.sleep(1000); } catch(InterruptedException e) {};
                                            ThrowActivity.this.resetSensor();
                                            try { Thread.sleep(400); } catch(InterruptedException e) {};
                                            ThrowActivity.this.resetNeutral();
                                            ThrowActivity.this.busyCalibration = false;
                                            cmdLeaveLastTime = System.currentTimeMillis();
                                            txBusy = false;
                                        }
                                    }).start();
                                }

                                // for unknown reasons an outgoing message avoids stalling of the incoming data stream
                                if (command.getString("Send alive") == String.valueOf(0)) {

                                    long deltaT = lTimeNow - cmdLeaveLastTime;
                                    if (deltaT < 1000) {
                                        return; // skip keep-alive if last command happened shortly before
                                    }

                                    txBusy = true;
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // send a dummy sequence
                                            switch (bwtModel) {
                                                case BWT901CL:
                                                    byte[] CommandZero2 = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
                                                    ThrowActivity.this.mBluetoothService.Send(CommandZero2);
                                                    break;
                                                case BWT61CL:
                                                    byte[] CommandZero = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
                                                    ThrowActivity.this.mBluetoothService.Send(CommandZero);
                                                default:
                                            }
                                            cmdLeaveLastTime = System.currentTimeMillis();
                                            txBusy = false;
                                        }
                                    }).start();
                                }

                                // for unknown reasons an outgoing message avoids stalling of the incoming data stream
                                if (command.getString("Send BWT901 Read Config") == String.valueOf(0)) {

                                    long deltaT = lTimeNow - cmdLeaveLastTime;
                                    if (deltaT < 500) {
                                        return; // skip keep-alive if last command happened shortly before
                                    }

                                    txBusy = true;
                                    configCount++;

                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // try to read config from a BWT901CL
                                            switch (bwtModel) {
                                                case UNKNOWN:
                                                    byte[] CommandRead = {(byte) 0xFF, (byte) 0xAA, (byte) 0x27, (byte) 0x6E, (byte) 0x00}; // read config command
                                                    ThrowActivity.this.mBluetoothService.Send(CommandRead);
                                                    break;
                                            }
                                            cmdLeaveLastTime = System.currentTimeMillis();
                                            txBusy = false;
                                        }
                                    }).start();
                                }

                                // send sensor configuration setting
                                if (command.getString("Configure sensor") == String.valueOf(0)) {
                                    txBusy = true;
                                    ThrowActivity.this.busyConfigure = true;
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // ensure guard time to keep-alive messages
                                            long deltaT = lTimeNow - cmdLeaveLastTime;
                                            if (deltaT < 400) {
                                                try { Thread.sleep(deltaT); } catch(InterruptedException e) {};
                                            }
                                            switch (bwtModel) {
                                                case BWT901CL:
                                                    // 6-axis mode
                                                    byte[] cmdStart = {(byte)0xFF, (byte)0xAA, (byte)0x69, (byte)0x88, (byte)0xB5};
                                                    byte[] cmdAxis = {(byte)0xFF, (byte)0xAA, (byte)0x24, (byte)0x01, (byte)0x00};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdStart);
                                                    try { Thread.sleep(250); } catch(InterruptedException e) {};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdAxis);
                                                    try { Thread.sleep(300); } catch(InterruptedException e) {};
                                                    // horizontal installation
                                                    byte[] cmdInstallDir = {(byte)0xFF, (byte)0xAA, (byte)0x23, (byte)0x00, (byte)0x00};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdStart);
                                                    try { Thread.sleep(250); } catch(InterruptedException e) {};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdInstallDir);
                                                    try { Thread.sleep(300); } catch(InterruptedException e) {};
                                                    // bandwidth 20Hz
                                                    byte[] cmdBandwidth = {(byte)0xFF, (byte)0xAA, (byte)0x1F, (byte)0x04, (byte)0x00};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdStart);
                                                    try { Thread.sleep(250); } catch(InterruptedException e) {};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdBandwidth);
                                                    try { Thread.sleep(300); } catch(InterruptedException e) {};
                                                    // output rate 10Hz
                                                    byte[] cmdOutRate = {(byte)0xFF, (byte)0xAA, (byte)0x03, (byte)0x06, (byte)0x00};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdOutRate);
                                                    try { Thread.sleep(250); } catch(InterruptedException e) {};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdBandwidth);
                                                    try { Thread.sleep(300); } catch(InterruptedException e) {};
                                                    // return content , use default 0x51 Acceleration Output, 0x52 Angular Velocity, 0x53 Angle Output and 0x54 Magnetic Output
                                                    byte[] cmdReturnContent = {(byte)0xFF, (byte)0xAA, (byte)0x02, (byte)0x01E, (byte)0x00};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdReturnContent);
                                                    try { Thread.sleep(250); } catch(InterruptedException e) {};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdBandwidth);
                                                    try { Thread.sleep(300); } catch(InterruptedException e) {};
                                                    // save configuration
                                                    byte[] SaveCmd = {(byte)0xFF, (byte)0xAA, (byte)0x00, (byte)0x00, (byte)0x00};
                                                    ThrowActivity.this.mBluetoothService.Send(SaveCmd);
                                                    try { Thread.sleep(300); } catch(InterruptedException e) {};
                                                    break;
                                                case BWT61CL:
                                                    try { Thread.sleep(2000); } catch(InterruptedException e) { }
                                                    byte[] CommandZero = {(byte) 0xFF, (byte) 0xAA, (byte) 0x00};
                                                    // horizontal installation
                                                    byte[] cmdString1 = {(byte) 0xFF, (byte) 0xAA, (byte) 0x65};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdString1);
                                                    try { Thread.sleep(250); } catch(InterruptedException e) { }
                                                    ThrowActivity.this.mBluetoothService.Send(CommandZero);
                                                    try { Thread.sleep(100); } catch(InterruptedException e) { }
                                                    try { Thread.sleep(2000); } catch(InterruptedException e) { }
                                                    // Update rate 100Hz (115200 baudrate)
                                                    byte[] cmdString2 = {(byte) 0xFF, (byte) 0xAA, (byte) 0x64};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdString2);
                                                    try { Thread.sleep(250); } catch(InterruptedException e) { }
                                                    ThrowActivity.this.mBluetoothService.Send(CommandZero);
                                                    try { Thread.sleep(100); } catch(InterruptedException e) { }
                                                    // bandwidth 21 Hz
                                                    byte[] cmdString3 = {(byte) 0xFF, (byte) 0xAA, (byte) 0x85};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdString3);
                                                    try { Thread.sleep(250); } catch(InterruptedException e) { }
                                                    ThrowActivity.this.mBluetoothService.Send(CommandZero);
                                                    try { Thread.sleep(100); } catch(InterruptedException e) { }
                                                    // static detection 0.122 deg/sec
                                                    byte[] cmdString4 = {(byte) 0xFF, (byte) 0xAA, (byte) 0x71};
                                                    ThrowActivity.this.mBluetoothService.Send(cmdString4);
                                                    try { Thread.sleep(250); } catch(InterruptedException e) { }
                                                    ThrowActivity.this.mBluetoothService.Send(CommandZero);
                                                    try { Thread.sleep(100); } catch(InterruptedException e) { }
                                                    break;
                                                default:
                                                    break;
                                            }
                                            ThrowActivity.this.busyConfigure = false;
                                            cmdLeaveLastTime = System.currentTimeMillis();
                                            txBusy = false;
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
        }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        autoStart = true;

        // keep activity screen on always
        getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

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
                onOpenDialogThresholdAlert(dialogType.T_LIMIT,0, 0);
            }
        });

        maxAlert.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                onOpenDialogThresholdAlert(dialogType.T_LIMIT,1, 0);
            }
        });

        // chord button
        final Button chordButton = findViewById(R.id.inChordButton);
        chordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onOpenDialogThresholdAlert(dialogType.T_CHORD,0, 0);
            }
        });

        // calibration confirm box (at longclick reset)
        final Button resetButton = findViewById(R.id.buttonResetAngle);
        resetButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onOpenDialogThresholdAlert(dialogType.T_CALIBRATE,0, 0);
                return true;
            }
        });

        // BT status button 1 (log-click)
        final Button btStatusButton = findViewById(R.id.buttonBTStatus);
        btStatusButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onOpenDialogThresholdAlert(dialogType.T_BT_SETTING,0, 0);
                return true;
            }
        });

        // BT status button 2 (long-click)
        final Button btStatus2Button = findViewById(R.id.buttonBTStatus2);
        btStatus2Button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onOpenDialogThresholdAlert(dialogType.T_BT_SETTING,0, 1);
                return true;
            }
        });

        // show differences enable/disable button
        final Button showDiffButton = findViewById(R.id.buttonShowDiff);
        showDiffButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mGaugeViewModel.toggleDiffVisible();
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

        // read device preference
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
		
        // read main preferences
        MainPrefs mainPreferences = BluetoothPreferences.getMainPrefs(getApplicationContext());

        // Z Axes
        switch (mainPreferences.zMode) {
            case IGNORE:
                mGaugeViewModel.setIgnoreZ(true);
                break;
            case FULL:
                mGaugeViewModel.setIgnoreZ(false);
                break;
        }

        // length unit
        String lu = mainPreferences.units.toString();
        mGaugeViewModel.setLengthUnits(lu);

        // throw calculation method
        switch (mainPreferences.throwCalcMethod) {
            case ORTHO:
                mGaugeViewModel.setThrowCalcMethod(MainPrefs.throwCalcMethodT.ORTHO);
                break;
            case CHORD:
                mGaugeViewModel.setThrowCalcMethod(MainPrefs.throwCalcMethodT.CHORD);
                break;
        }

        // sensor model
        switch (mainPreferences.witModel) {
            case AUTO:
                bwtModel = bwtModelT.UNKNOWN;
                break;
            case BWT61CL:
                bwtModel = bwtModelT.BWT61CL;
                break;
            case BWT901CL:
                bwtModel = bwtModelT.BWT901CL;
                break;
        }

        // send sensor configuration at startup
        switch (mainPreferences.sensorConfigMode) {
            case AUTO:
                sendSensorConfigStartup = true;
                break;
            case MANUAL:
                sendSensorConfigStartup = false;
                break;
        }

        // watch BT activity and display status line
        btWatcher.start();
    }

    public  void resetNeutral() {
        mGaugeViewModel.resetNeutral();
    }

    public void resetSensor() {
        mGaugeViewModel.resetSensorPosition();
    }

    public boolean hasResumed() {
        return mGaugeViewModel.HasResumed();
    }

    // dialog for MaxTravel, MinTravel and Chord
    private void onOpenDialogThresholdAlert(final dialogType t, final int lohi, final int channel) {

        Resources res = getResources();
        String strTitle = "";
        String strDescription = "";
        String strValue = "";
        String strWitModel = bwtModel.toString();
        switch (t) {
            case T_LIMIT:
                if (lohi == 0) {
                    strTitle = res.getString(R.string.txt_dlg_min_negative_travel);
                    strValue = mGaugeViewModel.getMinTravelSetNum();

                } else {
                    strTitle = res.getString(R.string.txt_dlg_max_positive_travel);
                    strValue = mGaugeViewModel.getMaxTravelSetNum();
                }
                break;
            case T_CHORD:
                strTitle = res.getString(R.string.txt_chord);
                strValue = mGaugeViewModel.getChordValueNum();
                break;
            case T_CALIBRATE:
                strTitle = res.getString(R.string.txt_dlg_calibrate) + " " + strWitModel;
                strDescription = res.getString(R.string.txt_dlg_calibrate_desc);
                strValue = "";
                break;
            case T_BT_SETTING:
                strTitle = res.getString(R.string.txt_dlg_bt_settings) + " " + strWitModel;
                strDescription = res.getString(R.string.txt_dlg_bt_settings_desc);
                strValue = "";
                break;
            default:
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(strTitle);
        if (strDescription != "") {
            builder.setMessage(strDescription);
        }
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
                    case T_BT_SETTING:
                        mGaugeViewModel.onBTStatusClicked();
                        break;
                    default:
                }
            }
        });
        builder.setNegativeButton(getApplication().getString(R.string.txt_dlg_cancel), new DialogInterface.OnClickListener() {
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

        private boolean btConnected[] = new boolean[2];

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
                    btConnectedAll = btConnected[0] && btConnected[1];
                    witKnown = (bwtModel != bwtModelT.UNKNOWN);
                    mGaugeViewModel.setButtonResetAngleEnable(
                            !(busyReset || busyCalibration || busyConfigure) && btConnectedAll && witKnown);
                    mGaugeViewModel.notifyPropertyChanged(BR.buttonResetAngleEnable);
                    mGaugeViewModel.setButtonCalibrateEnable(
                            !(busyReset || busyCalibration || busyConfigure) && btConnectedAll && witKnown);
                    mGaugeViewModel.notifyPropertyChanged(BR.buttonCalibrateEnable);
                    mGaugeViewModel.setButtonBTStatus(
                            !(busyReset || busyCalibration || busyConfigure) && btConnectedAll && witKnown);
                    mGaugeViewModel.setButtonBTStatus2(
                            !(busyReset || busyCalibration || busyConfigure) && btConnectedAll && witKnown);
                    mGaugeViewModel.notifyPropertyChanged(BR.buttonBTStatusEnable);
                    mGaugeViewModel.notifyPropertyChanged(BR.buttonBTStatus2Enable);
                } else {
                    btConnectedAll = btConnected[0];
                    witKnown = (bwtModel != bwtModelT.UNKNOWN);
                    mGaugeViewModel.setButtonResetAngleEnable(
                            !(busyReset || busyCalibration || busyConfigure) && btConnectedAll && witKnown);
                    mGaugeViewModel.notifyPropertyChanged(BR.buttonResetAngleEnable);
                    mGaugeViewModel.setButtonCalibrateEnable(
                            !(busyReset || busyCalibration || busyConfigure) && btConnectedAll && witKnown);
                    mGaugeViewModel.notifyPropertyChanged(BR.buttonCalibrateEnable);
                    mGaugeViewModel.setButtonBTStatus(
                            !(busyReset || busyCalibration || busyConfigure) && btConnectedAll && witKnown);
                    mGaugeViewModel.notifyPropertyChanged(BR.buttonBTStatusEnable);
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
                        btConnected[channel] = true;
                    } else {
                        btColor[channel] = ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_red, null);
                        btTextAlt[channel] = "connected wait";
                        btShowAlt[channel] = true;
                        btConnected[channel] = false;
                    }
                    break;
                case BluetoothState.STATE_CONNECTING:
                    btColor[channel] = ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_red, null);
                    btTextAlt[channel] = "connecting";
                    btShowAlt[channel] = true;
                    btConnected[channel] = false;
                    break;
                case BluetoothState.STATE_LISTEN:
                    btColor[channel] = ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_red, null);
                    btTextAlt[channel] = "listening";
                    btShowAlt[channel] = true;
                    btConnected[channel] = false;
                    break;

                default:
                    btColor[channel] = ResourcesCompat.getDrawable(getApplication().getResources(), R.drawable.layout_range_red, null);
                    btTextAlt[channel] = "opening";
                    btShowAlt[channel] = true;
                    btConnected[channel] = false;
                    break;
            }
        }
    }
}
