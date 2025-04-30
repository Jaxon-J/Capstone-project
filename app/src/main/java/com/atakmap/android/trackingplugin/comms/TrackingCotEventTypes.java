package com.atakmap.android.trackingplugin.comms;

public class TrackingCotEventTypes {
    public static class DEVICE_FOUND {
        public static final String eltName = "device_found";
        public static class attrs {
            public static final String name = "user_given_name";
            public static final String macAddress = "mac_address";
            public static final String rssi = "rssi";
        }
    }
//    REQUEST_HISTORY(eltName = "request_device_history"),
//    DEVICE_HISTORY(eltName = "device_history"),
//    REQUEST_WHITELIST(eltName = "request_whitelist"),
//    SEND_WHITELIST(eltName = "send_whitelist");
}