package com.atakmap.android.trackingplugin;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager2.widget.ViewPager2;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.trackingplugin.plugin.R;
import com.atakmap.android.trackingplugin.ui.TabViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class TrackingPluginDropDownReceiver extends DropDownReceiver {
    private static final String TAG = Constants.createTag(TrackingPluginDropDownReceiver.class);
    private final Context pluginContext;
    private final View mainView;
    public BluetoothReceiver btReceiver;

    /// Entry point to plugin.
    protected TrackingPluginDropDownReceiver(final MapView mapView, final Context context) {
        super(mapView);
        this.pluginContext = context;
        // get extract main_layout and set it to a view, to be shown on "SHOW_PLUGIN" event
        mainView = PluginLayoutInflater.inflate(context, R.layout.main_layout, null);

        // initialize device data repository
        DeviceListManager.initialize(context);
        // initialize bluetooth scanner
        btReceiver = new BluetoothReceiver(context);

        // logic for setting up the tabs via TabLayout, ViewPager2, TabLayoutMediator
        TabLayout tabLayout = mainView.findViewById(R.id.tabLayout);
        ViewPager2 pager = mainView.findViewById(R.id.viewPager);
        pager.setAdapter(new TabViewPagerAdapter(context, btReceiver));
        // set ViewPager2 component height as the maximum height of all tab layouts, so nothing gets cut off.
        pager.post(() -> {
            int height = 0;
            for (int i = 0; i < Constants.TAB_COUNT; i++) {
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
        // TabLayoutMediator magic, where the names are actually set
        TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, pager, (tab, position) -> tab.setText(Constants.TAB_LAYOUTS.get(position).first));
        mediator.attach();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case ACTIONS.SHOW_PLUGIN:
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
}