package com.atakmap.android.trackingplugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeviceListManager {
    private static final String DEVICE_LIST_ENTRY_NAME = "device_list";
    private static final String TAG = Constants.createTag(DeviceListManager.class);
    private static final Map<Lists, Map<String, DeviceInfo>> deviceLists = new HashMap<>();
    private static final Map<Lists, Set<DeviceListChangeListener>> listeners = new HashMap<>();
    private static Context pluginContext;

    // Private constructor to prevent instantiation
    private DeviceListManager() {
        // No instantiation
    }

    public static void addChangeListener(Lists list, DeviceListChangeListener listener) {
        getListeners(list).add(listener);
    }

    /**
     * Initialize the DeviceListManager with the plugin context.
     */
    public static void initialize(Context pluginContext) {
        if (DeviceListManager.pluginContext == null) {
            DeviceListManager.pluginContext = pluginContext;
        }
    }

    public static List<DeviceInfo> getDeviceList(Lists listType) {
        return Collections.unmodifiableList(new ArrayList<>(getDeviceMap(listType).values()));
    }

    /// If device with MAC Address exists within list, entry will be overwritten.
    public static void addOrUpdateDevice(Lists listType, DeviceInfo deviceInfo) {
        Map<String, DeviceInfo> deviceList = getDeviceMap(listType);
        deviceList.put(deviceInfo.macAddress, deviceInfo);
        saveDevicesToPreferences(listType, deviceList);
    }

    ///  Returns true if device was present in the list.
    public static void removeDevice(Lists listType, String macAddress) {
        Map<String, DeviceInfo> deviceList = getDeviceMap(listType);
        if (!deviceList.containsKey(macAddress))
            return;
        deviceList.remove(macAddress);
        saveDevicesToPreferences(listType, deviceList);
    }

    public static boolean containsDevice(Lists listType, String macAddress) {
        return getDeviceMap(listType).containsKey(macAddress);
    }

    @Nullable
    public static DeviceInfo getDevice(Lists listType, String macAddress) {
        return getDeviceMap(listType).get(macAddress);
    }

    public static void clearList(Lists listType) {
        Map<String, DeviceInfo> empty = new HashMap<>();

        deviceLists.put(listType, empty);
        saveDevicesToPreferences(listType, empty);
    }


    private static void checkInitialization() {
        if (pluginContext == null)
            throw new IllegalStateException("DeviceListManager is not initialized. Call DeviceListManager.initialize(context) first.");
    }

    private static Set<DeviceListChangeListener> getListeners(Lists list) {
        if (listeners.containsKey(list)) return listeners.get(list);
        Set<DeviceListChangeListener> listListeners = new HashSet<>();
        listeners.put(list, listListeners);
        return listListeners;
    }

    private static Map<String, DeviceInfo> getDeviceMap(Lists listType) {
        checkInitialization();
        if (deviceLists.containsKey(listType)) return deviceLists.get(listType);

        SharedPreferences prefs = pluginContext.getSharedPreferences(listType.sharedPrefsFilename, Context.MODE_PRIVATE);
        String json = prefs.getString(DEVICE_LIST_ENTRY_NAME, "{}");
        Map<String, DeviceInfo> list = parseDevicesFromJson(json);
        deviceLists.put(listType, list);
        return list;
    }


    private static void saveDevicesToPreferences(Lists listType, Map<String, DeviceInfo> devices) {
        checkInitialization();
        SharedPreferences listPref = pluginContext.getSharedPreferences(listType.sharedPrefsFilename, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = listPref.edit();
        String json = convertDevicesToJson(devices);
        editor.putString(DEVICE_LIST_ENTRY_NAME, json);
        editor.apply();

        // This function is called every time a device list is changed. Notify all listeners.
        for (DeviceListChangeListener listener : getListeners(listType)) {
            listener.onDeviceListChange(new ArrayList<>(getDeviceMap(listType).values()));
        }
    }

    // JSON conversion methods
    private static Map<String, DeviceInfo> parseDevicesFromJson(String json) {
        // if DeviceInfo changes after sharedPreference file is written to, this won't handle that case, will fail.
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


    public enum Lists {
        WHITELIST("devices_whitelist"), SENSORLIST("devices_sensors");

        public final String sharedPrefsFilename;

        Lists(String sharedPrefsFilename) {
            this.sharedPrefsFilename = sharedPrefsFilename;
        }
    }

    public interface DeviceListChangeListener {
        void onDeviceListChange(List<DeviceInfo> devices);
    }

    // All public fields in this class must be strings (or else it'll open up an even bigger headache with JSON serialization).
    public static class DeviceInfo {
        public final String name;
        public final String macAddress;
        public final String rssi;

        public DeviceInfo(String macAddress, String name, int rssi) {
            this.name = name;
            this.macAddress = macAddress;
            this.rssi = Integer.toString(rssi);
        }

        // for JSON serialization "parseDevicesFromJson"
        private DeviceInfo() {
            this.name = null;
            this.macAddress = null;
            this.rssi = "-1";
        }
    }
}