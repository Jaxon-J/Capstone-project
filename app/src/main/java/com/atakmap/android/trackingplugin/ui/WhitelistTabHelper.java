package com.atakmap.android.trackingplugin.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.trackingplugin.DeviceInfo;
import com.atakmap.android.trackingplugin.DeviceListManager;
import com.atakmap.android.trackingplugin.DeviceMapDisplay;
import com.atakmap.android.trackingplugin.plugin.R;
import com.atakmap.android.trackingplugin.plugin.TrackingPlugin;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import gov.tak.api.ui.IHostUIService;
import gov.tak.api.ui.Pane;
import gov.tak.api.ui.PaneBuilder;

// TODO: maybe rename this once it comes together
// TODO: look into re-architecting this into listener patterns.
@Deprecated
public class WhitelistTabHelper {
    private final Context context;
    private final IHostUIService uiService;
    private final TableLayout tableLayout;
    private final Button addDeviceButton;
    public WhitelistTabHelper(IHostUIService uiService, View tabView) {
        this.context = tabView.getContext();
        this.uiService = uiService;
        tableLayout = tabView.findViewById(R.id.whitelistDeviceTable);
        addDeviceButton = tabView.findViewById(R.id.addDeviceButton);
    }

    // TODO: check if the things we pass to the constructor would be better suited here. if that's the case, we can make this static, i bet.
    public void setup() {
        // populate table
        final List<DeviceInfo> existingWhitelist = DeviceListManager.getDeviceList(DeviceListManager.ListType.WHITELIST);
        for (DeviceInfo deviceInfo : existingWhitelist)
            addRowToTable(deviceInfo);

        // get the objects we need to

        // when add device button is clicked, reset the view to default (no text, no colors, etc)
        addDeviceButton.setOnClickListener(v -> {
            uiService.closePane(TrackingPlugin.primaryPane);
            uiService.showPane(constructAddDevicePane("", "", TrackingPlugin.primaryPane, null),
                    null);
        });
    }

    private void addRowToTable(DeviceInfo deviceInfo) {
        final TableRow row = (TableRow) LayoutInflater.from(context)
                .inflate(R.layout.device_table_row_layout, tableLayout, false);

        // set the text fields
        ((TextView) row.findViewById(R.id.deviceRowNameText)).setText(deviceInfo.name);
        ((TextView) row.findViewById(R.id.deviceRowMacAddressText)).setText(deviceInfo.macAddress);

        // checkbox will set visibility
        row.findViewById(R.id.deviceRowVisibilityCheckbox).setOnClickListener(v ->
                DeviceMapDisplay.setVisibility(deviceInfo.macAddress, ((ToggleButton) v).isChecked())
        );

        // add row click behavior
        row.setOnClickListener((View v) -> {
            uiService.closePane(TrackingPlugin.primaryPane);
            uiService.showPane(constructDeviceInfoPane(deviceInfo, row), null);
        });

        // row is prepared, add it to the table
        tableLayout.addView(row);
    }

    private Pane constructAddDevicePane(String defaultName, String defaultMacAddress, Pane returnPane, @Nullable TableRow deviceRow) {
        // get all the objects we need for behavior.
        final Pair<View, Pane> addDeviceViewPane = getViewPane(R.layout.add_device_pane);
        final View addDeviceView = addDeviceViewPane.first;
        final Pane addDevicePane = addDeviceViewPane.second;
        final EditText nameEditText = addDeviceView.findViewById(R.id.addDeviceNameTextEntry);
        final EditText macEditText = addDeviceView.findViewById(R.id.addDeviceMacTextEntry);

        // set default text to be shown.
        nameEditText.setText(defaultName);
        macEditText.setText(defaultMacAddress);

        // enter button click
        addDeviceView.findViewById(R.id.addDevicePaneEnterButton).setOnClickListener(v -> {
            String deviceName = nameEditText.getText().toString();
            String deviceMac = macEditText.getText().toString().toUpperCase();

            boolean valid = true;
            // check if mac address is good
            if (deviceMac.isEmpty() || !deviceMac.matches("(?:[A-F0-9]{2}:){5}[A-F0-9]{2}")) {
                // FIXME: this turns *black* for some reason. figure out why and fix.
                macEditText.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.empty_string_error)));
                valid = false;
            }
            // check if name is empty
            if (deviceName.isEmpty()) {
                // twas empty, turn it red
                nameEditText.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.empty_string_error)));
                valid = false;
            }
            // none of the checks tripped, that means it's valid, add stuff.
            if (valid) {
                // they were, add/update table and return to main plugin pane
                DeviceInfo existingEntry = DeviceListManager.getDevice(DeviceListManager.ListType.WHITELIST, deviceMac);
                String deviceUuid = null;
                if (existingEntry != null) deviceUuid = existingEntry.uuid;
                DeviceInfo deviceInfo = new DeviceInfo(deviceName, deviceMac, -1, false, deviceUuid);
                DeviceListManager.addOrUpdateDevice(DeviceListManager.ListType.WHITELIST, deviceInfo);
                if (deviceRow == null) {
                    // if row wasn't passed in, add a new one
                    addRowToTable(deviceInfo);
                } else {
                    // row was passed in, update that
                    ((TextView) deviceRow.findViewById(R.id.deviceRowNameText)).setText(defaultName);
                    ((TextView) deviceRow.findViewById(R.id.deviceRowMacAddressText)).setText(defaultMacAddress);
                }
                uiService.closePane(addDevicePane);
                uiService.showPane(returnPane, null);
            }
        });

        // cancel button click, just return to main plugin pane
        addDeviceView.findViewById(R.id.addDevicePaneCancelButton).setOnClickListener(v -> {
            uiService.closePane(addDevicePane);
            uiService.showPane(returnPane, null);
        });

        return addDevicePane;
    }

    private Pane constructDeviceInfoPane(DeviceInfo deviceInfo, TableRow row) {
        Pair<View, Pane> devInfoViewPane = getViewPane(R.layout.device_info_pane);
        final View deviceInfoView = devInfoViewPane.first;
        final Pane deviceInfoPane = devInfoViewPane.second;

        // populate text fields
        // TODO: get firstSeen stats from a leger where we store device scan hits.
        final Map<Integer, String> textInfoMap = Map.of(
                R.id.deviceInfoPaneNameText, deviceInfo.name,
                R.id.deviceInfoPaneMacText, deviceInfo.macAddress,
                R.id.deviceInfoPaneFirstSeenText, "NOT IMPLEMENTED",
                R.id.deviceInfoPaneFirstSeenByText, "NOT IMPLEMENTED",
                R.id.deviceInfoPaneLastSeenText, deviceInfo.seenTimeEpochMillis == -1 ? "-" : new Timestamp(deviceInfo.seenTimeEpochMillis).toString(),
                R.id.deviceInfoPaneLastSeenByText, deviceInfo.observerDeviceName == null ? "-" : deviceInfo.observerDeviceName
        );
        for (Map.Entry<Integer, String> entry : textInfoMap.entrySet())
            ((TextView) deviceInfoView.findViewById(entry.getKey())).setText(entry.getValue());

        // back button takes us back to main plugin pane
        deviceInfoView.findViewById(R.id.deviceInfoPaneBackButton).setOnClickListener(v -> {
            uiService.closePane(deviceInfoPane);
            uiService.showPane(TrackingPlugin.primaryPane, null);
        });

        // locate button
        deviceInfoView.findViewById(R.id.deviceInfoPaneLocateButton).setOnClickListener(v -> {
            // TODO: get this working
        });

        // delete button
        deviceInfoView.findViewById(R.id.deviceInfoPaneDeleteButton).setOnClickListener(v -> {
            // TODO: FIXME: "Are you sure?" prompt is essential.
            // remove data
            DeviceListManager.removeDevice(DeviceListManager.ListType.WHITELIST, deviceInfo.macAddress);

            // remove table row associated with device
            tableLayout.removeView(row);

            // back to main plugin pane
            uiService.closePane(deviceInfoPane);
            uiService.showPane(TrackingPlugin.primaryPane, null);
        });

        // edit button
        deviceInfoView.findViewById(R.id.deviceInfoPaneEditButton).setOnClickListener(v -> {
            uiService.closePane(deviceInfoPane);
            uiService.showPane(constructAddDevicePane(deviceInfo.name, deviceInfo.macAddress, deviceInfoPane, row), null);
        });
        return deviceInfoPane;
    }

    private Pair<View, Pane> getViewPane(int layoutId) {
        final View view = PluginLayoutInflater.inflate(context, layoutId);
        final Pane pane = new PaneBuilder(view)
                .setMetaValue(Pane.RELATIVE_LOCATION, Pane.Location.Default)
                // pane will take up 50% of screen width in landscape mode
                .setMetaValue(Pane.PREFERRED_WIDTH_RATIO, 0.5D)
                // pane will take up 50% of screen height in portrait mode
                .setMetaValue(Pane.PREFERRED_HEIGHT_RATIO, 0.5D)
                .build();
        return new Pair<>(view, pane);
    }
}
