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

// @removable = unsure if code is doing anything useful, might be removed later

// TODO: test behavior of this service, see if it works as expected.
// Also want to figure out when android events "actions" are triggered, might need to
// modify code based on how these events are timed
// https://developer.android.com/develop/connectivity/bluetooth/find-bluetooth-devices#discover-devices

public class BluetoothTrackerService extends Service {

    // Constant strings
    public static final String DEVICE_FOUND = BluetoothTrackerService.class.getName() + ".DEVICE_FOUND";
    private static final String TAG = BluetoothTrackerService.class.getSimpleName();

    private final ArrayList<BluetoothDevice> discovered = new ArrayList<>();
    private boolean isScanning = false;
    private BluetoothAdapter bluetoothAdapter;
    private BroadcastReceiver bluetoothReceiver;
    private int discoveryRounds = 0;


    /**
     * @noinspection unused
     */
    // Allows activities to get an instance of the service.
    // @removable
    public static Intent getServiceIntent(Context context) {
        return new Intent(context, BluetoothTrackerService.class);
    }

    // Triggered when service is started.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // @removable
        discovered.clear();

        // Initialize adapter
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        startDiscovery();
        return START_STICKY;
    }

    // Triggered when service is created.
    @Override
    @SuppressLint("MissingPermission")
    public void onCreate() {
        super.onCreate();

        // Initialize action receiver that'll respond to bluetooth events
        bluetoothReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                // ACTION_FOUND behavior
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null && !discovered.contains(device)) {
                        Log.d(TAG, String.format("Device found (name: %s, mac: %s)", device.getName(), device.getAddress()));
                        discovered.add(device); // Add device to store
                        // Create and send our own DEVICE_FOUND intent (event), for other receivers
                        // @removable
                        Intent deviceFoundIntent = new Intent(DEVICE_FOUND);
                        deviceFoundIntent.putExtra("device", device);
                        sendBroadcast(deviceFoundIntent);
                    }
                    // ACTION_DISCOVERY_FINISHED behavior
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    Log.d(TAG, "Discovery cycle ended. Clearing found devices.");
                    discovered.clear();
                    startDiscovery();
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
        isScanning = true;
        discoveryRounds += 1;
        Log.d(TAG, String.format("Starting Bluetooth discovery (round %d)...", discoveryRounds));

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        if (!bluetoothAdapter.startDiscovery()) {
            Log.e(TAG, "Failed to start Bluetooth discovery.");
        }
    }

    @SuppressLint("MissingPermission")
    public void stopTracking() {
        if (!isScanning) return;
        isScanning = false;
        Log.d(TAG, "Stopping Bluetooth scanning...");
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    // Triggered when service is destroyed.
    @Override
    @SuppressLint("MissingPermission")
    public void onDestroy() {
        isScanning = true; // bypass isScanning check in stopTracking. feels hacky.
        stopTracking();
        if (bluetoothReceiver != null) {
            unregisterReceiver(bluetoothReceiver);
            bluetoothReceiver = null;
        }
        super.onDestroy();
    }

    // "Binding" allows other apps to use this service. Nope.
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
