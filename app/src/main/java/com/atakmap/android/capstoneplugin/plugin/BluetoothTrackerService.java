package com.atakmap.android.capstoneplugin.plugin;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
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
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// TODO: pick through this with a fine-tooth brush. claude spit this out after i told it to make
// changes. i realized i made several flawed assumptions, haven't done due diligence on making
// sure this code is functional. looks like an amalgamation of the initial code and
// alterations, like a heavy heavy refactor instead of a complete rewrite, which means
// some of my flawed assumptions may still be present, on top of the fact that it's raw AI code.
// looks like a step in the right direction, though. note: most comments below this block aren't mine.

// open questions:
// Can BluetoothLE be present on host device without standard Bluetooth?
//   (assuming this is false, both in here and in manifest.)

/**
 * A foreground service that continuously scans for nearby Bluetooth devices
 * using both Classic Bluetooth and BLE technologies.
 */
public class BluetoothTrackerService extends Service {
    // Constants
    public static final String DEVICE_FOUND = "com.atakmap.tracking.actions.DEVICE_FOUND";
    public static final String ORIGIN_DEVICE_ID = "origin_id";
    public static final String NOTIF_CHANNEL_ID = "atak_tracking_plugin_uno25";
    public static final String NOTIF_CHANNEL_NAME = "ATAK Tracking";
    public static final int NOTIF_ID = 1374;
    private static final String TAG = Constants.TAG_PREFIX + "BTService";

    // Commands that can be sent to the service
    public static final String ACTION_START_SCANNING = "com.atakmap.tracking.actions.START_SCANNING";
    public static final String ACTION_STOP_SCANNING = "com.atakmap.tracking.actions.STOP_SCANNING";

    // Track discovered devices using a Set for automatic duplicate prevention
    private final Set<BluetoothDevice> discoveredClassicDevices = new HashSet<>();
    private final Set<BluetoothDevice> discoveredBleDevices = new HashSet<>();

    private boolean isScanning = false;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BroadcastReceiver intentReceiver;
    private int discoveryRounds = 0;
    private String thisDeviceId;
    private PowerManager.WakeLock wakeLock;
    private final Handler handler = new Handler();

    // BLE scan callback
    private final ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "onScanResult() called");
            BluetoothDevice device = result.getDevice();
            if (device != null && !discoveredBleDevices.contains(device)) {
                String deviceName = safeGetDeviceName(device);

                Log.d(TAG, String.format("BLE Device found (name: %s, mac: %s)",
                        deviceName, device.getAddress()));

                discoveredBleDevices.add(device);

                // Broadcast device discovery to interested components
                Intent deviceFoundIntent = new Intent(DEVICE_FOUND);
                deviceFoundIntent.putExtra("device", device);
                deviceFoundIntent.putExtra("rssi", result.getRssi());
                deviceFoundIntent.putExtra("isBle", true);
                deviceFoundIntent.putExtra(ORIGIN_DEVICE_ID, thisDeviceId);
                sendBroadcast(deviceFoundIntent);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE scan failed with error code: " + errorCode);
            // Attempt to restart the scan after a delay
            if (isScanning) {
                handler.postDelayed(() -> startBleScan(), 5000);
            }
        }
    };

    /**
     * Creates an intent to start the Bluetooth tracking service
     */
    public static Intent createIntent(Context context, String action) {
        // Validation here?
        Log.d(TAG, "createIntent() called");
        Intent intent = new Intent(context, BluetoothTrackerService.class);
        intent.setAction(action);
        return intent;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called");
        // Critical checks that must happen at startup
        if (!hasRequiredPermissions()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Create and display notification to run as foreground service
        createNotificationChannel();
        Notification notif = createNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Log.d(TAG, "STARTED FOREGROUND WITH TYPE");
            startForeground(NOTIF_ID, notif, FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE/*| FOREGROUND_SERVICE_TYPE_LOCATION*/);
        } else {
            Log.d(TAG, "STARTED FOREGROUND WITHOUT TYPE");
            startForeground(NOTIF_ID, notif);
        }

        // Initialize device ID if not already set
        if (thisDeviceId == null) {
            thisDeviceId = UUID.randomUUID().toString();
        }

        // Initialize Bluetooth adapter if not already set
        if (!initializeBluetoothAdapter()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // Process the intent action
        String action = intent != null ? intent.getAction() : null;
        if (action == null || action.equals(ACTION_START_SCANNING)) {
            requestBatteryOptimizationExemption();
            startScanning();
        } else if (action.equals(ACTION_STOP_SCANNING)) {
            stopScanning();
            stopSelf();
            return START_NOT_STICKY;
        }

        // Return START_REDELIVER_INTENT to restart with last intent if killed
        return START_REDELIVER_INTENT;
    }

    /**
     * Initialize the Bluetooth adapter
     * @return true if adapter initialized successfully, false otherwise
     */
    private boolean initializeBluetoothAdapter() {
        Log.d(TAG, "initializeBluetoothAdapter() called");
        if (bluetoothAdapter != null && bleScanner != null)
            return true;
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e(TAG, "BluetoothManager not available. Service cannot run.");
            return false;
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth is not supported on this device. Service cannot run.");
            return false;
        }
        if (bluetoothAdapter.isEnabled()) {
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        } else {
            Log.e(TAG, "Bluetooth must be enabled for service to run.");
            return false;
        }
        return true;
    }

    /**
     * Check if Bluetooth is ready (adapter exists and is enabled)
     */
    private boolean bluetoothNotInitialized() {
        Log.d(TAG, "bluetoothNotInitialized() called");
        return bluetoothAdapter == null || !bluetoothAdapter.isEnabled();
    }

    /**
     * Safely get device name with appropriate permission checks
     */
    private String safeGetDeviceName(BluetoothDevice device) {
        Log.d(TAG, "safeGetDeviceName() called");
        String deviceName = "Unknown";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    deviceName = device.getName();
                }
            } else {
                deviceName = device.getName();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting device name", e);
        }
        return deviceName;
    }

    /**
     * Requests exemption from battery optimization to keep service running
     */
    private void requestBatteryOptimizationExemption() {
        Log.d(TAG, "requestBatteryOptimizationExemption() called");
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        String packageName = getPackageName();
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            Log.i(TAG, "Battery optimization may affect service. Consider requesting exemption.");
            // Note: You cannot directly request this from a service
            // This would typically be done from an activity before starting the service
        }
    }

    @SuppressLint({"WakelockTimeout", "ForegroundServiceType"})
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate() called");
        super.onCreate();

        // Create wake lock to keep CPU running for this service
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ATAK:BluetoothTrackerWakeLock");
        wakeLock.acquire();  // Acquire without timeout for continuous operation

        // Set up Bluetooth broadcast receiver
        setupBluetoothReceiver();
    }

    private void setupBluetoothReceiver() {
        Log.d(TAG, "setupBluetoothReceiver() called");
        intentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;
                switch (action) {
                    case BluetoothDevice.ACTION_FOUND: {
                        Log.d(TAG, "INTENT: ACTION_FOUND RECEIVED");
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device != null && !discoveredClassicDevices.contains(device)) {
                            String deviceName = safeGetDeviceName(device);

                            Log.d(TAG, String.format("Classic Device found (name: %s, mac: %s)",
                                    deviceName, device.getAddress()));

                            discoveredClassicDevices.add(device);

                            // Broadcast device discovery to interested components
                            Intent deviceFoundIntent = new Intent(DEVICE_FOUND);
                            deviceFoundIntent.putExtra("device", device);
                            deviceFoundIntent.putExtra("isBle", false);
                            deviceFoundIntent.putExtra(ORIGIN_DEVICE_ID, thisDeviceId);
                            sendBroadcast(deviceFoundIntent);
                        }
                        break;
                    }
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED: {
                        Log.d(TAG, "Classic discovery cycle ended. Clearing found devices.");
                        discoveredClassicDevices.clear();

                        // If we're still scanning, restart discovery
                        if (isScanning) {
                            startClassicDiscovery();
                        }
                        break;
                    }
                    case BluetoothAdapter.ACTION_STATE_CHANGED: {
                        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                        if (state == BluetoothAdapter.STATE_OFF) {
                            Log.d(TAG, "Bluetooth turned off. Stopping scans.");
                            stopScanning();
                        } else if (state == BluetoothAdapter.STATE_ON && isScanning) {
                            Log.d(TAG, "Bluetooth turned on. Restarting scans.");
                            // Update bleScanner reference when BT is re-enabled
                            if (bluetoothAdapter != null) {
                                bleScanner = bluetoothAdapter.getBluetoothLeScanner();
                            }
                            startScanning();
                        }
                        break;
                    }
                }
            }
        };

        // Register for relevant Bluetooth events
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(intentReceiver, filter);
    }

    private void createNotificationChannel() {
        Log.d(TAG, "createNotificationChannel() called");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIF_CHANNEL_ID,
                    NOTIF_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);  // Use LOW importance to reduce visual disruption

            channel.setDescription("Used for Bluetooth device tracking");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Log.d(TAG, "createNotification() called");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, NOTIF_CHANNEL_ID)
                    .setContentTitle("ATAK Tracker Service")
                    .setContentText("Scanning for nearby devices")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setOngoing(true)
                    .build();
        } else {
            return new Notification.Builder(this)
                    .setContentTitle("ATAK Tracker Service")
                    .setContentText("Scanning for nearby devices")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setOngoing(true)
                    .build();
        }
    }

    /**
     * Start both Classic and BLE scanning
     */
    private void startScanning() {
        Log.d(TAG, "startScanning() called");
        if (bluetoothNotInitialized()) {
            Log.e(TAG, "Bluetooth adapter not available or disabled");
            return;
        }

        isScanning = true;

        // Start both scanning methods
        startClassicDiscovery();
        startBleScan();

        // Set up a periodic restart of the BLE scan to ensure it keeps running
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isScanning) {
                    Log.d(TAG, "Restarting BLE scan as part of periodic maintenance");
                    restartBleScanning();
                    handler.postDelayed(this, 30 * 60 * 1000); // Every 30 minutes
                }
            }
        }, 30 * 60 * 1000); // First run after 30 minutes
    }

    /**
     * Start Classic Bluetooth discovery
     */
    private void startClassicDiscovery() {
        Log.d(TAG, "startClassicDiscovery() called");
        if (bluetoothNotInitialized()) return;

        discoveryRounds++;
        Log.d(TAG, String.format("Starting Classic Bluetooth discovery (round %d)...", discoveryRounds));

        try {
            // Optimized to always cancel discovery first, simplifies logic
            bluetoothAdapter.cancelDiscovery();

            if (!bluetoothAdapter.startDiscovery()) {
                Log.e(TAG, "Failed to start Classic Bluetooth discovery.");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when starting Classic Bluetooth discovery", e);
        }
    }

    /**
     * Start BLE scanning
     */
    @SuppressLint("MissingPermission")
    private void startBleScan() {
        Log.d(TAG, "startBleScan() called");
        if (bluetoothNotInitialized()) return;

        if (bleScanner == null) {
            bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (bleScanner == null) {
                Log.e(TAG, "BLE Scanner not available");
                return;
            }
        }

        Log.d(TAG, "Starting BLE scanning...");

        // Configure BLE scan settings for continuous scanning
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Use highest power mode for continuous discovery
                .setReportDelay(0) // Report immediately
                .build();

        // Start the scan
        bleScanner.startScan(null, settings, bleScanCallback);
    }

    /**
     * Restart BLE scanning (can help prevent scan timeouts)
     */
    @SuppressLint("MissingPermission")
    private void restartBleScanning() {
        Log.d(TAG, "restartBleScanning() called");
        // Every code path is subsequent to permission checks, no need to do it here.
        if (bleScanner != null) {
            try {
                // Stop existing scan
                bleScanner.stopScan(bleScanCallback);

                // Clear discovered devices
                discoveredBleDevices.clear();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping BLE scan during restart", e);
                // Continue to startBleScan anyway
            }
        }

        // Start scan again
        startBleScan();
    }

    /**
     * Stop all Bluetooth scanning
     */
    private void stopScanning() {
        Log.d(TAG, "stopScanning() called");
        if (!isScanning) return;

        isScanning = false;
        Log.d(TAG, "Stopping all Bluetooth scanning...");

        // Stop Classic Bluetooth discovery
        try {
            if (bluetoothAdapter != null) {
                bluetoothAdapter.cancelDiscovery();
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when stopping Classic Bluetooth discovery", e);
        }

        // Stop BLE scanning
        try {
            if (bleScanner != null) {
                bleScanner.stopScan(bleScanCallback);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when stopping BLE scan", e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "BLE adapter in incorrect state when trying to stop scan", e);
        }

        // Remove any pending callbacks
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called");
        // Stop all scanning
        stopScanning();

        // Unregister receiver
        if (intentReceiver != null) {
            try {
                unregisterReceiver(intentReceiver);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver not registered or already unregistered", e);
            }
            intentReceiver = null;
        }

        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved() called. Service will continue running.");
        super.onTaskRemoved(rootIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind() called");
        // This service doesn't support binding
        return null;
    }

    /**
     * Checks if the app has all required permissions for Bluetooth operations
     */
    private boolean hasRequiredPermissions() {
        // may be refactoring permission handling into its own class.
        Log.d(TAG, "hasRequiredPermissions() called");
        boolean hasPerms = true;
        Set<String> missingPerms = new HashSet<>();
        Set<String> neededPerms;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            neededPerms = Set.of(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.WAKE_LOCK);
        } else {
            neededPerms = Set.of(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WAKE_LOCK);
        }
        for (String perm : neededPerms) {
            if (checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED)
                continue;
            missingPerms.add(perm);
            hasPerms = false;
        }
        if (!hasPerms) {
            Log.e(TAG, "Cannot start service. Missing permissions: " + String.join(",", missingPerms));
        }
        return hasPerms;
    }
}