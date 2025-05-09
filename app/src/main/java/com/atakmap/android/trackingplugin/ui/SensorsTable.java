package com.atakmap.android.trackingplugin.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.atakmap.android.contact.Contacts;
import com.atakmap.android.trackingplugin.comms.DeviceCotDispatcher;
import com.atakmap.android.trackingplugin.plugin.R;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class SensorsTable implements Contacts.OnContactsChangedListener {
    private static final Map<String, String> nameUidSensorMap = new HashMap<>();
    private final View tabView;

    public SensorsTable(View tabView) {
        this.tabView = tabView;
        Contacts.getInstance().addListener(this);
    }

    public void refreshUi() {
        tabView.post(() -> {
            TableLayout tableLayout = tabView.findViewById(R.id.sensorTable);
            tableLayout.removeAllViews();
            for (Map.Entry<String, String> nameUid : nameUidSensorMap.entrySet()) {
                TableRow row = (TableRow) LayoutInflater.from(tabView.getContext())
                        .inflate(R.layout.sensor_table_row_layout, tableLayout, false);
                ((TextView) row.findViewById(R.id.sensorTableNameLabel)).setText(nameUid.getKey());
                row.findViewById(R.id.sensorTableRequestWhitelistButton)
                        .setOnClickListener(v -> DeviceCotDispatcher.sendWhitelistRequest(nameUid.getValue()));
                tableLayout.addView(row);
            }
            setTableHeight();
        });
    }

    private void setTableHeight() {
        ScrollView tableContainer = tabView.findViewById(R.id.sensorTableScrollContainer);
        tableContainer.post(() -> {
            View tableHeader = tabView.findViewById(R.id.sensorTableHeaderRowTable);
            Button refreshButton = tabView.findViewById(R.id.sensorRefreshButton);
            ViewGroup.MarginLayoutParams buttonMargins = (ViewGroup.MarginLayoutParams) refreshButton.getLayoutParams();
            int tableHeight = tabView.getMeasuredHeight() -
                    tableHeader.getMeasuredHeight() -
                    buttonMargins.topMargin -
                    refreshButton.getMeasuredHeight() -
                    buttonMargins.bottomMargin;
            ViewGroup.LayoutParams params = tableContainer.getLayoutParams();
            params.height = tableHeight;
            tableContainer.setLayoutParams(params);
        });
    }

    public void addSensor(String name, String uid) {
        nameUidSensorMap.put(name, uid);
        refreshUi();
    }

    public void refreshData() {
        nameUidSensorMap.clear();
        DeviceCotDispatcher.discoverPluginContacts(null);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                refreshUi();
            }
        }, 1000);
    }

    @Override
    public void onContactsSizeChange(Contacts contacts) {}

    @Override
    public void onContactChanged(String uuid) {
        if (!nameUidSensorMap.containsKey(uuid)) return;
        nameUidSensorMap.put(uuid, Contacts.getInstance().getContactByUuid(uuid).getName());
        refreshUi();
    }
}
