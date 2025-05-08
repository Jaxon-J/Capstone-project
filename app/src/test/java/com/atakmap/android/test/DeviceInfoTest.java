package com.atakmap.android.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.trackingplugin.DeviceInfo;
///Test to ensure our deviceInfo test functionality is working. This should be a good indicator that this class
/// has not been broken with any edits.
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class DeviceInfoTest {

    @Test
    public void testDeviceInfoConstructor() {
        String uuid = "123e4567-e89b-12d3-a456-426614174000";
        DeviceInfo device = new DeviceInfo("Test Device", "00:11:22:33:44:55", -70, false, uuid, MapView.getDeviceUid());

        assertEquals("Test Device", device.name);
        assertEquals("00:11:22:33:44:55", device.macAddress);
        assertEquals(-70, device.rssi);
        assertFalse(device.mock);
        assertEquals(uuid, device.uuid);
    }

    @Test
    public void testDefaultConstructor() {
        DeviceInfo device = new DeviceInfo();

        assertNull(device.name);
        assertNull(device.macAddress);
        assertEquals(-1, device.rssi);
        assertFalse(device.mock);
        assertNotNull("UUID should be auto-generated", device.uuid);
    }

    @Test
    public void testGeneratedUUIDIsUnique() {
        DeviceInfo d1 = new DeviceInfo("A", "01:02:03:04:05:06", -40, true, null, MapView.getDeviceUid());
        DeviceInfo d2 = new DeviceInfo("B", "06:05:04:03:02:01", -40, true, null, MapView.getDeviceUid());
        assertNotEquals("UUIDs should be auto-generated and unique", d1.uuid, d2.uuid);
    }

    @Test
    public void testMacFormatAndUniquenessInMock() {
        List<DeviceInfo> mockDevices = generateMockDevicesForTest(10);
        Set<String> macSet = new HashSet<>();
        for (DeviceInfo device : mockDevices) {
            assertNotNull(device.macAddress);
            assertTrue(macSet.add(device.macAddress)); // Check uniqueness
            String[] parts = device.macAddress.split(":");
            assertEquals(6, parts.length);
            for (String part : parts) {
                assertTrue("MAC format invalid: " + part, part.matches("[0-9a-fA-F]{1,2}"));
            }
        }
    }

    // Local test-safe version of mock generator (bypasses DeviceStorageManager)
    private List<DeviceInfo> generateMockDevicesForTest(int count) {
        List<DeviceInfo> devices = new ArrayList<>();
        Set<String> usedMacs = new HashSet<>();
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            String macAddr;
            do {
                StringBuilder macBuilder = new StringBuilder();
                for (int j = 0; j < 6; j++) {
                    macBuilder.append(String.format("%02x", rand.nextInt(256)));
                    if (j != 5) macBuilder.append(":");
                }
                macAddr = macBuilder.toString();
            } while (usedMacs.contains(macAddr));
            usedMacs.add(macAddr);

            devices.add(new DeviceInfo("mock" + i, macAddr, rand.nextInt(100), true, null, MapView.getDeviceUid()));
        }
        return devices;
    }
}
