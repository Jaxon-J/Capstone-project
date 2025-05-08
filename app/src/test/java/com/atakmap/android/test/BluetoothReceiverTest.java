package com.atakmap.android.test;

import android.content.Context;

import android.content.pm.PackageManager;

import com.atakmap.android.trackingplugin.BluetoothReceiver;
import com.atakmap.android.trackingplugin.DeviceInfo;
import com.atakmap.android.trackingplugin.DeviceStorageManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class BluetoothReceiverTest {

    @Test
    public void BtPermissionsreturnsFalse() {
        Context mockContext = mock(Context.class);
        when(mockContext.checkSelfPermission(anyString())).thenReturn(PackageManager.PERMISSION_DENIED);

        boolean result = BluetoothReceiver.hasAllBtPermissions(mockContext);
        assertFalse(result);
    }

    @Test
    public void BtPermissionsreturnsTrue() {
        Context mockContext = mock(Context.class);
        when(mockContext.checkSelfPermission(anyString())).thenReturn(PackageManager.PERMISSION_GRANTED);

        boolean result = BluetoothReceiver.hasAllBtPermissions(mockContext);
        assertTrue(result);
    }

    @Test
    public void SetWhitelistwithoutStorageCall() throws Exception {
        // Manually set up the static map so that BluetoothReceiver constructor doesn't crash
        Field mapField = DeviceStorageManager.class.getDeclaredField("listTypeMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Enum, Map<String, DeviceInfo>> listTypeMap = (Map<Enum, Map<String, DeviceInfo>>) mapField.get(null);
        if (listTypeMap == null) {
            listTypeMap = new HashMap<>();
            mapField.set(null, listTypeMap);
        }
        if (!listTypeMap.containsKey(DeviceStorageManager.ListType.WHITELIST)) {
            listTypeMap.put(DeviceStorageManager.ListType.WHITELIST, new HashMap<>());
        }

        // Create a device manually
        DeviceInfo device = new DeviceInfo("Mock Device", "00:11:22:33:44:55", -60, false, "uuid", "deviceUid");

        // Create mock context for constructor
        Context context = mock(Context.class);
        when(context.getSystemService(anyString())).thenReturn(null);

        Constructor<BluetoothReceiver> constructor = BluetoothReceiver.class.getDeclaredConstructor(Context.class);
        constructor.setAccessible(true);
        BluetoothReceiver receiver = constructor.newInstance(context);

        // Call method directly
        receiver.onDeviceListChange(Collections.singletonList(device));

        // Check whitelist field
        Field whitelistField = BluetoothReceiver.class.getDeclaredField("whitelistMacAddresses");
        whitelistField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> whitelist = (Set<String>) whitelistField.get(receiver);

        assertNotNull(whitelist);
        assertTrue(whitelist.contains("00:11:22:33:44:55"));
    }

    @Test
    public void nullActions() {
        Context mockContext = mock(Context.class);
        BluetoothReceiver receiver = mock(BluetoothReceiver.class, Mockito.withSettings()
                .useConstructor(mockContext)
                .defaultAnswer(CALLS_REAL_METHODS));

        receiver.onReceive(mockContext, new android.content.Intent());
        // No exception = pass
    }
}
