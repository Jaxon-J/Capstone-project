package com.atakmap.android.trackingplugin;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import com.atakmap.android.maps.MapView;

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

// TODO: could maybe rename this to DeviceStorageManger to avoid confusion. If whitelist is the only persistent data,
//  could also just rename to WhitelistHandler and get rid of "list" abstractions i.e. "ListType"
/// Class that handles persistent data involving lists of devices, primarily constructed for whitelist and sensor list.
public class DeviceStorageManager {
    private static final String DEVICE_LIST_ENTRY_NAME = "device_list";
    private static final String TAG = Constants.createTag(DeviceStorageManager.class);
    private static final Map<ListType, Map<String, DeviceInfo>> listTypeMap = new HashMap<>();
    private static final Map<ListType, Set<DeviceListChangeListener>> listeners = new HashMap<>();

    // Private constructor to prevent instantiation
    private DeviceStorageManager() {
        // No instantiation
    }

    public static void addChangeListener(ListType listType, DeviceListChangeListener listener) {
        getListeners(listType).add(listener);
    }

    public static List<DeviceInfo> getDeviceList(ListType listType) {
        return Collections.unmodifiableList(new ArrayList<>(getDeviceMap(listType).values()));
    }

    /// If DeviceInfo was constructed with UUID as "null", a new entry will be created.
    /// If UUID was supplied when constructing DeviceInfo, the entry with that UUID will be overwritten.
    public static void addOrUpdateDevice(ListType listType, DeviceInfo deviceInfo) {
        Map<String, DeviceInfo> deviceList = getDeviceMap(listType);
        deviceList.put(deviceInfo.uuid, deviceInfo);
        saveDevicesToPreferences(listType, deviceList);
    }

    public static void removeDevice(ListType listType, String uuid) {
        Map<String, DeviceInfo> deviceList = getDeviceMap(listType);
        if (!deviceList.containsKey(uuid)) return;
        deviceList.remove(uuid);
        saveDevicesToPreferences(listType, deviceList);
    }

    public static boolean containsDevice(ListType listType, String uuid) {
        return getDeviceMap(listType).containsKey(uuid);
    }

    /// Returns device in the device list. If it isn't in the list, it will return null.
    @Nullable
    public static DeviceInfo getDevice(ListType listType, String uuid) {
        if (uuid == null)
            uuid = ""; // avoid NullPointerException
        return getDeviceMap(listType).get(uuid);
    }

    public static Set<String> getUuids(ListType listType) {
        return getDeviceMap(listType).keySet();
    }

    public static String getUuid(ListType listType, String macAddress) {
        if (macAddress == null) return null;
        for (Map.Entry<String, DeviceInfo> entry : getDeviceMap(listType).entrySet()) {
            DeviceInfo deviceInfo = entry.getValue();
            if (macAddress.equals(deviceInfo.macAddress))
                return entry.getKey();
        }
        return null;
    }

    public static void clearList(ListType listType) {
        Map<String, DeviceInfo> empty = new HashMap<>();

        listTypeMap.put(listType, empty);
        saveDevicesToPreferences(listType, empty);
    }

    private static Set<DeviceListChangeListener> getListeners(ListType listType) {
        if (listeners.containsKey(listType)) return listeners.get(listType);
        Set<DeviceListChangeListener> listListeners = new HashSet<>();
        listeners.put(listType, listListeners);
        return listListeners;
    }

    // Methods that access SharedPreferences file.
    private static Map<String, DeviceInfo> getDeviceMap(ListType listType) {
        if (listTypeMap.containsKey(listType)) return listTypeMap.get(listType);

        // list hasn't been loaded yet, get it from SharedPreferences file (need main app context for SharedPrefs)
        SharedPreferences prefs = MapView.getMapView().getContext().getSharedPreferences(listType.sharedPrefsFilename, Context.MODE_PRIVATE);
        String json = prefs.getString(DEVICE_LIST_ENTRY_NAME, "{}");
        Map<String, DeviceInfo> list = parseDevicesFromJson(json);
        if (list == null) throw new RuntimeException("JSON parsing failed. Gonna bail here.");
        listTypeMap.put(listType, list);
        return list;
    }


    private static void saveDevicesToPreferences(ListType listType, Map<String, DeviceInfo> devices) {
        SharedPreferences listPref = MapView.getMapView().getContext().getSharedPreferences(listType.sharedPrefsFilename, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = listPref.edit();
        String json = convertDeviceMapToJson(devices);
        if (json == null) throw new RuntimeException("JSON serializing failed. Bailing here.");
        Log.d(TAG, String.format("Updating %s to JSON: %s", listType.sharedPrefsFilename, json));
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
            String devUuid = it.next();

            JSONObject devJsonEntry;
            try {
                devJsonEntry = baseJson.getJSONObject(devUuid);
            } catch (JSONException e) {
                // error message is an assumption on the only way I could see this could be reached.
                String errStr = "Tried to get JSON object associated with UUID %s. Got something else instead.\nJSON: %s";
                Log.e(TAG, String.format(errStr, devUuid, json));
                return null;
            }

            DeviceInfo devInfo = new DeviceInfo();
            for (Field field : DeviceInfo.class.getFields()) {
                int modifiers = field.getModifiers();
                // only check public final String fields.
                if (!Modifier.isPublic(modifiers) || !Modifier.isFinal(modifiers) || !field.getType().getSimpleName().equals("String"))
                    continue;
                String fieldVal;
                try {
                    fieldVal = devJsonEntry.getString(field.getName());
                } catch (JSONException e) {
                    // reached when StoredDeviceInfo has a field that isn't in the JSON. setting a default value instead.
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
            devMap.put(devUuid, devInfo);
        }
        return devMap;
    }

    @Nullable
    private static String convertDeviceMapToJson(Map<String, DeviceInfo> deviceMap) {
        JSONObject baseJson = new JSONObject();
        for (Map.Entry<String, DeviceInfo> entry : deviceMap.entrySet()) {
            JSONObject devInfoJson = new JSONObject();
            DeviceInfo devInfo = entry.getValue();
            if (devInfo.mock) continue;
            try {
                for (Field field : DeviceInfo.class.getFields()) {
                    int modifiers = field.getModifiers();
                    // only allow public final String's to be serialized.
                    if (!Modifier.isPublic(modifiers) || !Modifier.isFinal(modifiers) || !field.getType().getSimpleName().equals("String"))
                        continue;
                    try {
                        devInfoJson.put(field.getName(), field.get(devInfo));
                    } catch (IllegalAccessException e) {
                        String errStr = "Attempted to access an inaccessible field %s.%s when writing to JSON.";
                        Log.w(TAG, String.format(errStr, DeviceInfo.class.getSimpleName(), field.getName()));
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
     * All classes who implement this interface must register with DeviceStorageManager by calling
     * {@link #addChangeListener(ListType, DeviceListChangeListener)}, passing itself as the listener a.k.a. "this".
     */
    public interface DeviceListChangeListener {
        void onDeviceListChange(List<DeviceInfo> devices);
    }
}