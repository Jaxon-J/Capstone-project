package com.atakmap.android.trackingplugin.ui;

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
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.trackingplugin.BluetoothReceiver;
import com.atakmap.android.trackingplugin.Constants;
import com.atakmap.android.trackingplugin.DeviceInfo;
import com.atakmap.android.trackingplugin.DeviceListManager;
import com.atakmap.android.trackingplugin.DeviceMapDisplay;
import com.atakmap.android.trackingplugin.plugin.R;
import com.atakmap.android.trackingplugin.plugin.TrackingPlugin;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import gov.tak.api.ui.IHostUIService;
import gov.tak.api.ui.Pane;
import gov.tak.api.ui.PaneBuilder;

public class TabViewPagerAdapter extends RecyclerView.Adapter<TabViewPagerAdapter.TabViewHolder> {
    private static final String TAG = Constants.createTag(TabViewPagerAdapter.class);
    private final Context context;
    private final IHostUIService uiService;
    private boolean devicesTabInitialized = false; // TODO: maybe make this a list for all tabs if there's other necessary init logic.

    public TabViewPagerAdapter(Context context, IHostUIService uiService) {
        this.context = context;
        this.uiService = uiService;
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
                holder.itemView.findViewById(R.id.trackingScanButton)
                        .setOnClickListener((View v) -> {
                            Button b = (Button) v;
                            if (b.getText().equals(context.getString(R.string.tracking_start_scan))) {
                                Intent startScanIntent = new Intent(BluetoothReceiver.ACTIONS.BLE_START_SCAN);
                                AtakBroadcast.getInstance().sendBroadcast(startScanIntent);
                                b.setText(context.getString(R.string.tracking_stop_scan));
                                return;
                            }
                            Intent stopScanIntent = new Intent(BluetoothReceiver.ACTIONS.BLE_STOP_SCAN);
                            AtakBroadcast.getInstance().sendBroadcast(stopScanIntent);
                            b.setText(context.getString(R.string.tracking_start_scan));
                        });
                break;
            }
            case Constants.WHITELIST_TABNAME: {
                if (!devicesTabInitialized) {
                    // set up device table
                    TableLayout devTable = holder.itemView.findViewById(R.id.whitelistDeviceTable);
                    List<DeviceInfo> devices = DeviceListManager.getDeviceList(DeviceListManager.ListType.WHITELIST);

                    for (DeviceInfo devInfo : devices)
                        addDeviceToTable(devTable, devInfo);

                    // set up "add devices" pop-up
                    // TODO: add_device_popup more sense as a FrameView not a ScrollView, maybe?
                    View popupView = LayoutInflater.from(context).inflate(R.layout.add_device_popup, (ViewGroup) holder.itemView, false);
                    PopupWindow window = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                    window.setFocusable(true);
                    window.setBackgroundDrawable(new ColorDrawable(Color.WHITE)); // white background, maybe not.

                    popupView.findViewById(R.id.addDeviceEnterButton).setOnClickListener(v -> {
                        String deviceName = ((EditText) popupView.findViewById(R.id.addDeviceNameTextEntry)).getText().toString();
                        String deviceMac = ((EditText) popupView.findViewById(R.id.addDeviceMacTextEntry)).getText().toString();

                        DeviceInfo newDevice = new DeviceInfo(deviceName, deviceMac, -1, false);
                        DeviceListManager.addOrUpdateDevice(DeviceListManager.ListType.WHITELIST, newDevice);
                        addDeviceToTable(devTable, newDevice);

                        window.dismiss();
                    });

                    popupView.findViewById(R.id.addDeviceCancelButton).setOnClickListener(v -> window.dismiss());

                    // show pop-up by clicking add devices button.
                    holder.itemView.findViewById(R.id.addDeviceButton).setOnClickListener(v -> {
                        ((EditText) popupView.findViewById(R.id.addDeviceNameTextEntry)).setText("");
                        ((EditText) popupView.findViewById(R.id.addDeviceMacTextEntry)).setText("");

                        window.showAtLocation(holder.itemView, Gravity.CENTER, 0, 0);
                    });

                    devicesTabInitialized = true;
                }

            }
            case Constants.SENSORS_TABNAME: {
                break;
            }
            case Constants.DEBUG_TABNAME: {
                // ble scan button
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
                // whitelist enabled button
                holder.itemView.findViewById(R.id.whitelistEnabledCheckBox)
                        .setOnClickListener((View v) ->
                                AtakBroadcast.getInstance().sendBroadcast(
                                    new Intent(
                                        ((CheckBox) v).isChecked()
                                            ? BluetoothReceiver.ACTIONS.ENABLE_WHITELIST
                                            : BluetoothReceiver.ACTIONS.DISABLE_WHITELIST)));
                // "place circle" button
                holder.itemView.findViewById(R.id.debugPlaceCircleButton)
                        .setOnClickListener((View v) -> {
                            String macAddrDebug = "DEBUG_CIRCLE";
                            Button b = (Button) v;
                            if (b.getText().equals(context.getString(R.string.place_circle))) {
                                DeviceMapDisplay.show(macAddrDebug);
                                b.setText(R.string.remove_circle);
                            } else {
                                DeviceMapDisplay.hide(macAddrDebug);
                                b.setText(R.string.place_circle);
                            }
                        });
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

    private void addDeviceToTable(TableLayout table, DeviceInfo devInfo) {
        TableRow row = (TableRow) LayoutInflater.from(context)
                .inflate(R.layout.device_table_row_layout, table, false);
        ((TextView) row.findViewById(R.id.deviceInfoPaneNameText)).setText(devInfo.name);
        ((TextView) row.findViewById(R.id.deviceRowMacAddressText)).setText(devInfo.macAddress);
        ((ToggleButton) row.findViewById(R.id.deviceRowVisibilityCheckbox)).setOnClickListener((View v) -> {
            if (((ToggleButton) v).isChecked())
                DeviceMapDisplay.show(devInfo.macAddress);
            else
                DeviceMapDisplay.hide(devInfo.macAddress);
        });
        table.addView(row);

        row.setOnClickListener((View v) -> {
            Pane devInfoPane = constructDeviceInfoPane(devInfo);
            uiService.showPane(devInfoPane, null);
        });
    }

    private Pane constructDeviceInfoPane(DeviceInfo deviceInfo) {
        // get view from layout, construct pane with it
        View deviceInfoView = PluginLayoutInflater.inflate(context, R.layout.device_info_pane);
        Pane deviceInfoPane = new PaneBuilder(deviceInfoView)
                .setMetaValue(Pane.RELATIVE_LOCATION, Pane.Location.Default)
                // pane will take up 50% of screen width in landscape mode
                .setMetaValue(Pane.PREFERRED_WIDTH_RATIO, 0.5D)
                // pane will take up 50% of screen height in portrait mode
                .setMetaValue(Pane.PREFERRED_HEIGHT_RATIO, 0.5D)
                .build();

        // populate text fields
        Map<Integer, String> textInfoMap = Map.of(
                R.id.deviceInfoPaneNameText, deviceInfo.name,
                R.id.deviceInfoPaneMacText, deviceInfo.macAddress,
                R.id.deviceInfoPaneFirstSeenText, "NOT IMPLEMENTED",
                R.id.deviceInfoPaneFirstSeenByText, "NOT IMPLEMENTED",
                R.id.deviceInfoPaneLastSeenText, new Timestamp(deviceInfo.lastSeenEpochMillis).toString(),
                R.id.deviceInfoPaneLastSeenByText, "NOT IMPLEMENTED"
        );
        for (Map.Entry<Integer, String> entry : textInfoMap.entrySet())
            ((TextView) deviceInfoView.findViewById(entry.getKey())).setText(entry.getValue());

        // add functionality to buttons
        deviceInfoView.findViewById(R.id.deviceInfoPaneBackButton).setOnClickListener((View v) -> {
            uiService.showPane(TrackingPlugin.primaryPane, null);
        });
        // TODO: locate / edit / delete buttons
        return deviceInfoPane;
    }
}