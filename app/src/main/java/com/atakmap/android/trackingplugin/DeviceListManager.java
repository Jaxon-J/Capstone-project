package com.atakmap.android.trackingplugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Class that handles persistent data involving lists of devices, primarily constructed for whitelist and sensor list.
public class DeviceListManager {
    private static final String DEVICE_LIST_ENTRY_NAME = "device_list";
    private static final String TAG = Constants.createTag(DeviceListManager.class);
    private static final Map<ListType, Map<String, DeviceInfo>> listTypeMap = new HashMap<>();
    private static final Map<ListType, Set<DeviceListChangeListener>> listeners = new HashMap<>();
    private static Context pluginContext;

    // Private constructor to prevent instantiation
    private DeviceListManager() {
        // No instantiation
    }

    public static void addChangeListener(ListType listType, DeviceListChangeListener listener) {
        getListeners(listType).add(listener);
    }

    /**
     * Initialize the DeviceListManager with the plugin context.
     */
    public static void initialize(Context pluginContext) {
        if (DeviceListManager.pluginContext == null) {
            DeviceListManager.pluginContext = pluginContext;
        }
    }

    public static List<DeviceInfo> getDeviceList(ListType listType) {
        return Collections.unmodifiableList(new ArrayList<>(getDeviceMap(listType).values()));
    }

    /// If device with existing MAC Address exists within list, entry will be overwritten.
    public static void addOrUpdateDevice(ListType listType, DeviceInfo deviceInfo) {
        Map<String, DeviceInfo> deviceList = getDeviceMap(listType);
        deviceList.put(deviceInfo.macAddress, deviceInfo);
        saveDevicesToPreferences(listType, deviceList);
    }

    public static void removeDevice(ListType listType, String macAddress) {
        Map<String, DeviceInfo> deviceList = getDeviceMap(listType);
        if (!deviceList.containsKey(macAddress)) return;
        deviceList.remove(macAddress);
        saveDevicesToPreferences(listType, deviceList);
    }

    public static boolean containsDevice(ListType listType, String macAddress) {
        return getDeviceMap(listType).containsKey(macAddress);
    }

    @Nullable
    public static DeviceInfo getDevice(ListType listType, String macAddress) {
        return getDeviceMap(listType).get(macAddress);
    }

    public static void clearList(ListType listType) {
        Map<String, DeviceInfo> empty = new HashMap<>();

        listTypeMap.put(listType, empty);
        saveDevicesToPreferences(listType, empty);
    }


    private static void checkInitialization() {
        if (pluginContext == null)
            throw new IllegalStateException("DeviceListManager is not initialized. Call DeviceListManager.initialize(context) first.");
    }

    private static Set<DeviceListChangeListener> getListeners(ListType listType) {
        if (listeners.containsKey(listType)) return listeners.get(listType);
        Set<DeviceListChangeListener> listListeners = new HashSet<>();
        listeners.put(listType, listListeners);
        return listListeners;
    }

    // Methods that access SharedPreferences file.
    private static Map<String, DeviceInfo> getDeviceMap(ListType listType) {
        checkInitialization();
        if (listTypeMap.containsKey(listType)) return listTypeMap.get(listType);

        SharedPreferences prefs = pluginContext.getSharedPreferences(listType.sharedPrefsFilename, Context.MODE_PRIVATE);
        String json = prefs.getString(DEVICE_LIST_ENTRY_NAME, "{}");
        Map<String, DeviceInfo> list = parseDevicesFromJson(json);
        if (list == null) throw new RuntimeException("JSON parsing failed. Gonna bail here.");
        listTypeMap.put(listType, list);
        return list;
    }


    private static void saveDevicesToPreferences(ListType listType, Map<String, DeviceInfo> devices) {
        checkInitialization();
        SharedPreferences listPref = pluginContext.getSharedPreferences(listType.sharedPrefsFilename, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = listPref.edit();
        String json = convertDeviceMapToJson(devices);
        editor.putString(DEVICE_LIST_ENTRY_NAME, json);
        editor.apply();

        // This function is called every time a device list is changed. Notify all listeners.
        for (DeviceListChangeListener listener : getListeners(listType)) {
            listener.onDeviceListChange(getDeviceList(listType));
        }
    }

    // JSON conversion methods
    /// @return Returns map if successful. Upon failure, will log and return null.
    @Nullable
    private static Map<String, DeviceInfo> parseDevicesFromJson(String json) {
        JSONObject baseJson;
        try {
            baseJson = new JSONObject(json);
        } catch (JSONException e) {
            String errStr = "Couldn't parse JSON, probably improperly formatted.\nJSON: %s";
            Log.e(TAG, String.format(errStr, json));
            return null;
        }
        Map<String, DeviceInfo> devMap = new HashMap<>();
        for (Iterator<String> it = baseJson.keys(); it.hasNext(); ) {
            String macAddr = it.next();

            JSONObject devJsonEntry;
            try {
                devJsonEntry = baseJson.getJSONObject(macAddr);
            } catch (JSONException e) {
                // error message is an assumption on the only way I could see this could be reached.
                String errStr = "Tried to get JSON object associated with mac address %s. Got something else instead.\nJSON: %s";
                Log.e(TAG, String.format(errStr, macAddr, json));
                return null;
            }

            DeviceInfo devInfo = new DeviceInfo();
            for (Field field : DeviceInfo.class.getFields()) {
                String fieldVal;
                try {
                    fieldVal = devJsonEntry.getString(field.getName());
                } catch (JSONException e) {
                    // reached when DeviceInfo has a field that isn't in the JSON. setting a default value instead.
                    fieldVal = "";
                }

                field.setAccessible(true); // need to call this in order to modify a "final" property.

                try {
                    field.set(devInfo, fieldVal);
                } catch (IllegalAccessException e) {
                    String errStr = "Attempted to write to inaccessible field %s.%s when parsing JSON.";
                    Log.w(TAG, String.format(errStr, DeviceInfo.class.getSimpleName(), field.getName()));
                    return null;
                }
            }
            devMap.put(macAddr, devInfo);
        }
        return devMap;
    }

    @Nullable
    private static String convertDeviceMapToJson(Map<String, DeviceInfo> deviceMap) {
        JSONObject baseJson = new JSONObject();
        for (Map.Entry<String, DeviceInfo> entry : deviceMap.entrySet()) {
            JSONObject devInfoJson = new JSONObject();
            DeviceInfo devInfo = entry.getValue();
            try {
                for (Field field : DeviceInfo.class.getFields()) {
                    if (Modifier.isPublic(field.getModifiers())) {
                        try {
                            devInfoJson.put(field.getName(), field.get(devInfo));
                        } catch (IllegalAccessException e) {
                            String errStr = "Attempted to access an inaccessible field %s.%s when writing to JSON.";
                            Log.w(TAG, String.format(errStr, DeviceInfo.class.getSimpleName(), field.getName()));
                        }
                    }
                }
                baseJson.put(entry.getKey(), devInfoJson);
            } catch (JSONException e) {
                // only way we reach here is if one of the "JSONObject.put" calls failed for some reason
                Log.e(TAG, e.getMessage());
                return null;
            }
        }
        return baseJson.toString();
    }


    /// All valid lists that can be accessed.
    public enum ListType {
        WHITELIST("devices_whitelist"), SENSORLIST("devices_sensors");

        public final String sharedPrefsFilename;

        ListType(String sharedPrefsFilename) {
            this.sharedPrefsFilename = sharedPrefsFilename;
        }
    }

    /**
     * Implemented by classes who want to listen to changes on a device list.
     * All classes who implement this interface must register with DeviceListManager by calling
     * {@link #addChangeListener(ListType, DeviceListChangeListener)}, passing itself as the listener a.k.a. "this".
     */
    public interface DeviceListChangeListener {
        void onDeviceListChange(List<DeviceInfo> devices);
    }

    // All public fields in this class must be strings (or else it'll open up an even bigger headache with JSON serialization).
    /// Class that contains device information.
    public static class DeviceInfo {
        public final String name;
        public final String macAddress;
        public final String rssi;

        public DeviceInfo(String macAddress, String name, int rssi) {
            this.name = name;
            this.macAddress = macAddress;
            this.rssi = Integer.toString(rssi);
        }

        // for JSON serialization method
        private DeviceInfo() {
            this.name = null;
            this.macAddress = null;
            this.rssi = "-1";
        }
    }
}