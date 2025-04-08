package com.atakmap.android.trackingplugin.ui;

import static com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupWindow;
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
import com.atakmap.android.trackingplugin.DeviceListManager;
import com.atakmap.android.trackingplugin.DeviceListManager.StoredDeviceInfo;
import com.atakmap.android.trackingplugin.LiveDeviceInfo;
import com.atakmap.android.trackingplugin.plugin.BuildConfig;
import com.atakmap.android.trackingplugin.plugin.R;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class TabViewPagerAdapter extends RecyclerView.Adapter<TabViewPagerAdapter.TabViewHolder> {
    private static final String TAG = Constants.createTag(TabViewPagerAdapter.class);
    private final Context context;
    private final BluetoothReceiver btReceiver;
    private boolean devicesTabInitialized = false; // TODO: maybe make this a list for all tabs if there's other necessary init logic.

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


    /// Main method that is called to initialize UI logic. It is called upon initialization, and all subsequent times the tab is changed.
    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: " + Constants.TAB_LAYOUTS.get(position).first);
        // This switch is called: 1) when initialized, 2) every time tab is switched to.
        switch (holder.tabName) {
            case Constants.TRACKING_TABNAME: {
                break;
            }
            case Constants.DEVICES_TABNAME: {
                if (!devicesTabInitialized) {
                    // set up device table
                    TableLayout devTable = holder.itemView.findViewById(R.id.devicesTableLayout);
                    List<StoredDeviceInfo> devices = DeviceListManager.getDeviceList(DeviceListManager.ListType.WHITELIST);

                    for (StoredDeviceInfo devInfo : devices)
                        addDeviceToTable(devTable, devInfo, DeviceListManager.ListType.WHITELIST);

                    // set up "add devices" pop-up
                    // TODO: add_device_popup more sense as a FrameView not a ScrollView, maybe?
                    View popupView = LayoutInflater.from(context).inflate(R.layout.add_device_popup, (ViewGroup) holder.itemView, false);
                    PopupWindow window = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                    window.setFocusable(true);
                    window.setBackgroundDrawable(new ColorDrawable(Color.WHITE)); // white background, maybe not.

                    popupView.findViewById(R.id.EnterButton).setOnClickListener(v -> {
                        String deviceName = ((EditText) popupView.findViewById(R.id.deviceIDEntry)).getText().toString();
                        String deviceMac = ((EditText) popupView.findViewById(R.id.MACEntry)).getText().toString();

                        StoredDeviceInfo newDevice = new StoredDeviceInfo(deviceName, deviceMac, false);
                        addDeviceToTable(devTable, newDevice, DeviceListManager.ListType.WHITELIST);

                        window.dismiss();
                    });

                    popupView.findViewById(R.id.CancelButton).setOnClickListener(v -> window.dismiss());

                    // show pop-up by clicking add devices button.
                    holder.itemView.findViewById(R.id.addDeviceButton).setOnClickListener(v -> {
                        ((EditText) popupView.findViewById(R.id.deviceIDEntry)).setText("");
                        ((EditText) popupView.findViewById(R.id.MACEntry)).setText("");

                        window.showAtLocation(holder.itemView, Gravity.CENTER, 0, 0);
                    });

                    devicesTabInitialized = true;
                }

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
                                    selfPoint.getLongitude(), selfPoint.getAltitude(), selfPoint.getAltitudeReference(), 11, 11);
                            PlacePointTool.MarkerCreator mc = new PlacePointTool.MarkerCreator(trackedPoint);
                            mc.setType("a-u-G");
                            mc.setCallsign("tracked");
                            Marker trackedMarker = mc.placePoint();
                        });

                // debug bluetooth scanning
                DocumentedIntentFilter btIntentFilter = new DocumentedIntentFilter();
                btIntentFilter.addAction(BluetoothReceiver.ACTIONS.BLE_START_SCAN);
                btIntentFilter.addAction(BluetoothReceiver.ACTIONS.BLE_STOP_SCAN);
                btIntentFilter.addAction(BluetoothReceiver.ACTIONS.ENABLE_SCAN_WHITELIST);
                btIntentFilter.addAction(BluetoothReceiver.ACTIONS.DISABLE_SCAN_WHITELIST);
                AtakBroadcast.getInstance().registerReceiver(this.btReceiver, btIntentFilter);


                holder.itemView.findViewById(R.id.bleScanDebugButton)
                        .setOnClickListener((View v) -> {
                            Button b = (Button) v;
                            boolean isEnabled = b.getText()
                                    .equals(context.getString(R.string.ble_scan_enabled));
                            if (isEnabled) {
                                Intent stopScanIntent = new Intent(BluetoothReceiver.ACTIONS.BLE_STOP_SCAN);
                                AtakBroadcast.getInstance().sendBroadcast(stopScanIntent);
                                b.setText(context.getString(R.string.ble_scan_disabled));
                                return;
                            }
                            Intent startScanIntent = new Intent(BluetoothReceiver.ACTIONS.BLE_START_SCAN);
                            AtakBroadcast.getInstance().sendBroadcast(startScanIntent);
                            b.setText(context.getString(R.string.ble_scan_enabled));
                        });
                holder.itemView.findViewById(R.id.whitelistCheckBox)
                        .setOnClickListener((View v) ->
                                AtakBroadcast.getInstance().sendBroadcast(
                                    new Intent(
                                        ((CheckBox) v).isChecked()
                                            ? BluetoothReceiver.ACTIONS.ENABLE_SCAN_WHITELIST
                                            : BluetoothReceiver.ACTIONS.DISABLE_SCAN_WHITELIST)));
                break;
            }
            default: {
                // if all tabs are here, this is unreachable
                Log.w(TAG, String.format("onBindViewHolder: tab \"%s\" unknown", holder.tabName));
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return Constants.TAB_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        return position; // passes position to onCreateViewHolder instead of default (0)
    }

    /// Add data here that will be relevant to tab logic, that can be accessed in {@link #onBindViewHolder(TabViewHolder, int)} via the first argument.
    public static class TabViewHolder extends RecyclerView.ViewHolder {
        public String tabName;

        public TabViewHolder(@NonNull View itemView, String name) {
            super(itemView);
            this.tabName = name;
        }
    }

    private void addDeviceToTable(TableLayout table, StoredDeviceInfo devInfo, DeviceListManager.ListType associatedList) {
        TableRow row = (TableRow) LayoutInflater.from(context)
                .inflate(R.layout.device_table_row_layout, table, false);
        ((TextView) row.getChildAt(0)).setText(devInfo.name);
        ((TextView) row.getChildAt(1)).setText(devInfo.macAddress);
        row.getChildAt(2).setOnClickListener(v -> {
            DeviceListManager.removeDevice(associatedList, devInfo.macAddress);
            table.removeView(row);
        });
        table.addView(row);
    }
}