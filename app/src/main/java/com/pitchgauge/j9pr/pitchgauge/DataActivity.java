package com.pitchgauge.j9pr.pitchgauge;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.pitchgauge.j9pr.pitchgauge.databinding.ActivityRawdataBinding;

import java.util.Locale;

public class DataActivity extends AppCompatActivity {

    private ThrowGaugeViewModel mGaugeViewModel = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService mBluetoothService = null;
    private String mConnectedDeviceName = null;
    private final Handler mHandler = new DataActivity.C02231();
    private final Handler handler = new DataActivity.C02314();
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

    /* renamed from: com.example.DataMonitor$4 */
    class C02314 extends Handler {
        C02314() {
        }

        public void handleMessage(Message msg) {
            byte[] byteReceived = (byte[]) msg.obj;
            DataActivity.this.mBluetoothService.CopeSerialData(byteReceived.length, byteReceived, 0);
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
                        switch (DataActivity.this.RunMode) {
                            case 0:
                                switch (DataActivity.this.iCurrentGroup) {
                                    case 0:
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum1)).setText(msg.getData().getString("Date"));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum2)).setText(msg.getData().getString("Time"));
                                        return;
                                    case 1:
                                        fData[0] = fData[0] * ((float) DataActivity.this.ar);
                                        fData[1] = fData[1] * ((float) DataActivity.this.ar);
                                        fData[2] = fData[2] * ((float) DataActivity.this.ar);
                                        DataActivity.this.mGaugeViewModel.setAccelerations(Float.valueOf(fData[0]), Float.valueOf(fData[1]), Float.valueOf(fData[2]));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum1)).setText(String.format("% 10.2fg", new Object[]{Float.valueOf(fData[0])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum2)).setText(String.format("% 10.2fg", new Object[]{Float.valueOf(fData[1])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum3)).setText(String.format("% 10.2fg", new Object[]{Float.valueOf(fData[2])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum4)).setText(String.format("% 10.2fg", new Object[]{Double.valueOf(Math.sqrt((double) (((fData[0] * fData[0]) + (fData[1] * fData[1])) + (fData[2] * fData[2]))))}));
                                        return;
                                    case 2:
                                        fData[3] = fData[3] * ((float) DataActivity.this.av);
                                        fData[4] = fData[4] * ((float) DataActivity.this.av);
                                        fData[5] = fData[5] * ((float) DataActivity.this.av);
                                        DataActivity.this.mGaugeViewModel.setVelocities(Float.valueOf(fData[3]), Float.valueOf(fData[4]), Float.valueOf(fData[5]));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum1)).setText(String.format("% 10.2f°/s", new Object[]{Float.valueOf(fData[3])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum2)).setText(String.format("% 10.2f°/s", new Object[]{Float.valueOf(fData[4])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum3)).setText(String.format("% 10.2f°/s", new Object[]{Float.valueOf(fData[5])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum4)).setText(String.format("% 10.2f°/s", new Object[]{Double.valueOf(Math.sqrt((double) (((fData[3] * fData[3]) + (fData[4] * fData[4])) + (fData[5] * fData[5]))))}));
                                        return;
                                    case 3:
                                        // Roll Pitch Yaw
                                        DataActivity.this.mGaugeViewModel.setAngles(Float.valueOf(fData[6]), Float.valueOf(fData[7]), Float.valueOf(fData[8]));
                                        String angle = String.format(Locale.getDefault() ,"%3.1f", new Object[]{Float.valueOf(fData[7])});
                                        //ThrowActivity.this.mGaugeViewModel.setAngle(angle);
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum1)).setText(String.format("% 10.2f°", new Object[]{Float.valueOf(fData[6])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum2)).setText(String.format("% 10.2f°", new Object[]{Float.valueOf(fData[7])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum3)).setText(String.format("% 10.2f°", new Object[]{Float.valueOf(fData[8])}));
                                        if (DataActivity.this.type == 6) {
                                            DataActivity.this.mGaugeViewModel.setTemperature(Float.valueOf(((float) (fData[8] / 340.0f)) + 36.53f));
                                            //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum4)).setText(String.format("% 10.2f℃", new Object[]{Double.valueOf(((double) (fData[8] / 340.0f)) + 36.53d)}));
                                            return;
                                        }
                                        DataActivity.this.mGaugeViewModel.setTemperature(Float.valueOf(fData[16]));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum4)).setText(String.format("% 10.2f℃", new Object[]{Float.valueOf(fData[16])}));
                                        return;
                                    case 4:
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum1)).setText(String.format("% 10.0f", new Object[]{Float.valueOf(fData[9])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum2)).setText(String.format("% 10.0f", new Object[]{Float.valueOf(fData[10])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum3)).setText(String.format("% 10.0f", new Object[]{Float.valueOf(fData[11])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum4)).setText(String.format("% 10.2f", new Object[]{Double.valueOf(Math.sqrt((double) (((fData[9] * fData[9]) + (fData[10] * fData[10])) + (fData[11] * fData[11]))))}));
                                        return;
                                    case 5:
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum1)).setText(String.format("% 10.2f", new Object[]{Float.valueOf(fData[12])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum2)).setText(String.format("% 10.2f", new Object[]{Float.valueOf(fData[13])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum3)).setText(String.format("% 10.2f", new Object[]{Float.valueOf(fData[14])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum4)).setText(String.format("% 10.2f", new Object[]{Float.valueOf(fData[15])}));
                                        return;
                                    case 6:
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum1)).setText(String.format("% 10.2fPa", new Object[]{Float.valueOf(fData[17])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum2)).setText(String.format("% 10.2fm", new Object[]{Float.valueOf(fData[18])}));
                                        return;
                                    case 7:
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum1)).setText(String.format("% 14.6f°", new Object[]{Float.valueOf(fData[19])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum2)).setText(String.format("% 14.6f°", new Object[]{Float.valueOf(fData[20])}));
                                        return;
                                    case 8:
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum1)).setText(String.format("% 10.2fm", new Object[]{Float.valueOf(fData[21])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum2)).setText(String.format("% 10.2f°", new Object[]{Float.valueOf(fData[22])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum3)).setText(String.format("% 10.2fm/s", new Object[]{Float.valueOf(fData[23])}));
                                        return;
                                    case 9:
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum1)).setText(String.format("% 7.3f", new Object[]{Float.valueOf(fData[24])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum2)).setText(String.format("% 7.3f", new Object[]{Float.valueOf(fData[25])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum3)).setText(String.format("% 7.3f", new Object[]{Float.valueOf(fData[26])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum4)).setText(String.format("% 7.3f", new Object[]{Float.valueOf(fData[27])}));
                                        return;
                                    case 10:
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum1)).setText(String.format("% 5.0f", new Object[]{Float.valueOf(fData[28])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum2)).setText(String.format("% 7.1f", new Object[]{Float.valueOf(fData[29])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum3)).setText(String.format("% 7.1f", new Object[]{Float.valueOf(fData[30])}));
                                        //((TextView) DataMonitor.this.findViewById(C0242R.id.tvNum4)).setText(String.format("% 7.1f", new Object[]{Float.valueOf(fData[31])}));
                                        return;
                                    default:
                                        return;
                                }
                            case 1:
                                //((TextView) DataMonitor.this.findViewById(C0242R.id.tvAccX)).setText(String.format("% 10.2fg", new Object[]{Float.valueOf(fData[0])}));
                                DataActivity.this.sOffsetAccX = (short) ((int) ((fData[0] / 16.0f) * 32768.0f));
                                //((TextView) DataMonitor.this.findViewById(C0242R.id.tvAccY)).setText(String.format("% 10.2fg", new Object[]{Float.valueOf(fData[1])}));
                                DataActivity.this.sOffsetAccY = (short) ((int) ((fData[1] / 16.0f) * 32768.0f));
                                //((TextView) DataMonitor.this.findViewById(C0242R.id.tvAccZ)).setText(String.format("% 10.2fg", new Object[]{Float.valueOf(fData[2])}));
                                DataActivity.this.sOffsetAccZ = (short) ((int) (((fData[2] - 1.0f) / 16.0f) * 32768.0f));
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
                    DataActivity.this.mConnectedDeviceName = msg.getData().getString("device_name");
                    Toast.makeText(DataActivity.this.getApplicationContext(), "Connected to " + DataActivity.this.mConnectedDeviceName, Toast.LENGTH_LONG).show();
                    return;
                case 5:
                    Toast.makeText(DataActivity.this.getApplicationContext(), msg.getData().getString("toast"), Toast.LENGTH_LONG).show();
                    return;
                default:
                    return;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityRawdataBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_rawdata);
        mGaugeViewModel = ViewModelProviders.of(this).get(ThrowGaugeViewModel.class);
        binding.setCommandthrowViewModel(mGaugeViewModel);
        binding.setLifecycleOwner(this);

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
            //mDevices
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        try {
            this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (this.mBluetoothAdapter == null) {
                //Toast.makeText(this, getString(C0242R.string.Bluetoothbad), 1).show();
                return;
            }
            if (mBluetoothService == null) {
                this.mBluetoothService = new BluetoothService(this, this.mHandler);
                if(this.mBluetoothService != null && this.device != null) {
                    this.mBluetoothService.connect(this.device, 0);
                }
            }
            this.writeBuffer = new byte[512];
            this.readBuffer = new byte[512];
            this.isOpen = false;
            SerialPortOpen();

        } catch (Exception e) {
        }
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
                if (DataActivity.this.mBluetoothService != null && DataActivity.this.mBluetoothService.getState() == 0) {
                    DataActivity.this.mBluetoothService.start();
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
        new DataActivity.readThread().start();
        return true;
    }

    private class readThread extends Thread {
        private readThread() {
        }

        public void run() {
            byte[] buffer = new byte[4096];
            while (true) {
                Message msg = Message.obtain();
                if (DataActivity.this.isOpen) {
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
