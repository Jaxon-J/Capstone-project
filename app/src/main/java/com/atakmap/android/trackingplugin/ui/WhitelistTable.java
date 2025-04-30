package com.atakmap.android.trackingplugin.ui;

import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.trackingplugin.Constants;
import com.atakmap.android.trackingplugin.DeviceInfo;
import com.atakmap.android.trackingplugin.DeviceStorageManager;
import com.atakmap.android.trackingplugin.plugin.R;
import com.atakmap.android.trackingplugin.plugin.TrackingPlugin;
import com.atakmap.android.util.ATAKUtilities;

import java.util.List;
import java.util.Map;

import gov.tak.api.ui.IHostUIService;
import gov.tak.api.ui.Pane;
import gov.tak.api.ui.PaneBuilder;

public class WhitelistTable implements DeviceStorageManager.DeviceListChangeListener {
    private static final String TAG = Constants.createTag(WhitelistTable.class);
    private final int ROW_DEVICE_UUID_KEY = 538462893;
    private final int FIELD_INVALID_MESSAGE_KEY = 238472837;
    private final IHostUIService uiService;
    private final View tabView;

    public WhitelistTable(IHostUIService uiService, View tabView) {
        this.uiService = uiService;
        this.tabView = tabView;
        DeviceStorageManager.addChangeListener(DeviceStorageManager.ListType.WHITELIST, this);
    }


    // table setup


    public void setup() {
        setTableHeight();
        refreshTable();
        tabView.findViewById(R.id.addDeviceButton).setOnClickListener(v -> {
            uiService.closePane(TrackingPlugin.primaryPane);
            uiService.showPane(constructAddDevicePane(null), null);
        });
    }

    private void setTableHeight() {
        // this is very layout-dependent so if the layout changes, this probably will have to as well in order to work.
        ScrollView tableContainer = tabView.findViewById(R.id.whitelistTableScrollContainer);
        tableContainer.post(() -> {
            View addDeviceButton = tabView.findViewById(R.id.addDeviceButton);
            View tableHeader = tabView.findViewById(R.id.deviceTableHeaderRowTable);
            ViewGroup.MarginLayoutParams buttonMargins = (ViewGroup.MarginLayoutParams) addDeviceButton.getLayoutParams();
            int tableHeight = tabView.getMeasuredHeight();
            tableHeight -= tableHeader.getMeasuredHeight();
            tableHeight -= buttonMargins.topMargin;
            tableHeight -= addDeviceButton.getMeasuredHeight();
            tableHeight -= buttonMargins.bottomMargin;

            ViewGroup.LayoutParams params = tableContainer.getLayoutParams();
            params.height = tableHeight;
            tableContainer.setLayoutParams(params);
        });
    }

    public void refreshTable() {
        TableLayout tableLayout = tabView.findViewById(R.id.whitelistDeviceTable);
        tableLayout.removeAllViews();

        // loop through all UUID's in the whitelist
        for (String uuid : DeviceStorageManager.getUuids(DeviceStorageManager.ListType.WHITELIST)) {
            final TableRow row = (TableRow) LayoutInflater.from(tabView.getContext())
                    .inflate(R.layout.device_table_row_layout, tableLayout, false);
            DeviceInfo deviceInfo = DeviceStorageManager.getDevice(DeviceStorageManager.ListType.WHITELIST, uuid);
            if (deviceInfo == null) {
                Log.w(TAG, "Device must be in whitelist before adding it to the table. Invalid UUID: " + uuid);
                return;
            }
            // associating the row to the device via tag
            row.setTag(ROW_DEVICE_UUID_KEY, deviceInfo.uuid);

            // set the text fields
            ((TextView) row.findViewById(R.id.deviceRowNameText)).setText(deviceInfo.name);
            ((TextView) row.findViewById(R.id.deviceRowMacAddressText)).setText(deviceInfo.macAddress);

            // checkbox will set visibility
            row.findViewById(R.id.deviceRowVisibilityCheckbox).setOnClickListener(v -> {
                // TODO: SET VISIBILITY HERE.
            });

            // add row click behavior
            row.setOnClickListener((View v) -> {
                uiService.closePane(TrackingPlugin.primaryPane);
                uiService.showPane(constructDeviceInfoPane((String) row.getTag(ROW_DEVICE_UUID_KEY)), null);
            });

            // row is prepared, add it to the table
            tableLayout.addView(row);
        }
    }


    // add device pane


    private Pane constructAddDevicePane(@Nullable String uuid) {
        // TODO: this may be incredibly inefficient. not a big deal in the grand scheme, but if it becomes an issue,
        //  look into re-using panes and utilizing the IPaneLifetimeListener in the showPane calls instead.
        final Pair<View, Pane> addDeviceViewPane = getViewPane(R.layout.add_device_pane);
        final View addDeviceView = addDeviceViewPane.first;
        final Pane addDevicePane = addDeviceViewPane.second;
        final EditText nameEditText = addDeviceView.findViewById(R.id.addDeviceNameTextEntry);
        final EditText macAddressEditText = addDeviceView.findViewById(R.id.addDeviceMacTextEntry);

        // set initial state
        String defaultName = "";
        String defaultMacAddress = "";
        if (uuid != null) {
            DeviceInfo whitelistEntry = DeviceStorageManager.getDevice(DeviceStorageManager.ListType.WHITELIST, uuid);
            if (whitelistEntry == null) {
                Log.w(TAG, "Tried to bring up Add Device window with UUID that is not in whitelist. UUID: " + uuid);
            } else {
                defaultName = whitelistEntry.name;
                defaultMacAddress = whitelistEntry.macAddress;
            }
        }

        nameEditText.setText(defaultName);
        macAddressEditText.setText(defaultMacAddress);
        setAddDeviceFieldValidators(defaultName, defaultMacAddress, nameEditText, macAddressEditText);

        // enter button
        addDeviceView.findViewById(R.id.addDevicePaneEnterButton).setOnClickListener(v -> {
            String enteredName = nameEditText.getText().toString();
            String enteredMacAddress = macAddressEditText.getText().toString().toUpperCase();

            boolean caughtInvalid = false;

            // this is an expensive check (no duplicate mac addresses), doing it here to avoid checking on every keystroke.
            // no entry in whitelist (currentDevice info is null) -> check against all existing MAC addresses
            // is in whitelist already (currentDeviceInfo is NOT null) -> check against all MAC addresses except the one it already has
            DeviceInfo currentDeviceInfo = DeviceStorageManager.getDevice(DeviceStorageManager.ListType.WHITELIST, uuid);
            if ((currentDeviceInfo == null || !enteredMacAddress.equals(currentDeviceInfo.macAddress))
                    && DeviceStorageManager.getUuid(DeviceStorageManager.ListType.WHITELIST, enteredMacAddress) != null) {
                macAddressEditText.setTag(FIELD_INVALID_MESSAGE_KEY, "MAC address is already in use.");
            }

            // grab error messages that were set by the validators. check them here.
            String nameInvalidText = (String) nameEditText.getTag(FIELD_INVALID_MESSAGE_KEY);
            String macAddressInvalidText = (String) macAddressEditText.getTag(FIELD_INVALID_MESSAGE_KEY);
            if (nameInvalidText != null) {
                nameEditText.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(tabView.getContext(), R.color.empty_string_error)));
                uiService.showToast(nameInvalidText);
                caughtInvalid = true;
            }
            if (macAddressInvalidText != null) {
                macAddressEditText.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(tabView.getContext(), R.color.empty_string_error)));
                uiService.showToast(macAddressInvalidText);
                caughtInvalid = true;
            }
            if (caughtInvalid) return;
            // validate mac address field


            DeviceInfo enteredDeviceInfo = new DeviceInfo(enteredName, enteredMacAddress, -1, false, uuid);
            // this triggers the onDeviceListChange, no need to manually refresh the table here.
            DeviceStorageManager.addOrUpdateDevice(DeviceStorageManager.ListType.WHITELIST, enteredDeviceInfo);
            uiService.closePane(addDevicePane);
            // TODO: showPane could be passed in to go back to where user was previously.
            uiService.showPane(TrackingPlugin.primaryPane, null);
        });

        addDeviceView.findViewById(R.id.addDevicePaneCancelButton).setOnClickListener(v -> {
            uiService.closePane(addDevicePane);
            uiService.showPane(TrackingPlugin.primaryPane, null);
        });

        return addDevicePane;
    }

    public void setAddDeviceFieldValidators(String defaultName, String defaultMacAddress, EditText nameField, EditText macAddressField) {
        TextWatcher nameFieldValidator = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // name has to exist, that's basically it.
                if (s.length() == 0) {
                    nameField.setTag(FIELD_INVALID_MESSAGE_KEY, "Please enter a name.");
                } else {
                    nameField.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(tabView.getContext(), R.color.white)));
                    nameField.setTag(FIELD_INVALID_MESSAGE_KEY, null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        TextWatcher macAddressFieldValidator = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // mac address needs to be an actual mac address
                if (s.length() == 0 || !s.toString().toUpperCase().matches("(?:[A-F0-9]{2}:){5}[A-F0-9]{2}")) {
                    macAddressField.setTag(FIELD_INVALID_MESSAGE_KEY, "Please enter valid MAC address.");
                } else {
                    macAddressField.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(tabView.getContext(), R.color.white)));
                    macAddressField.setTag(FIELD_INVALID_MESSAGE_KEY, null);
                }

            }

            @Override
            public void afterTextChanged(Editable s) {}
        };
        nameField.addTextChangedListener(nameFieldValidator);
        macAddressField.addTextChangedListener(macAddressFieldValidator);
        nameFieldValidator.onTextChanged(defaultName, 0, 0, 0);
        macAddressFieldValidator.onTextChanged(defaultMacAddress, 0, 0, 0);
    }


    // device info pane


    private Pane constructDeviceInfoPane(String uuid) {
        final Pair<View, Pane> deviceInfoViewPane = getViewPane(R.layout.device_info_pane);
        final View deviceInfoView = deviceInfoViewPane.first;
        final Pane deviceInfoPane = deviceInfoViewPane.second;
        final DeviceInfo deviceInfo = DeviceStorageManager.getDevice(DeviceStorageManager.ListType.WHITELIST, uuid);
        if (deviceInfo == null) {
            Log.w(TAG, "Tried to bring up a device information pane for a device that does not exist. UUID: " + uuid);
            return null;
        }
        final Map<Integer, String> textInfoMap = Map.of(
                R.id.deviceInfoPaneNameText, deviceInfo.name,
                R.id.deviceInfoPaneMacText, deviceInfo.macAddress,
                R.id.deviceInfoPaneFirstSeenText, "NOT IMPLEMENTED",
                R.id.deviceInfoPaneFirstSeenByText, "NOT IMPLEMENTED",
                R.id.deviceInfoPaneLastSeenText, "NOT IMPLEMENTED",
                R.id.deviceInfoPaneLastSeenByText, "NOT IMPLEMENTED"
        );
        for (Map.Entry<Integer, String> entry : textInfoMap.entrySet())
            ((TextView) deviceInfoView.findViewById(entry.getKey())).setText(entry.getValue());

        // back button
        deviceInfoView.findViewById(R.id.deviceInfoPaneBackButton).setOnClickListener(v -> {
            uiService.closePane(deviceInfoPane);
            uiService.showPane(TrackingPlugin.primaryPane, null);
        });

        // locate button
        deviceInfoView.findViewById(R.id.deviceInfoPaneLocateButton).setOnClickListener(v -> {
            // TODO: get this working
            MapItem deviceMapItem = MapView.getMapView().getRootGroup().deepFindUID(deviceInfo.uuid);
            ATAKUtilities.scaleToFit(deviceMapItem);
        });

        // delete button
        deviceInfoView.findViewById(R.id.deviceInfoPaneDeleteButton).setOnClickListener(v -> {
            // TODO: FIXME: "Are you sure?" prompt is essential.
            // (triggers table refresh, see onDeviceListChange)
            DeviceStorageManager.removeDevice(DeviceStorageManager.ListType.WHITELIST, deviceInfo.uuid);

            // back to main plugin pane
            uiService.closePane(deviceInfoPane);
            uiService.showPane(TrackingPlugin.primaryPane, null);
        });

        // edit button
        deviceInfoView.findViewById(R.id.deviceInfoPaneEditButton).setOnClickListener(v -> {
            uiService.closePane(deviceInfoPane);
            uiService.showPane(constructAddDevicePane(deviceInfo.uuid), null);
        });

        return deviceInfoPane;
    }


    // util/misc methods


    @Override
    public void onDeviceListChange(List<DeviceInfo> devices) {
        // TODO: in the future, see if we can be a bit more granular with updates
        //  right now it's just full purge and repopulate everything.
        refreshTable();
    }

    private Pair<View, Pane> getViewPane(int layoutId) {
        final View view = PluginLayoutInflater.inflate(tabView.getContext(), layoutId);
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

/*
set up functionality of table view on setup()

every time addDevicePane is shown, it is passed a Uuid that tags what device needs to be changed.

every time deviceInfoPane is shown, it is passed a Uuid that tags what device information needs to be grabbed.
fresh from DeviceStorageManager every time.

 */