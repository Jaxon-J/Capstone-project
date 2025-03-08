package com.atakmap.android.trackingplugin;

public final class Constants {
    public static final String TAG_PREFIX = "TrackPlug.";

    public static String createTag(Class clazz) {
        return TAG_PREFIX + clazz.getSimpleName();
    }
}
