///This is a simple JUnit test that makes sure our core classes have not changed there tags, ensuring that a push has not
/// messed with the core classes and how they exist everywhere else

package com.atakmap.android.test;


import org.junit.Test;
import static org.junit.Assert.*;

import com.atakmap.android.trackingplugin.BluetoothReceiver;
import com.atakmap.android.trackingplugin.Constants;
import com.atakmap.android.trackingplugin.plugin.TrackingPlugin;


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
    }
}

