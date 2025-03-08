package com.atakmap.android.trackingplugin;

import android.Manifest;
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
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class BluetoothReceiver extends BroadcastReceiver {
    private static final String TAG = Constants.createTag(BluetoothReceiver.class);
    private final BluetoothLeScanner scanner;
    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BtUtil.logDeviceFromScanResult(result);
        }
    };

    public BluetoothReceiver(Context context) {
        BluetoothManager manager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter btAdapter = manager.getAdapter();
        scanner = btAdapter.getBluetoothLeScanner();
    }

    // WE'RE REFACTORING PERMS INTO AN ACTIVITY. THIS IS TEMPORARY
    private static boolean hasAllBtPerms(Context context) {
        /*
        startDiscovery:
          - prerequisite: ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION
          - ver <= R: BLUETOOTH_ADMIN
          - ver >= S: BLUETOOTH_SCAN
        device.getName()
          - ver <= R: BLUETOOTH
          - ver >= S: BLUETOOTH_CONNECT
         */
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
        for (String perm : perms)
            if (context.checkSelfPermission(perm) == PackageManager.PERMISSION_DENIED) {
                Log.e(TAG, "Don't have permission: " + perm);
                return false;
            }
        return true;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Got intent");
        String action = intent.getAction();
        if (action == null || !hasAllBtPerms(context)) return;

        switch (action) {
            case BluetoothDevice.ACTION_FOUND:
                Log.d(TAG, "ACTION_FOUND");
                BtUtil.logDeviceFromIntent(intent, context);
                break;
            case ACTIONS.START_SCAN:
                Log.d(TAG, "START_SCAN");
                // when we get the whitelist going, use that as a scanfilter and pass to
                // startScan, documentation says it'll keep going even if locked if we provide
                // this filter. Also look into ScanSettings, has some good stuff.
                scanner.startScan(scanCallback);
                break;
            case ACTIONS.STOP_SCAN:
                Log.d(TAG, "STOP_SCAN");
                scanner.stopScan(scanCallback);
                break;
        }
    }

    public static final class ACTIONS {
        public static final String START_SCAN = "com.atakmap.android.trackingplugin.START_SCAN";
        public static final String STOP_SCAN = "com.atakmap.android.trackingplugin.STOP_SCAN";
    }
}
