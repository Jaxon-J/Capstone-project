package com.atakmap.android.trackingplugin;

import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.maps.RootMapGroup;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import gov.tak.api.util.AttributeSet;

public class DeviceMapDisplay {
    public static final String DEVICE_RADIUS_GROUP_NAME = "scan-radius-" + (new Random()).nextInt();
    private static final String ATTRIBUTE_SET_KEY = "found-device-attributes";
    private static final String LAST_SEEN_ATTRIBUTE_KEY = "found-device-last-seen";
    private static final int ALLOWANCE = 50; // need this to allow a tad of time overlap between removal polls.
    private static final int TIMEOUT_TIME = 15000; // 15 seconds
    private static MapGroup regionGroup; // the logical local "folder" our DrawingCircles will go in
    private static Timer poller;
    private static Map<String, DrawingCircle> foundDevices;

    /// Devices need to be refreshed in a scan region, otherwise it will be removed after a specified timeout period.
    public static void addOrRefreshDevice(DeviceInfo deviceInfo) {
        // grab time at the moment this function is called.
        long currentTime = Calendar.getInstance().getTimeInMillis();

        if (foundDevices == null)
            foundDevices = new HashMap<>();

        DrawingCircle devCircle = foundDevices.get(deviceInfo.macAddress);
        if (devCircle == null) {
            // device hasn't been added to the map yet, do that here.
            devCircle = createCircle(deviceInfo);

            if (regionGroup == null) {
                RootMapGroup rootGroup = MapView.getMapView().getRootGroup();
                // if the map group already exists for some reason, take it.
                regionGroup = rootGroup.deepFindMapGroup(DEVICE_RADIUS_GROUP_NAME);
                if (regionGroup == null)
                    regionGroup = rootGroup.addGroup(DEVICE_RADIUS_GROUP_NAME);
            }
            regionGroup.addItem(devCircle);
            foundDevices.put(deviceInfo.macAddress, devCircle);
        }

        // overwrite "last seen" metadata with current moment.
        AttributeSet attrSet = new AttributeSet();
        attrSet.setAttribute(LAST_SEEN_ATTRIBUTE_KEY, currentTime);
        devCircle.setMetaAttributeSet(ATTRIBUTE_SET_KEY, attrSet);

        // TODO: somehow grab value from visibility toggle on the whitelist and set it here.
        //  this is really clunky, though.
        if (false /* put toggle check here */) {
            devCircle.setVisible(true);
        }
    }

    public static void start() {
        poller = new Timer();
        poller.schedule(new TimerTask() {
            @Override
            public void run() {
                // grab last time we saw the device, if we didn't see it in this POLL_TIME window, remove it (timed out)
                long thresholdTime = Calendar.getInstance().getTimeInMillis() - TIMEOUT_TIME - ALLOWANCE;
                for (Map.Entry<String, DrawingCircle> foundEntry : foundDevices.entrySet()) {
                    long lastSeen = foundEntry.getValue().getMetaAttributeSet(ATTRIBUTE_SET_KEY).getLongAttribute(LAST_SEEN_ATTRIBUTE_KEY);
                    if (lastSeen < thresholdTime) {
                        regionGroup.removeItem(foundEntry.getValue());
                        foundDevices.remove(foundEntry.getKey());
                    }
                }
            }
        }, TIMEOUT_TIME, TIMEOUT_TIME);
    }

    public static void stop() {
        if (poller != null) {
            poller.cancel();
        }
        if (regionGroup != null && foundDevices != null) {
            for (DrawingCircle devCircle : foundDevices.values()) {
                regionGroup.removeItem(devCircle);
            }
        }
        poller = null;
        regionGroup = null;
        foundDevices = null;
    }

    /// Shows device circle on the map if it has been detected. Otherwise does nothing.
    public static void show(String macAddress) {
        DrawingCircle circle = foundDevices.get(macAddress);
        if (circle != null)
            circle.setVisible(true);
    }

    public static void hide(String macAddress) {
        DrawingCircle circle = foundDevices.get(macAddress);
        if (circle != null)
            circle.setVisible(false);
    }

    private static DrawingCircle createCircle(DeviceInfo deviceInfo) {
        MapView mapView = MapView.getMapView();
        DrawingCircle circle = new DrawingCircle(mapView, deviceInfo.macAddress);

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
        // TODO: see if we can get default colors going like https://medialab.github.io/iwanthue
        //  this should be a preference that users can set otherwise.

        return circle;
    }
}
