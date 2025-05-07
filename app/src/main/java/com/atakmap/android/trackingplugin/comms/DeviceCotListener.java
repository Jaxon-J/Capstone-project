package com.atakmap.android.trackingplugin.comms;

import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.atakmap.android.contact.Contacts;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.trackingplugin.Constants;
import com.atakmap.android.trackingplugin.DeviceInfo;
import com.atakmap.android.trackingplugin.DeviceStorageManager;
import com.atakmap.android.trackingplugin.plugin.TrackingPlugin;
import com.atakmap.android.trackingplugin.ui.WhitelistTable;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.comms.CotServiceRemote;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import gov.tak.api.util.AttributeSet;

public class DeviceCotListener implements DeviceStorageManager.DeviceListChangeListener {
    public static final MapGroup deviceGroup = MapView.getMapView().getRootGroup().addGroup("devices");
    private static final Map<String, Set<Pair<String, GeoPoint>>> devicePositions = new HashMap<>();
    /// Key is the whitelist UID, value is the associated Marker's UID.
    private static final Map<String, String> whitelistMarkerUidMap = new HashMap<>();
    private static final String TAG = Constants.createTag(DeviceCotListener.class);
    private static CotServiceRemote.CotEventListener cotEventListener;
    private static final Map<String, Consumer<CotEvent>> eventFnMap = Map.of(
            CotDetailTypes.DEVICE_FOUND.typeName, DeviceCotListener::onDeviceFound,
            CotDetailTypes.DEVICE_REMOVE.typeName, DeviceCotListener::onDeviceRemove,
            CotDetailTypes.DISCOVERY_REQUEST.typeName, DeviceCotListener::onDiscoverRequest,
            CotDetailTypes.DISCOVERY_RESPONSE.typeName, DeviceCotListener::onDiscoverResponse,
            CotDetailTypes.WHITELIST_REQUEST.typeName, DeviceCotListener::onWhitelistRequest,
            CotDetailTypes.WHITELIST_RESPONSE.typeName, DeviceCotListener::onWhitelistResponse
    );

    public static void initialize() {
        cotEventListener = (cotEvent, bundle) -> {
            Consumer<CotEvent> cotFunc = eventFnMap.get(cotEvent.getType());
            if (cotFunc == null) return;
            Log.d(TAG, "RECEIVED " + cotEvent.getType() + " EVENT: " + cotEvent);
            cotFunc.accept(cotEvent);
        };
        CommsMapComponent.getInstance().addOnCotEventListener(cotEventListener);
    }

    public static void uninitialize() {
        CommsMapComponent.getInstance().removeOnCotEventListener(cotEventListener);
        cotEventListener = null;
    }

    private static void onDiscoverRequest(CotEvent cotEvent) {
        String requestUid = cotEvent.getDetail().getChild(CotDetailTypes.DISCOVERY_REQUEST.eltName)
                .getAttribute(CotDetailTypes.DISCOVERY_REQUEST.attrs.reqUid);
        if (TrackingPlugin.sensorsTable != null) {
            TrackingPlugin.sensorsTable.addSensor(Contacts.getInstance().getContactByUuid(requestUid).getName(), requestUid);
        }
        DeviceCotDispatcher.sendDiscoveryResponse(requestUid);
    }

    private static void onDiscoverResponse(CotEvent cotEvent) {
        String responseUid = cotEvent.getDetail().getChild(CotDetailTypes.DISCOVERY_RESPONSE.eltName)
                .getAttribute(CotDetailTypes.DISCOVERY_RESPONSE.attrs.resUid);
        TrackingPlugin.sensorsTable.addSensor(Contacts.getInstance().getContactByUuid(responseUid).getName(), responseUid);
    }

    private static void onWhitelistRequest(CotEvent cotEvent) {
        String contactUid = cotEvent.getDetail().getChild(CotDetailTypes.WHITELIST_REQUEST.eltName)
                .getAttribute(CotDetailTypes.WHITELIST_REQUEST.attrs.reqUid);
        DeviceCotDispatcher.sendWhitelistResponse(contactUid);
    }

    private static void onWhitelistResponse(CotEvent cotEvent) {
        CotDetail whitelistDetial = cotEvent.getDetail().getChild(CotDetailTypes.WHITELIST_RESPONSE.eltName);
        for(CotDetail deviceDetail : whitelistDetial.getChildren()) {
            String deviceMacAddress = deviceDetail.getAttribute(CotDetailTypes.WHITELIST_RESPONSE.deviceElt.attrs.macAddress);
            if (DeviceStorageManager.getUuid(DeviceStorageManager.ListType.WHITELIST, deviceMacAddress) != null)
                continue; // already exists in whitelist, skip it.
            String deviceName = deviceDetail.getAttribute(CotDetailTypes.WHITELIST_RESPONSE.deviceElt.attrs.name);
            DeviceInfo newDevice = new DeviceInfo(deviceName, deviceMacAddress, 0, false, UUID.randomUUID().toString(), MapView.getDeviceUid());
            DeviceStorageManager.addOrUpdateDevice(DeviceStorageManager.ListType.WHITELIST, newDevice);
        }
    }

    private static void onDeviceFound(CotEvent cotEvent) {
        // grab attributes from CotDetail
        CotDetail detail = cotEvent.findDetail(CotDetailTypes.DEVICE_FOUND.eltName);

        String macAddress = detail.getAttribute(CotDetailTypes.DEVICE_FOUND.attrs.macAddress);
        String whitelistUid = DeviceStorageManager.getUuid(DeviceStorageManager.ListType.WHITELIST, macAddress);
        if (whitelistUid == null)
            return; // not in whitelist, ignore.
        String name = detail.getAttribute(CotDetailTypes.DEVICE_FOUND.attrs.name);
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
            String markerUid = UUID.randomUUID().toString();
            marker = new Marker(cotEvent.getGeoPoint(), markerUid);
            whitelistMarkerUidMap.put(whitelistUid, markerUid);
            marker.setType("a-u-G");
            marker.setMetaAttributeSet(CotDetailTypes.MAPITEM_INFO.attrSetName, attrSet);
            marker.setTitle(name);
            marker.setEditable(false);
        }
        GeoPoint position = addPosition(macAddress, new Pair<>(sensorUid, cotEvent.getGeoPoint()));
        marker.setPoint(position);
        if (deviceGroup.getItemById(marker.getSerialId()) == null) {
            deviceGroup.addItem(marker);
        }
        marker.setVisible(Boolean.TRUE.equals(WhitelistTable.visibilityMap.get(whitelistUid)));
    }

    private static void onDeviceRemove(CotEvent cotEvent) {
        CotDetail detail = cotEvent.findDetail(CotDetailTypes.DEVICE_REMOVE.eltName);
        String macAddress = detail.getAttribute(CotDetailTypes.DEVICE_REMOVE.attrs.macAddress);
        String sensorUid = detail.getAttribute(CotDetailTypes.DEVICE_REMOVE.attrs.sensorUid);
        Marker item = getMarkerByMacAddress(macAddress);
        if (item == null) return;
        GeoPoint position = removePosition(macAddress, sensorUid);
        if (position == null) {
            if (whitelistMarkerUidMap.containsValue(item.getUID()))
                whitelistMarkerUidMap.remove(item.getUID());
            deviceGroup.removeItem(item);
        } else {
            item.setPoint(position);
        }
    }

    @Nullable
    public static Marker getMarkerByMacAddress(String macAddress) {
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

    /// Returns null if there are no positions left after operation.
    @Nullable
    private static GeoPoint removePosition(String macAddress, String sensorUid) {
        Set<Pair<String, GeoPoint>> existingPositions = devicePositions.get(macAddress);
        if (existingPositions == null || existingPositions.isEmpty())
            return null;
        // TODO: after deleting the marker, it got here and spat an error after stopped scanning. worth investigating.
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

    public static void setVisibility(String macAddress, boolean visible) {
        for (MapItem item : deviceGroup.getItems()) {
            String itemMacAddress = item.getMetaAttributeSet(CotDetailTypes.MAPITEM_INFO.attrSetName).getStringAttribute(CotDetailTypes.MAPITEM_INFO.attrs.macAddress);
            if (macAddress.equals(itemMacAddress)) {
                item.setVisible(visible);
                break;
            }
        }
    }

    @Override
    public void onDeviceListChange(List<DeviceInfo> devices) {
        // check if things need to be yeeted out
        Set<String> whitelistUids = new HashSet<>();
        for (DeviceInfo deviceInfo : devices)
            whitelistUids.add(deviceInfo.uuid);
        Set<String> removedEntries = new HashSet<>();
        for (String localWhitelistUid : whitelistMarkerUidMap.keySet()) {
            MapItem item = deviceGroup.findItem("uid", whitelistMarkerUidMap.get(localWhitelistUid));
            if (item == null) continue;

            // no longer on whitelist, remove from things.
            if (!whitelistUids.contains(localWhitelistUid)) {
                deviceGroup.removeItem(item);
                removedEntries.add(localWhitelistUid);
            }
        }
        for (String toRemove : removedEntries)
            whitelistMarkerUidMap.remove(toRemove);

        // change names and mac addresses
        for (DeviceInfo deviceInfo : devices) {
            MapItem item = deviceGroup.findItem("uid", whitelistMarkerUidMap.get(deviceInfo.uuid));
            if (item == null) continue;

            AttributeSet attrSet = item.getMetaAttributeSet(CotDetailTypes.MAPITEM_INFO.attrSetName);
            attrSet.setAttribute(CotDetailTypes.MAPITEM_INFO.attrs.name, deviceInfo.name);
            attrSet.setAttribute(CotDetailTypes.MAPITEM_INFO.attrs.macAddress, deviceInfo.macAddress);
        }
    }
}
