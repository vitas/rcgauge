package com.pitchgauge.j9pr.pitchgauge;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static com.pitchgauge.j9pr.pitchgauge.BluetoothPipe.DEVICE_BT;
import static com.pitchgauge.j9pr.pitchgauge.BluetoothPipe.REQUEST_ENABLE_BT;

public class DeviceListActivity extends BluetoothBaseActivity {

    private static final String TAG = "DeviceListActivity";
    private OnItemClickListener mDeviceClickListener = new OnDeviceSelectionClick();
    private ArrayAdapter<DeviceTag> mNewDevicesArrayAdapter;
    private ArrayAdapter<DeviceTag> mPairedDevicesArrayAdapter;
    private final BroadcastReceiver mReceiver = new BLbroadcastReceiver();
    ListView pairedListView;

    class OnDeviceDiscoveryClick implements OnClickListener {

        public void onClick(View v) {
            DeviceListActivity.this.doDiscovery();
            v.setVisibility(View.GONE);
        }
    }

    class OnDeviceSelectionClick implements OnItemClickListener {

        public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {

            if (!hasMoreSelectedItems()) {
                ListView lv = (ListView) adapterView;
                lv.setItemChecked(position, false);
            }

        }
    }

    private boolean hasMoreSelectedItems() {
        int cntChoice = pairedListView.getCount();

        int countSelected = 0;
        if (cntChoice > 0) {
            SparseBooleanArray sparseBooleanArray = pairedListView.getCheckedItemPositions();
            for (int i = 0; i < cntChoice; i++) {
                if (sparseBooleanArray.get(i)) {
                    countSelected++;
                }
                if (countSelected > 2) {
                    Toast.makeText(getApplicationContext(), "Only max 2 sensors are supported!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }
        return true;
    }

    class OnDeviceMultiselectClick implements OnClickListener {

        public void onClick(View v) {

            if (hasMoreSelectedItems()) {

                int cntChoice = pairedListView.getCount();

                SparseBooleanArray sparseBooleanArray = pairedListView.getCheckedItemPositions();

                ArrayList<DeviceTag> tags = new ArrayList<>();
                int pos = 0;
                for (int i = 0; i < cntChoice; i++) {
                    if (sparseBooleanArray.get(i)) {
                        DeviceTag tag = (DeviceTag) pairedListView.getItemAtPosition(i);
                        tag.setPos(pos);
                        tags.add(tag);
                        pos++;
                    }
                }

                mBluetoothPipe.cancelDiscovery();

                BluetoothPreferences.setKeyrings(getApplicationContext(),  tags);

                if (!tags.isEmpty()) {
                    Intent intent = new Intent();
                    intent.putParcelableArrayListExtra(DEVICE_BT, tags );

                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        }
    }

    class BLbroadcastReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.bluetooth.device.action.FOUND".equals(action)) {
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                if (device.getBondState() != BOND_BONDED) {
                    DeviceTag tag = new DeviceTag();
                    tag.setName(device.getName());
                    tag.setAddress(device.getAddress());
                    mNewDevicesArrayAdapter.add(tag);
                }
            } else if ("android.bluetooth.adapter.action.DISCOVERY_FINISHED".equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle("Device list");
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    //DeviceListActivity.this.mNewDevicesArrayAdapter.add("none_found");
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        autoStart = false;

        requestWindowFeature(5);
        setContentView(R.layout.device_list);
        setResult(0);

        ((Button) findViewById(R.id.button_scan)).setOnClickListener(new OnDeviceDiscoveryClick());
        this.mPairedDevicesArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice);
        this.mNewDevicesArrayAdapter = new ArrayAdapter(this, R.layout.device_name);
        pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        pairedListView.setItemsCanFocus(false);

        pairedListView.setAdapter(this.mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(this.mDeviceClickListener);
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(this.mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(this.mDeviceClickListener);

        registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.device.action.FOUND"));
        registerReceiver(this.mReceiver, new IntentFilter("android.bluetooth.adapter.action.DISCOVERY_FINISHED"));


        if(mBluetoothPipe.getBluetoothAdapter() != null) {
            Set<BluetoothDevice> pairedDevices = mBluetoothPipe.getBluetoothAdapter().getBondedDevices();
            if (pairedDevices.size() > 0) {
                findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);

                ArrayList<DeviceTag> savedDevices = BluetoothPreferences.getKeyrings(getApplicationContext());

                for (BluetoothDevice device : pairedDevices) {
                    boolean found = false;
                    for (DeviceTag sTag: savedDevices) {
                        if (sTag.getAddress().equalsIgnoreCase(device.getAddress())) {
                            this.mPairedDevicesArrayAdapter.add(sTag);
                            pairedListView.setItemChecked(sTag.getPos(),true); //Don't make the same mistake I did by calling this function before setting the listview adapter.
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        DeviceTag tag = new DeviceTag();
                        tag.setName(device.getName());
                        tag.setAddress(device.getAddress());
                        this.mPairedDevicesArrayAdapter.add(tag);
                    }
                }
            }
        }
        //this.mPairedDevicesArrayAdapter.add("None paired");
        findViewById(R.id.button_select).setOnClickListener(new OnDeviceMultiselectClick());

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!mBluetoothPipe.isBluetoothEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.mReceiver);

        if (mBluetoothPipe != null) {
            mBluetoothPipe.cancelDiscovery();
        }

    }

    private void doDiscovery() {
        Log.d(TAG, "doDiscovery()");

        setTitle("Scanning");

        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        if (mBluetoothPipe.isDiscovery()) {
            mBluetoothPipe.cancelDiscovery();
        }
        mBluetoothPipe.startDiscovery();
    }
}
