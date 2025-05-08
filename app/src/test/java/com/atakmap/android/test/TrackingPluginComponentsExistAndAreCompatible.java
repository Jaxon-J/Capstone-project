///This is a simple JUnit test that makes sure our core classes have not changed there tags, ensuring that a push has not
/// messed with the core classes and how they exist everywhere else

package com.atakmap.android.test;


import org.junit.Test;
import static org.junit.Assert.*;

import com.atakmap.android.trackingplugin.BluetoothReceiver;
import com.atakmap.android.trackingplugin.Constants;
import com.atakmap.android.trackingplugin.DeviceInfo;
import com.atakmap.android.trackingplugin.DeviceStorageManager;
import com.atakmap.android.trackingplugin.comms.CotDetailTypes;
import com.atakmap.android.trackingplugin.comms.DeviceCotDispatcher;
import com.atakmap.android.trackingplugin.comms.DeviceCotListener;
import com.atakmap.android.trackingplugin.plugin.PluginNativeLoader;
import com.atakmap.android.trackingplugin.plugin.TrackingPlugin;
import com.atakmap.android.trackingplugin.ui.TabViewPagerAdapter;
import com.atakmap.android.trackingplugin.ui.WhitelistTable;


public class TrackingPluginComponentsExistAndAreCompatible {
    @Test
    public void testCreateTag() {
        String expectedTag = "TrackPlug.TrackingPlugin";
        String actualTag = Constants.createTag(TrackingPlugin.class);
        assertEquals(expectedTag, actualTag);

        String expectedConstantsTag = "TrackPlug.Constants";
        String actualConstantsTag = Constants.createTag(Constants.class);
        assertEquals(expectedConstantsTag, actualConstantsTag);

        String expectedReceiverTag = "TrackPlug.BluetoothReceiver";
        String actualReceiverTag = Constants.createTag(BluetoothReceiver.class);
        assertEquals(expectedReceiverTag, actualReceiverTag);

        String expectedDITag = "TrackPlug.DeviceInfo";
        String actualDITag = Constants.createTag(DeviceInfo.class);
        assertEquals(expectedDITag, actualDITag);

        String expectedStorageTag = "TrackPlug.DeviceStorageManager";
        String actualStorageTag = Constants.createTag(DeviceStorageManager.class);
        assertEquals(expectedStorageTag, actualStorageTag);

        String expectedCotDetailTag = "TrackPlug.CotDetailTypes";
        String actualCotDetailTag = Constants.createTag(CotDetailTypes.class);
        assertEquals(expectedCotDetailTag, actualCotDetailTag);

        String expectedCotDispatcherTag = "TrackPlug.DeviceCotDispatcher";
        String actualCotDispatcherTag = Constants.createTag(DeviceCotDispatcher.class);
        assertEquals(expectedCotDispatcherTag, actualCotDispatcherTag);

        String expectedCotListenerTag = "TrackPlug.DeviceCotListener";
        String actualCotListenerTag = Constants.createTag(DeviceCotListener.class);
        assertEquals(expectedCotListenerTag, actualCotListenerTag);

        String expectedPlugInNativeLoaderTag = "TrackPlug.PluginNativeLoader";
        String actualPlugInNativeLoaderTag = Constants.createTag(PluginNativeLoader.class);
        assertEquals(expectedPlugInNativeLoaderTag, actualPlugInNativeLoaderTag);

        String expectedTrackPluginTag = "TrackPlug.TrackingPlugin";
        String actualTrackPluginTag = Constants.createTag(TrackingPlugin.class);
        assertEquals(expectedTrackPluginTag, actualTrackPluginTag);

        String expectedTabViewTag = "TrackPlug.TabViewPagerAdapter";
        String actualTabViewTag = Constants.createTag(TabViewPagerAdapter.class);
        assertEquals(expectedTabViewTag, actualTabViewTag);

        String expectedWhitelistTableTag = "TrackPlug.WhitelistTable";
        String actualWhitelistTableTag = Constants.createTag(WhitelistTable.class);
        assertEquals(expectedWhitelistTableTag, actualWhitelistTableTag);
    }
}

