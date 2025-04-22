package com.atakmap.android.test;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

import com.atakmap.android.trackingplugin.DeviceInfo;
import com.atakmap.android.trackingplugin.DeviceListManager;

public class DeviceInfoTest {

    @Test
    public void testDeviceInfoConstructor() {
        DeviceInfo device = new DeviceInfo("Test Device", "00:11:22:33:44:55", -70, false);
        assertEquals("Test Device", device.name);
        assertEquals("00:11:22:33:44:55", device.macAddress);
        assertEquals(-70, device.rssi);
        assertFalse(device.mock);
    }

    @Test
    public void testDefaultConstructor() {
        DeviceInfo device = new DeviceInfo();
        assertNull(device.name);
        assertNull(device.macAddress);
        assertEquals(-1, device.rssi);
        assertFalse(device.mock);
    }

    @Test
    public void testMockDevicesGeneration_uniqueMacs() {
        List<DeviceInfo> mockDevices = DeviceInfo.getMockDevices(10, DeviceListManager.ListType.WHITELIST);
        assertEquals(10, mockDevices.size());

        Set<String> macSet = new HashSet<>();
        for (DeviceInfo device : mockDevices) {
            assertNotNull(device.name);
            assertTrue(device.name.startsWith("mock"));
            assertNotNull(device.macAddress);
            assertTrue(device.mock);
            assertTrue(device.rssi > 0);
            assertFalse(macSet.contains(device.macAddress)); // ensure uniqueness
            macSet.add(device.macAddress);
        }
    }

    @Test
    public void testMockDeviceMacFormat() {
        List<DeviceInfo> mockDevices = DeviceInfo.getMockDevices(5, DeviceListManager.ListType.WHITELIST);
        for (DeviceInfo device : mockDevices) {
            String[] parts = device.macAddress.split(":");
            assertEquals(6, parts.length);
            for (String part : parts) {
                assertTrue("Invalid MAC format part: " + part, part.matches("[0-9a-fA-F]{1,2}"));
            }
        }
    }
}
