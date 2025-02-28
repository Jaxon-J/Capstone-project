package com.atakmap.android.capstoneplugin.plugin;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.atakmap.android.ipc.AtakBroadcast;

import java.util.ArrayList;
import java.util.UUID;

// @removable = unsure if code is doing anything useful, might be removed later

// TODO: test behavior of this service, see if it works as expected.
// Also want to figure out when android events "actions" are triggered, might need to
// modify code based on how these events are timed
// https://developer.android.com/develop/connectivity/bluetooth/find-bluetooth-devices#discover-devices

public class BluetoothTrackerService extends Service {
    // Constant strings
    public static final String DEVICE_FOUND = "com.atakmap.tracking.actions.DEVICE_FOUND";
    public static final String ORIGIN_DEVICE_ID = "origin_id";
    public static final String NOTIF_CHANNEL_ID = "atak_tracking_plugin_uno25";
    public static final String NOTIF_CHANNEL_NAME = "ATAK Tracking";
    public static final int NOTIF_ID = 1374; // random
    private static final String TAG = Constants.TAG_PREFIX + "BTService";
    private final ArrayList<BluetoothDevice> discovered = new ArrayList<>();
    private boolean isScanning = false;
    private BluetoothAdapter bluetoothAdapter;
    private BroadcastReceiver bluetoothReceiver;
    private int discoveryRounds = 0;
    private String thisDeviceId;

    /**
     * Allows activities to get an instance of the service.
     *
     * @removable
     * @noinspection unused
     */
    public static Intent getServiceIntent(Context context) {
        return new Intent(context, BluetoothTrackerService.class);
    }

    // Triggered when service is started.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // @removable
        discovered.clear();
        thisDeviceId = UUID.randomUUID().toString();

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

        bluetoothReceiver = new BroadcastReceiver() {
            // Register, so it hooks into android events.
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) action = "";

                switch (action) {
                    case BluetoothDevice.ACTION_FOUND: {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        // ADD WHITELIST CHECK HERE WHEN AVAILABLE, also debug flag to track
                        // everything for testing purposes
                        if (device != null && !discovered.contains(device)) {
                            Log.d(TAG, String.format("Device found (name: %s, mac: %s)", device.getName(), device.getAddress()));
                            discovered.add(device); // Add device to store
                            // Create and send our own DEVICE_FOUND intent (event), for other
                            // receivers @removable
                            Intent deviceFoundIntent = new Intent(DEVICE_FOUND);
                            deviceFoundIntent.putExtra("device", device);
                            deviceFoundIntent.putExtra(ORIGIN_DEVICE_ID, thisDeviceId);
                            AtakBroadcast.getInstance().sendBroadcast(deviceFoundIntent);
                        }
                        break;
                    }
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED: {
                        Log.d(TAG, "Discovery cycle ended. Clearing found devices.");
                        discovered.clear();
                        startDiscovery();
                        break;
                    }
                }
            }
        };
        AtakBroadcast.DocumentedIntentFilter filter = new AtakBroadcast.DocumentedIntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND, String.format("ATrack: device found " + "(origin: %s)", thisDeviceId));
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED, String.format("ATrack: " + "discovery finished (origin: %s)", thisDeviceId));
        AtakBroadcast.getInstance().registerReceiver(bluetoothReceiver, filter);

        // Set up required notification to boot up foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIF_CHANNEL_ID, NOTIF_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
        Notification notif = new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID).setContentTitle("ATAK Tracker Service").setContentText("ATAK tracker is currently scanning over Bluetooth").setSmallIcon(R.drawable.ic_launcher).build();
        startForeground(NOTIF_ID, notif);
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
            Log.e(TAG, "Failed to start Bluetooth discovery. Bluetooth is either missing or disabled.");
            stopForeground(true);
            stopSelf();
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
            AtakBroadcast.getInstance().unregisterReceiver(bluetoothReceiver);
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
