package com.pitchgauge.j9pr.pitchgauge;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DeviceListActivity extends AppCompatActivity implements PermissionDialogFragment.PermissionDialogListener {

    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    private static final String TAG = "DeviceListActivity";
    private BluetoothAdapter mBtAdapter;
    private OnItemClickListener mDeviceClickListener = new DeviceButtonClickListener();
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private final BroadcastReceiver mReceiver = new DiscoveryBroadCastReceiver();

    private static final int PERMISSION_REQUEST = 20000;
    private static final int REQUEST_DEVICE_DISCOVERY = 20001;
    private static final int REQUEST_SELECT_DEVICE  = 20002;
    private int m_UserRequestType = REQUEST_SELECT_DEVICE;
    private View selectedView;

    String[] permissions= new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    public void onRequestPermission() {
        checkPermissions();
    }

    private  boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p: permissions) {
            result = ContextCompat.checkSelfPermission(this,p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    PERMISSION_REQUEST);

            return false;
        }
        return true;
    }

    protected boolean ensurePermissions() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                final DialogFragment dialog = new PermissionDialogFragment();
                Bundle args = new Bundle();
                args.putInt("MSG", R.string.rationale_message_storage);
                dialog.setArguments(args);
                dialog.show(getSupportFragmentManager(), null);
                return false;
            }
            return checkPermissions();
        }

        return true;
    }

    protected void onPermissionGranted(int code) {
        switch(code) {
            case REQUEST_DEVICE_DISCOVERY:
                discoveryDevice();
                break;
            case REQUEST_SELECT_DEVICE:
                selectDevice();
        }
    }

    private void discoveryDevice() {
        DeviceListActivity.this.doDiscovery();

    }

    private void selectDevice() {
        DeviceListActivity.this.mBtAdapter.cancelDiscovery();
        String info = ((TextView) selectedView).getText().toString();
        String address = info.substring(info.length() - 17);
        Log.e("--", "BT" + info + "~" + address);
        Intent intent = new Intent();
        intent.putExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS, address);
        DeviceListActivity.this.setResult(-1, intent);
        DeviceListActivity.this.finish();
    }

    protected  void onPermissionNotGranted() {

        final Dialog bleDialog = Utils.createSimpleOkErrorDialog(
                this,
                getString(R.string.app_name),
                getString(R.string.error_no_write_storage_permissions)
        );
        bleDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                //finish();
            }
        });
        bleDialog.show();

    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final @NonNull String[] permissions, final @NonNull int[] grantResults) {

        if (PERMISSION_REQUEST == requestCode ) {

            if (permissions.length > 0 && grantResults.length > 0) {

                try {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                        onPermissionGranted(m_UserRequestType);
                    } else {
                        onPermissionNotGranted();
                    }
                }
                catch(Exception e) {
                    ensurePermissions();
                }

            }
        }
    }

    class DeviceScanButtonClickListener implements OnClickListener {

        public void onClick(View v) {
            m_UserRequestType = REQUEST_DEVICE_DISCOVERY;

            if (ensurePermissions()) {
                onPermissionGranted(REQUEST_DEVICE_DISCOVERY);
                //v.setVisibility(View.GONE);  //was 8
            }
        }
    }

    /* renamed from: com.example.DeviceListActivity$2 */
    class DeviceButtonClickListener implements OnItemClickListener {

        public void onItemClick(AdapterView<?> adapterView, View v, int arg2, long arg3) {

            selectedView = v;
            m_UserRequestType = REQUEST_SELECT_DEVICE;

            if (ensurePermissions()) {
                onPermissionGranted(REQUEST_SELECT_DEVICE);
            }
        }
    }

    class DiscoveryBroadCastReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.bluetooth.device.action.FOUND".equals(action)) {
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                if (device.getBondState() != 12) {
                    DeviceListActivity.this.mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if ("android.bluetooth.adapter.action.DISCOVERY_FINISHED".equals(action)) {
                DeviceListActivity.this.setTitle("Device list");
                if (DeviceListActivity.this.mNewDevicesArrayAdapter.getCount() == 0) {
                    DeviceListActivity.this.mNewDevicesArrayAdapter.add("none_found");
                }
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(5);
        setContentView(R.layout.device_list);
        setResult(0);
        ((Button) findViewById(R.id.button_scan)).setOnClickListener(new DeviceScanButtonClickListener());
        this.mPairedDevicesArrayAdapter = new ArrayAdapter(this, R.layout.device_name);
        this.mNewDevicesArrayAdapter = new ArrayAdapter(this, R.layout.device_name);
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(this.mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(this.mDeviceClickListener);
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(this.mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(this.mDeviceClickListener);
        registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.device.action.FOUND"));
        registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.adapter.action.DISCOVERY_FINISHED"));
        this.mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if(this.mBtAdapter != null) {
            Set<BluetoothDevice> pairedDevices = this.mBtAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
                for (BluetoothDevice device : pairedDevices) {
                    this.mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
        }
        this.mPairedDevicesArrayAdapter.add("None paired");
    }

    protected void onDestroy() {
        super.onDestroy();
        if (this.mBtAdapter != null) {
            this.mBtAdapter.cancelDiscovery();
        }
        unregisterReceiver(this.mReceiver);
    }

    private void doDiscovery() {

        Log.d(TAG, "doDiscovery()");

        if(this.mBtAdapter == null)
            return;

        setTitle("Scanning");
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
        if (this.mBtAdapter.isDiscovering()) {
            this.mBtAdapter.cancelDiscovery();
        }
        this.mBtAdapter.startDiscovery();
    }
}
