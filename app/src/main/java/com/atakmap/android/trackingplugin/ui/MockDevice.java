package com.atakmap.android.trackingplugin.ui;

import java.util.ArrayList;


public class MockDevice {

    private static ArrayList<MockDevice> devices = new ArrayList<>();
    private String deviceID;
    private String MAC;
    MockDevice(String deviceID, String MAC) {
        this.deviceID = deviceID;
        this.MAC = MAC;
        devices.add(this);
    }

    public static ArrayList<MockDevice> getDevices() {
        return devices;
    }

    public String getID() {
        return this.deviceID;
    }

    public String getMAC() {
        return this.MAC;
    }


}
