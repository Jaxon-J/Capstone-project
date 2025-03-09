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
import android.os.Build;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

// TODO: if we're still looking for a foreground service, look into hooking this receiver up with
//  AtakBroadcast, then passing it to a bound service? idk tbh

public class BluetoothReceiver extends BroadcastReceiver {
    private static final String TAG = Constants.createTag(BluetoothReceiver.class);
    private final Map<String, String> deviceMap = new HashMap<>();

    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String macAddr = device.getAddress();
            String name = device.getName();
            if (deviceMap.get(macAddr) == null || (deviceMap.get(macAddr)
                    .equals("null") && !name.isEmpty())) {
                deviceMap.put(device.getAddress(), device.getName());
                deviceLog(device.getName(), device.getAddress());
            }
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
    private BluetoothLeScanner scanner;
    private BluetoothAdapter btAdapter;
    private boolean isScanning = false;

    public BluetoothReceiver(Context context) {
        BluetoothManager manager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager == null) {
            Log.e(TAG,
                    "Could not get bluetooth manager. Bluetooth may not be supported on this " +
                            "device.");
            return;
        }
        this.btAdapter = manager.getAdapter();
        if (this.btAdapter == null) {
            Log.e(TAG, "Could not get bluetooth adapter for some reason.");
            return;
        }
        this.scanner = this.btAdapter.getBluetoothLeScanner();
    }

    private static void deviceLog(String name, String address) {
        Log.d(TAG, String.format("Logged Device (name: %s - mac: %s)", name, address));
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
        } else if (this.btAdapter == null || this.scanner == null) {
            Log.w(TAG, "Receiver was not initialized properly. Returning...");
            return;
        }

        switch (action) {
            case ACTIONS.BLE_START_SCAN:
                Log.d(TAG, "BLE_START_SCAN");
                // TODO: when we get the whitelist going, use that as a ScanFilter list and pass to
                //  startScan, documentation says it'll keep going even if locked if we provide
                //  this filter. Also look into ScanSettings, has some good stuff.
                this.scanner.startScan(scanCallback);
                break;
            case ACTIONS.BLE_STOP_SCAN:
                Log.d(TAG, "BLE_STOP_SCAN");
                this.scanner.stopScan(scanCallback);
                break;
            case ACTIONS.CLASSIC_START_DISCOVERY:
                Log.d(TAG, "CLASSIC_START_DISCOVERY");
                if (!this.btAdapter.isEnabled()) {
                    Log.w(TAG, "Tried to start discovery when bluetooth was disabled.");
                    return;
                }
                boolean btStarted = this.btAdapter.startDiscovery();
                if (!btStarted) Log.e(TAG, "Could not start classic discovery for some reason.");
                else Log.d(TAG, "Discovery process starting...");
                isScanning = true;
                break;
            case ACTIONS.CLASSIC_STOP_DISCOVERY:
                Log.d(TAG, "CLASSIC_STOP_DISCOVERY");
                boolean btStopped = this.btAdapter.cancelDiscovery();
                if (!btStopped) Log.e(TAG, "Could not cancel classic discovery for some reason.");
                isScanning = false;
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                Log.d(TAG, "Classic discovery did indeed start!");
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                Log.d(TAG, "Classic discovery finished. Restarting...");
                if (isScanning && this.btAdapter.isEnabled()) this.btAdapter.startDiscovery();
                else Log.d(TAG, "Just kidding we're stopping now");
                break;
            case BluetoothDevice.ACTION_FOUND:
                BluetoothDevice device;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE,
                            BluetoothDevice.class);
                } else {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                }
                if (device == null) {
                    Log.w(TAG, "ACTION_FOUND action did not have accompanying EXTRA_DEVICE " +
                            "parcel" + ".");
                    return;
                }
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                if (name == null) name = device.getName();
                deviceLog(name, device.getAddress());
        }
    }

    public static final class ACTIONS {
        public static final String BLE_START_SCAN = "com.atakmap.android.trackingplugin" +
                ".BLE_START_SCAN";
        public static final String BLE_STOP_SCAN = "com.atakmap.android.trackingplugin" +
                ".BLE_STOP_SCAN";
        public static final String CLASSIC_START_DISCOVERY =
                "com.atakmap.android.trackingplugin" + ".CLASSIC_START_DISCOVERY";
        public static final String CLASSIC_STOP_DISCOVERY =
                "com.atakmap.android.trackingplugin" + ".CLASSIC_STOP_DISCOVERY";
    }
}
