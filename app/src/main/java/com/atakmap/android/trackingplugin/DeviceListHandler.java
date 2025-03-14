package com.atakmap.android.trackingplugin;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class DeviceListHandler {
    private static final String DEFAULT_DEVICENAME = "(null)";
    private final SharedPreferences prefs;

    public DeviceListHandler(Context context, Lists list) {
        this.prefs = context.getSharedPreferences(list.sharedPrefsFileName, Context.MODE_PRIVATE);
    }

    @SuppressLint("MissingPermission")
    public void saveDevice(BluetoothDevice device) {
        this.saveEntry(device.getAddress(), device.getName());
    }

    // base method for putting entries into the whitelist. indexed by macAddress, will overwrite.
    public void saveEntry(String macAddress, String name) {
        if (name == null) name = "";
        SharedPreferences.Editor edit = this.prefs.edit();
        JSONObject data = new JSONObject();
        try {
            // ADD DATA FIELDS HERE
            data.put("name", name);

            // put in whitelist file indexed by mac addr
            edit.putString(macAddress, data.toString());
            edit.apply();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public Entry getEntry(String macAddress) {
        String entryJsonStr = this.prefs.getString(macAddress, null);
        if (entryJsonStr == null) return new Entry("", "", false);
        try {
            JSONObject entryJson = new JSONObject(entryJsonStr);

            // retrieve data fields here
            String name = entryJson.optString("name", DEFAULT_DEVICENAME);
            return new Entry(name, macAddress, true);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Entry> getAllEntries() {
        List<Entry> entries = new ArrayList<>();
        for (String addr : this.prefs.getAll().keySet()) {
            entries.add(this.getEntry(addr));
        }
        return entries;
    }

    public enum Lists {
        WHITELIST("whitelist_devices");

        public final String sharedPrefsFileName;

        Lists(String sharedPrefsFilename) {
            this.sharedPrefsFileName = sharedPrefsFilename;
        }
    }

    public static class Entry {
        public final String name;
        public final String macAddress;
        public final boolean exists;

        private Entry(String name, String macAddress, boolean exists) {
            this.name = name;
            this.macAddress = macAddress;
            this.exists = exists;
        }
    }
}
