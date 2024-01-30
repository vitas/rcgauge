package com.pitchgauge.j9pr.pitchgauge;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import static com.pitchgauge.j9pr.pitchgauge.BluetoothPipe.DEVICE_ADDRESS;
import static com.pitchgauge.j9pr.pitchgauge.BluetoothPipe.DEVICE_NAME;
import static com.pitchgauge.j9pr.pitchgauge.BluetoothPipe.DEVICE_POS;

public class BluetoothService extends Service {
    // well-known Bluetooth serial board SPP UUID
    private static final UUID MY_UUID = UUID.fromString((String)"00001101-0000-1000-8000-00805F9B34FB");

    private static final String NAME = "BluetoothData";

    private short IDNow;
    private short IDSave = (short) 0;
    private int SaveState = -1;
    private int ar = 16;
    private int av = 2000;
    private int iError = 0;
    long[] lLastTime = {System.currentTimeMillis(), System.currentTimeMillis()};
    private AcceptThread mAcceptThread;
    private final BluetoothAdapter mAdapter;
    private ConnectThread mConnectThread;
    public ConnectedThread mConnectedThread;
    private Handler mHandler;
    private Handler mDataHandler;

    private int mState;
    MyFile myFile;
    private int sDataSave = 0;
    String strDate = "";
    String strTime = "";
    boolean mSuspend = false;

    private List<String> mDeviceAddresses;
    private List<String> mDeviceNames;
    private List<ConnectedThread> mConnThreads;
    private static ArrayList<ConnectedThread> activeConnectedThreads=new ArrayList<ConnectedThread>();

    private List<BluetoothSocket> mSockets;
    private List<float[]> mfDatas;
    private List<Queue<Byte>> mQueueBuffers;
    private List<byte[]> mPackBuffers;

    
    private static final int MAX_SENSOR_COUNT = 2;
    private final String TAG = "BLService";

    private BackgroundBluetoothBinder mBinder = new BackgroundBluetoothBinder();

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public void setDataHandler(Handler handler) {
        mDataHandler = handler;
    }


    public class BackgroundBluetoothBinder extends Binder {
        public BluetoothService service() {
            return BluetoothService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mBinder = null;
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //checkIsBleEnabled();

        this.start();
        Log.d(TAG, "onStartCommand() - start/connect");

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        if (mHandler != null) {
            // Give the new state to the Handler so the UI Activity can update
            mHandler.obtainMessage(BluetoothState.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
        }
    }

    private class AcceptThread extends Thread {
        private BluetoothServerSocket mmServerSocket;
        boolean isRunning = true;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(BluetoothService.NAME, BluetoothService.MY_UUID);

            } catch (IOException e) {
            }
            this.mmServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread");
            BluetoothSocket socket = null;
            while (mState != BluetoothState.STATE_CONNECTED && isRunning) {

                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }

                if (socket != null) {

                    synchronized (BluetoothService.this) {
                        switch (mState) {
                            case BluetoothState.STATE_LISTEN:
                            case BluetoothState.STATE_CONNECTING:
                                connected(socket, socket.getRemoteDevice(), getAvailablePosIndexForNewConnection(socket.getRemoteDevice()));
                                break;
                            case BluetoothState.STATE_NONE:
                            case BluetoothState.STATE_CONNECTED:
                                // Either not ready or already connected. Terminate
                                // new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            if (mmServerSocket != null) {
	            try {
	                this.mmServerSocket.close();
	                mmServerSocket = null;

	            } catch (IOException e) {
	            }
			}
        }

        public void kill() {
            isRunning = false;
        }
    }


    private class ConnectThread extends Thread {
        private final BluetoothDevice mmDevice;
        private final BluetoothSocket mmSocket;
        private int position;

        public ConnectThread(BluetoothDevice device, UUID uuid, int pos) {
            this.mmDevice = device;
            BluetoothSocket tmp = null;
            position = pos;
            try {
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
            }
            this.mmSocket = tmp;
        }

        public void run() {
            setName("ConnectThread" + position);

            //mAdapter.cancelDiscovery();
            try {
                this.mmSocket.connect();

            } catch (IOException e) {

                try {
                    this.mmSocket.close();
                } catch (IOException e2) {
                }
                connectionFailed(mmDevice);
                return;
            }

            synchronized (BluetoothService.this) {
                BluetoothService.this.mConnectThread = null;
            }

            mDeviceAddresses.set(position, mmDevice.getAddress());
            mDeviceNames.set(position, mmDevice.getName());
            mSockets.set(position, mmSocket);

            connected(mmSocket, mmDevice, position);

        }

        public void cancel() {
            try {
                this.mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final BluetoothSocket mmSocket;
        private boolean isRunningConnectedThread = true;
        private int position;

        public ConnectedThread(BluetoothSocket socket, int pos) {
            activeConnectedThreads.add(this);
            this.mmSocket = socket;
            this.position = pos;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }
            this.mmInStream = tmpIn;
            this.mmOutStream = tmpOut;
        }

        public void run() {
            byte[] tempInputBuffer = new byte[1024];
            setName("ConnectedThread" + position);

            while (isRunningConnectedThread) {
                try {
                    int acceptedLen = this.mmInStream.read(tempInputBuffer);
                    if (acceptedLen > 0) {
                        int positionIndex = getPosIndexOfDevice(mmSocket.getRemoteDevice());
                        if (positionIndex != -1) {
                            BluetoothService.this.CopeSerialData(acceptedLen, tempInputBuffer, positionIndex);
                        }
                    }


                } catch (IOException e) {
                    Log.e(TAG, "got disconnected " + mmSocket.getRemoteDevice(), e);
                    connectionLost(mmSocket.getRemoteDevice());
                    return;
                }
            }
            try {
                this.mmSocket.close();
            } catch (IOException e) {
            }
        }

        public void write(byte[] buffer) {
            try {
                this.mmOutStream.write(buffer);
                BluetoothService.this.mDataHandler.obtainMessage(BluetoothState.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
                mmOutStream.flush();

            } catch (IOException e) {
            }
        }

        public void cancel() {
            try {this.mmInStream.close();} catch (Exception e) {}
            try {this.mmOutStream.close();} catch (Exception e) {}
            try {this.mmSocket.close();} catch (Exception e) {}
            isRunningConnectedThread = false;
        }
    }

    public BluetoothService() {
        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = BluetoothState.STATE_NONE;
        resetLists();
    }

    private void resetLists() {
        mDeviceAddresses = new ArrayList<>(MAX_SENSOR_COUNT);
        mDeviceNames = new ArrayList<>(MAX_SENSOR_COUNT);
        mConnThreads = new ArrayList<>(MAX_SENSOR_COUNT);
        mSockets = new ArrayList<>(MAX_SENSOR_COUNT);
        mfDatas = new ArrayList<>(MAX_SENSOR_COUNT);
        mPackBuffers = new ArrayList<>(MAX_SENSOR_COUNT);
        mQueueBuffers = new ArrayList<>(MAX_SENSOR_COUNT);
                
        for (int i = 0; i < MAX_SENSOR_COUNT; i++) {
            mDeviceAddresses.add(null);
            mDeviceNames.add(null);
            mConnThreads.add(null);
            mSockets.add(null);
            mfDatas.add(new float[36]);
            mPackBuffers.add(new byte[11]);
            mQueueBuffers.add(new LinkedList());
        }
    }

    public boolean isDeviceConnectedAtPos(int position) {
        if (mConnThreads.get(position) == null && mSockets.get(position) != null) {
            return false;
        }
        return true;
    }


    private int getPosIndexOfDevice(BluetoothDevice device) {
        for (int i = 0; i < mDeviceAddresses.size(); i++) {
            if (mDeviceAddresses.get(i) != null
                    && mDeviceAddresses.get(i).equalsIgnoreCase(
                    device.getAddress()))
                return i;
        }
        return -1;
    }

    public int getAvailablePosIndexForNewConnection(BluetoothDevice device) {
        if (getPosIndexOfDevice(device) == -1) {
            for (int i = 0; i < mDeviceAddresses.size(); i++) {
                if (mDeviceAddresses.get(i) == null) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void Send(byte[] out) {
        // When writing, try to write out to all connected threads
        Log.d(TAG, "Start Writing..." + mConnThreads.size());
        for (int i = 0; i < mConnThreads.size(); i++) {
            try {
                // Create temporary object
                ConnectedThread r;
                // Synchronize a copy of the ConnectedThread
                synchronized (this) {
                    if (mState != BluetoothState.STATE_CONNECTED) return;
                    r = mConnThreads.get(i);
                }
                // Perform the write unsynchronized
                if (r != null) {
                    if (r.isAlive())
                        r.write(out);
                    else
                        r.cancel();
                }
            } catch (Exception e) {
            }
        }
    }


    public synchronized int getState() {
        return this.mState;
    }

    public synchronized void start() {

        // Cancel any thread attempting to make a connection
        if (this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(BluetoothState.STATE_LISTEN);

        if (this.mAcceptThread == null) {
            this.mAcceptThread = new AcceptThread();
            this.mAcceptThread.start();
        }
    }

    public synchronized void connect(BluetoothDevice device, int pos) {

        if (getPosIndexOfDevice(device) == -1) {

            Log.d(TAG, "BluetoothService connect(): connect to: " + device.getName() + " Address: " + device.getAddress());

            if (isDeviceConnectedAtPos(pos) && mState == BluetoothState.STATE_CONNECTED) {
                Log.d(TAG, "Already connected to: " + device.getName()+ " Address: " + device.getAddress()+ " ignore");
                return;
            }

            if (mState == BluetoothState.STATE_CONNECTING) {
                if (mConnectThread != null) {
                    mConnectThread.cancel();
                    mConnectThread = null;
                }
            }

            if (mConnThreads.get(pos) != null) {
                mConnThreads.get(pos).cancel();
                mConnThreads.set(pos, null);
            }

            try {
                // using the well-known Bluetooth serial board SPP UUID
                Log.d(TAG, "BluetoothService connect(): Connect device UUID===" + MY_UUID);

                ConnectThread mConnectThread = new ConnectThread(device, MY_UUID, pos);
                mConnectThread.start();

                setState(BluetoothState.STATE_CONNECTING);

            } catch (Exception e) {
                Log.e(TAG, "Exception connect to: " + device.getName()+ " Address: " + device.getAddress(), e);

            }
        }
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, int pos) {

        mDeviceAddresses.set(pos, device.getAddress());
        mDeviceNames.set(pos, device.getName());
        mSockets.set(pos, socket);

        if (this.mConnectThread != null) {
            this.mConnectThread.cancel();
            this.mConnectThread = null;
        }

		// TODO AHa: why kill mAcceptThread so early, exit from blocking accept()
        if (this.mAcceptThread != null) {
            this.mAcceptThread.cancel();
            this.mAcceptThread.kill();
            this.mAcceptThread = null;
        }
        this.mConnectedThread = new ConnectedThread(socket, pos);
        mConnThreads.set(pos, mConnectedThread);
        this.mConnectedThread.start();

        Message msg = this.mHandler.obtainMessage(BluetoothState.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(DEVICE_NAME, device.getName());
        bundle.putString(DEVICE_ADDRESS, device.getAddress());
        bundle.putInt(DEVICE_POS, pos);

        msg.setData(bundle);
        this.mHandler.sendMessage(msg);
        setState(BluetoothState.STATE_CONNECTED);
    }

    private void stopping() {

        // stop all ConnectedThreads
        Iterator<ConnectedThread> itr = activeConnectedThreads.iterator();
        while (itr.hasNext()) {
            ConnectedThread t = itr.next();
            if (t.isAlive()) {
                t.cancel();
                Log.d(TAG, "BTService.stopped() id=" + t.getId() + " t=" + t.toString());
            }
            itr.remove();
        }
		
        if (this.mAcceptThread != null) {
            this.mAcceptThread.cancel();
            this.mAcceptThread = null;
        }

        for (int i = 0; i < MAX_SENSOR_COUNT; i++) {
            mDeviceNames.set(i, null);
            mDeviceAddresses.set(i, null);
            mSockets.set(i, null);
        }

        if (mAdapter != null){
            mAdapter.cancelDiscovery();
        }
        setState(BluetoothState.STATE_NONE);

    }

    @Override
    public boolean stopService(Intent name) {
        stopping();
        return super.stopService(name);
    }

    public synchronized void stop() {
        stopping();
        stopSelf();
    }

    private void connectionFailed(BluetoothDevice device) {
        setState(BluetoothState.STATE_LISTEN);
        Message msg = this.mHandler.obtainMessage(BluetoothState.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothPipe.TOAST, "Failed connect " + device.getName()+ "\n" + device.getAddress());
        msg.setData(bundle);
        this.mHandler.sendMessage(msg);

        BluetoothService.this.start();
    }

    private void connectionLost(BluetoothDevice device) {
        int positionIndex = getPosIndexOfDevice(device);
        if (positionIndex != -1) {

            Log.d(TAG, "getPosIndexOfDevice(device) ==="
                    + mDeviceAddresses.get(getPosIndexOfDevice(device)));

            mDeviceAddresses.set(positionIndex, null);
            mDeviceNames.set(positionIndex, null);
            mSockets.set(positionIndex, null);
            mConnThreads.set(positionIndex, null);

            setState(BluetoothState.STATE_LISTEN);

            Message msg = this.mHandler.obtainMessage(BluetoothState.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(BluetoothPipe.TOAST, "Device connection was lost from " + device.getName()+ " " + device.getAddress());
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }

        // TODO AHa: restart not working as expected, connectionLost is called on destroy/stop also
        BluetoothService.this.start();
    }

    public void Suspend(boolean state){
        mSuspend = state;
    }

    // decode sensor output packets
    public void CopeSerialData(int acceptedLen, byte[] tempInputBuffer, int pos) {
        for (int i = 0; i < acceptedLen; i++) {
            mQueueBuffers.get(pos).add(Byte.valueOf(tempInputBuffer[i]));
        }
        while (mQueueBuffers.get(pos).size() >= 11) {
            if (((Byte) mQueueBuffers.get(pos).poll()).byteValue() == (byte) 85) { // header 0x55
                byte sHead = ((Byte) mQueueBuffers.get(pos).poll()).byteValue();
                if ((sHead & 240) == 80) {
                    this.iError = 0;
                }
                for (int j = 0; j < 9; j++) {
                    mPackBuffers.get(pos)[j] = ((Byte) mQueueBuffers.get(pos).poll()).byteValue();
                }
                switch (sHead) {
                    case (byte) 80: // 0x50 time
                        int ms = (((short) mPackBuffers.get(pos)[7]) << 8) | (((short) mPackBuffers.get(pos)[6]) & 255);
                        this.strDate = String.format("20%02d-%02d-%02d", new Object[]{Byte.valueOf(mPackBuffers.get(pos)[0]), Byte.valueOf(mPackBuffers.get(pos)[1]), Byte.valueOf(mPackBuffers.get(pos)[2])});
                        this.strTime = String.format(" %02d:%02d:%02d.%03d", new Object[]{Byte.valueOf(mPackBuffers.get(pos)[3]), Byte.valueOf(mPackBuffers.get(pos)[4]), Byte.valueOf(mPackBuffers.get(pos)[5]), Integer.valueOf(ms)});
                        //RecordData(sHead, this.strDate + this.strTime);
                        break;
                    case (byte) 81: // 0x51 acceleration
                        mfDatas.get(pos)[0] = ((float) ((((short) mPackBuffers.get(pos)[1]) << 8) | (((short) mPackBuffers.get(pos)[0]) & 255))) / 32768.0f;
                        mfDatas.get(pos)[1] = ((float) ((((short) mPackBuffers.get(pos)[3]) << 8) | (((short) mPackBuffers.get(pos)[2]) & 255))) / 32768.0f;
                        mfDatas.get(pos)[2] = ((float) ((((short) mPackBuffers.get(pos)[5]) << 8) | (((short) mPackBuffers.get(pos)[4]) & 255))) / 32768.0f;
                        mfDatas.get(pos)[16] = ((float) ((((short) mPackBuffers.get(pos)[7]) << 8) | (((short) mPackBuffers.get(pos)[6]) & 255))) / 100.0f;
                        //RecordData(sHead, String.format("% 10.2f", new Object[]{Float.valueOf(mfDatas.get(pos)[0])}) + String.format("% 10.2f", new Object[]{Float.valueOf(mfDatas.get(pos)[1])}) + String.format("% 10.2f", new Object[]{Float.valueOf(mfDatas.get(pos)[2])}) + " ");
                        break;
                    case (byte) 82: // 0x52 angular velocity
                        mfDatas.get(pos)[3] = ((float) ((((short) mPackBuffers.get(pos)[1]) << 8) | (((short) mPackBuffers.get(pos)[0]) & 255))) / 32768.0f;
                        mfDatas.get(pos)[4] = ((float) ((((short) mPackBuffers.get(pos)[3]) << 8) | (((short) mPackBuffers.get(pos)[2]) & 255))) / 32768.0f;
                        mfDatas.get(pos)[5] = ((float) ((((short) mPackBuffers.get(pos)[5]) << 8) | (((short) mPackBuffers.get(pos)[4]) & 255))) / 32768.0f;
                        mfDatas.get(pos)[16] = ((float) ((((short) mPackBuffers.get(pos)[7]) << 8) | (((short) mPackBuffers.get(pos)[6]) & 255))) / 100.0f;
                        //RecordData(sHead, String.format("% 10.2f", new Object[]{Float.valueOf(mfDatas.get(pos)[3])}) + String.format("% 10.2f", new Object[]{Float.valueOf(mfDatas.get(pos)[4])}) + String.format("% 10.2f", new Object[]{Float.valueOf(mfDatas.get(pos)[5])}) + " ");
                        break;
                    case (byte) 83: // 0x53 angle output
                        mfDatas.get(pos)[6] = (((float) ((((short) mPackBuffers.get(pos)[1]) << 8) | (((short) mPackBuffers.get(pos)[0]) & 255))) / 32768.0f) * 180.0f;
                        mfDatas.get(pos)[7] = (((float) ((((short) mPackBuffers.get(pos)[3]) << 8) | (((short) mPackBuffers.get(pos)[2]) & 255))) / 32768.0f) * 180.0f;
                        mfDatas.get(pos)[8] = (((float) ((((short) mPackBuffers.get(pos)[5]) << 8) | (((short) mPackBuffers.get(pos)[4]) & 255))) / 32768.0f) * 180.0f;
                        mfDatas.get(pos)[16] = ((float) ((((short) mPackBuffers.get(pos)[7]) << 8) | (((short) mPackBuffers.get(pos)[6]) & 255))) / 100.0f;
                        //RecordData(sHead, String.format("% 10.2f", new Object[]{Float.valueOf(mfDatas.get(pos)[6])}) + String.format("% 10.2f", new Object[]{Float.valueOf(mfDatas.get(pos)[7])}) + String.format("% 10.2f", new Object[]{Float.valueOf(mfDatas.get(pos)[8])}));
                        break;
                    case (byte) 84:
                        mfDatas.get(pos)[9] = (float) ((((short) mPackBuffers.get(pos)[1]) << 8) | (((short) mPackBuffers.get(pos)[0]) & 255));
                        mfDatas.get(pos)[10] = (float) ((((short) mPackBuffers.get(pos)[3]) << 8) | (((short) mPackBuffers.get(pos)[2]) & 255));
                        mfDatas.get(pos)[11] = (float) ((((short) mPackBuffers.get(pos)[5]) << 8) | (((short) mPackBuffers.get(pos)[4]) & 255));
                        mfDatas.get(pos)[16] = ((float) ((((short) mPackBuffers.get(pos)[7]) << 8) | (((short) mPackBuffers.get(pos)[6]) & 255))) / 100.0f;
                        //RecordData(sHead, String.format("% 10.2f", new Object[]{Float.valueOf(mfDatas.get(pos)[9])}) + String.format("% 10.2f", new Object[]{Float.valueOf(mfDatas.get(pos)[10])}) + String.format("% 10.2f", new Object[]{Float.valueOf(mfDatas.get(pos)[11])}));
                        break;
                    case (byte) 85:
                        mfDatas.get(pos)[12] = (float) ((((short) mPackBuffers.get(pos)[1]) << 8) | (((short) mPackBuffers.get(pos)[0]) & 255));
                        mfDatas.get(pos)[13] = (float) ((((short) mPackBuffers.get(pos)[3]) << 8) | (((short) mPackBuffers.get(pos)[2]) & 255));
                        mfDatas.get(pos)[14] = (float) ((((short) mPackBuffers.get(pos)[5]) << 8) | (((short) mPackBuffers.get(pos)[4]) & 255));
                        mfDatas.get(pos)[15] = (float) ((((short) mPackBuffers.get(pos)[7]) << 8) | (((short) mPackBuffers.get(pos)[6]) & 255));
                        //RecordData(sHead, String.format("% 7.0f", new Object[]{Float.valueOf(mfDatas.get(pos)[12])}) + String.format("% 7.0f", new Object[]{Float.valueOf(mfDatas.get(pos)[13])}) + String.format("% 7.0f", new Object[]{Float.valueOf(mfDatas.get(pos)[14])}) + String.format("% 7.0f", new Object[]{Float.valueOf(mfDatas.get(pos)[15])}));
                        break;
                    case (byte) 86:
                        mfDatas.get(pos)[17] = (float) (((((((long) mPackBuffers.get(pos)[3]) << 24) & -16777216) | ((((long) mPackBuffers.get(pos)[2]) << 16) & 16711680)) | ((((long) mPackBuffers.get(pos)[1]) << 8) & 65280)) | (((long) mPackBuffers.get(pos)[0]) & 255));
                        mfDatas.get(pos)[18] = ((float) (((((((long) mPackBuffers.get(pos)[7]) << 24) & -16777216) | ((((long) mPackBuffers.get(pos)[6]) << 16) & 16711680)) | ((((long) mPackBuffers.get(pos)[5]) << 8) & 65280)) | (((long) mPackBuffers.get(pos)[4]) & 255))) / 100.0f;
                        //RecordData(sHead, String.format("% 10.2f", new Object[]{Float.valueOf(mfDatas.get(pos)[17])}) + String.format("% 10.2f", new Object[]{Float.valueOf(mfDatas.get(pos)[18])}));
                        break;
                    case (byte) 87:
                        long Longitude = ((((((long) mPackBuffers.get(pos)[3]) << 24) & -16777216) | ((((long) mPackBuffers.get(pos)[2]) << 16) & 16711680)) | ((((long) mPackBuffers.get(pos)[1]) << 8) & 65280)) | (((long) mPackBuffers.get(pos)[0]) & 255);
                        mfDatas.get(pos)[19] = (float) (((double) (Longitude / 10000000)) + ((((double) ((float) (Longitude % 10000000))) / 100000.0d) / 60.0d));
                        long Latitude = ((((((long) mPackBuffers.get(pos)[7]) << 24) & -16777216) | ((((long) mPackBuffers.get(pos)[6]) << 16) & 16711680)) | ((((long) mPackBuffers.get(pos)[5]) << 8) & 65280)) | (((long) mPackBuffers.get(pos)[4]) & 255);
                        mfDatas.get(pos)[20] = (float) (((double) (Latitude / 10000000)) + ((((double) ((float) (Latitude % 10000000))) / 100000.0d) / 60.0d));
                        //RecordData(sHead, String.format("% 14.6f", new Object[]{Float.valueOf(mfDatas.get(pos)[19])}) + String.format("% 14.6f", new Object[]{Float.valueOf(mfDatas.get(pos)[20])}));
                        break;
                    case (byte) 88:
                        mfDatas.get(pos)[21] = ((float) ((((short) mPackBuffers.get(pos)[1]) << 8) | (((short) mPackBuffers.get(pos)[0]) & 255))) / 10.0f;
                        mfDatas.get(pos)[22] = ((float) ((((short) mPackBuffers.get(pos)[3]) << 8) | (((short) mPackBuffers.get(pos)[2]) & 255))) / 100.0f;
                        mfDatas.get(pos)[23] = ((float) (((((((long) mPackBuffers.get(pos)[7]) << 24) & -16777216) | ((((long) mPackBuffers.get(pos)[6]) << 16) & 16711680)) | ((((long) mPackBuffers.get(pos)[5]) << 8) & 65280)) | (((long) mPackBuffers.get(pos)[4]) & 255))) / 1000.0f;
                        //RecordData(sHead, String.format("% 10.2f", new Object[]{Float.valueOf(mfDatas.get(pos)[21])}) + String.format("% 10.2f", new Object[]{Float.valueOf(mfDatas.get(pos)[22])}) + String.format("% 10.2f", new Object[]{Float.valueOf(mfDatas.get(pos)[23])}));
                        break;
                    case (byte) 89: // 0x59 quaternion
                        mfDatas.get(pos)[24] = ((float) ((((short) mPackBuffers.get(pos)[1]) << 8) | (((short) mPackBuffers.get(pos)[0]) & 255))) / 32768.0f;
                        mfDatas.get(pos)[25] = ((float) ((((short) mPackBuffers.get(pos)[3]) << 8) | (((short) mPackBuffers.get(pos)[2]) & 255))) / 32768.0f;
                        mfDatas.get(pos)[26] = ((float) ((((short) mPackBuffers.get(pos)[5]) << 8) | (((short) mPackBuffers.get(pos)[4]) & 255))) / 32768.0f;
                        mfDatas.get(pos)[27] = ((float) ((((short) mPackBuffers.get(pos)[7]) << 8) | (((short) mPackBuffers.get(pos)[6]) & 255))) / 32768.0f;
                        //RecordData(sHead, String.format("% 7.3f", new Object[]{Float.valueOf(mfDatas.get(pos)[24])}) + String.format("% 7.3f", new Object[]{Float.valueOf(mfDatas.get(pos)[25])}) + String.format("% 7.3f", new Object[]{Float.valueOf(mfDatas.get(pos)[26])}) + String.format("% 7.3f", new Object[]{Float.valueOf(mfDatas.get(pos)[27])}));
                        break;
                    case (byte) 90:
                        mfDatas.get(pos)[28] = (float) ((((short) mPackBuffers.get(pos)[1]) << 8) | (((short) mPackBuffers.get(pos)[0]) & 255));
                        mfDatas.get(pos)[29] = ((float) ((((short) mPackBuffers.get(pos)[3]) << 8) | (((short) mPackBuffers.get(pos)[2]) & 255))) / 100.0f;
                        mfDatas.get(pos)[30] = ((float) ((((short) mPackBuffers.get(pos)[5]) << 8) | (((short) mPackBuffers.get(pos)[4]) & 255))) / 100.0f;
                        mfDatas.get(pos)[31] = ((float) ((((short) mPackBuffers.get(pos)[7]) << 8) | (((short) mPackBuffers.get(pos)[6]) & 255))) / 100.0f;
                        //RecordData(sHead, String.format("% 5.0f", new Object[]{Float.valueOf(mfDatas.get(pos)[28])}) + String.format("% 7.1f", new Object[]{Float.valueOf(mfDatas.get(pos)[29])}) + String.format("% 7.1f", new Object[]{Float.valueOf(mfDatas.get(pos)[30])}) + String.format("% 7.1f", new Object[]{Float.valueOf(mfDatas.get(pos)[31])}));
                        break;
                    case (byte) 95: // BWT901 Read Response
                        mfDatas.get(pos)[32] = (float) ((((short) mPackBuffers.get(pos)[1]) << 8) | (((short) mPackBuffers.get(pos)[0]) & 255));
                        mfDatas.get(pos)[33] = (float) ((((short) mPackBuffers.get(pos)[3]) << 8) | (((short) mPackBuffers.get(pos)[2]) & 255));
                        mfDatas.get(pos)[34] = (float) ((((short) mPackBuffers.get(pos)[5]) << 8) | (((short) mPackBuffers.get(pos)[4]) & 255));
                        mfDatas.get(pos)[35] = (float) ((((short) mPackBuffers.get(pos)[7]) << 8) | (((short) mPackBuffers.get(pos)[6]) & 255));
                        //RecordData(sHead, String.format("% 5.0f", new Object[]{Float.valueOf(mfDatas.get(pos)[28])}) + String.format("% 7.1f", new Object[]{Float.valueOf(mfDatas.get(pos)[29])}) + String.format("% 7.1f", new Object[]{Float.valueOf(mfDatas.get(pos)[30])}) + String.format("% 7.1f", new Object[]{Float.valueOf(mfDatas.get(pos)[31])}));
                        break;
                    default:
                        break;
                }
            }
            this.iError++;
        }

        long lTimeNow = System.currentTimeMillis();
        long delta = lTimeNow - this.lLastTime[pos];
        if (delta > 10) { // avoid short update intervals
            this.lLastTime[pos] = lTimeNow;
            //Log.e(TAG, "pos=" + pos + " inputBuffer delta(ms)=" + delta);
            if (mDataHandler != null) {
                Message msg = this.mDataHandler.obtainMessage(BluetoothState.MESSAGE_READ);
                Bundle bundle = new Bundle();
                bundle.putFloatArray("Data", mfDatas.get(pos));
                bundle.putString("Date", this.strDate);
                bundle.putString("Time", this.strTime);
                bundle.putInt("Pos", pos);
                msg.setData(bundle);
                if (!mSuspend) {
                    this.mDataHandler.sendMessage(msg);
                }
            }
        }
    }

    public void RecordData(byte ID, String str) {
        boolean Repeat = false;
        short sData = (short) (1 << (ID & 15));
        try {
            if ((this.IDNow & sData) != sData || sData >= this.sDataSave) {
                this.IDNow = (short) (this.IDNow | sData);
            } else {
                this.IDSave = this.IDNow;
                this.IDNow = sData;
                Repeat = true;
            }
            this.sDataSave = sData;
            switch (this.SaveState) {
                case 0:
                    this.myFile.Close();
                    this.SaveState = -1;
                    return;
                case 1:
                    this.myFile = new MyFile("/mnt/sdcard/Record.txt");
                    String s = "StartTime：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ").format(new Date(System.currentTimeMillis())) + "\r\n";
                    if ((this.IDSave & 2) > 0) {
                        s = s + "  AX： AY： AZ：";
                    }
                    if ((this.IDSave & 4) > 0) {
                        s = s + "  WX： WY： WZ：";
                    }
                    if ((this.IDSave & 8) > 0) {
                        s = s + "    AngleX：   AngleY：   AngleZ：";
                    }
                    if ((this.IDSave & 16) > 0) {
                        s = s + "   MagX：   MagY：   MagZ：";
                    }
                    if ((this.IDSave & 32) > 0) {
                        s = s + "Port0：Port1：Port2：Port3：";
                    }
                    if ((this.IDSave & 64) > 0) {
                        s = s + "    Pressure：    Height：";
                    }
                    if ((this.IDSave & 128) > 0) {
                        s = s + "        Longitude：        Latitude：";
                    }
                    if ((this.IDSave & 256) > 0) {
                        s = s + "    Elevation：    Coures：    Ground velocity：";
                    }
                    if ((this.IDSave & 512) > 0) {
                        s = s + "   q0：   q1：   q2：   q3：";
                    }
                    if ((this.IDSave & 1024) > 0) {
                        s = s + "Star Number：PDOP： HDOP： VDOP：";
                    }
                    this.myFile.Write(s + "\r\n");
                    if (Repeat) {
                        this.myFile.Write(str);
                        this.SaveState = 2;
                        return;
                    }
                    return;
                case 2:
                    if (Repeat) {
                        this.myFile.Write("  \r\n");
                    }
                    this.myFile.Write(str);
                    return;
                default:
                    return;
            }
        } catch (Exception e) {
        }
    }

    public void setRecord(boolean record) {
        if (record) {
            this.SaveState = 1;
        } else {
            this.SaveState = 0;
        }
    }
}
