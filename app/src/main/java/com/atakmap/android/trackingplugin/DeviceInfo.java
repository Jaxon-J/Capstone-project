package com.atakmap.android.trackingplugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/// Class that stores information about a device.
/// Note that only "public final String" fields will be stored persistently if passed to
/// {@link DeviceListManager#addOrUpdateDevice(DeviceListManager.ListType, DeviceInfo)}
public class DeviceInfo {
    public final String name;
    public final String macAddress;
    public int rssi;
    public final boolean mock;

    public DeviceInfo(String name, String macAddr, int rssi, boolean mock) {
        this.name = name;
        this.macAddress = macAddr;
        this.rssi = rssi;
        this.mock = mock;
    }

    /// SHOULD ONLY BE CALLED FOR JSON SERIALIZATION.
    public DeviceInfo() {
        this(null, null, -1, false);
    }

    public static List<DeviceInfo> getMockDevices(int numberOfDevices, DeviceListManager.ListType associatedList) {
        List<DeviceInfo> mockDeviceList = new ArrayList<>();
        Random rand = new Random(System.currentTimeMillis());
        for (int i = 0; i < numberOfDevices; i++) {
            String macAddr;
            do {
                StringBuilder macBuilder = new StringBuilder();
                for (int j = 0; j < 6; j++) {
                    macBuilder.append(Integer.toString(rand.nextInt(255), 16));
                    if (j != 5) macBuilder.append(":");
                }
                macAddr = macBuilder.toString();
            // test data and real data should have mutually exclusive mac addresses.
            } while (DeviceListManager.getDevice(associatedList, macAddr) != null);
            mockDeviceList.add(new DeviceInfo("mock" + i, macAddr, (rand.nextInt(20) + 1) * 5, true));
        }
        return mockDeviceList;
    }
}
