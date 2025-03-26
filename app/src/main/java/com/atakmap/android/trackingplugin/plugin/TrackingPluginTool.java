package com.atakmap.android.trackingplugin.plugin;

import android.content.Context;

import com.atak.plugins.impl.AbstractPluginTool;
import com.atakmap.android.trackingplugin.TrackingPluginDropDownReceiver;

import gov.tak.api.util.Disposable;

public class TrackingPluginTool extends AbstractPluginTool implements Disposable {
    public TrackingPluginTool(Context context) {
        super(context,
                context.getString(R.string.app_name),
                context.getString(R.string.app_desc),
                context.getDrawable(R.drawable.ic_launcher),
                TrackingPluginDropDownReceiver.ACTIONS.SHOW_PLUGIN);
    }

    @Override
    public void dispose() {
        // idk what sort of clean-up code goes here tbh.
    }
}
