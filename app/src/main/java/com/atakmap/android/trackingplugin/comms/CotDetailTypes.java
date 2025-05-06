package com.atakmap.android.trackingplugin.comms;

public class CotDetailTypes {
    public static class MAPITEM_INFO {
        public static final String attrSetName = "tracked_info";
        public static class attrs {
            public static final String name = "name";
            public static final String macAddress = "mac_address";
            public static final String rssi = "rssi";
        }
    }

    public static class DEVICE_FOUND {
        public static final String eltName = "device_found";
        public static final String typeName = "t-x-device-found";
        public static class attrs {
            public static final String name = "user_given_name";
            public static final String macAddress = "mac_address";
            public static final String rssi = "rssi";
            public static final String sensorUid = "sensor_uid";
        }
    }

    public static class DEVICE_REMOVE {
        public static final String eltName = "device_remove";
        public static final String typeName = "t-x-device-remove";
        public static class attrs {
            public static final String macAddress = "mac_address";
            public static final String sensorUid = "sensor_uid";
        }
    }
//    REQUEST_HISTORY(eltName = "request_device_history"),
//    DEVICE_HISTORY(eltName = "device_history"),
//    REQUEST_WHITELIST(eltName = "request_whitelist"),
//    SEND_WHITELIST(eltName = "send_whitelist");
}