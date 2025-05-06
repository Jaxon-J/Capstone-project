package com.atakmap.android.trackingplugin.comms;

import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.trackingplugin.Constants;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.comms.CotServiceRemote;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import gov.tak.api.util.AttributeSet;

public class DeviceCotListener {
    private static final MapGroup deviceGroup = MapView.getMapView().getRootGroup().addGroup("devices");
    private static final Map<String, Set<Pair<String, GeoPoint>>> devicePositions = new HashMap<>();
    private static final String TAG = Constants.createTag(DeviceCotListener.class);

    private static final CotServiceRemote.CotEventListener cotEventListener = (cotEvent, bundle) -> {
        switch (cotEvent.getType()) {
            case CotDetailTypes.DEVICE_FOUND.typeName: {
                onDeviceFound(cotEvent);
                break;
            }
            case CotDetailTypes.DEVICE_REMOVE.typeName: {
                onDeviceRemove(cotEvent);
                break;
            }
        }
    };

    public static void initialize() {
        CommsMapComponent.getInstance().addOnCotEventListener(cotEventListener);
    }

    public static void uninitialize() {
        CommsMapComponent.getInstance().removeOnCotEventListener(cotEventListener);
    }

    private static void onDeviceFound(CotEvent cotEvent) {
        Log.d(TAG, "RECEIVED FOUND EVENT: " + cotEvent);
        // grab attributes from CotDetail
        CotDetail detail = cotEvent.findDetail(CotDetailTypes.DEVICE_FOUND.eltName);

        // TODO: would you like to add this to your whitelist? prompt

        String name = detail.getAttribute(CotDetailTypes.DEVICE_FOUND.attrs.name);
        String macAddress = detail.getAttribute(CotDetailTypes.DEVICE_FOUND.attrs.macAddress);
        String sensorUid = detail.getAttribute(CotDetailTypes.DEVICE_FOUND.attrs.sensorUid);
        String rssi = detail.getAttribute(CotDetailTypes.DEVICE_FOUND.attrs.rssi);

        // put in marker's attribute set
        AttributeSet attrSet = new AttributeSet();
        attrSet.setAttribute(CotDetailTypes.MAPITEM_INFO.attrs.name, name);
        attrSet.setAttribute(CotDetailTypes.MAPITEM_INFO.attrs.macAddress, macAddress);
        attrSet.setAttribute(CotDetailTypes.MAPITEM_INFO.attrs.rssi, rssi);

        // TODO: if whitelist is obeyed, replace random UUID with whitelist UUID?

        Marker marker = getMarkerByMacAddress(macAddress);
        if (marker == null) {
            marker = new Marker(cotEvent.getGeoPoint(), UUID.randomUUID().toString());
            marker.setType("a-u-G");
            marker.setMetaAttributeSet(CotDetailTypes.MAPITEM_INFO.attrSetName, attrSet);
        }
        GeoPoint position = addPosition(macAddress, new Pair<>(sensorUid, cotEvent.getGeoPoint()));
        marker.setPoint(position);
        if (deviceGroup.getItemById(marker.getSerialId()) == null) {
            deviceGroup.addItem(marker);
        }
    }

    private static void onDeviceRemove(CotEvent cotEvent) {
        Log.d(TAG, "RECEIVED REMOVE EVENT: " + cotEvent);
        CotDetail detail = cotEvent.findDetail(CotDetailTypes.DEVICE_REMOVE.eltName);
        String macAddress = detail.getAttribute(CotDetailTypes.DEVICE_REMOVE.attrs.macAddress);
        String sensorUid = detail.getAttribute(CotDetailTypes.DEVICE_REMOVE.attrs.sensorUid);
        Marker item = getMarkerByMacAddress(macAddress);
        if (item == null) return;
        GeoPoint position = removePosition(macAddress, sensorUid);
        if (position == null) {
            deviceGroup.removeItem(item);
        } else {
            item.setPoint(position);
        }
    }

    @Nullable
    private static Marker getMarkerByMacAddress(String macAddress) {
        for (MapItem item : deviceGroup.getItems()) {
            String itemMacAddress = item.getMetaAttributeSet(CotDetailTypes.MAPITEM_INFO.attrSetName)
                    .getStringAttribute(CotDetailTypes.MAPITEM_INFO.attrs.macAddress, "");
            if (macAddress.equals(itemMacAddress))
                return (Marker)item;
        }
        return null;
    }

    private static GeoPoint addPosition(String macAddress, Pair<String, GeoPoint> newPosition) {
        Set<Pair<String, GeoPoint>> existingPositions = devicePositions.get(macAddress);
        if (existingPositions == null) {
            existingPositions = new HashSet<>();
            devicePositions.put(macAddress, existingPositions);
        }
        existingPositions.add(newPosition);
        return getDevicePosition(macAddress);
    }

    @Nullable
    private static GeoPoint removePosition(String macAddress, String sensorUid) {
        Set<Pair<String, GeoPoint>> existingPositions = devicePositions.get(macAddress);
        if (existingPositions == null || existingPositions.isEmpty())
            return null;
        for (Pair<String, GeoPoint> pos : existingPositions) {
            if (sensorUid.equals(pos.first)) {
                existingPositions.remove(pos);
            }
        }
        return getDevicePosition(macAddress);
    }

    @Nullable
    private static GeoPoint getDevicePosition(String macAddress) {
        Set<Pair<String, GeoPoint>> uidPositions = devicePositions.get(macAddress);
        if (uidPositions == null || uidPositions.isEmpty())
            return null;
        double latitude = 0;
        double longitude = 0;
        for (Pair<String, GeoPoint> positions : uidPositions) {
            latitude += positions.second.getLatitude();
            longitude += positions.second.getLongitude();
        }
        latitude /= uidPositions.size();
        longitude /= uidPositions.size();
        return new GeoPoint(latitude, longitude);
    }
}
