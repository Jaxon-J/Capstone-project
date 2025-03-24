package com.atakmap.android.trackingplugin.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.atakmap.android.trackingplugin.plugin.R;
import com.atakmap.android.trackingplugin.model.DeviceModel;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
    private List<DeviceModel> deviceList;

    public DeviceAdapter(List<DeviceModel> deviceList) {
        this.deviceList = deviceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.devices_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceModel device = deviceList.get(position);
        holder.deviceName.setText(device.getName());
        holder.deviceMac.setText(device.getMacAddress());
        holder.deviceRssi.setText("RSSI: " + device.getRssi());
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public void updateList(List<DeviceModel> newList) {
        deviceList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName, deviceMac, deviceRssi;

        public ViewHolder(View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.device_name);
            deviceMac = itemView.findViewById(R.id.device_mac);
            deviceRssi = itemView.findViewById(R.id.device_rssi);
        }
    }
}

