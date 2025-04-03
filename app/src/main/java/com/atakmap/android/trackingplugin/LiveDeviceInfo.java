package com.atakmap.android.trackingplugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LiveDeviceInfo {
    public final String name;
    public final String macAddr;
    public final String rssi;

    public LiveDeviceInfo(String name, String macAddr, String rssi) {
        this.name = name;
        this.macAddr = macAddr;
        this.rssi = rssi;
    }

    public LiveDeviceInfo(String name, String macAddr, int rssi) {
        this(name, macAddr, Integer.toString(rssi));
    }

    public LiveDeviceInfo(String name, String macAddr) {
        this(name, macAddr, "?");
    }

    public static List<LiveDeviceInfo> getMockDevices(int numberOfDevices) {
        List<LiveDeviceInfo> mockDeviceList = new ArrayList<>();
        Random rand = new Random(System.currentTimeMillis());
        for (int i = 0; i < numberOfDevices; i++) {
            String name = "mock" + i;
            StringBuilder macBuilder = new StringBuilder();
            for (int j = 0; j < 6; j++) {
                macBuilder.append(Integer.toString(rand.nextInt(255), 16));
                if (j != 5) macBuilder.append(":");
            }
            int rssi = -5 * (rand.nextInt(15) + 1);
            mockDeviceList.add(new LiveDeviceInfo(name, macBuilder.toString(), rssi));
        }
        return mockDeviceList;
    }
}
