package com.atakmap.android.capstoneplugin.plugin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.atak.plugins.impl.PluginContextProvider;
import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.HashSet;
import java.util.Set;

import gov.tak.api.plugin.IPlugin;
import gov.tak.api.plugin.IServiceController;
import gov.tak.api.ui.IHostUIService;
import gov.tak.api.ui.Pane;
import gov.tak.api.ui.PaneBuilder;
import gov.tak.api.ui.ToolbarItem;
import gov.tak.api.ui.ToolbarItemAdapter;
import gov.tak.platform.marshal.MarshalManager;

public class TrackingPlugin implements IPlugin {

    public static final String TAG = Constants.TAG_PREFIX + "Main";
    IServiceController serviceController;

    Context pluginContext;
    IHostUIService uiService;
    ToolbarItem toolbarItem;
    Pane pluginViewPane;
    private static int DEBUG_PIN_COUNT;
    private boolean debug_scanning = false;

    public TrackingPlugin(IServiceController serviceController) {
        this.serviceController = serviceController;
        final PluginContextProvider ctxProvider = serviceController.getService(PluginContextProvider.class);
        if (ctxProvider != null) {
            pluginContext = ctxProvider.getPluginContext();
            pluginContext.setTheme(R.style.ATAKPluginTheme);
        }

        // obtain the UI service
        uiService = serviceController.getService(IHostUIService.class);

        // initialize the toolbar button for the plugin

        // create the button
        toolbarItem = new ToolbarItem.Builder(pluginContext.getString(R.string.app_name), MarshalManager.marshal(pluginContext.getResources().getDrawable(R.drawable.ic_launcher, null), android.graphics.drawable.Drawable.class, gov.tak.api.commons.graphics.Bitmap.class)).setListener(new ToolbarItemAdapter() {
            @Override
            public void onClick(ToolbarItem item) {
                showPane();
            }
        }).build();
    }

    @Override
    public void onStart() {
        // the plugin is starting, add the button to the toolbar
        if (uiService == null) return;

        uiService.addToolbarItem(toolbarItem);
    }

    @Override
    public void onStop() {
        // the plugin is stopping, remove the button from the toolbar
        if (uiService == null) return;

        uiService.removeToolbarItem(toolbarItem);
    }

    private void showPane() {
        // instantiate the plugin view if necessary
        if (pluginViewPane == null) {
            // Remember to use the PluginLayoutInflator if you are actually inflating a custom view
            // In this case, using it is not necessary - but I am putting it here to remind
            // developers to look at this Inflator

            pluginViewPane = new PaneBuilder(PluginLayoutInflater.inflate(pluginContext, R.layout.main_layout, null))
                    // relative location is set to default; pane will switch location dependent on
                    // current orientation of device screen
                    .setMetaValue(Pane.RELATIVE_LOCATION, Pane.Location.Default)
                    // pane will take up 50% of screen width in landscape mode
                    .setMetaValue(Pane.PREFERRED_WIDTH_RATIO, 0.5D)
                    // pane will take up 50% of screen height in portrait mode
                    .setMetaValue(Pane.PREFERRED_HEIGHT_RATIO, 0.5D).build();
        }

        // if the plugin pane is not visible, show it!
        if (!uiService.isPaneVisible(pluginViewPane)) {
            Log.d(TAG, "Plugin pane opened");
            uiService.showPane(pluginViewPane, null);
        }

        View pluginView = MarshalManager.marshal(pluginViewPane, Pane.class, View.class);
        final Button getPermsBtn = pluginView.findViewById(R.id.grantPermissionsDebugButton);
        final Button trackDebugBtn = pluginView.findViewById(R.id.trackingStartDebugButton);
        getPermsBtn.setOnClickListener(this::onGetPermsButtonClick);
        trackDebugBtn.setOnClickListener(this::onTrackDebugButtonClick);
    }

    private void onTrackDebugButtonClick(View v) {
        if (!getPerms()) return;
        Log.d(TAG, "Debug tracking button pressed");
        Button trackBtn = (Button) v;
        Intent btServiceIntent = new Intent(pluginContext, BluetoothTrackerService.class);
        if (debug_scanning) {
            trackBtn.setText("Start Tracking");
            debug_scanning = false;
            pluginContext.stopService(btServiceIntent);
            return;
        }
        trackBtn.setText("Stop Tracking");
        debug_scanning = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pluginContext.startForegroundService(btServiceIntent);
        } else {
            pluginContext.startService(btServiceIntent);
        }
        Intent startScanning = new Intent(BluetoothTrackerService.ACTION_START_SCANNING);
        pluginContext.sendBroadcast(startScanning);
    }

    private boolean getPerms() {
        // probably refactoring this later into PermissionsHandler or something
        boolean hasPerms = true;
        Set<String> missingPerms = new HashSet<>();
        Set<String> neededPerms;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            neededPerms = Set.of(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.WAKE_LOCK);
        } else {
            neededPerms = Set.of(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WAKE_LOCK);
        }
        for (String perm : neededPerms) {
            if (pluginContext.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED)
                continue;
            missingPerms.add(perm);
            hasPerms = false;
        }
        if (!hasPerms) {

            MarshalManager.marshal(pluginViewPane, Pane.class, View.class);
            Log.e(TAG, "Cannot start service. Missing permissions: " + String.join(",", missingPerms));
        }
        return hasPerms;
    }


    private void onGetPermsButtonClick(View v) {
        // request permissions here? might need a PermissionsHandler or something
    }

//    private static void placePinButtonClick(View v) {
//        MapView mapView = MapView.getMapView();
//        GeoPoint myLocation = mapView.getSelfMarker().getPoint();
//        double diameter = 0.5;
//        double angle = Math.random() * Math.PI * 2;
//        GeoPoint newLocation = new GeoPoint(myLocation.getLatitude() + diameter * Math.cos(angle), myLocation.getLongitude() + diameter * Math.sin(angle));
//        PlacePointTool.MarkerCreator creator = new PlacePointTool.MarkerCreator(newLocation)
//                .setColor((int) Math.floor(Math.random() * 0xFFFFFF))
//                .setCallsign("DEBUG" + DEBUG_PIN_COUNT)
//                .setUid("tracking:debug-point-" + DEBUG_PIN_COUNT++)
//                .setType("a-u-G")
//                .setHow("bluetooth pickup from " + mapView.getSelfMarker().getUID());
//        Marker marker = creator.placePoint();
//        marker.getMetaString("", "");
//    }
}
