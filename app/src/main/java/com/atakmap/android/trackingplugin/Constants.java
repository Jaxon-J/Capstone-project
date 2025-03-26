package com.atakmap.android.trackingplugin;

import androidx.core.util.Pair;

import com.atakmap.android.trackingplugin.plugin.R;

import java.util.List;

public final class Constants {
    public static final String TAG_PREFIX = "TrackPlug.";
    public static final String TRACKING_TABNAME = "Tracking";
    public static final String DEVICES_TABNAME = "Devices";
    public static final String SENSORS_TABNAME = "Sensors";
    public static final String DEBUG_TABNAME = "Debug";
    // this list order = UI tab order
    public static final List<Pair<String, Integer>> TAB_LAYOUTS =
            List.of(new Pair<>(TRACKING_TABNAME, R.layout.tracking_layout),
                    new Pair<>(DEVICES_TABNAME, R.layout.devices_layout),
                    new Pair<>(SENSORS_TABNAME, R.layout.sensors_layout),
                    new Pair<>(DEBUG_TABNAME, R.layout.debug_layout));

    public static String createTag(Class<?> clazz) {
        return TAG_PREFIX + clazz.getSimpleName();
    }
}
