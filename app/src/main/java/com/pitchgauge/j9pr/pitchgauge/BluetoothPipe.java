package com.pitchgauge.j9pr.pitchgauge;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;


public class BluetoothPipe {

    public static final String DEVICE_BT = "btdevice";
    public static final String DEVICE_NAME = "device_name";
    public static final String DEVICE_ADDRESS = "device_address";
    public static final String DEVICE_POS = "device_pos";

    public static final String TOAST = "toast";

    public static final int REQUEST_CONNECT_DEVICE = 1384;
    public static final int REQUEST_ENABLE_BT = 1385;

    private Context mContext;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothService mBluetoothService;

    private boolean isConnected;
    private boolean isConnecting;
    private boolean isServiceRunning;
    private OnDataReceivedListener mDataReceivedListener;
    private BluetoothConnectionListener mBluetoothConnectionListener;
    private AutoConnectionListener mAutoConnectionListener;
    private BluetoothStateListener mBluetoothStateListener;
    private Handler mDataHandler;

    public BluetoothPipe(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean isBluetoothAvailable() {
        try {
            if (mBluetoothAdapter == null || mBluetoothAdapter.getAddress().equals(null))
                return false;
        } catch (NullPointerException e) {
            return false;
        }
        return true;
    }

    public boolean isBluetoothEnabled() {
        return mBluetoothAdapter!= null && mBluetoothAdapter.isEnabled();
    }

    public boolean isServiceAvailable() {
        return mBluetoothService != null;
    }

    public boolean startDiscovery() {
        return mBluetoothAdapter.startDiscovery();
    }

    public boolean isDiscovery() {
        return mBluetoothAdapter.isDiscovering();
    }

    public boolean cancelDiscovery() {
        return mBluetoothAdapter.cancelDiscovery();
    }

    public void setupService(BluetoothService blService, Handler handler) {
        mBluetoothService = blService;
        mDataHandler = handler;
        mBluetoothService.setHandler(mHandler);
        mBluetoothService.setDataHandler(mDataHandler);
    }


    public  void connect(BluetoothDevice device, int pos) {
        if (isServiceAvailable()) {
            mBluetoothService.connect(device,pos );
        }
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    public int getServiceState() {
        if(mBluetoothService != null)
            return mBluetoothService.getState();
        else
            return -1;
    }

    public void startService() {
        if (mBluetoothService != null) {
            if (mBluetoothService.getState() == BluetoothState.STATE_NONE) {
                isServiceRunning = true;
                mBluetoothService.start();
            }
        }
    }

    public void stopService() {
        if (mBluetoothService != null) {
            isServiceRunning = false;
            mBluetoothService.stop();
        }
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (mBluetoothService != null) {
                    isServiceRunning = false;
                    mBluetoothService.stop();
                }
            }
        }, 500);
    }

    public void disconnect() {
        if(mBluetoothService != null) {
            isServiceRunning = false;
            mBluetoothService.stop();
            if(mBluetoothService.getState() == BluetoothState.STATE_NONE) {
                isServiceRunning = true;
                mBluetoothService.start();
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothState.MESSAGE_WRITE:
                    //Log.d(TA)
                    break;
                case BluetoothState.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf);
                    if(readBuf != null && readBuf.length > 0) {
                        if(mDataReceivedListener != null)
                            mDataReceivedListener.onDataReceived(readBuf, readMessage);
                    }
                    break;
                case BluetoothState.MESSAGE_DEVICE_NAME:

                    DeviceTag tag = new DeviceTag();
                    tag.setName(msg.getData().getString(DEVICE_NAME));
                    tag.setAddress(msg.getData().getString(DEVICE_ADDRESS));
                    tag.setPos(msg.getData().getInt(DEVICE_POS, 0));

                    if(mBluetoothConnectionListener != null)
                        mBluetoothConnectionListener.onDeviceConnected(tag);
                    isConnected = true;
                    break;
                case BluetoothState.MESSAGE_TOAST:
                    Toast.makeText(mContext, msg.getData().getString(TOAST)
                            , Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothState.MESSAGE_STATE_CHANGE:
                    if(mBluetoothStateListener != null)
                        mBluetoothStateListener.onServiceStateChanged(msg.arg1);
                    if(isConnected && msg.arg1 != BluetoothState.STATE_CONNECTED) {
                        if(mBluetoothConnectionListener != null)
                            mBluetoothConnectionListener.onDeviceDisconnected();
                        autoConnect("yoyo");
                        /*if(isAutoConnectionEnabled) {
                            isAutoConnectionEnabled = false;
                            autoConnect(keyword);
                        }*/
                        isConnected = false;
                    }

                    if(!isConnecting && msg.arg1 == BluetoothState.STATE_CONNECTING) {
                        isConnecting = true;
                    } else if(isConnecting) {
                        if(msg.arg1 != BluetoothState.STATE_CONNECTED) {
                            if(mBluetoothConnectionListener != null)
                                mBluetoothConnectionListener.onDeviceConnectionFailed();
                        }
                        isConnecting = false;
                    }
                    break;
            }
        }
    };

    public void enable() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.enable();
        }
    }

    public void send(byte[] data, boolean CRLF) {
        if(mBluetoothService.getState() == BluetoothState.STATE_CONNECTED) {
            if(CRLF) {
                byte[] data2 = new byte[data.length + 2];
                for(int i = 0 ; i < data.length ; i++)
                    data2[i] = data[i];
                data2[data2.length - 2] = 0x0A;
                data2[data2.length - 1] = 0x0D;
                mBluetoothService.Send(data2);
            } else {
                mBluetoothService.Send(data);
            }
        }
    }

    public void send(String data, boolean CRLF) {
        if(mBluetoothService.getState() == BluetoothState.STATE_CONNECTED) {
            if(CRLF)
                data += "\r\n";
            mBluetoothService.Send(data.getBytes());
        }
    }

    public void setBluetoothStateListener (BluetoothStateListener listener) {
        mBluetoothStateListener = listener;
    }

    public void setOnDataReceivedListener (OnDataReceivedListener listener) {
        mDataReceivedListener = listener;
    }

    public void setBluetoothConnectionListener (BluetoothConnectionListener listener) {
        mBluetoothConnectionListener = listener;
    }

    public void setAutoConnectionListener(AutoConnectionListener listener) {
        mAutoConnectionListener = listener;
    }

    public void autoConnect(String keyword) {
    }

    public interface BluetoothStateListener {
        public void onServiceStateChanged(int state);
    }

    public interface AutoConnectionListener {
        public void onAutoConnectionStarted();
        public void onNewConnection(DeviceTag deviceTag);
    }

    public interface OnDataReceivedListener {
        public void onDataReceived(byte[] data, String message);
    }

    public interface BluetoothConnectionListener {
        public void onDeviceConnected(DeviceTag deviceTag);
        public void onDeviceDisconnected();
        public void onDeviceConnectionFailed();
    }

}
