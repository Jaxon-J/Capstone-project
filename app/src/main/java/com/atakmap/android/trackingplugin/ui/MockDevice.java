package com.atakmap.android.trackingplugin.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MockDevice {

    private String deviceName;
    private String macAddress;
    private static List<MockDevice> mockDeviceList;

    MockDevice(String deviceName, String macAddress) {
        this.deviceName = deviceName;
        this.macAddress = macAddress;
    }

    public static List<MockDevice> getDevices(int numberOfDevices) {
        if (mockDeviceList == null) {
            mockDeviceList = new ArrayList<>();
            Random rand = new Random(System.currentTimeMillis());
            for (int i = 0; i < numberOfDevices; i++) {
                String name = "mock" + i;
                String mac = generateMacAddress(rand);
                mockDeviceList.add(new MockDevice(name, mac));
            }
        }
        return mockDeviceList;
    }

    private static String generateMacAddress(Random rand) {
        StringBuilder mac = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            mac.append(Integer.toString(rand.nextInt(255), 16));
            if (i != 5) mac.append(":");
        }
        return mac.toString();
    }

    public String getID() {
        return this.deviceName;
    }

    public String getMacAddress() {
        return this.macAddress;
    }


}
