package com.atakmap.android.trackingplugin.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.trackingplugin.comms.DeviceCotDispatcher;
import com.atakmap.android.trackingplugin.plugin.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.tak.api.ui.IHostUIService;

public class SensorsTable implements Contacts.OnContactsChangedListener {
    private static final Map<String, String> nameUidSensorMap = new HashMap<>();
    private static final Set<String> contactUids = new HashSet<>();
    private final IHostUIService uiService;
    private static boolean heightSet = false;
    private final View tabView;

    public SensorsTable(IHostUIService uiService, View tabView) {
        this.uiService = uiService;
        this.tabView = tabView;
        for (Contact contact : Contacts.getInstance().getAllContacts()) {
            contactUids.add(contact.getUid());
        }
        Contacts.getInstance().addListener(this);
    }

    public void refreshTable() {
        setTableHeight();
        TableLayout tableLayout = tabView.findViewById(R.id.sensorTable);
        tableLayout.removeAllViews();
        for (Map.Entry<String, String> nameUid : nameUidSensorMap.entrySet()) {
            TableRow row = (TableRow) LayoutInflater.from(tabView.getContext())
                    .inflate(R.layout.sensor_table_row_layout, tableLayout, false);
            ((TextView) row.findViewById(R.id.sensorTableNameLabel)).setText(nameUid.getKey());
            row.findViewById(R.id.sensorTableRequestWhitelistButton).setOnClickListener(v ->
                    DeviceCotDispatcher.sendWhitelistRequest(nameUid.getValue()));
            tableLayout.addView(row);
        }
    }

    private void setTableHeight() {
        ScrollView tableContainer = tabView.findViewById(R.id.sensorTableScrollContainer);
        tableContainer.post(() -> {
            View tableHeader = tabView.findViewById(R.id.sensorTableHeaderRowTable);
            Button refreshButton = tabView.findViewById(R.id.sensorRefreshButton);
            ViewGroup.MarginLayoutParams buttonMargins = (ViewGroup.MarginLayoutParams) refreshButton.getLayoutParams();
            int tableHeight =
                    tabView.getMeasuredHeight() -
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
        refreshTable();
    }

    public void userInvokedRefresh() {
        onContactsSizeChange(Contacts.getInstance());
    }

    @Override
    public void onContactsSizeChange(Contacts contacts) {
        List<String> newUids = new ArrayList<>();
        List<String> allContactUids = contacts.getAllContactUuids();
        for (String uid : allContactUids) {
            if (!contactUids.contains(uid)) {
                newUids.add(uid);
            }
        }
        contactUids.addAll(newUids);
        DeviceCotDispatcher.discoverPluginContacts(newUids.toArray(new String[0]));
        for (String uid : contactUids) {
            if (!allContactUids.contains(uid)) {
                contactUids.remove(uid);
            }
        }
        refreshTable();
    }

    @Override
    public void onContactChanged(String uuid) {
        if (!nameUidSensorMap.containsKey(uuid)) return;
        nameUidSensorMap.put(uuid, Contacts.getInstance().getContactByUuid(uuid).getName());
        refreshTable();
    }
}
