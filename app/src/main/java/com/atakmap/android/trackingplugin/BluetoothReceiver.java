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
    private BluetoothLeScanner scanner;
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

        @Override
        public void onScanFailed(int errorCode) {
            switch (errorCode) {
                case ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY:
                    Log.e(TAG, "SCAN FAIL: Scanning too frequently...");
                    break;
                case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                    Log.e(TAG, "SCAN FAIL: Another scan is already taking place.");
                    break;
                case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    Log.e(TAG, "SCAN FAIL: Application registration failed.");
                    break;
                case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                    Log.e(TAG, "SCAN FAIL: Bluetooth LE scanner is not supported on this device.");
                    break;
                case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                    Log.e(TAG, "SCAN FAIL: Something just straight up broke.");
                    break;
                case ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES:
                    Log.e(TAG, "SCAN FAIL: Hardware resources unavailable for Bluetooth LE scan.");
                    break;
            }
        }
    };

    public BluetoothReceiver(Context context) {
        // TODO: add null checks in here, log on error.
        BluetoothManager manager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager == null) {
            Log.e(TAG, "Could not get bluetooth manager. Bluetooth may not be supported on this device.");
            return;
        }
        BluetoothAdapter btAdapter = manager.getAdapter();
        if (btAdapter == null) {
            Log.e(TAG, "Could not get bluetooth adapter for some reason.");
            return;
        }
        this.scanner = btAdapter.getBluetoothLeScanner();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            Log.w(TAG, "Action was null. Returning...");
            return;
        } else if (!PermissionsActivity.hasAllBtPermissions(context)) {
            Log.e(TAG, "Returning..."); // hasAllBtPermissions does logging
            return;
        } else if (scanner == null) {
            Log.w(TAG, "Scanner was not initialized properly. Returning...");
            return;
        }

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
