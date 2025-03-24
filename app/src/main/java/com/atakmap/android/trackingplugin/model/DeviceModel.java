package com.atakmap.android.trackingplugin.model;

public class DeviceModel {
    private String name;
    private String macAddr;
    private int rssi;

    public DeviceModel(String name, String macAddress, int rssi) {
        this.name = name;
        this.macAddr = macAddress;
        this.rssi = rssi;
    }

    public String getName() {
        return name;
    }

    public String getMacAddress() {
        return macAddr;
    }

    public int getRssi() {
        return rssi;
    }

    @Override
    public String toString() {
        return name + " (" + macAddr + ") RSSI: " + rssi;
    }
}

