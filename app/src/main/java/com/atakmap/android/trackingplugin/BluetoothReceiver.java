package com.atakmap.android.trackingplugin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.trackingplugin.comms.DeviceCotEventDispatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

// NOTE: phones send BLE advertising signals that are picked up from previously paired phones,
//  even when unpaired. Only discontinues after Bluetooth gets reset on advertising device.

/**
 * BluetoothReceiver handles all the logic between a bluetooth scan and info retrieval from said scans.
 * This is particularly true for Bluetooth LE scans.
 */
public class BluetoothReceiver extends BroadcastReceiver implements DeviceStorageManager.DeviceListChangeListener {
    private static final String TAG = Constants.createTag(BluetoothReceiver.class);

    private BluetoothLeScanner scanner;
    private Set<String> whitelistMacAddresses;
    private static boolean isScanning = false;
    private boolean whitelistEnabled = true;
    public static int POLL_RATE_MILLIS = 5000;
    private static Timer poller;
    public static final Map<String, DeviceInfo> lastIntervalDevices = Collections.synchronizedMap(new HashMap<>());
    public static final Map<String, DeviceInfo> currentIntervalDevices = Collections.synchronizedMap(new HashMap<>());

    /// Object that is called via start/stopScan with the Bluetooth LE scanner to hook in functionality upon events that happen when scan is in progress.
    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String scannedMacAddress = device.getAddress();
            if (whitelistEnabled && !whitelistMacAddresses.contains(scannedMacAddress))
                return;
            String scannedName = device.getName();
            if (scannedName == null) scannedName = "unknown";
            else if (scannedName.length() >= 12)
                scannedName = scannedName.substring(0, 12);

            String existingUuid = DeviceStorageManager.getUuid(DeviceStorageManager.ListType.WHITELIST, scannedMacAddress);
            DeviceInfo deviceInfo = DeviceStorageManager.getDevice(DeviceStorageManager.ListType.WHITELIST, existingUuid);
            if (whitelistEnabled) {
                assert deviceInfo != null; // we are getting exclusively whitelist entries. if it's null something's wrong.
                deviceInfo = new DeviceInfo(deviceInfo, result.getRssi());
            } else {
                deviceInfo = new DeviceInfo(scannedName, scannedMacAddress, result.getRssi(), true, null, MapView.getMapView().getSelfMarker().getUID());
            }
            Log.d(TAG, String.format("BLE Device found - (name: %-12s mac: %s)", scannedName, scannedMacAddress));
            synchronized (currentIntervalDevices) {
                currentIntervalDevices.put(deviceInfo.uuid, deviceInfo);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            isScanning = false;
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

    private final ScanSettings scanSettings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .build();

    /// @param context Context for overall plugin.
    public BluetoothReceiver(Context context) {
        BluetoothAdapter btAdapter;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (manager == null) {
                Log.e(TAG, "Could not get bluetooth manager. Could be the case that Bluetooth is not supported on this device?");
                return;
            }
            btAdapter = manager.getAdapter();
        } else {
            btAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        if (btAdapter == null) {
            Log.e(TAG, "Could not get bluetooth adapter for some reason.");
            return;
        }
        this.scanner = btAdapter.getBluetoothLeScanner();
        onDeviceListChange(DeviceStorageManager.getDeviceList(DeviceStorageManager.ListType.WHITELIST));
        DeviceStorageManager.addChangeListener(DeviceStorageManager.ListType.WHITELIST, this);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            Log.w(TAG, "Action was null. Returning...");
            return;
        } else if (!hasAllBtPermissions(context)) {
            Log.e(TAG, "Returning..."); // hasAllBtPermissions does logging
            return;
        } else if (this.scanner == null) {
            Log.w(TAG, "Receiver was not initialized properly. Returning...");
            return;
        }

        switch (action) {
            case ACTIONS.BLE_START_SCAN: {
                Log.d(TAG, "BLE_START_SCAN");
                if (whitelistEnabled && whitelistMacAddresses.isEmpty()) {
                    Log.w(TAG, "Tried to start scan with no whitelist. Scan will not start.");
                } else {
                    startScan();
                }
                break;
            }
            case ACTIONS.BLE_STOP_SCAN: {
                Log.d(TAG, "BLE_STOP_SCAN");
                stopScan();
                break;
            }
            case ACTIONS.ENABLE_WHITELIST:
            case ACTIONS.DISABLE_WHITELIST: {
                whitelistEnabled = action.equals(ACTIONS.ENABLE_WHITELIST);
                Log.d(TAG, (whitelistEnabled ? "ENABLE" : "DISABLE") + "_SCAN_WHITELIST");
                resetScan();
                break;
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startScan() {
        // starting and stopping scan are gated around isScanning flag, which implicitly assumes calls will always fully execute uninterrupted.
        // this should be okay for the most part, but might get finicky if something gets interrupted and the flag is left in a desync'd state.
        if (isScanning) return;
        this.scanner.startScan(null, scanSettings, scanCallback);
        poller = new Timer();
        poller.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (currentIntervalDevices) {
                    synchronized (lastIntervalDevices) {
                        for (Map.Entry<String, DeviceInfo> entry : currentIntervalDevices.entrySet()) {
                            if (!lastIntervalDevices.containsKey(entry.getKey())) {
                                DeviceCotEventDispatcher.sendDeviceFound(entry.getValue());
                            }
                        }
                        lastIntervalDevices.clear();
                        lastIntervalDevices.putAll(currentIntervalDevices);
                        currentIntervalDevices.clear();
                    }
                }
            }
        }, POLL_RATE_MILLIS);
        isScanning = true;
        /*
        Polling logic:
        - if found in this interval, mark down as found.
        - get difference between lastIntervalDevices and currentIntervalDevices
        - if in last, not in current: remove
        - if in current, not in last: add
        - if in both: no change.
         */
    }

    @SuppressLint("MissingPermission")
    private void stopScan() {
        if (!isScanning) return;
        this.scanner.stopScan(scanCallback);
        poller.cancel();
        poller = null;
        isScanning = false;
    }

    private void resetScan() {
        if (isScanning) {
            stopScan();
            startScan();
        }
    }

    public static boolean hasAllBtPermissions(Context context) {
        /*
        startDiscovery:
          - prerequisite: ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION
          - ver <= R: BLUETOOTH_ADMIN
          - ver >= S: BLUETOOTH_SCAN
        device.getName()
          - ver <= R: BLUETOOTH
          - ver >= S: BLUETOOTH_CONNECT
         */
        List<String> perms = getPermsList();
        List<String> missingPerms = new ArrayList<>();
        for (String perm : perms)
            if (context.checkSelfPermission(perm) == PackageManager.PERMISSION_DENIED) {
                Log.e(TAG, "Don't have permission: " + perm);
                missingPerms.add(perm);
            }
        return missingPerms.isEmpty();
    }

    /// Exclusively called by {@link #hasAllBtPermissions(Context)}
    @NonNull
    private static List<String> getPermsList() {
        List<String> perms = new ArrayList<>(); // ugly but whatever
        perms.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_SCAN);
            perms.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            perms.add(Manifest.permission.BLUETOOTH);
            perms.add(Manifest.permission.BLUETOOTH_ADMIN);
        }
        return perms;
    }

    @Override
    public void onDeviceListChange(List<DeviceInfo> devices) {
        whitelistMacAddresses = new HashSet<>();
        for (DeviceInfo deviceInfo : devices)
            whitelistMacAddresses.add(deviceInfo.macAddress);
        if (whitelistEnabled) resetScan();
    }

    /// Class made for grouping all the actions that can be registered for {@link BluetoothReceiver}
    public static final class ACTIONS {
        public static final String BLE_START_SCAN = "com.atakmap.android.trackingplugin.BLE_START_SCAN";
        public static final String BLE_STOP_SCAN = "com.atakmap.android.trackingplugin.BLE_STOP_SCAN";

        public static final String ENABLE_WHITELIST = "com.atakmap.android.trackingplugin.ENABLE_SCAN_WHITELIST";
        public static final String DISABLE_WHITELIST = "com.atakmap.android.trackingplugin.DISABLE_SCAN_WHITELIST";
    }
}
