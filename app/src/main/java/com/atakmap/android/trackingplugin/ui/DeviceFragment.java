package com.atakmap.android.trackingplugin.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.atakmap.android.trackingplugin.plugin.R;
import com.atakmap.android.trackingplugin.adapter.DeviceAdapter;
import com.atakmap.android.trackingplugin.model.DeviceModel;
import java.util.ArrayList;
import java.util.List;

public class DeviceFragment extends Fragment {
    private RecyclerView recyclerView;
    private DeviceAdapter adapter;
    private List<DeviceModel> deviceList;
    private static DeviceFragment instance;

    public static DeviceFragment getInstance() {
        return instance;
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.devices_layout, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        deviceList = new ArrayList<>();
        adapter = new DeviceAdapter(deviceList);
        recyclerView.setAdapter(adapter);

        return view;
    }


    public void addDevice(String name, String mac, int rssi) {
        DeviceModel newDevice = new DeviceModel(name, mac, rssi);
        deviceList.add(newDevice);
        adapter.notifyDataSetChanged();
    }

}
