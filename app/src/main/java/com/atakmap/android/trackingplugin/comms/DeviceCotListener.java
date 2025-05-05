package com.atakmap.android.trackingplugin.comms;

import android.util.Log;

import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.trackingplugin.Constants;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.comms.CotServiceRemote;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;

import java.util.UUID;

import gov.tak.api.util.AttributeSet;

public class DeviceCotListener {
    private static final MapGroup deviceGroup = MapView.getMapView().getRootGroup().addGroup("devices");
    private static final String TAG = Constants.createTag(DeviceCotListener.class);
//    static DefaultCommsProvider provider;
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
//        DefaultCommsProvider provider = new DefaultCommsProvider();
//        provider.addCoTMessageListener((cotMessage, rxEndpointId) -> {
//            Log.d(TAG, "CAUGHT MESSAGE: " + cotMessage);
//        });
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
        attrSet.setAttribute(CotDetailTypes.MAPITEM_INFO.attrs.sensorUid, sensorUid);
        attrSet.setAttribute(CotDetailTypes.MAPITEM_INFO.attrs.rssi, rssi);

        // TODO: if whitelist is obeyed, replace random UUID with whitelist UUID
        Marker marker = new Marker(cotEvent.getGeoPoint(), UUID.randomUUID().toString());
        marker.setType("a-u-G");
        marker.setPoint(cotEvent.getGeoPoint());
        marker.setMetaAttributeSet(CotDetailTypes.MAPITEM_INFO.attrSetName, attrSet);
        deviceGroup.addItem(marker);
    }

    private static void onDeviceRemove(CotEvent cotEvent) {
        Log.d(TAG, "RECEIVED REMOVE EVENT: " + cotEvent);
        CotDetail detail = cotEvent.findDetail(CotDetailTypes.DEVICE_REMOVE.eltName);
        String macAddress = detail.getAttribute(CotDetailTypes.DEVICE_REMOVE.attrs.macAddress);
        for (MapItem item : deviceGroup.getItems()) {
            String itemMacAddress = item.getMetaAttributeSet(CotDetailTypes.MAPITEM_INFO.attrSetName)
                    .getStringAttribute(CotDetailTypes.MAPITEM_INFO.attrs.macAddress, "");
            if (macAddress.equals(itemMacAddress)) {
                deviceGroup.removeItem(item);
                break;
            }
        }
    }
}
