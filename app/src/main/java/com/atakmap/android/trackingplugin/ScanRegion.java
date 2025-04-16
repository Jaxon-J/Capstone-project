package com.atakmap.android.trackingplugin;

import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.RootMapGroup;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import gov.tak.api.util.AttributeSet;

public class ScanRegion {
    private static final String DEVICE_LIST_META_KEY = "device_list";
    private static final int ALLOWANCE = 50;
    private static final int POLL_TIME = 15000; // 15 seconds
    private static MapGroup regionGroup;
    private static DrawingCircle circle;
    private static Timer poller;
    private static Map<String, DeviceInfo> foundDevices;

    /// Devices need to be refreshed in a scan region, otherwise it will be removed after a specified timeout period.
    public static void addOrRefreshDevice(DeviceInfo deviceInfo) {
        if (foundDevices == null)
            foundDevices = new HashMap<>();
        DeviceInfo existingInfo = foundDevices.get(deviceInfo.macAddress);
        if (existingInfo == null) {
            foundDevices.put(deviceInfo.macAddress, deviceInfo);
            // FIXME: this is hella deprecated. figure out how to set with setMetaAttributeSet
            circle.setMetaStringArrayList(DEVICE_LIST_META_KEY, new ArrayList<>(foundDevices.keySet()));
        } else {
            existingInfo.lastSeenEpochMillis = Calendar.getInstance().getTimeInMillis();
        }
    }

    public static void init() {
        // Create the circle to put on the map
        MapView mapView = MapView.getMapView();
        circle = new DrawingCircle(mapView, Constants.DEVICE_RADIUS_CIRCLE_NAME);

        // positioning
        circle.setCenterPoint(mapView.getSelfMarker().getGeoPointMetaData());
        mapView.getSelfMarker().addOnPointChangedListener((PointMapItem selfMarker) -> circle.setCenterPoint(selfMarker.getGeoPointMetaData()));

        // style
        circle.setRadius(10);
        circle.setVisible(false); // invisible by default

        // metadata / behavior
        circle.setMetaBoolean("archive", false);
        circle.setMetaBoolean("editable", false);
        circle.setEditable(false);
        circle.setClickable(false);

        // add to map
        RootMapGroup rootGroup = mapView.getRootGroup();
        if ((regionGroup = rootGroup.deepFindMapGroup(Constants.RADIUS_GROUP_NAME)) == null)
            regionGroup = rootGroup.addGroup(Constants.RADIUS_GROUP_NAME);
        regionGroup.addItem(circle);

        // Handle device logic

        // device poll
        poller = new Timer();
        poller.schedule(new TimerTask() {
            @Override
            public void run() {
                long thresholdTime = Calendar.getInstance().getTimeInMillis() - POLL_TIME - ALLOWANCE;
                for (DeviceInfo deviceInfo : foundDevices.values()) {
                    if (deviceInfo.lastSeenEpochMillis < thresholdTime)
                        foundDevices.remove(deviceInfo.macAddress);
                }
                // FIXME: see fixme above, same deal.
                circle.setMetaStringArrayList(DEVICE_LIST_META_KEY, new ArrayList<>(foundDevices.keySet()));
            }
        }, POLL_TIME, POLL_TIME);
    }
    public static void show() {
        if (circle == null)
            init();
        circle.setVisible(true);
    }

    public static void hide() {
        if (circle == null)
            init();
        else // invisible by default, only set it if already initialized.
            circle.setVisible(false);
    }

    public static void destroy() {
        poller.cancel();
        if (regionGroup == null || circle == null)
            return; // uninitialized, no need to remove.
        regionGroup.removeItem(circle);
    }
}
