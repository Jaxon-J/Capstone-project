package com.atakmap.android.capstoneplugin.plugin;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;

// TODO: find means of removing devices from "discovered"
// speed up start/stop bluetooth discovery as a "poll rate"?
// refactor behavior out of onDestroy to "stopTracking" and call it from bluetoothReceiver to
// get that going potentially.
// TODO: rename class to something more distinct.
// don't want naming conflicts with any normal android stuff, even if just for logs.

public class BluetoothService extends Service {

    public static final String DEVICE_FOUND = String.format("%s.DEVICE_FOUND", BluetoothService.class.getName());
    // Constant strings
    private static final String TAG = BluetoothService.class.getSimpleName();
    private final ArrayList<BluetoothDevice> discovered = new ArrayList<>();
    private boolean isScanning = false;
    private BluetoothAdapter bluetoothAdapter;
    private BroadcastReceiver bluetoothReceiver;

    // Allows activities to get an instance of the service with provided context.
    // Might be unnecessary.
    /** @noinspection unused */
    public static Intent getServiceIntent(Context context) {
        return new Intent(context, BluetoothService.class);
    }

    // Triggered when service is started.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting Bluetooth scanning...");
        isScanning = true;
        discovered.clear();

        // Initialize adapter
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        startDiscovery();
        return START_STICKY;
    }

    // Triggered when service is created.
    @SuppressLint("MissingPermission")
    public void onCreate() {
        super.onCreate();

        // Initialize receiver that'll go off on ACTION_FOUND and ACTION_DISCOVERY_FINISHED
        bluetoothReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                // ACTION_FOUND behavior
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null && !discovered.contains(device)) {
                        Log.d(TAG, String.format("Device found (name: %s, mac: %s)", device.getName(), device.getAddress()));
                        discovered.add(device);
                        broadcastDeviceFound(device);
                    }
                    // ACTION_DISCOVER_FINISHED behavior
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    Log.d(TAG, "Discovery cycle ended.");
                    if (isScanning) {
                        startDiscovery();
                    }
                }
            }
        };

        // Register, so it hooks into android events.
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bluetoothReceiver, filter);
    }

    @SuppressLint("MissingPermission")
    private void startDiscovery() {
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        if (!bluetoothAdapter.startDiscovery()) {
            Log.e(TAG, "Failed to start Bluetooth discovery.");
        }
    }

    // Triggered when service is destroyed.
    @Override
    @SuppressLint("MissingPermission")
    public void onDestroy() {
        if (!isScanning) return;

        isScanning = false;
        Log.d(TAG, "Stopping Bluetooth scanning...");
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        if (bluetoothReceiver != null) {
            unregisterReceiver(bluetoothReceiver);
            bluetoothReceiver = null;
        }
        super.onDestroy();
    }

    // For onReceive() discoverability, for other potential receivers. (May be unnecessary.)
    private void broadcastDeviceFound(BluetoothDevice device) {
        Intent intent = new Intent(DEVICE_FOUND);
        intent.putExtra("device", device);
        sendBroadcast(intent);
    }

    // "Binding" allows other apps to use this service. Nope.
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

