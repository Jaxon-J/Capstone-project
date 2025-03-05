package com.atakmap.android.trackingplugin;

public final class Constants {
    public static final String TAG_PREFIX = "TrackingPlugin.";

    public static String createTag(Object clazz) {
        return TAG_PREFIX + clazz.getClass().getSimpleName();
    }
}
