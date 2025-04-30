package com.atakmap.android.trackingplugin.comms;

import android.util.Log;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.trackingplugin.Constants;
import com.atakmap.android.trackingplugin.DeviceInfo;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

@Deprecated
public class DeviceCotHandlerOld {
    private static final String TAG = Constants.createTag(DeviceCotHandlerOld.class);
    private static final String DEVICE_RADIUS_GROUP_NAME = "scan-radius-" + (new Random()).nextInt();
    private static final String ATTRIBUTE_SET_KEY = "found-device-attributes";
    private static final String LAST_SEEN_ATTRIBUTE_KEY = "found-device-last-seen";
    private static final int ALLOWANCE_MILLIS = 50; // need this to allow a tad of time overlap between removal polls.
    public static int pollRateMillis = 5000;
    private static boolean initialized = false;
    private static MapGroup regionGroup; // the logical local "folder" our DrawingCircle's will go in
    private static Timer poller;
    private static final Map<String, DeviceInfo> foundDevices = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, Boolean> visibilityMap = new HashMap<>();
    private static boolean isPolling = false;

    private DeviceCotHandlerOld() {
        // prevent instantiation. this is a static class.
    }

    /// Devices need to be refreshed in a scan region, otherwise it will be removed after a specified timeout period.
    public static void addOrRefreshDevice(DeviceInfo deviceInfo) {
        if (logOnUninitialized()) return;

        /*
        Data necessary with CoT event:
        - current trackers that have the device scanned and their locations (for triangulation)
        - identifiers i.e. mac address + uuid
         */

        DeviceInfo foundDeviceInfo = foundDevices.get(deviceInfo.uuid);
        if (foundDeviceInfo == null) {
            // device hasn't been added to the map yet, do that here.
            CotDetail trackedDetail = new CotDetail();
            trackedDetail.setElementName("tracked");
            trackedDetail.setAttribute("mac_address", deviceInfo.macAddress);
            trackedDetail.setAttribute("user_given_name", deviceInfo.name);
            trackedDetail.setAttribute("rssi", Integer.toString(deviceInfo.rssi));
            CotEvent deviceFoundEvent = new CotEvent(deviceInfo.uuid,
                    "a-u-G",
                    "2.0",
                    new CotPoint(MapView.getMapView().getSelfMarker().getPoint()),
                    new CoordinatedTime(),
                    new CoordinatedTime(),
                    new CoordinatedTime(System.currentTimeMillis() + pollRateMillis),
                    "m-p",
                    trackedDetail,
                    null,
                    null,
                    null);
            Log.d("ASDFREW", deviceFoundEvent.toString());
            assert false;
            CotMapComponent.getInternalDispatcher().dispatch(deviceFoundEvent);

            synchronized (foundDevices) {
                foundDevices.put(deviceInfo.uuid, deviceInfo);
            }
        }
    }

    public static void initialize() {
        if (initialized) return;
        // set up regionGroup
        initialized = true;
    }

    public static void destroy() {
        if (!initialized) return;

        stopPolling();
        MapView.getMapView().getRootGroup().removeGroup(regionGroup);
        regionGroup = null;
        initialized = false;
    }

    public static void stopPolling() {
        isPolling = false;
    }

    public static void startPolling() {
        if (isPolling) return;
        poller = new Timer();
        poller.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!isPolling) {
                    foundDevices.clear();
                    poller.cancel();
                    poller = null;
                    return;
                }

                // TODO: if we're going to associate a track history with the circles, this is where we would update it.
                // grab last time we saw the device, if we didn't see it in this TIMEOUT_TIME window, remove it (timed out)
                long thresholdTime = Calendar.getInstance().getTimeInMillis() - pollRateMillis - ALLOWANCE_MILLIS;
                List<String> removeUuids = new ArrayList<>();
                synchronized (foundDevices) {
//                    for (Map.Entry<String, DeviceInfo> foundEntry : foundDevices.entrySet()) {
//                        long lastSeen = foundEntry.getValue()
//                                .getMetaAttributeSet(ATTRIBUTE_SET_KEY)
//                                .getLongAttribute(LAST_SEEN_ATTRIBUTE_KEY);
//                        if (lastSeen < thresholdTime) {
//                            removeUuids.add(foundEntry.getKey());
//                        }
//                    }
                }
                for (String uuid : removeUuids) {
                    foundDevices.remove(uuid);
                }
            }
        }, 0, pollRateMillis);
        isPolling = true;
    }

    /// Sets visibility of the circle associated with the UUID. If it doesn't exist on the map, it will set
    /// the default visibility once it appears.
    public static void setVisibility(String uuid, boolean visible) {
        if (logOnUninitialized()) return;

//        DrawingCircle circle = foundDevices.get(uuid);
//        if (circle != null)
//            circle.setVisible(visible);
//        visibilityMap.put(uuid, visible);
    }

    private static DrawingCircle createCircle(DeviceInfo deviceInfo) {
        MapView mapView = MapView.getMapView();
        DrawingCircle circle = new DrawingCircle(mapView, deviceInfo.uuid);

        // positioning
        circle.setCenterPoint(mapView.getSelfMarker().getGeoPointMetaData());
        // TODO: handle when selfMarker isn't on the map. probably NullRefException right now
        // FIXME: this is a band-aid until history tracking gets implemented. should *not* follow user.
        mapView.getSelfMarker().addOnPointChangedListener((PointMapItem selfMarker) -> circle.setCenterPoint(selfMarker.getGeoPointMetaData()));

        // style
        // change to points, with colors inhereted from the tracker device, OR base on mac address
        // maybe: click on item brings up relevant deviceInfoPane
        circle.setRadius(10);

        // check if show() has been called on this device prior to being instantiated, set visibility accordingly.
        circle.setVisible(Boolean.TRUE.equals(visibilityMap.get(deviceInfo.uuid)));

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
            Log.e(TAG, "Must call " + DeviceCotHandlerOld.class.getSimpleName() + ".initialize() before any method call.");
        }
        return unInit;
    }
}
