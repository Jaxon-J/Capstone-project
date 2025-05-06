package com.atakmap.android.test;

import android.content.Context;
import android.content.SharedPreferences;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.trackingplugin.DeviceInfo;
import com.atakmap.android.trackingplugin.DeviceStorageManager;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class DeviceStorageMangerTest {

    private Context mockContext;
    private SharedPreferences mockPrefs;
    private SharedPreferences.Editor mockEditor;

    private static final String UUID = "test-uuid";

    @Before
    public void setUp() {
        // Mock SharedPreferences, Context, and MapView
        mockContext = mock(Context.class);
        mockPrefs = mock(SharedPreferences.class);
        mockEditor = mock(SharedPreferences.Editor.class);

        when(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs);
        when(mockPrefs.getString(anyString(), anyString())).thenReturn("{}");
        when(mockPrefs.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);

        // Static mocking MapView.getMapView().getContext()
        MockedStatic<MapView> mapViewStatic = Mockito.mockStatic(MapView.class);
        MapView mockMapView = mock(MapView.class);
        mapViewStatic.when(MapView::getMapView).thenReturn(mockMapView);
        when(mockMapView.getContext()).thenReturn(mockContext);
    }

    @Test
    public void testAddAndGetDevice() {
        // Create a test device
        DeviceInfo testDevice = new DeviceInfo();
        setField(testDevice, "uuid", UUID);
        setField(testDevice, "name", "Test Device");
        setField(testDevice, "macAddress", "00:11:22:33:44:55");

        // Add device
        DeviceStorageManager.addOrUpdateDevice(DeviceStorageManager.ListType.WHITELIST, testDevice);

        // Retrieve device
        DeviceInfo result = DeviceStorageManager.getDevice(DeviceStorageManager.ListType.WHITELIST, UUID);

        assertNotNull(result);
        assertEquals("Test Device", result.name);
        assertEquals("00:11:22:33:44:55", result.macAddress);
    }

    // Helper to set final String fields
    private void setField(DeviceInfo deviceInfo, String fieldName, String value) {
        try {
            java.lang.reflect.Field field = DeviceInfo.class.getField(fieldName);
            field.setAccessible(true);
            java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            field.set(deviceInfo, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}