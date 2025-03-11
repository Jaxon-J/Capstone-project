package com.atakmap.android.trackingplugin;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.viewpager2.widget.ViewPager2;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.trackingplugin.plugin.R;
import com.atakmap.android.trackingplugin.ui.TabViewPagerAdapter;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

public class TrackingPluginDropDownReceiver extends DropDownReceiver {
    private final Context pluginContext;
    private final View mainView;
    private final List<Pair<String, Integer>> tabInfo = List.of(
            new Pair<>("Tracking", R.layout.tracking_layout),
            new Pair<>("Devices", R.layout.devices_layout),
            new Pair<>("Sensors", R.layout.sensors_layout),
            new Pair<>("Debug", R.layout.debug_layout));
    public static BluetoothReceiver btReceiver;

    protected TrackingPluginDropDownReceiver(final MapView mapView, final Context context) {
        super(mapView);
        this.pluginContext = context;
        // set up UI, set main layout to be viewed when SHOW_PLUGIN action is triggered
        mainView = PluginLayoutInflater.inflate(context, R.layout.main_layout, null);
        // set up all receivers/UI responses here

        // tabs logic
        TabLayout tabLayout = mainView.findViewById(R.id.tabLayout);
        ViewPager2 pager = mainView.findViewById(R.id.viewPager);
        pager.setAdapter(new TabViewPagerAdapter(pluginContext, tabInfo));
        // set height as the maximum height of any tab
        pager.post(() -> {
            int height = 0;
            for (int i = 0; i < tabInfo.size(); i++) {
                View view = pager.getChildAt(i);
                if (view != null) {
                    view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    height = Math.max(height, view.getMeasuredHeight());
                }
            }
            ViewGroup.LayoutParams params = pager.getLayoutParams();
            params.height = height;
            pager.setLayoutParams(params);
        });
        TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, pager,
                (tab, position) -> tab.setText(tabInfo.get(position).first));
        mediator.attach();

        btReceiver = new BluetoothReceiver(pluginContext);

        // debug marker placement
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case ACTIONS.SHOW_PLUGIN: // UI ENTRY POINT
                if (!isClosed()) unhideDropDown();
                // showDropDown has several more overloads. if we want to listen to
                // resize/open/close events,
                // we implement DropDown.StateChangeListener and add "false, this" parameters here.
                showDropDown(mainView, HALF_WIDTH, FULL_WIDTH, FULL_WIDTH, HALF_HEIGHT);
                break;
        }
    }

    @Override
    protected void disposeImpl() {
        if (btReceiver != null) {
            AtakBroadcast.getInstance().unregisterReceiver(btReceiver);
            btReceiver = null;
        }
    }

    public static final class ACTIONS {
        public static final String SHOW_PLUGIN = "com.atakmap.android.trackingplugin.SHOW_PLUGIN";
    }

    // If this ever gets refactored out, it needs the pluginContext and tabInfo passed in as
    // parameters

}