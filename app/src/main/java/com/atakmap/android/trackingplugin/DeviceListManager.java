package com.atakmap.android.trackingplugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DeviceListManager {
    private static final String TAG = Constants.createTag(DeviceListManager.class);
    private static final Map<Lists, MutableLiveData<List<DeviceInfo>>> deviceListsCache = new HashMap<>();
    // Internal map to maintain MAC address indexing for efficient lookups
    private static final Map<Lists, Map<String, DeviceInfo>> internalMaps = new HashMap<>();
    private static Context pluginContext;

    // Private constructor to prevent instantiation
    private DeviceListManager() {
        // No instantiation
    }

    /**
     * Initialize the DeviceListManager with an application context
     * Call this once in your Application class or main activity
     */
    public static void initialize(Context pluginContext) {
        if (DeviceListManager.pluginContext == null) {
            DeviceListManager.pluginContext = pluginContext;
        }
    }

    /**
     * Ensure the manager has been initialized
     */
    private static void checkInitialization() {
        if (pluginContext == null) {
            throw new IllegalStateException("DeviceListManager is not initialized. Call DeviceListManager.initialize(context) first.");
        }
    }

    /**
     * Get LiveData as a List<DeviceInfo> for a specific device list
     */
    public static LiveData<List<DeviceInfo>> getDeviceList(Lists listType) {
        checkInitialization();

        if (!deviceListsCache.containsKey(listType)) {
            // Create new LiveData for this list
            MutableLiveData<List<DeviceInfo>> liveData = new MutableLiveData<>();

            // Load initial data - store in internal map and convert to list for LiveData
            Map<String, DeviceInfo> devicesMap = loadDevicesFromPreferences(listType);
            internalMaps.put(listType, devicesMap);

            // Convert map to list for LiveData
            List<DeviceInfo> devicesList = new ArrayList<>(devicesMap.values());
            liveData.setValue(devicesList);

            // Store in cache
            deviceListsCache.put(listType, liveData);
        }

        return deviceListsCache.get(listType);
    }

    /**
     * Add or update a device in a specific list
     * @noinspection DataFlowIssue
     */
    public static void addOrUpdateDevice(Lists listType, String macAddress, DeviceInfo deviceInfo) {
        checkInitialization();

        // Update internal map
        ensureInternalMapExists(listType);
        Map<String, DeviceInfo> devicesMap = internalMaps.get(listType);
        devicesMap.put(macAddress, deviceInfo);

        // Save to preferences
        saveDevicesToPreferences(listType, devicesMap);

        // Update LiveData with new list
        updateLiveDataFromMap(listType);
    }

    /**
     * Remove a device from a specific list
     * @noinspection DataFlowIssue
     */
    public static void removeDevice(Lists listType, String macAddress) {
        checkInitialization();

        // Update internal map
        ensureInternalMapExists(listType);
        Map<String, DeviceInfo> devicesMap = internalMaps.get(listType);
        if (devicesMap.containsKey(macAddress)) {
            devicesMap.remove(macAddress);

            // Save to preferences
            saveDevicesToPreferences(listType, devicesMap);

            // Update LiveData with new list
            updateLiveDataFromMap(listType);
        }
    }

    /**
     * Check if a device exists in the specified list
     * @noinspection DataFlowIssue
     */
    public static boolean containsDevice(Lists listType, String macAddress) {
        checkInitialization();

        ensureInternalMapExists(listType);
        return internalMaps.get(listType).containsKey(macAddress);
    }

    /**
     * Get a specific device by MAC address
     * @noinspection DataFlowIssue
     */
    public static DeviceInfo getDevice(Lists listType, String macAddress) {
        checkInitialization();

        ensureInternalMapExists(listType);
        return internalMaps.get(listType).get(macAddress);
    }

    /**
     * Clear all devices from a specific list
     * @noinspection unused, DataFlowIssue
     */
    public static void clearList(Lists listType) {
        checkInitialization();

        // Clear internal map
        internalMaps.put(listType, new HashMap<>());

        // Save empty map to preferences
        saveDevicesToPreferences(listType, new HashMap<>());

        // Update LiveData with empty list
        ensureLiveDataExists(listType);
        deviceListsCache.get(listType).setValue(new ArrayList<>());
    }

    // Helper methods
    private static void ensureInternalMapExists(Lists listType) {
        if (!internalMaps.containsKey(listType)) {
            internalMaps.put(listType, loadDevicesFromPreferences(listType));
        }
    }

    private static void ensureLiveDataExists(Lists listType) {
        if (!deviceListsCache.containsKey(listType)) {
            MutableLiveData<List<DeviceInfo>> liveData = new MutableLiveData<>();
            liveData.setValue(new ArrayList<>());
            deviceListsCache.put(listType, liveData);
        }
    }

    /** @noinspection DataFlowIssue*/
    private static void updateLiveDataFromMap(Lists listType) {
        ensureLiveDataExists(listType);
        List<DeviceInfo> devicesList = new ArrayList<>(internalMaps.get(listType).values());
        deviceListsCache.get(listType).setValue(devicesList);
    }

    private static Map<String, DeviceInfo> loadDevicesFromPreferences(Lists listType) {
        SharedPreferences prefs = pluginContext.getSharedPreferences(listType.getSharedPrefsFilename(), Context.MODE_PRIVATE);
        String json = prefs.getString("device_list", "{}");
        return parseDevicesFromJson(json);
    }

    private static void saveDevicesToPreferences(Lists listType, Map<String, DeviceInfo> devices) {
        String json = convertDevicesToJson(devices);
        pluginContext.getSharedPreferences(listType.getSharedPrefsFilename(), Context.MODE_PRIVATE)
                .edit()
                .putString("device_list", json)
                .apply();
    }

    // JSON conversion methods
    private static Map<String, DeviceInfo> parseDevicesFromJson(String json) {
        try {
            JSONObject baseJson = new JSONObject(json);
            Map<String, DeviceInfo> devMap = new HashMap<>();
            for (Iterator<String> it = baseJson.keys(); it.hasNext(); ) {
                String macAddr = it.next();
                JSONObject devJsonEntry = baseJson.getJSONObject(macAddr);
                DeviceInfo devInfo = new DeviceInfo();
                for (Field field : DeviceInfo.class.getFields()) {
                    String fieldVal = devJsonEntry.getString(field.getName()); // why DeviceInfo fields must be strings.
                    field.setAccessible(true);
                    field.set(devInfo, fieldVal); // documentation on .set explains why setAccessible is okay here.
                }
                devMap.put(macAddr, devInfo);
            }
            return devMap;
        } catch (JSONException e) {
            throw new RuntimeException("Improperly handled JSON stuff in parseDevicesFromJson.");
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Java reflection failed in parseDevicesFromJson.");
            Log.e(TAG, "    " + json);
            throw new RuntimeException(e);
        }
    }

    private static String convertDevicesToJson(Map<String, DeviceInfo> devices) {
        try {
            JSONObject baseJson = new JSONObject();
            for (Map.Entry<String, DeviceInfo> entry : devices.entrySet()) {
                JSONObject devInfoJson = new JSONObject();
                DeviceInfo devInfo = entry.getValue();
                for (Field field : DeviceInfo.class.getFields()) {
                    devInfoJson.put(field.getName(), field.get(devInfo));
                }
                baseJson.put(entry.getKey(), devInfoJson);
            }
            return baseJson.toString();
        } catch (JSONException e) {
            throw new RuntimeException("Improperly handled JSON stuff in convertDevicesToJson.");
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Java reflection failed in convertDevicesToJson.");
            Log.e(TAG, "    " + Arrays.toString(devices.keySet().toArray()));
            throw new RuntimeException(e);
        }
    }

    // Your Lists enum
    public enum Lists {
        WHITELIST("devices_whitelist"),
        SENSORLIST("devices_sensors");

        private final String sharedPrefsFilename;

        Lists(String sharedPrefsFilename) {
            this.sharedPrefsFilename = sharedPrefsFilename;
        }

        public String getSharedPrefsFilename() {
            return sharedPrefsFilename;
        }
    }

    /// Direct changes to the fields in this class will not be reflected elsewhere, it must be added to the DeviceListManager.
    // All public fields in this class must be strings (or else it'll open up an even bigger headache with JSON serialization).
    public static class DeviceInfo {
        public final String name;
        public final String macAddress;

        public DeviceInfo(String macAddress, String name) {
            this.name = name;
            this.macAddress = macAddress;
        }

        // for JSON serialization "parseDevicesFromJson"
        private DeviceInfo() {
            this.name = null;
            this.macAddress = null;
        }
    }
}