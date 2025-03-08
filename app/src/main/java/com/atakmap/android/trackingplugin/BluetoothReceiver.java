package com.atakmap.android.trackingplugin;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// TODO: if we're still looking for a foreground service, look into hooking this receiver up with
//  AtakBroadcast, then passing it to a bound service? idk tbh

public class BluetoothReceiver extends BroadcastReceiver {
    private static final String TAG = Constants.createTag(BluetoothReceiver.class);
    private final BluetoothLeScanner scanner;
    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            Log.d(TAG, String.format("Logged Device (name: %s - mac: %s)", device.getName(),
                    device.getAddress()));
            // TODO: device info is here. need to pass into somewhere.
            //  probably class variable passed in via constructor
        }
    };

    public BluetoothReceiver(Context context) {
        // TODO: add null checks in here, log on error. set private variable
        //  canScan, update UI behavior based what the getter of canScan returns.
        //  maybe have a refresh button that will do the check again and updates canScan.
        BluetoothManager manager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = manager.getAdapter();
        this.scanner = btAdapter.getBluetoothLeScanner();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Got intent");
        String action = intent.getAction();
        if (action == null || !PermissionsActivity.hasAllBtPermissions(context)) return;

        switch (action) {
            case ACTIONS.START_SCAN:
                Log.d(TAG, "START_SCAN");
                // TODO: when we get the whitelist going, use that as a ScanFilter list and pass to
                //  startScan, documentation says it'll keep going even if locked if we provide
                //  this filter. Also look into ScanSettings, has some good stuff.
                this.scanner.startScan(scanCallback);
                break;
            case ACTIONS.STOP_SCAN:
                Log.d(TAG, "STOP_SCAN");
                this.scanner.stopScan(scanCallback);
                break;
        }
    }

    public static final class ACTIONS {
        public static final String START_SCAN = "com.atakmap.android.trackingplugin.START_SCAN";
        public static final String STOP_SCAN = "com.atakmap.android.trackingplugin.STOP_SCAN";
    }
}
