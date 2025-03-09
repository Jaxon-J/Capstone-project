package com.atakmap.android.trackingplugin;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.trackingplugin.plugin.R;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.coremap.maps.coords.GeoPoint;

public class TrackingPluginDropDownReceiver extends DropDownReceiver {
    private final Context pluginContext;
    private final View mainView;
    BluetoothReceiver btReceiver;

    protected TrackingPluginDropDownReceiver(final MapView mapView, final Context context) {
        super(mapView);
        this.pluginContext = context;
        // set up UI, set main layout to be viewed when SHOW_PLUGIN action is triggered
        mainView = PluginLayoutInflater.inflate(pluginContext, R.layout.main_layout, null);
        // set up all receivers/UI responses here

        // PIN/MARKER PLACEMENT
        mainView.findViewById(R.id.debugPlacePinButton).setOnClickListener((View v) -> {
            GeoPoint selfPoint = MapView.getMapView().getSelfMarker().getPoint();
            GeoPoint trackedPoint = new GeoPoint(selfPoint.getLatitude(), // 0.0000035 = 10ft
                    selfPoint.getLongitude(), selfPoint.getAltitude(),
                    selfPoint.getAltitudeReference(), 11, 11);
            PlacePointTool.MarkerCreator mc = new PlacePointTool.MarkerCreator(trackedPoint);
            mc.setType("a-u-G");
            mc.setCallsign("tracked");
            Marker trackedMarker = mc.placePoint();
        });

        // BLUETOOTH SCANNING
        btReceiver = new BluetoothReceiver(pluginContext);
        DocumentedIntentFilter btIntentFilter = new DocumentedIntentFilter();
        btIntentFilter.addAction(BluetoothReceiver.ACTIONS.BLE_START_SCAN);
        btIntentFilter.addAction(BluetoothReceiver.ACTIONS.BLE_STOP_SCAN);
        btIntentFilter.addAction(BluetoothReceiver.ACTIONS.CLASSIC_START_DISCOVERY);
        btIntentFilter.addAction(BluetoothReceiver.ACTIONS.CLASSIC_STOP_DISCOVERY);
        btIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        btIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        btIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        AtakBroadcast.getInstance().registerReceiver(btReceiver, btIntentFilter);

        mainView.findViewById(R.id.bleScanDebugButton)
                .setOnClickListener(this::onBleScanDebugButtonClick);
        mainView.findViewById(R.id.classicScanDebugButton)
                .setOnClickListener(this::onClassicScanDebugButtonClick);
    }

    private void onBleScanDebugButtonClick(View v) {
        Button b = (Button) v;
        boolean isEnabled = b.getText().equals(pluginContext.getString(R.string.ble_scan_enabled));
        if (isEnabled) {
            Intent stopScanIntent = new Intent(BluetoothReceiver.ACTIONS.BLE_STOP_SCAN);
            AtakBroadcast.getInstance().sendBroadcast(stopScanIntent);
            b.setText(pluginContext.getString(R.string.ble_scan_disabled));
            return;
        }
        Intent startScanIntent = new Intent(BluetoothReceiver.ACTIONS.BLE_START_SCAN);
        AtakBroadcast.getInstance().sendBroadcast(startScanIntent);
        b.setText(pluginContext.getString(R.string.ble_scan_enabled));
    }

    private void onClassicScanDebugButtonClick(View v) {
        Button b = (Button) v;
        boolean isEnabled = b.getText().equals(pluginContext.getString(R.string.classic_scan_enabled));
        if (isEnabled) {
            Intent stopScanIntent = new Intent(BluetoothReceiver.ACTIONS.CLASSIC_STOP_DISCOVERY);
            AtakBroadcast.getInstance().sendBroadcast(stopScanIntent);
            b.setText(pluginContext.getString(R.string.classic_scan_disabled));
            return;
        }
        Intent startScanIntent = new Intent(BluetoothReceiver.ACTIONS.CLASSIC_START_DISCOVERY);
        AtakBroadcast.getInstance().sendBroadcast(startScanIntent);
        b.setText(pluginContext.getString(R.string.classic_scan_enabled));
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case ACTIONS.SHOW_PLUGIN: // UI ENTRY POINT
                if (!isClosed()) unhideDropDown();
                // showDropDown has several more overloads. if we want to listen to
                // resize/open/close events,
                // we implement DropDown.StateChangeListener and add "false, this" parameters here.
                showDropDown(mainView, HALF_WIDTH, FULL_WIDTH, FULL_WIDTH, HALF_HEIGHT);
                break;
        }
    }

    @Override
    protected void disposeImpl() {
        if (btReceiver != null) {
            AtakBroadcast.getInstance().unregisterReceiver(btReceiver);
            btReceiver = null;
        }
    }

    public static final class ACTIONS {
        public static final String SHOW_PLUGIN = "com.atakmap.android.trackingplugin.SHOW_PLUGIN";
    }
}
