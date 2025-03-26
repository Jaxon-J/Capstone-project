package com.atakmap.android.trackingplugin.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.trackingplugin.BluetoothReceiver;
import com.atakmap.android.trackingplugin.Constants;
import com.atakmap.android.trackingplugin.plugin.R;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.ArrayList;

public class TabViewPagerAdapter extends RecyclerView.Adapter<TabViewPagerAdapter.TabViewHolder> {
    private static final String TAG = Constants.createTag(TabViewPagerAdapter.class);
    private final Context context;
    private final BluetoothReceiver btReceiver;

    public TabViewPagerAdapter(Context context, BluetoothReceiver btReceiver) {
        this.context = context;
        this.btReceiver = btReceiver;
    }

    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {
        Log.d(TAG, "onCreateViewHolder: " + Constants.TAB_LAYOUTS.get(position).first);
        View tabLayout = LayoutInflater.from(this.context)
                .inflate(Constants.TAB_LAYOUTS.get(position).second, parent, false);
        return new TabViewHolder(tabLayout, Constants.TAB_LAYOUTS.get(position).first);
    }


    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: " + Constants.TAB_LAYOUTS.get(position).first);
        // This switch is for putting functionality on each page.
        switch (holder.tabName) {
            case Constants.TRACKING_TABNAME:
            case Constants.DEVICES_TABNAME: {

                new DevicesTabHandler(holder.itemView,this.context);

                break;
            }
            case Constants.SENSORS_TABNAME: {
                break;
            }
            case Constants.DEBUG_TABNAME: {
                holder.itemView.findViewById(R.id.debugPlacePinButton)
                        .setOnClickListener((View v) -> {
                            GeoPoint selfPoint = MapView.getMapView().getSelfMarker().getPoint();
                            GeoPoint trackedPoint = new GeoPoint(selfPoint.getLatitude(),
                                    // 0.0000035 = 10ft
                                    selfPoint.getLongitude(), selfPoint.getAltitude(),
                                    selfPoint.getAltitudeReference(), 11, 11);
                            PlacePointTool.MarkerCreator mc =
                                    new PlacePointTool.MarkerCreator(trackedPoint);
                            mc.setType("a-u-G");
                            mc.setCallsign("tracked");
                            Marker trackedMarker = mc.placePoint();
                        });

                // debug bluetooth scanning
                AtakBroadcast.DocumentedIntentFilter btIntentFilter =
                        new AtakBroadcast.DocumentedIntentFilter();
                btIntentFilter.addAction(BluetoothReceiver.ACTIONS.BLE_START_SCAN);
                btIntentFilter.addAction(BluetoothReceiver.ACTIONS.BLE_STOP_SCAN);
                btIntentFilter.addAction(BluetoothReceiver.ACTIONS.CLASSIC_START_DISCOVERY);
                btIntentFilter.addAction(BluetoothReceiver.ACTIONS.CLASSIC_STOP_DISCOVERY);
                btIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                btIntentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                btIntentFilter.addAction(BluetoothDevice.ACTION_FOUND);
                AtakBroadcast.getInstance().registerReceiver(this.btReceiver, btIntentFilter);


                holder.itemView.findViewById(R.id.bleScanDebugButton)
                        .setOnClickListener(this::onBleScanDebugButtonClick);
                holder.itemView.findViewById(R.id.classicScanDebugButton)
                        .setOnClickListener(this::onClassicScanDebugButtonClick);
                break;
            }
            default: {
                // if all tabs are here, this is unreachable
                Log.w(TAG, String.format("Please add string \"%s\" to Constants.java and use " +
                        "that" + " variable as a case in TabViewPagerAdapter.onBindViewHolder",
                        holder.tabName));
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return Constants.TAB_LAYOUTS.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position; // passes position to onCreateViewHolder instead of default (0)
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
        boolean isEnabled = b.getText().equals(context.getString(R.string.classic_scan_enabled));
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

    public static class TabViewHolder extends RecyclerView.ViewHolder {
        public String tabName;

        public TabViewHolder(@NonNull View itemView, String name) {
            super(itemView);
            this.tabName = name;
        }
    }
}