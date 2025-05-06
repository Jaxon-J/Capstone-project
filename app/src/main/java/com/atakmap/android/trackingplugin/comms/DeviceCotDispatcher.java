package com.atakmap.android.trackingplugin.comms;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.trackingplugin.Constants;
import com.atakmap.android.trackingplugin.DeviceInfo;
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
        CotDetail rootDetail = new CotDetail();
        CotDetail foundDeviceDetail = new CotDetail(CotDetailTypes.DEVICE_FOUND.eltName);
        foundDeviceDetail.setAttribute(CotDetailTypes.DEVICE_FOUND.attrs.name, deviceInfo.name);
        foundDeviceDetail.setAttribute(CotDetailTypes.DEVICE_FOUND.attrs.macAddress, deviceInfo.macAddress);
        foundDeviceDetail.setAttribute(CotDetailTypes.DEVICE_FOUND.attrs.rssi, Integer.toString(deviceInfo.rssi));
        foundDeviceDetail.setAttribute(CotDetailTypes.DEVICE_FOUND.attrs.sensorUid, deviceInfo.sensorUid);
        rootDetail.addChild(foundDeviceDetail);

        CotEvent cotEvent = new CotEvent(
                UUID.randomUUID().toString(),
                CotDetailTypes.DEVICE_FOUND.typeName,
                CotEvent.VERSION_2_0,
                new CotPoint(MapView.getMapView().getSelfMarker().getPoint()),
                new CoordinatedTime(),
                new CoordinatedTime(),
                new CoordinatedTime(),
                CotEvent.HOW_MACHINE_GENERATED,
                rootDetail,
                null, null, null);

        if (MapView.getDeviceUid().equals(deviceInfo.sensorUid)) {
            CotMapComponent.getExternalDispatcher().dispatch(cotEvent);
        }
        CotMapComponent.getInternalDispatcher().dispatch(cotEvent);
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
        CotDetail rootDetail = new CotDetail();
        rootDetail.addChild(removeDetail);
        CotEvent cotEvent = new CotEvent(
                UUID.randomUUID().toString(),
                CotDetailTypes.DEVICE_REMOVE.typeName,
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
        CotMapComponent.getExternalDispatcher().dispatch(cotEvent);
        CotMapComponent.getInternalDispatcher().dispatch(cotEvent);
    }

    public static void sendDeviceRemoval(Set<DeviceInfo> deviceInfoSet) {
        for (DeviceInfo deviceInfo : deviceInfoSet)
            sendDeviceRemoval(deviceInfo);
    }
}
/*
DEVICE FOUND COTEVENT:
- device_found element: user_given_name, mac_address, rssi
- (utilize time/stale for timeout logic)
<event version='2.0' uid='4276a749-d7cb-fefa-b8a4-e3f2825ba57f' type='a-u-G'
    time='2025-04-27T12:48:31.173Z' start='2025-04-27T12:48:31.173Z'
    stale='2025-04-27T12:48:36.149Z' how='m-p' access='Undefined'>
    <point lat='41.257342' lon='-96.102356' hae='297.04' ce='25.9' le='9999999.0' />
    <device_found rssi='-62' mac_address='F5:6C:69:FB:C0:46' user_given_name='vr lighthouse' />
</event>
 */



/*



 */









