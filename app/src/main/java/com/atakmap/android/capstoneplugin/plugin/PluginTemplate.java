
package com.atakmap.android.capstoneplugin.plugin;

import android.content.Context;
import android.view.View;
import android.widget.Button;

import com.atak.plugins.impl.PluginContextProvider;
import com.atak.plugins.impl.PluginLayoutInflater;

import gov.tak.api.plugin.IPlugin;
import gov.tak.api.plugin.IServiceController;
import gov.tak.api.ui.IHostUIService;
import gov.tak.api.ui.Pane;
import gov.tak.api.ui.PaneBuilder;
import gov.tak.api.ui.ToolbarItem;
import gov.tak.api.ui.ToolbarItemAdapter;
import gov.tak.platform.marshal.MarshalManager;

public class PluginTemplate implements IPlugin {

    IServiceController serviceController;
    Context pluginContext;
    IHostUIService uiService;
    ToolbarItem toolbarItem;
    Pane templatePane;
    Pane secondaryPane;
    Pane tertiaryPane;

    public PluginTemplate(IServiceController serviceController) {
        this.serviceController = serviceController;
        final PluginContextProvider ctxProvider = serviceController
                .getService(PluginContextProvider.class);
        if (ctxProvider != null) {
            pluginContext = ctxProvider.getPluginContext();
            pluginContext.setTheme(R.style.ATAKPluginTheme);
        }

        // obtain the UI service
        uiService = serviceController.getService(IHostUIService.class);

        // initialize the toolbar button for the plugin

        // create the button
        toolbarItem = new ToolbarItem.Builder(
                pluginContext.getString(R.string.app_name),
                MarshalManager.marshal(
                        pluginContext.getResources().getDrawable(R.drawable.ic_launcher),
                        android.graphics.drawable.Drawable.class,
                        gov.tak.api.commons.graphics.Bitmap.class))
                .setListener(new ToolbarItemAdapter() {
                    @Override
                    public void onClick(ToolbarItem item) {
                        showPane();
                    }
                })
                .build();
    }

    @Override
    public void onStart() {
        // the plugin is starting, add the button to the toolbar
        if (uiService == null)
            return;

        uiService.addToolbarItem(toolbarItem);
    }

    @Override
    public void onStop() {
        // the plugin is stopping, remove the button from the toolbar
        if (uiService == null)
            return;

        uiService.removeToolbarItem(toolbarItem);
    }

    private void showPane() {
        // instantiate the plugin view if necessary
        if(templatePane == null) {
            // Remember to use the PluginLayoutInflator if you are actually inflating a custom view
            // In this case, using it is not necessary - but I am putting it here to remind
            // developers to look at this Inflator

            View rootView = PluginLayoutInflater.inflate(pluginContext, R.layout.main_layout, null);

            Button devicesButton = rootView.findViewById(R.id.devicesButton1);
            Button sensorsButton = rootView.findViewById(R.id.sensorsButton1);

            devicesButton.setOnClickListener(v -> {
                uiService.closePane(templatePane);
                uiService.showPane(secondaryPane,null);

            });

            sensorsButton.setOnClickListener(v -> {
                uiService.closePane(templatePane);
                uiService.showPane(tertiaryPane,null);

            });

            templatePane = new PaneBuilder(rootView)
                    // relative location is set to default; pane will switch location dependent on
                    // current orientation of device screen
                    .setMetaValue(Pane.RELATIVE_LOCATION, Pane.Location.Default)
                    // pane will take up 50% of screen width in landscape mode
                    .setMetaValue(Pane.PREFERRED_WIDTH_RATIO, 0.5D)
                    // pane will take up 50% of screen height in portrait mode
                    .setMetaValue(Pane.PREFERRED_HEIGHT_RATIO, 0.5D)
                    .build();


        }

        if (secondaryPane == null) {

            View rootView = PluginLayoutInflater.inflate(pluginContext, R.layout.secondary_layout, null);

            Button trackingButton = rootView.findViewById(R.id.trackingButton2);
            Button sensorsButton = rootView.findViewById(R.id.sensorsButton2);

            trackingButton.setOnClickListener(v -> {
                uiService.closePane(secondaryPane);
                uiService.showPane(templatePane,null);

            });

            sensorsButton.setOnClickListener(v -> {
                uiService.closePane(secondaryPane);
                uiService.showPane(tertiaryPane,null);

            });

            secondaryPane = new PaneBuilder(rootView)
                    // relative location is set to default; pane will switch location dependent on
                    // current orientation of device screen
                    .setMetaValue(Pane.RELATIVE_LOCATION, Pane.Location.Default)
                    // pane will take up 50% of screen width in landscape mode
                    .setMetaValue(Pane.PREFERRED_WIDTH_RATIO, 0.5D)
                    // pane will take up 50% of screen height in portrait mode
                    .setMetaValue(Pane.PREFERRED_HEIGHT_RATIO, 0.5D)
                    .build();

        }

        if (tertiaryPane == null) {

            View rootView = PluginLayoutInflater.inflate(pluginContext, R.layout.tertiary_layout, null);

            Button trackingButton = rootView.findViewById(R.id.trackingButton3);
            Button devicesButton = rootView.findViewById(R.id.devicesButton3);

            trackingButton.setOnClickListener(v -> {
                uiService.closePane(tertiaryPane);
                uiService.showPane(templatePane,null);

            });

            devicesButton.setOnClickListener(v -> {
                uiService.closePane(tertiaryPane);
                uiService.showPane(secondaryPane,null);

            });

            tertiaryPane = new PaneBuilder(rootView)
                    // relative location is set to default; pane will switch location dependent on
                    // current orientation of device screen
                    .setMetaValue(Pane.RELATIVE_LOCATION, Pane.Location.Default)
                    // pane will take up 50% of screen width in landscape mode
                    .setMetaValue(Pane.PREFERRED_WIDTH_RATIO, 0.5D)
                    // pane will take up 50% of screen height in portrait mode
                    .setMetaValue(Pane.PREFERRED_HEIGHT_RATIO, 0.5D)
                    .build();

        }
        ;


        // if the plugin pane is not visible, show it!
        if(!uiService.isPaneVisible(templatePane)) {
            uiService.showPane(templatePane, null);
        }
    }
}
