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
        addDeviceButton.setOnClickListener(v ->
            uiService.showPane(constructAddDevicePane("", "", TrackingPlugin.primaryPane),
                    null)
        );
    }

    public void addOrUpdateWhitelist(DeviceInfo deviceInfo) {
        // whether or not it exists already, we need to update the data.
        DeviceListManager.addOrUpdateDevice(DeviceListManager.ListType.WHITELIST, deviceInfo);
        // go through all rows (skipping dummy row, start at i=1) to see if mac addr already exists.
        for (int i = 1; i < tableLayout.getChildCount(); i++) {
            final TableRow existingRow = (TableRow) tableLayout.getChildAt(i);
            final String existingMac = ((TextView) existingRow.findViewById(R.id.deviceRowMacAddressText)).getText().toString();
            if (deviceInfo.macAddress.equals(existingMac)) {
                // does exist, update the row and return
                ((TextView) existingRow.findViewById(R.id.deviceRowNameText)).setText(deviceInfo.name);
                return;
            }
        }
        // device doesn't exist in the table. add it to both table and whitelist.
        addRowToTable(deviceInfo);
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
        row.setOnClickListener((View v) ->
            uiService.showPane(constructDeviceInfoPane(deviceInfo), null));

        // row is prepared, add it to the table
        tableLayout.addView(row);
    }

    private Pane constructAddDevicePane(String name, String macAddress, Pane returnPane) {
        // get all the objects we need for behavior.
        final Pair<View, Pane> addDeviceViewPane = getViewPane(R.layout.add_device_pane);
        final View addDeviceView = addDeviceViewPane.first;
        final EditText nameEditText = addDeviceView.findViewById(R.id.addDeviceNameTextEntry);
        final EditText macEditText = addDeviceView.findViewById(R.id.addDeviceMacTextEntry);

        // set default text to be shown.
        nameEditText.setText(name);
        macEditText.setText(macAddress);

        // enter button click
        addDeviceView.findViewById(R.id.addDevicePaneEnterButton).setOnClickListener(v -> {
            String deviceName = nameEditText.getText().toString();
            String deviceMac = macEditText.getText().toString();

            // check if both fields are populated
            if (!deviceName.isEmpty() && !deviceMac.isEmpty()) {
                // they were, add/update table and return to main plugin pane
                addOrUpdateWhitelist(new DeviceInfo(deviceName, deviceMac, -1, false));
                uiService.showPane(returnPane, null);
            } else {
                // check which field wasn't populated, turn it red, don't move on
                if (deviceName.isEmpty()) {
                    nameEditText.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.empty_string_error)));
                }
                if (deviceMac.isEmpty()) {
                    macEditText.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(context, R.color.empty_string_error)));
                }
            }
        });

        // cancel button click, just return to main plugin pane
        addDeviceView.findViewById(R.id.addDevicePaneCancelButton).setOnClickListener(v ->
                uiService.showPane(returnPane, null));

        return addDeviceViewPane.second;
    }

    private Pane constructDeviceInfoPane(DeviceInfo deviceInfo) {
        Pair<View, Pane> devInfoViewPane = getViewPane(R.layout.device_info_pane);
        final View deviceInfoView = devInfoViewPane.first;

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
        deviceInfoView.findViewById(R.id.deviceInfoPaneBackButton).setOnClickListener(v ->
                uiService.showPane(TrackingPlugin.primaryPane, null));

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
            for (int i = 1; i < tableLayout.getChildCount(); i++) {
                TableRow row = (TableRow) tableLayout.getChildAt(i);
                String rowMacAddress = ((TextView) row.findViewById(R.id.deviceRowMacAddressText)).getText().toString();
                if (deviceInfo.macAddress.equals(rowMacAddress)) {
                    tableLayout.removeViewAt(i);
                    break;
                }
            }
            // back to main plugin pane
            uiService.showPane(TrackingPlugin.primaryPane, null);
        });

        // edit button
        final Pane deviceInfoPane = devInfoViewPane.second;
        deviceInfoView.findViewById(R.id.deviceInfoPaneEditButton).setOnClickListener(v ->
            uiService.showPane(constructAddDevicePane(deviceInfo.name, deviceInfo.macAddress, deviceInfoPane), null));
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
