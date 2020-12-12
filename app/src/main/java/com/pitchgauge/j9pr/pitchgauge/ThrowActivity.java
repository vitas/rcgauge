package com.pitchgauge.j9pr.pitchgauge;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.arch.lifecycle.Observer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.pitchgauge.j9pr.pitchgauge.databinding.ThrowActivityBinding;

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
                                    byte[] ResetZaxis = {(byte) 0xFF, (byte) 0xAA, (byte) 0x52};
                                    ThrowActivity.this.mBluetoothService.Suspend(true);
                                    ThrowActivity.this.mBluetoothService.Send(ResetZaxis);
                                    try { Thread.sleep(100); } catch(InterruptedException e) { }
                                    ThrowActivity.this.resetSensor();
                                    ThrowActivity.this.mBluetoothService.Suspend(false);
                                    while (!ThrowActivity.this.hasResumed()) ;
                                    ThrowActivity.this.resetNeutral();
                                }
                            }).start();
                        }
                        if (command.getString("Reset sensor") == "Calibrate") {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    byte[] CalibrationCmd = {(byte) 0xFF, (byte) 0xAA, (byte) 0x67};
                                    ThrowActivity.this.mBluetoothService.Suspend(true);
                                    ThrowActivity.this.mBluetoothService.Send(CalibrationCmd);
                                    try { Thread.sleep(5000); } catch(InterruptedException e) { }
                                    ThrowActivity.this.resetSensor();
                                    ThrowActivity.this.mBluetoothService.Suspend(false);
                                    while (!ThrowActivity.this.hasResumed()) ;
                                    ThrowActivity.this.resetNeutral();
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

        Button minAlert = (Button)findViewById(R.id.buttonSetMinTravel);
        Button maxAlert = (Button)findViewById(R.id.buttonSetMaxTravel);

        minAlert.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                onOpenDialogThresholdAlert(0);
            }
        });

        maxAlert.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                onOpenDialogThresholdAlert(1);
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

    private void onOpenDialogThresholdAlert(int lohi){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if(lohi == 0)
            builder.setTitle(getApplication().getString(R.string.dlg_min_negative_travel));
        else
            builder.setTitle(getApplication().getString(R.string.dlg_max_positive_travel));
        // Set up the input
        input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        final int treshold = lohi;
        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(treshold == 0){
                    mGaugeViewModel.setMinTravel(input.getText().toString());
                    mGaugeViewModel.setMinTravel2(input.getText().toString());

                }else{
                    mGaugeViewModel.setMaxTravel(input.getText().toString());
                    mGaugeViewModel.setMaxTravel2(input.getText().toString());
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


}
