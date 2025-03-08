package com.atakmap.android.trackingplugin;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresPermission;

public class BtUtil {
    private static final String TAG = Constants.createTag(BtUtil.class);
    private static final boolean shownNameError = false;
    private static boolean shownIntentError = false; // anti-spam checks

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public static void logDeviceFromIntent(Intent intent, Context context) {
        BluetoothDevice device;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
        else device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (device == null) {
            if (!shownIntentError) {
                Log.w(TAG,
                        "Intent did not contain parcel BluetoothDevice.EXTRA_DEVICE. Wrong " +
                                "intent?");
                shownIntentError = true;
            }
            return;
        }
        String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
        if (name == null) name = device.getName();
        logDevice(device, name);
    }

    private static void logDevice(BluetoothDevice device, String name) {
        Log.d(TAG, String.format("Logged Device (name: %s - mac: %s)", name, device.getAddress()));
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public static void logDeviceFromScanResult(ScanResult result) {
        BluetoothDevice device = result.getDevice();
        logDevice(device, device.getName());
    }

    private static boolean hasNamePerms(Context context) {
        return (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R && context.checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }
}
