package com.atakmap.android.trackingplugin.comms;

import android.util.Log;

import com.atakmap.android.cot.detail.CotDetailHandler;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.trackingplugin.Constants;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.text.ParseException;
import java.util.Set;
import java.util.TreeSet;

import gov.tak.api.util.AttributeSet;

public class DeviceCotDetailHandler extends CotDetailHandler {
    private static final String TAG = Constants.createTag(DeviceCotDetailHandler.class);
    public DeviceCotDetailHandler() {
        super(Set.of(
                TrackingCotEventTypes.DEVICE_FOUND.eltName
                // TrackingCotEventTypes.SOME_OTHER_EVENT.eltName
        ));
    }

    @Override
    public CommsMapComponent.ImportResult toItemMetadata(MapItem mapItem, CotEvent cotEvent, CotDetail rootDetail) {
        for (CotDetail child : rootDetail.getChildren()) {
            switch (child.getElementName()) {
                case TrackingCotEventTypes.DEVICE_FOUND.eltName: {
                    String name = child.getAttribute(TrackingCotEventTypes.DEVICE_FOUND.attrs.name);
                    String macAddress = child.getAttribute(TrackingCotEventTypes.DEVICE_FOUND.attrs.macAddress);
                    int rssi = Integer.parseInt(child.getAttribute(TrackingCotEventTypes.DEVICE_FOUND.attrs.rssi));

                    AttributeSet attrSet = new AttributeSet();
                    attrSet.setAttribute(TrackingCotEventTypes.DEVICE_FOUND.attrs.name, name);
                    attrSet.setAttribute(TrackingCotEventTypes.DEVICE_FOUND.attrs.macAddress, macAddress);
                    attrSet.setAttribute(TrackingCotEventTypes.DEVICE_FOUND.attrs.rssi, rssi);

                    mapItem.setMetaAttributeSet(TrackingCotEventTypes.DEVICE_FOUND.eltName, attrSet);
                    mapItem.setClickPoint(cotEvent.getGeoPoint());

                    Log.d(TAG, String.format("RECEIVED DEVICE PACKET:\n\tNAME: %s\n\tMAC: %s\n\tRSSI: %d", name, macAddress, rssi));
                    return CommsMapComponent.ImportResult.SUCCESS;
                }
            }
        }
        return CommsMapComponent.ImportResult.IGNORE;
//        CotDetail detail = rootDetail.getChild(TRACKED_DETAIL_NAME);
//        String sampleAttr = detail.getAttribute("sample");
//
//        AttributeSet attrSet = new AttributeSet();
//        attrSet.setAttribute("sample_attr", sampleAttr);
//        mapItem.setMetaAttributeSet(TRACKED_ATTRIBUTE_SET_KEY, attrSet);
//        return CommsMapComponent.ImportResult.SUCCESS;
    }

    @Override
    public boolean toCotDetail(MapItem mapItem, CotEvent cotEvent, CotDetail rootDetail) {
        return true;
    }
}

class CotDetailTimeOrderedSet extends TreeSet<CotDetail> {
    private static final String TAG = Constants.createTag(CotDetailTimeOrderedSet.class);
    private static final String TIME_ATTRIBUTE = "time";
    public CotDetailTimeOrderedSet() {
        super((CotDetail detail1, CotDetail detail2) -> {
            try {
                CoordinatedTime time1 = CoordinatedTime.fromCot(detail1.getAttribute(TIME_ATTRIBUTE));
                CoordinatedTime time2 = CoordinatedTime.fromCot(detail2.getAttribute(TIME_ATTRIBUTE));
                return Long.signum(time1.millisecondDiff(time2));
            } catch (ParseException e) {
                Log.w(TAG, "Could not parse dates in history while organizing CoT's. Paths will be out of order.");
                return 0;
            }
        });
    }

    @Override
    public boolean add(CotDetail cotDetail) {
        if (cotDetail.getAttribute(TIME_ATTRIBUTE) == null) {
            cotDetail.setAttribute(TIME_ATTRIBUTE, CoordinatedTime.toCot(new CoordinatedTime()));
        }
        return super.add(cotDetail);
    }
}


/*

Inputs/Outputs?

Send to everyone:
CotMapComponent.getExternalDispatcher().dispatch(CotEvent);

Listeners:
extends (Abstract)CotEventImporter, CotImporterManager(MapView).registerImporter(AbstractCotEventImporter)
extends CotDetailHandler, CotDetailManager.getInstance().registerHandler(CotDetailHandler)

Automatically handles CoT to MapItem transformations. Need to register.
CotDetailHandler.toCotDetail(MapItem, CotEvent, CotDetail) data from MapItem arg -> CotDetail arg
CotDetailHandler.toItemMetaData(MapItem, CotEvent, CotDetail) data from CotDetail arg -> MapItem arg

MapItem to CoT Event:
CotEventFactory.createCotEvent(MapItem);


Listen:
MapView.getMapView().getMapEventDispatcher().addMapEventListener(MapEventType, MapEventDispatchListener)
MapEventDispatchListener


WHITELIST COTEVENT (just an idea, consolidate whitelist cotevents?):
- whitelist element

UPDATED WHITELIST COTEVENT:
- tracking_device_whitelist_update element: mac_address_old, mac_address_new

REQUEST WHITELIST COTEVENT:
- request_whitelist element

SEND WHITELIST COTEVENT:
- whitelist element
  - device element children: mac_address, user_name

HISTORY REQUEST COTEVENT:
- request_history element

HISTORY SEND COTEVENT:
- history element: mac_address
  - loctime element children: time, "point" attrs (lat, lon, hae, ce, le)

Tests:
run scan with empty whitelist, scan should not start.

 */
