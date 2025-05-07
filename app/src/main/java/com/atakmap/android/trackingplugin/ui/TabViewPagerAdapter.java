package com.atakmap.android.trackingplugin.ui;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.trackingplugin.BluetoothReceiver;
import com.atakmap.android.trackingplugin.Constants;
import com.atakmap.android.trackingplugin.DeviceStorageManager;
import com.atakmap.android.trackingplugin.comms.DeviceCotDispatcher;
import com.atakmap.android.trackingplugin.plugin.R;
import com.atakmap.android.trackingplugin.plugin.TrackingPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import gov.tak.api.ui.IHostUIService;

public class TabViewPagerAdapter extends RecyclerView.Adapter<TabViewPagerAdapter.TabViewHolder> {
    private static final String TAG = Constants.createTag(TabViewPagerAdapter.class);
    private final Context context;
    private final IHostUIService uiService;

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
    @SuppressLint("MissingPermission")
    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: " + Constants.TAB_LAYOUTS.get(position).first);
        // This switch is called: 1) when initialized, 2) every time tab is switched to.
        switch (holder.tabName) {
            case Constants.TRACKING_TABNAME: {
                EditText pollRateEditText = holder.itemView.findViewById(R.id.pollRateEditTextNumber);
                pollRateEditText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        try {
                            int newPollSeconds = Integer.parseInt(s.toString());
                            BluetoothReceiver.POLL_RATE_MILLIS = newPollSeconds * 1000;
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Poll time is not a number, did not update. Current: " + BluetoothReceiver.POLL_RATE_MILLIS);
                        }
                    }
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void afterTextChanged(Editable s) {}
                });
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
                if (TrackingPlugin.whitelistTable == null) {
                    TrackingPlugin.whitelistTable = new WhitelistTable(uiService, holder.itemView);
                    TrackingPlugin.whitelistTable.setup();
                }
                break;
            }
            case Constants.SENSORS_TABNAME: {
                DeviceCotDispatcher.discoverPluginContacts();
                if (TrackingPlugin.sensorsTable == null) {
                    TrackingPlugin.sensorsTable = new SensorsTable(uiService, holder.itemView);
                    TrackingPlugin.sensorsTable.setup();
                }
                break;
            }
            case Constants.DEBUG_TABNAME: {
                // ble scan button
                holder.itemView.findViewById(R.id.bleScanDebugButton)
                        .setOnClickListener(v -> {
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
                holder.itemView.findViewById(R.id.debugLogMacButton).setOnClickListener(v -> {
                    BluetoothAdapter btAdapter;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
                        if (manager == null) {
                            Log.e(TAG, "Could not get bluetooth manager. Could be the case that Bluetooth is not supported on this device?");
                            return;
                        }
                        btAdapter = manager.getAdapter();
                    } else {
                        btAdapter = BluetoothAdapter.getDefaultAdapter();
                    }
                    if (btAdapter == null) {
                        Log.e(TAG, "Could not get bluetooth adapter for some reason.");
                        return;
                    }
                    BluetoothLeScanner scanner = btAdapter.getBluetoothLeScanner();
                    Map<String, String> foundDevices = new HashMap<>();
                    ScanCallback scanCallback = new ScanCallback() {
                        @Override
                        public void onScanResult(int callbackType, ScanResult result) {
                            BluetoothDevice device = result.getDevice();
                            String macAddress = device.getAddress();
                            String name = device.getName();
                            String existingName = foundDevices.containsKey(macAddress) ? foundDevices.get(macAddress) : "";
                            assert existingName != null;
                            if (!foundDevices.containsKey(macAddress) || existingName.isEmpty()) {
                                if (name == null) name = "";
                                foundDevices.put(macAddress, name);
                            }
                        }
                    };
                    scanner.startScan(scanCallback);
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            scanner.stopScan(scanCallback);
                            StringBuilder sb = new StringBuilder();
                            for (Map.Entry<String, String> entry : foundDevices.entrySet()) {
                                String name = entry.getValue();
                                if (name.length() >= 12) name = name.substring(0, 12);
                                else if (name.isEmpty()) name = "unknown";
                                sb.append(String.format("    %-12s : %s\n", name, entry.getKey()));
                            }
                            Log.d(TAG, "Found Devices:\n" + sb);
                        }
                    }, 1000);
                });
                // clear whitelist button
                holder.itemView.findViewById(R.id.debugClearWhitelistButton)
                        .setOnClickListener(v -> DeviceStorageManager.clearList(DeviceStorageManager.ListType.WHITELIST));
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
}