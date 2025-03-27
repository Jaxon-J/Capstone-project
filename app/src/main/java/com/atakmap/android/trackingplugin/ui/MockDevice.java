package com.atakmap.android.trackingplugin.ui;

import android.net.MacAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.crypto.Mac;


public class MockDevice {

    private static ArrayList<MockDevice> devices = new ArrayList<>();
    private String deviceID;
    private String MAC;
    private static List<MockDevice> mockDeviceList;

    MockDevice(String deviceID, String MAC) {
        this.deviceID = deviceID;
        this.MAC = MAC;
        devices.add(this);
    }

    public static List<MockDevice> getDevices() {
        if (mockDeviceList == null) {
            mockDeviceList = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                String name = "mock" + i;
                String mac = generateMacAddress();
                mockDeviceList.add(new MockDevice(name, mac));
            }
        }
        return mockDeviceList;
    }

    private static String generateMacAddress() {
        Random rand = new Random(System.currentTimeMillis());
        StringBuilder mac = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            mac.append(Integer.toString(rand.nextInt(255), 16));
            if (i != 5) mac.append(":");
        }
        return mac.toString();
    }

    public String getID() {
        return this.deviceID;
    }

    public String getMAC() {
        return this.MAC;
    }


}
