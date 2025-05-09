package com.atakmap.android.trackingplugin.comms;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.trackingplugin.Constants;
import com.atakmap.android.trackingplugin.DeviceInfo;
import com.atakmap.android.trackingplugin.DeviceStorageManager;
import com.atakmap.android.trackingplugin.plugin.TrackingPlugin;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.Set;
import java.util.UUID;

public class DeviceCotDispatcher {
    private static final String TAG = Constants.createTag(DeviceCotDispatcher.class);

    public static void sendDeviceFound(DeviceInfo deviceInfo) {
        Marker marker = new Marker(UUID.randomUUID().toString());
        marker.setPoint(MapView.getMapView().getSelfMarker().getPoint());
        CotDetail foundDeviceDetail = new CotDetail(CotDetailTypes.DEVICE_FOUND.eltName);
        foundDeviceDetail.setAttribute(CotDetailTypes.DEVICE_FOUND.attrs.name, deviceInfo.name);
        foundDeviceDetail.setAttribute(CotDetailTypes.DEVICE_FOUND.attrs.macAddress, deviceInfo.macAddress);
        foundDeviceDetail.setAttribute(CotDetailTypes.DEVICE_FOUND.attrs.rssi, Integer.toString(deviceInfo.rssi));
        foundDeviceDetail.setAttribute(CotDetailTypes.DEVICE_FOUND.attrs.sensorUid, deviceInfo.sensorUid);

        CotEvent foundEvent = embedDetail(foundDeviceDetail, CotDetailTypes.DEVICE_FOUND.typeName);
        foundEvent.setPoint(new CotPoint(MapView.getMapView().getSelfMarker().getPoint()));
        Log.d(TAG, "SENDING DEVICE FOUND: " + foundEvent);

        if (MapView.getDeviceUid().equals(deviceInfo.sensorUid)) {
            CotMapComponent.getExternalDispatcher().dispatch(foundEvent);
        }
        CotMapComponent.getInternalDispatcher().dispatch(foundEvent);
    }

    public static void sendDeviceFound(Set<DeviceInfo> deviceInfoSet) {
        for (DeviceInfo deviceInfo : deviceInfoSet)
            sendDeviceFound(deviceInfo);
    }

    public static void sendDeviceRemoval(DeviceInfo deviceInfo) {
        CotDetail removeDetail = new CotDetail();
        removeDetail.setElementName(CotDetailTypes.DEVICE_REMOVE.eltName);
        removeDetail.setAttribute(CotDetailTypes.DEVICE_REMOVE.attrs.macAddress, deviceInfo.macAddress);
        removeDetail.setAttribute(CotDetailTypes.DEVICE_REMOVE.attrs.sensorUid, deviceInfo.sensorUid);
        CotEvent removeEvent = embedDetail(removeDetail, CotDetailTypes.DEVICE_REMOVE.typeName);
        Log.d(TAG, "SENDING DEVICE REMOVED: " + removeEvent);
        CotMapComponent.getExternalDispatcher().dispatch(removeEvent);
        CotMapComponent.getInternalDispatcher().dispatch(removeEvent);
    }

    public static void sendDeviceRemoval(Set<DeviceInfo> deviceInfoSet) {
        for (DeviceInfo deviceInfo : deviceInfoSet)
            sendDeviceRemoval(deviceInfo);
    }

    public static void sendWhitelistRequest(String requestUid) {
        CotDetail whitelistRequestDetail = new CotDetail(CotDetailTypes.WHITELIST_REQUEST.eltName);
        whitelistRequestDetail.setAttribute(CotDetailTypes.WHITELIST_REQUEST.attrs.reqUid, MapView.getDeviceUid());
        CotEvent requestEvent = embedDetail(whitelistRequestDetail, CotDetailTypes.WHITELIST_REQUEST.typeName);
        Bundle uidFilter = new Bundle();
        uidFilter.putStringArray("toUIDs", new String[]{requestUid});
        Log.d(TAG, "SENDING WHITELIST REQUEST TO: " + requestUid);
        CotMapComponent.getExternalDispatcher().dispatch(requestEvent, uidFilter);
    }

    public static void sendWhitelistResponse(String responseUid) {
        CotDetail whitelistResponseDetail = new CotDetail(CotDetailTypes.WHITELIST_RESPONSE.eltName);
        for (DeviceInfo deviceInfo : DeviceStorageManager.getDeviceList(DeviceStorageManager.ListType.WHITELIST)) {
            CotDetail deviceDetail = new CotDetail(CotDetailTypes.WHITELIST_RESPONSE.deviceElt.eltName);
            deviceDetail.setAttribute(CotDetailTypes.WHITELIST_RESPONSE.deviceElt.attrs.name, deviceInfo.name);
            deviceDetail.setAttribute(CotDetailTypes.WHITELIST_RESPONSE.deviceElt.attrs.macAddress, deviceInfo.macAddress);
            whitelistResponseDetail.addChild(deviceDetail);
        }
        CotEvent sendEvent = embedDetail(whitelistResponseDetail, CotDetailTypes.WHITELIST_RESPONSE.typeName);
        Bundle uidFilter = new Bundle();
        uidFilter.putStringArray("toUIDs", new String[]{responseUid});
        Log.d(TAG, "SEND WHITELIST RESPONSE TO: " + responseUid);
        CotMapComponent.getExternalDispatcher().dispatch(sendEvent, uidFilter);
    }

    public static void discoverPluginContacts(@Nullable String[] contactUids) {
        CotDetail reqDetail = new CotDetail(CotDetailTypes.DISCOVERY_REQUEST.eltName);
        reqDetail.setAttribute(CotDetailTypes.DISCOVERY_REQUEST.attrs.reqUid, MapView.getDeviceUid());
        CotEvent reqEvent = embedDetail(reqDetail, CotDetailTypes.DISCOVERY_REQUEST.typeName);
        Log.d(TAG, "SEND DISCOVERY REQUEST: " + reqEvent);
        if (contactUids == null) {
            CotMapComponent.getExternalDispatcher().dispatch(reqEvent);
            return;
        }
        Bundle uidFilter = new Bundle();
        uidFilter.putStringArray("toUIDs", contactUids);
        CotMapComponent.getExternalDispatcher().dispatch(reqEvent, uidFilter);
    }

    public static void sendDiscoveryResponse(String requestUid) {
        Contact reqContact = Contacts.getInstance().getContactByUuid(requestUid);
        if (reqContact != null && TrackingPlugin.sensorsTable != null)
            TrackingPlugin.sensorsTable.addSensor(reqContact.getName(), requestUid);
        CotDetail resDetail = new CotDetail(CotDetailTypes.DISCOVERY_RESPONSE.eltName);
        resDetail.setAttribute(CotDetailTypes.DISCOVERY_RESPONSE.attrs.resUid, MapView.getDeviceUid());
        Log.d(TAG, "SEND DISCOVERY RESPONSE: " + requestUid);
        CotEvent resEvent = embedDetail(resDetail, CotDetailTypes.DISCOVERY_RESPONSE.typeName);
        Bundle uidFilter = new Bundle();
        uidFilter.putStringArray("toUIDs", new String[]{requestUid});
        CotMapComponent.getExternalDispatcher().dispatch(resEvent, uidFilter);
    }

    private static CotEvent embedDetail(CotDetail detail, String type) {
        CotDetail rootDetail = new CotDetail();
        rootDetail.addChild(detail);
        return new CotEvent(
                UUID.randomUUID().toString(),
                type,
                CotEvent.VERSION_2_0,
                CotPoint.ZERO,
                new CoordinatedTime(),
                new CoordinatedTime(),
                new CoordinatedTime(),
                CotEvent.HOW_MACHINE_GENERATED,
                rootDetail,
                null,
                null,
                null
        );
    }
}
