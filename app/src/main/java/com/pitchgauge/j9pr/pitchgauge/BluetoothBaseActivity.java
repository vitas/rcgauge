package com.pitchgauge.j9pr.pitchgauge;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;


import java.util.ArrayList;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static com.pitchgauge.j9pr.pitchgauge.BluetoothPipe.DEVICE_BT;
import static com.pitchgauge.j9pr.pitchgauge.BluetoothPipe.REQUEST_ENABLE_BT;
import static com.pitchgauge.j9pr.pitchgauge.BluetoothState.STATE_NONE;

public class BluetoothBaseActivity extends AppCompatActivity {

    protected final String TAG = "BLActivity";

    protected BluetoothService mBluetoothService;

    public byte[] writeBuffer;
    public byte[] readBuffer;
    protected boolean isOpen;

    protected Handler mHandler ;
    protected boolean mIsBound;
    protected BluetoothPipe mBluetoothPipe;
    protected ArrayList<DeviceTag> mDeviceTags;
    protected boolean autoStart;

    protected ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (iBinder instanceof BluetoothService.BackgroundBluetoothBinder) {
                mBluetoothService = ((BluetoothService.BackgroundBluetoothBinder) iBinder).service();

                if (mHandler != null && autoStart) {
                    if (!mBluetoothPipe.isBluetoothEnabled()) {
                        mBluetoothPipe.enable();
                    } else if (!mBluetoothPipe.isServiceAvailable()) {
                        mBluetoothPipe.setupService(mBluetoothService, mHandler);
                        mBluetoothPipe.startService();
                        mBluetoothPipe.autoConnect("yoyo");
                    }

                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            connectDevices();
                        }
                    }, 2000);
                } else {
                    Log.w(TAG, "Service is not setup, data handler is null");
                }

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (mBluetoothPipe != null) {
                mBluetoothPipe.stopService();
            }
        }
    };

    protected void connectDevices() {
        if (mBluetoothPipe.getServiceState() != STATE_NONE) {
            if (mDeviceTags != null) {
                connectDevicesTags();
            } else {
                connectDevicesFromKeyring();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_OK) {

                mBluetoothPipe.setupService(mBluetoothService, mHandler);
                mBluetoothPipe.startService();
                mBluetoothPipe.autoConnect("yoyo");

            } else {
                Toast.makeText(getApplicationContext()
                        , "Bluetooth was not enabled."
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    protected void doBind() {
        bindService(new Intent(this, BluetoothService.class), serviceConnection, BIND_AUTO_CREATE);
        mIsBound = true;
    }

    protected void doUnbind() {
        if (mIsBound) {
            unbindService(serviceConnection);
            mIsBound = false;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBluetoothPipe = new BluetoothPipe(this);

        mBluetoothPipe.setBluetoothStateListener(new BluetoothPipe.BluetoothStateListener() {
            public void onServiceStateChanged(int state) {
                if(state == BluetoothState.STATE_CONNECTED)
                    Log.d(TAG, "State : Connected");
                else if(state == BluetoothState.STATE_CONNECTING)
                    Log.d(TAG, "State : Connecting");
                else if(state == BluetoothState.STATE_LISTEN)
                    Log.d(TAG, "State : Listen");
                else if(state == BluetoothState.STATE_NONE)
                    Log.d(TAG, "State : None");
            }
        });

        mBluetoothPipe.setOnDataReceivedListener(new BluetoothPipe.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {

                Log.d(TAG, "Record Message: "+ message + "\n");

            }
        });

        mBluetoothPipe.setBluetoothConnectionListener(new BluetoothPipe.BluetoothConnectionListener() {
            public void onDeviceConnected(DeviceTag deviceTag) {

                if (mDeviceTags == null) {
                    mDeviceTags = new ArrayList<>();
                }
                mDeviceTags.add(deviceTag);

                Toast.makeText(getApplicationContext()
                        , "Connected " + deviceTag.getName()+ " (" +(deviceTag.getPos()+1)+")"
                        , Toast.LENGTH_SHORT).show();

                SerialPortOpen();

            }

            public void onDeviceDisconnected() {
                Toast.makeText(getApplicationContext()
                        , "Connection lost"
                        , Toast.LENGTH_SHORT).show();
            }

            public void onDeviceConnectionFailed() {
                Log.d(TAG, "Unable to connect");
            }
        });

        mBluetoothPipe.setAutoConnectionListener(new BluetoothPipe.AutoConnectionListener() {
            public void onNewConnection(DeviceTag deviceTag) {

                if (mDeviceTags == null) {
                    mDeviceTags = new ArrayList<>();
                }
                mDeviceTags.add(deviceTag);

                Log.d(TAG, "New Connection - " + deviceTag.getName()+ " " +deviceTag.getAddress());
            }

            public void onAutoConnectionStarted() {
                Log.d(TAG, "Auto menu_connection started");
            }
        });

        if(getIntent().getExtras() != null) {
            mDeviceTags = getIntent().getExtras().getParcelableArrayList(DEVICE_BT);
        }

        doBind();

        this.writeBuffer = new byte[512];
        this.readBuffer = new byte[512];
        this.isOpen = false;

    }

    protected ArrayList<BluetoothDevice> getDevicesFromTags(ArrayList<DeviceTag> tags){
        ArrayList<BluetoothDevice> devices = new ArrayList<>();

        for (DeviceTag tag : tags) {
            BluetoothDevice device = mBluetoothPipe.getBluetoothAdapter().getRemoteDevice(tag.getAddress());
            if (device.getBondState() == BOND_BONDED) {
                devices.add(device);
            }
        }
        return devices;
    }

    protected void connectDevicesTags() {
        if (mDeviceTags == null) {
            Log.e(TAG, "No devices connect to");
            return ;
        }
        for (DeviceTag tag : mDeviceTags) {
            if (tag != null) {
                BluetoothDevice device = mBluetoothPipe.getBluetoothAdapter().getRemoteDevice(tag.getAddress());
                if (device != null) {
                    if (device.getBondState() == BOND_BONDED) {
                        if (tag.getPos() < 0) {
                            tag.setPos(mBluetoothService.getAvailablePosIndexForNewConnection(device));
                        }
                        Log.d(TAG, "connecting to device with address=" + tag.getAddress() + " on pos=" + tag.getPos() + " ");
                        mBluetoothPipe.connect(device, tag.getPos());

                    } else {
                        Log.w(TAG, "device address=" + tag.getAddress() + " on pos=" + tag.getPos() + " is not paired");
                        //TODO auto pairing
                    }
                } else {
                    //TODO remove tag, invalid
                }
            }
        }
    }
    private void connectDevicesFromKeyring() {
        ArrayList<DeviceTag> devices = BluetoothPreferences.getKeyrings(getApplicationContext());

        if (devices.size()>0) {
            mDeviceTags = devices;
            connectDevicesTags();
        } else {
            //TODO
            Log.d(TAG, "no saved devices found. goto selection activity");
        }
    }

    public synchronized void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");

        if (!mIsBound) {
            doBind();
        } else {

            if (mBluetoothPipe.isServiceAvailable() && autoStart) {

                if (mBluetoothPipe.getServiceState() == STATE_NONE) {

                    new Handler().postDelayed(new Runnable() {
                        public void run() {
                            mBluetoothPipe.startService();
                        }
                    }, 50);
                }

                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        connectDevicesFromKeyring();
                    }
                }, 2000);
            }
        }

    }


    @Override
    protected void onDestroy() {
        doUnbind();
        super.onDestroy();
    }

    private boolean SerialPortOpen() {
        this.isOpen = true;
        new readThread().start();
        return true;
    }

    private class readThread extends Thread {

        public void run() {
            byte[] buffer = new byte[4096];
            while (true) {
                Message msg = Message.obtain();
                if (BluetoothBaseActivity.this.isOpen) {
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
