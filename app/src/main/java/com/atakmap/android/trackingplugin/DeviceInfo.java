package com.atakmap.android.trackingplugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

// TODO: should visibility be stored persistently, or everything defaults to invisible on every time ATAK re-opens?

/// Class that stores information about a device.
/// Note that only "public final String" fields will be stored persistently if passed to
/// {@link DeviceListManager#addOrUpdateDevice(DeviceListManager.ListType, DeviceInfo)}
public class DeviceInfo {
    public final String uuid;
    public final String name;
    public final String macAddress;
    public int rssi;
    public long seenTimeEpochMillis = -1;
    public String observerDeviceName = null;
    public final boolean mock;
    private final static Random rand = new Random();

    public DeviceInfo(String name, String macAddr, int rssi, boolean mock, String uuid) {
        this.name = name;
        this.macAddress = macAddr;
        this.rssi = rssi;
        this.mock = mock;
        if (uuid == null) {
            this.uuid = (new UUID(rand.nextLong(), rand.nextLong())).toString();
        } else {
            this.uuid = uuid;
        }
    }


    /// SHOULD ONLY BE CALLED FOR JSON SERIALIZATION.
    public DeviceInfo() {
        this(null, null, -1, false, null);
    }

    public static List<DeviceInfo> getMockDevices(int numberOfDevices, DeviceListManager.ListType associatedList) {
        List<DeviceInfo> mockDeviceList = new ArrayList<>();
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
            mockDeviceList.add(new DeviceInfo("mock" + i, macAddr, (rand.nextInt(20) + 1) * 5, true, null));
        }
        return mockDeviceList;
    }
}
