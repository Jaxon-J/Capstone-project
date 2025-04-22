package com.atakmap.android.trackingplugin;

import android.util.Log;

import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import gov.tak.api.util.AttributeSet;

public class DeviceMapDisplay {
    private static final String TAG = Constants.createTag(DeviceMapDisplay.class);
    private static final String DEVICE_RADIUS_GROUP_NAME = "scan-radius-" + (new Random()).nextInt();
    private static final String ATTRIBUTE_SET_KEY = "found-device-attributes";
    private static final String LAST_SEEN_ATTRIBUTE_KEY = "found-device-last-seen";
    private static final int ALLOWANCE_MILLIS = 50; // need this to allow a tad of time overlap between removal polls.
    private static final int TIMEOUT_TIME_MILLIS = 15000;
    private static boolean initialized = false;
    private static MapGroup regionGroup; // the logical local "folder" our DrawingCircle's will go in
    private static Timer poller;
    private static Map<String, DrawingCircle> foundDevices;
    private static Map<String, Boolean> visibilityMap;

    private DeviceMapDisplay() {
        // prevent instantiation. this is a static class.
    }

    /// Devices need to be refreshed in a scan region, otherwise it will be removed after a specified timeout period.
    public static void addOrRefreshDevice(DeviceInfo deviceInfo) {
        if (logOnUninitialized()) return;

        // grab time at the moment this function is called.
        long currentTime = Calendar.getInstance().getTimeInMillis();

        DrawingCircle devCircle = foundDevices.get(deviceInfo.macAddress);
        if (devCircle == null) {
            // device hasn't been added to the map yet, do that here.
            devCircle = createCircle(deviceInfo);

            // check if show() has been called on this device prior to being instantiated, set visibility accordingly.
            if (Boolean.TRUE.equals(visibilityMap.get(deviceInfo.macAddress)))
                devCircle.setVisible(true);

            regionGroup.addItem(devCircle);
            foundDevices.put(deviceInfo.macAddress, devCircle);
        }

        // overwrite "last seen" metadata with current moment.
        AttributeSet attrSet = new AttributeSet();
        attrSet.setAttribute(LAST_SEEN_ATTRIBUTE_KEY, currentTime);
        devCircle.setMetaAttributeSet(ATTRIBUTE_SET_KEY, attrSet);
    }

    public static void initialize() {
        if (initialized) return;

        // set up regionGroup
        regionGroup = MapView.getMapView().getRootGroup().addGroup(DEVICE_RADIUS_GROUP_NAME);

        // set up maps
        foundDevices = new HashMap<>();
        visibilityMap = new HashMap<>();

        // set up timer that removes devices after some timeout
        poller = new Timer();
        poller.schedule(new TimerTask() {
            @Override
            public void run() {
                // grab last time we saw the device, if we didn't see it in this TIMEOUT_TIME window, remove it (timed out)
                long thresholdTime = Calendar.getInstance().getTimeInMillis() - TIMEOUT_TIME_MILLIS - ALLOWANCE_MILLIS;
                for (Map.Entry<String, DrawingCircle> foundEntry : foundDevices.entrySet()) {
                    long lastSeen = foundEntry.getValue().getMetaAttributeSet(ATTRIBUTE_SET_KEY).getLongAttribute(LAST_SEEN_ATTRIBUTE_KEY);
                    if (lastSeen < thresholdTime) {
                        regionGroup.removeItem(foundEntry.getValue());
                        foundDevices.remove(foundEntry.getKey());
                    }
                }
            }
        }, TIMEOUT_TIME_MILLIS, TIMEOUT_TIME_MILLIS);

        initialized = true;
    }

    public static void destroy() {
        if (!initialized) return;

        poller.cancel();
        MapView.getMapView().getRootGroup().removeGroup(regionGroup);
        poller = null;
        regionGroup = null;
        foundDevices = null;
        visibilityMap = null;
        initialized = false;
    }

    /// Sets visibility of the circle associated with the MAC address. If it doesn't exist on the map, it will set
    /// the default visibility once it appears.
    public static void setVisibility(String macAddress, boolean visible) {
        if (logOnUninitialized()) return;

        DrawingCircle circle = foundDevices.get(macAddress);
        if (circle != null)
            circle.setVisible(visible);
        if (visibilityMap == null)
            visibilityMap = new HashMap<>();
        visibilityMap.put(macAddress, visible);

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

    private static boolean logOnUninitialized() {
        boolean unInit = !initialized;
        if (unInit) {
            Log.e(TAG, "Must call " + DeviceMapDisplay.class.getSimpleName() + ".initialize() before any method call.");
        }
        return unInit;
    }
}
