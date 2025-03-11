package com.atakmap.android.trackingplugin;

import android.content.Context;
import android.content.Intent;
import android.util.Pair;
import android.view.View;
import android.widget.Button;

import androidx.viewpager2.widget.ViewPager2;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.trackingplugin.plugin.R;
import com.atakmap.android.trackingplugin.ui.TabViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.Tab;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

public class TrackingPluginDropDownReceiver extends DropDownReceiver {
    private final Context pluginContext;
    private final View mainView;
    private final List<Pair<String, Integer>> tabInfo = List.of(
            new Pair<>("Tracking", R.layout.tracking_layout),
            new Pair<>("Devices", R.layout.devices_layout),
            new Pair<>("Sensors", R.layout.sensors_layout),
            new Pair<>("Debug", R.layout.debug_layout));
    BluetoothReceiver btReceiver;

    protected TrackingPluginDropDownReceiver(final MapView mapView, final Context context) {
        super(mapView);
        this.pluginContext = context;
        // set up UI, set main layout to be viewed when SHOW_PLUGIN action is triggered
        mainView = PluginLayoutInflater.inflate(context, R.layout.main_layout, null);
        // set up all receivers/UI responses here
        TabLayout tabLayout = mainView.findViewById(R.id.tabLayout);
        tabLayout.removeAllTabs();
        ViewPager2 pager = mainView.findViewById(R.id.viewPager);
        pager.setAdapter(new TabViewPagerAdapter(pluginContext, tabInfo));
        TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, pager,
                (tab, position) -> tab.setText(tabInfo.get(position).first));
        mediator.attach();
        // PIN/MARKER PLACEMENT
//        mainView.findViewById(R.id.debugPlacePinButton).setOnClickListener((View v) -> {
//            GeoPoint selfPoint = MapView.getMapView().getSelfMarker().getPoint();
//            GeoPoint trackedPoint = new GeoPoint(selfPoint.getLatitude(), // 0.0000035 = 10ft
//                    selfPoint.getLongitude(), selfPoint.getAltitude(),
//                    selfPoint.getAltitudeReference(), 11, 11);
//            PlacePointTool.MarkerCreator mc = new PlacePointTool.MarkerCreator(trackedPoint);
//            mc.setType("a-u-G");
//            mc.setCallsign("tracked");
//            Marker trackedMarker = mc.placePoint();
//        });
//
//        // BLUETOOTH SCANNING
//        btReceiver = new BluetoothReceiver(pluginContext);
//        DocumentedIntentFilter btIntentFilter = new DocumentedIntentFilter();
//        btIntentFilter.addAction(BluetoothReceiver.ACTIONS.BLE_START_SCAN);
//        btIntentFilter.addAction(BluetoothReceiver.ACTIONS.BLE_STOP_SCAN);
//        btIntentFilter.addAction(BluetoothReceiver.ACTIONS.CLASSIC_START_DISCOVERY);
//        btIntentFilter.addAction(BluetoothReceiver.ACTIONS.CLASSIC_STOP_DISCOVERY);
//        btIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
//        btIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//        btIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
//        AtakBroadcast.getInstance().registerReceiver(btReceiver, btIntentFilter);
//
//        mainView.findViewById(R.id.bleScanDebugButton)
//                .setOnClickListener(this::onBleScanDebugButtonClick);
//        mainView.findViewById(R.id.classicScanDebugButton)
//                .setOnClickListener(this::onClassicScanDebugButtonClick);
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
        boolean isEnabled = b.getText()
                .equals(pluginContext.getString(R.string.classic_scan_enabled));
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


//    private class TabViewPagerAdapter extends FragmentStateAdapter {
//
//        TabData[] tabs = {
//                new TabData(R.string.tracking_tab, R.layout.tracking_layout),
//                new TabData(R.string.devices_tab, R.layout.devices_layout),
//                new TabData(R.string.sensors_tab, R.layout.sensors_layout),
//                new TabData(R.string.debug_tab, R.layout.debug_layout)
//        };
//
//        public void something(Context context) {
//
//        }
//        public TabViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle
//        lifecycle) {
//            super(fragmentManager, lifecycle);
//        }
//
//
//        @NonNull
//        @Override
//        public Fragment createFragment(int position) {
//            return tabs[position].fragment;
//        }
//
//        @Override
//        public int getItemCount() {
//            return tabs.length;
//        }
//    }
//
//    private final class TabData {
//        public final String name;
//        public final Fragment fragment;
//        public TabData(int stringId, int layoutId) {
//            this.name = pluginContext.getString(stringId);
//            this.fragment = new Fragment(layoutId);
//        }
//    }

    // If this ever gets refactored out, it needs the pluginContext and tabInfo passed in as
    // parameters

}