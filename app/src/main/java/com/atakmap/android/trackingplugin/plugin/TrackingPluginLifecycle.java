package com.atakmap.android.trackingplugin.plugin;

import com.atak.plugins.impl.AbstractPlugin;
import com.atak.plugins.impl.PluginContextProvider;
import com.atakmap.android.trackingplugin.TrackingPluginMapComponent;

import gov.tak.api.plugin.IServiceController;

public class TrackingPluginLifecycle extends AbstractPlugin {
    public TrackingPluginLifecycle(IServiceController serviceController) {
        super(serviceController,
                new TrackingPluginTool(serviceController.getService(PluginContextProvider.class)
                .getPluginContext()), new TrackingPluginMapComponent());
    }
}
