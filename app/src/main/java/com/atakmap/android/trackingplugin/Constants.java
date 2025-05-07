package com.atakmap.android.trackingplugin;

import androidx.core.util.Pair;

import com.atakmap.android.trackingplugin.plugin.R;

import java.util.List;

/**
 * Class that is meant to hold shared constant values.
 * Best practice is to use {@link #createTag(Class)} when making tag fields that will be passed to {@link android.util.Log} methods.
 */
public final class Constants {
    public static final String TAG_PREFIX = "TrackPlug.";
    public static final String TRACKING_TABNAME = "Tracking";
    public static final String WHITELIST_TABNAME = "Whitelist";
    public static final String SENSORS_TABNAME = "Sensors";
    public static final String DEBUG_TABNAME = "Debug";

    public static final String DEFAULT_DEVICE_NAME = "unknown";

    // Tab position index determined here, which is why this can't be a map.
    public static final List<Pair<String, Integer>> TAB_LAYOUTS = List.of(
            new Pair<>(TRACKING_TABNAME, R.layout.tracking_layout),
            new Pair<>(WHITELIST_TABNAME, R.layout.whitelist_layout),
            new Pair<>(SENSORS_TABNAME, R.layout.sensors_layout),
            new Pair<>(DEBUG_TABNAME, R.layout.debug_layout));
    public static final int TAB_COUNT = TAB_LAYOUTS.size();

    public static String createTag(Class<?> clazz) {
        return TAG_PREFIX + clazz.getSimpleName();
    }
}
