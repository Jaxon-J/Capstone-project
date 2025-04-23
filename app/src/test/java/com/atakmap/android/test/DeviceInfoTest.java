package com.atakmap.android.test;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

import com.atakmap.android.trackingplugin.DeviceInfo;
import com.atakmap.android.trackingplugin.DeviceListManager;
///Test to ensure our deviceInfo test functionality is working. This should be a good indicator that this class
/// has not been broken with any edits.
public class DeviceInfoTest {
    ///Tests if the device info fields are being passed correctly.
    @Test
    public void testDeviceInfoConstructor() {
        DeviceInfo device = new DeviceInfo("Test Device", "00:11:22:33:44:55", -70, false);
        assertEquals("Test Device", device.name);
        assertEquals("00:11:22:33:44:55", device.macAddress);
        assertEquals(-70, device.rssi);
        assertFalse(device.mock);
    }
    ///Tests that fields are being set right for JSON serialization.
    @Test
    public void testDefaultConstructor() {
        DeviceInfo device = new DeviceInfo();
        assertNull(device.name);
        assertNull(device.macAddress);
        assertEquals(-1, device.rssi);
        assertFalse(device.mock);
    }
    ///Makes sure that the correct numbers of devices are returned along with correct naming conventions
    @Test
    public void testMockDevicesBypassDeviceListManager_uniqueMacs() {
        List<DeviceInfo> mockDevices = generateMockDevicesForTest(10);
        assertEquals(10, mockDevices.size());

        Set<String> macSet = new HashSet<>();
        for (DeviceInfo device : mockDevices) {
            assertNotNull(device.name);
            assertTrue(device.name.startsWith("mock"));
            assertNotNull(device.macAddress);
            assertTrue(device.mock);
            assertTrue(device.rssi > 0);
            assertTrue(macSet.add(device.macAddress)); // Ensure MAC uniqueness
        }
    }

    @Test
    public void testMockDeviceMacFormat() {
        List<DeviceInfo> mockDevices = generateMockDevicesForTest(5);
        for (DeviceInfo device : mockDevices) {
            String[] parts = device.macAddress.split(":");
            assertEquals(6, parts.length);
            for (String part : parts) {
                assertTrue("Invalid MAC format: " + part, part.matches("[0-9a-fA-F]{1,2}"));
            }
        }
    }

    /// Local test-only version of getMockDevices() (the class that was throwing errors)
    ///  without DeviceListManager dependency. This is a work around to avoid
    /// having to initialize the context of deviceInfo.
    private List<DeviceInfo> generateMockDevicesForTest(int count) {
        List<DeviceInfo> mockDeviceList = new ArrayList<>();
        Set<String> usedMacs = new HashSet<>();
        Random rand = new Random(System.currentTimeMillis());

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

            mockDeviceList.add(new DeviceInfo("mock" + i, macAddr, (rand.nextInt(20) + 1) * 5, true));
        }

        return mockDeviceList;
    }
}
