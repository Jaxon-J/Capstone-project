package com.atakmap.android.trackingplugin;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.trackingplugin.plugin.R;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.coremap.maps.coords.GeoPoint;

public class TrackingPluginDropDownReceiver extends DropDownReceiver {
    private final Context pluginContext;
    private final View mainView;

    protected TrackingPluginDropDownReceiver(final MapView mapView, final Context context) {
        super(mapView);
        this.pluginContext = context;
        // set up UI, set main layout to be viewed when SHOW_PLUGIN action is triggered
        mainView = PluginLayoutInflater.inflate(pluginContext, R.layout.main_layout, null);
        // set up all receivers/UI responses here

        mainView.findViewById(R.id.debugPlacePinButton).setOnClickListener((View v) -> {
            GeoPoint selfPoint = MapView.getMapView().getSelfMarker().getPoint();
            GeoPoint trackedPoint = new GeoPoint(selfPoint.getLatitude(), // 0.0000035 = 10ft
                    selfPoint.getLongitude(), selfPoint.getAltitude(),
                    selfPoint.getAltitudeReference(), 11, 11);
            PlacePointTool.MarkerCreator mc = new PlacePointTool.MarkerCreator(trackedPoint);
            mc.setType("a-u-G");
            mc.setCallsign("tracked");
            Marker trackedMarker = mc.placePoint();
        });
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case ACTIONS.SHOW_PLUGIN: // UI ENTRY POINT
                if (!isClosed()) unhideDropDown();
                // showDropDown has several more overloads. if we want to listen to
                // resize/open/close events,
                // we implement DropDown.StateChangeListener and add "false, this" parameters here.
                showDropDown(mainView, HALF_WIDTH, FULL_WIDTH, FULL_WIDTH, HALF_HEIGHT);
                break;
        }
    }

    @Override
    protected void disposeImpl() {
    }

    public static final class ACTIONS { // purely here for namespacing
        public static final String SHOW_PLUGIN = "com.atakmap.android.trackingplugin.SHOW_PLUGIN";
    }
}
