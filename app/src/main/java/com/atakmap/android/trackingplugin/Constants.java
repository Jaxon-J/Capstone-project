package com.atakmap.android.trackingplugin;

import androidx.core.util.Pair;

import com.atakmap.android.trackingplugin.plugin.R;

import java.util.List;
import java.util.Random;

/**
 * Class that is meant to hold shared constant values.
 * Best practice is to use {@link #createTag(Class)} when making tag fields that will be passed to {@link android.util.Log} methods.
 */
public final class Constants {
    public static final String TAG_PREFIX = "TrackPlug.";
    public static final String TRACKING_TABNAME = "Tracking";
    public static final String DEVICES_TABNAME = "Devices";
    public static final String SENSORS_TABNAME = "Sensors";
    public static final String DEBUG_TABNAME = "Debug";

    public static final String DEFAULT_DEVICE_NAME = "unknown";

    public static final String DEVICE_RADIUS_CIRCLE_NAME = "radius-" + (new Random()).nextInt();
    public static final String RADIUS_GROUP_NAME = "tracking-plugin-radius-group";

    // Tab position index determined here, which is why this can't be a map.
    public static final List<Pair<String, Integer>> TAB_LAYOUTS = List.of(
            new Pair<>(TRACKING_TABNAME, R.layout.tracking_layout),
            new Pair<>(DEVICES_TABNAME, R.layout.devices_layout),
            new Pair<>(SENSORS_TABNAME, R.layout.sensors_layout),
            new Pair<>(DEBUG_TABNAME, R.layout.debug_layout));
    public static final int TAB_COUNT = TAB_LAYOUTS.size();

    public static String createTag(Class<?> clazz) {
        return TAG_PREFIX + clazz.getSimpleName();
    }
}
