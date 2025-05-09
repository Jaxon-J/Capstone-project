package com.atakmap.android.trackingplugin;

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
}
