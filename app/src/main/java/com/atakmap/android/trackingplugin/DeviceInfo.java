package com.atakmap.android.trackingplugin;

import com.atakmap.android.maps.MapView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

// TODO: should visibility be stored persistently, or everything defaults to invisible on every time ATAK re-opens?

/// Class that stores information about a device.
/// Note that only "public final String" fields will be stored persistently if passed to
/// {@link DeviceStorageManager#addOrUpdateDevice(DeviceStorageManager.ListType, DeviceInfo)}
public class DeviceInfo {
    public final String uuid;
    public final String name;
    public final String macAddress;
    public final String sensorUid;
    public int rssi;
    public final boolean mock;
    private final static Random rand = new Random();

    public DeviceInfo(String name, String macAddress, int rssi, boolean mock, String uuid, String sensorUid) {
        this.name = name;
        this.macAddress = macAddress;
        this.rssi = rssi;
        this.mock = mock;
        this.sensorUid = sensorUid;
        if (uuid == null) {
            this.uuid = (new UUID(rand.nextLong(), rand.nextLong())).toString();
        } else {
            this.uuid = uuid;
        }
    }

    /// Good constructor to use for updating rssi.
    public DeviceInfo(DeviceInfo deviceInfo, int rssi) {
        this(deviceInfo.name, deviceInfo.macAddress, rssi, deviceInfo.mock, deviceInfo.uuid, deviceInfo.sensorUid);
    }


    /// SHOULD ONLY BE CALLED FOR JSON SERIALIZATION.
    public DeviceInfo() {
        this(null, null, -1, false, null, null);
    }

    public static List<DeviceInfo> getMockDevices(int numberOfDevices, DeviceStorageManager.ListType associatedList) {
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
            } while (DeviceStorageManager.getDevice(associatedList, macAddr) != null);
            mockDeviceList.add(new DeviceInfo("mock" + i, macAddr, (rand.nextInt(20) + 1) * 5, true, null, MapView.getDeviceUid()));
        }
        return mockDeviceList;
    }
}
