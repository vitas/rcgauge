package com.pitchgauge.j9pr.pitchgauge;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.arch.lifecycle.Observer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;
import com.pitchgauge.j9pr.pitchgauge.databinding.ThrowActivityBinding;
import java.util.Locale;
import org.joml.Vector3f;

public class ThrowActivity extends AppCompatActivity {

    private ThrowGaugeViewModel mGaugeViewModel = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService mBluetoothService = null;
    private String mConnectedDeviceName = null;
    private final Handler mHandler = new C02231();
    private final Handler handler = new C02314();
    private final Handler mSendSensor = new SendSensor();
    public BluetoothDevice device;
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

    /* renamed from: com.example.DataMonitor$4 */
    class C02314 extends Handler {
        C02314() {
        }

        public void handleMessage(Message msg) {
            byte[] byteReceived = (byte[]) msg.obj;
            ThrowActivity.this.mBluetoothService.CopeSerialData(byteReceived.length, byteReceived, 0);
        }
    }

    class C02231 extends Handler {
        C02231() {
        }
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    switch (msg.arg1) {
                        case 0:
                        case 1:
                            //if (DataMonitor.this.mTitle != null) {
                            //    DataMonitor.this.mTitle.setText(C0242R.string.title_not_connected);
                            //    return;
                            //}
                            return;
                        case 2:
                            //if (DataMonitor.this.mTitle != null) {
                            //    DataMonitor.this.mTitle.setText(C0242R.string.title_connecting);
                            //    return;
                            //}
                            return;
                        case 3:
                            //if (DataMonitor.this.mTitle != null) {
                            //    DataMonitor.this.mTitle.setText(C0242R.string.title_connected_to + DataMonitor.this.mConnectedDeviceName);
                            //    return;
                            //}
                            return;
                        default:
                            return;
                    }
                case 2:
                    try {
                        float[] fData = msg.getData().getFloatArray("Data");
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
                                        ThrowActivity.this.mGaugeViewModel.setAccelerations(Float.valueOf(fData[0]), Float.valueOf(fData[1]), Float.valueOf(fData[2]));
                                        fData[3] = fData[3] * ((float) ThrowActivity.this.av);
                                        fData[4] = fData[4] * ((float) ThrowActivity.this.av);
                                        fData[5] = fData[5] * ((float) ThrowActivity.this.av);
                                        ThrowActivity.this.mGaugeViewModel.setVelocities(Float.valueOf(fData[3]), Float.valueOf(fData[4]), Float.valueOf(fData[5]));
                                        // Roll Pitch Yaw
                                        ThrowActivity.this.mGaugeViewModel.setAngles(Float.valueOf(fData[6]), Float.valueOf(fData[7]), Float.valueOf(fData[8]));
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
                case 4:
                    ThrowActivity.this.mConnectedDeviceName = msg.getData().getString("device_name");
                    Toast.makeText(ThrowActivity.this.getApplicationContext(), "Connected to " + ThrowActivity.this.mConnectedDeviceName, Toast.LENGTH_LONG).show();
                    return;
                case 5:
                    Toast.makeText(ThrowActivity.this.getApplicationContext(), msg.getData().getString("toast"), Toast.LENGTH_LONG).show();
                    return;
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
                case 1:
                    if (ThrowActivity.this.mBluetoothService != null) {

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                byte[] ResetZaxis = {(byte)0xFF, (byte)0xAA, (byte)0x52};
                                ThrowActivity.this.mBluetoothService.Suspend(true);
                                ThrowActivity.this.mBluetoothService.Send(ResetZaxis);
                                ThrowActivity.this.resetSensor();
                                ThrowActivity.this.mBluetoothService.Suspend(false);
                                while(!ThrowActivity.this.hasResumed());
                                ThrowActivity.this.resetNeutral();
                                }
                        }).start();
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

        ThrowActivityBinding binding = DataBindingUtil.setContentView(this, R.layout.throw_activity);
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

        this.RunMode = 0;

        if(getIntent().getExtras() != null) {
            this.device = getIntent().getExtras().getParcelable("btdevice");
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        try {
            this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (this.mBluetoothAdapter == null) {
                //Toast.makeText(this, getString(C0242R.string.Bluetoothbad), 1).show();
                return;
            }
            if (this.mBluetoothService == null) {
                this.mBluetoothService = new BluetoothService(this, this.mHandler);
                if(this.mBluetoothService != null && this.device != null)
                    this.mBluetoothService.connect(this.device, 0);
            }
            this.writeBuffer = new byte[512];
            this.readBuffer = new byte[512];
            this.isOpen = false;
            SerialPortOpen();

        } catch (Exception e) {
        }
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
            builder.setTitle("Min negative travel");
        else
            builder.setTitle("Max positive travel");
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
                }else{
                    mGaugeViewModel.setMaxTravel(input.getText().toString());
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public synchronized void onResume() {
        super.onResume();
        Log.e("--", "onResume");
        if (this.mBluetoothAdapter == null)
            return;
        if (!this.mBluetoothAdapter.isEnabled()) {
            this.mBluetoothAdapter.enable();
        }
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (ThrowActivity.this.mBluetoothService != null && ThrowActivity.this.mBluetoothService.getState() == 0) {
                    ThrowActivity.this.mBluetoothService.start();
                }
            }
        }, 1000);
    }

    public synchronized void onPause() {
        super.onPause();
        Log.e("--", "onPause");
    }

    public void onStop() {
        super.onStop();
        Log.e("--", "onStop");
    }
    public void onDestroy() {
        super.onDestroy();
        if (this.mBluetoothService != null) {
            this.mBluetoothService.stop();
        }
    }

    private boolean SerialPortOpen() {

            this.isOpen = true;
            new readThread().start();
            return true;
    }

    private class readThread extends Thread {
        private readThread() {
        }

        public void run() {
            byte[] buffer = new byte[4096];
            while (true) {
                Message msg = Message.obtain();
                if (ThrowActivity.this.isOpen) {
                        //ThrowActivity.this.handler.sendMessage(msg);
                    }
                else {
                    try {
                        Thread.sleep(50);
                        return;
                    } catch (Exception e) {
                        return;
                    }
                }
            }
        }
    }
}
