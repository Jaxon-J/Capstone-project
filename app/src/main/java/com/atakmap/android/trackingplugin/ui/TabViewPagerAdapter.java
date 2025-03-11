package com.atakmap.android.trackingplugin.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.trackingplugin.BluetoothReceiver;
import com.atakmap.android.trackingplugin.Constants;
import com.atakmap.android.trackingplugin.TrackingPluginDropDownReceiver;
import com.atakmap.android.trackingplugin.plugin.R;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.List;

public class TabViewPagerAdapter extends RecyclerView.Adapter<TabViewPagerAdapter.TabViewHolder> {
    private static final String TAG = Constants.createTag(TabViewPagerAdapter.class);
    private final Context context;
    private final List<Pair<String, Integer>> tabInfo;

    public TabViewPagerAdapter(Context context, List<Pair<String, Integer>> tabNameLayoutIdPairs) {
        this.context = context;
        this.tabInfo = tabNameLayoutIdPairs;
    }

    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {
        Log.d(TAG, "onCreateViewHolder: " + tabInfo.get(position).first);
        View tabLayout = LayoutInflater.from(this.context)
                .inflate(tabInfo.get(position).second, parent, false);
        return new TabViewHolder(tabLayout, tabInfo.get(position).first);
    }

    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: " + tabInfo.get(position).first);
         switch (holder.tabName) {
             case "Debug":
                 holder.itemView.findViewById(R.id.debugPlacePinButton).setOnClickListener((View v) -> {
                     GeoPoint selfPoint = MapView.getMapView().getSelfMarker().getPoint();
                     GeoPoint trackedPoint = new GeoPoint(selfPoint.getLatitude(), // 0.0000035 = 10ft
                             selfPoint.getLongitude(), selfPoint.getAltitude(),
                             selfPoint.getAltitudeReference(), 11, 11);
                     PlacePointTool.MarkerCreator mc = new PlacePointTool.MarkerCreator(trackedPoint);
                     mc.setType("a-u-G");
                     mc.setCallsign("tracked");
                     Marker trackedMarker = mc.placePoint();
                 });

                 // debug bluetooth scanning
                 AtakBroadcast.DocumentedIntentFilter btIntentFilter = new AtakBroadcast.DocumentedIntentFilter();
                 btIntentFilter.addAction(BluetoothReceiver.ACTIONS.BLE_START_SCAN);
                 btIntentFilter.addAction(BluetoothReceiver.ACTIONS.BLE_STOP_SCAN);
                 btIntentFilter.addAction(BluetoothReceiver.ACTIONS.CLASSIC_START_DISCOVERY);
                 btIntentFilter.addAction(BluetoothReceiver.ACTIONS.CLASSIC_STOP_DISCOVERY);
                 btIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                 btIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                 btIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
                 AtakBroadcast.getInstance().registerReceiver(TrackingPluginDropDownReceiver.btReceiver, btIntentFilter);


                 holder.itemView.findViewById(R.id.bleScanDebugButton)
                         .setOnClickListener(this::onBleScanDebugButtonClick);
                 holder.itemView.findViewById(R.id.classicScanDebugButton)
                         .setOnClickListener(this::onClassicScanDebugButtonClick);
        // update tabs here, case [TabName]: [behavior]; break;
         }
    }

    @Override
    public int getItemCount() {
        return tabInfo.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position; // passes position to onCreateViewHolder instead of default (0)
    }

    public static class TabViewHolder extends RecyclerView.ViewHolder {
        public String tabName;

        public TabViewHolder(@NonNull View itemView, String name) {
            super(itemView);
            this.tabName = name;
        }
    }

    private void onBleScanDebugButtonClick(View v) {
        Button b = (Button) v;
        boolean isEnabled = b.getText().equals(context.getString(R.string.ble_scan_enabled));
        if (isEnabled) {
            Intent stopScanIntent = new Intent(BluetoothReceiver.ACTIONS.BLE_STOP_SCAN);
            AtakBroadcast.getInstance().sendBroadcast(stopScanIntent);
            b.setText(context.getString(R.string.ble_scan_disabled));
            return;
        }
        Intent startScanIntent = new Intent(BluetoothReceiver.ACTIONS.BLE_START_SCAN);
        AtakBroadcast.getInstance().sendBroadcast(startScanIntent);
        b.setText(context.getString(R.string.ble_scan_enabled));
    }

    private void onClassicScanDebugButtonClick(View v) {
        Button b = (Button) v;
        boolean isEnabled = b.getText()
                .equals(context.getString(R.string.classic_scan_enabled));
        if (isEnabled) {
            Intent stopScanIntent = new Intent(BluetoothReceiver.ACTIONS.CLASSIC_STOP_DISCOVERY);
            AtakBroadcast.getInstance().sendBroadcast(stopScanIntent);
            b.setText(context.getString(R.string.classic_scan_disabled));
            return;
        }
        Intent startScanIntent = new Intent(BluetoothReceiver.ACTIONS.CLASSIC_START_DISCOVERY);
        AtakBroadcast.getInstance().sendBroadcast(startScanIntent);
        b.setText(context.getString(R.string.classic_scan_enabled));
    }
}