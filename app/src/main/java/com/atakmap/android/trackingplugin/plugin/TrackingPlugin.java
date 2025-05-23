
package com.atakmap.android.trackingplugin.plugin;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.res.ResourcesCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.atak.plugins.impl.PluginContextProvider;
import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.trackingplugin.BluetoothReceiver;
import com.atakmap.android.trackingplugin.Constants;
import com.atakmap.android.trackingplugin.DeviceInfo;
import com.atakmap.android.trackingplugin.DeviceStorageManager;
import com.atakmap.android.trackingplugin.comms.DeviceCotListener;
import com.atakmap.android.trackingplugin.ui.SensorsTable;
import com.atakmap.android.trackingplugin.ui.TabViewPagerAdapter;
import com.atakmap.android.trackingplugin.ui.WhitelistTable;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.plugin.IPlugin;
import gov.tak.api.plugin.IServiceController;
import gov.tak.api.ui.IHostUIService;
import gov.tak.api.ui.Pane;
import gov.tak.api.ui.PaneBuilder;
import gov.tak.api.ui.ToolbarItem;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.ui.MotionEvent;

public class TrackingPlugin implements IPlugin {

    public static final String TAG = Constants.createTag(TrackingPlugin.class);
    IServiceController serviceController;
    Context pluginContext;
    IHostUIService uiService;
    ToolbarItem toolbarItem;
    public static Pane primaryPane;
    BluetoothReceiver btReceiver;
    ViewPager2 pager;
    View mainTemplate;
    TabLayout tabLayout;
    boolean primaryPaneInitialized = false;
    public static WhitelistTable whitelistTable; // FIXME: completely re-architect the plugin so we don't have a memory leak here pls thx
    public static SensorsTable sensorsTable; // FIXME: completely re-architect the plugin so we don't have a memory leak here pls thx

    public TrackingPlugin(IServiceController serviceController) {
        Log.d(TAG, "Tracking plugin instantiated");
        this.serviceController = serviceController;
        final PluginContextProvider ctxProvider = serviceController
                .getService(PluginContextProvider.class);
        uiService = serviceController.getService(IHostUIService.class);
        if (ctxProvider == null || uiService == null) {
            throw new RuntimeException("Could not retrieve services necessary to run the plugin.");
        }
        pluginContext = ctxProvider.getPluginContext();
        pluginContext.setTheme(R.style.ATAKPluginTheme);

        // create button that will be added to the toolbar in onStart
        toolbarItem = new ToolbarItem.Builder(
                pluginContext.getString(R.string.app_name),
                MarshalManager.marshal(
                        ResourcesCompat.getDrawable(pluginContext.getResources(), R.drawable.ic_launcher, null),
                        Drawable.class,
                        Bitmap.class))
                .setListener((ToolbarItem item, MotionEvent event) -> showPane())
                .build();
    }

    @Override
    public void onStart() {
        Log.d(TAG, "Tracking plugin started");
        // COMMS:
        DeviceCotListener.initialize();

        // UI:
        uiService.addToolbarItem(toolbarItem);

        // BT:
        btReceiver = new BluetoothReceiver(pluginContext);
        AtakBroadcast.DocumentedIntentFilter btIntentFilter = new AtakBroadcast.DocumentedIntentFilter();
        btIntentFilter.addAction(BluetoothReceiver.ACTIONS.BLE_START_SCAN);
        btIntentFilter.addAction(BluetoothReceiver.ACTIONS.BLE_STOP_SCAN);
        AtakBroadcast.getInstance().registerReceiver(btReceiver, btIntentFilter);

        List<DeviceInfo> whitelist = DeviceStorageManager.getDeviceList(DeviceStorageManager.ListType.WHITELIST);
        for (DeviceInfo deviceInfo : whitelist)
            if (WhitelistTable.visibilityMap.containsKey(deviceInfo.uuid))
                WhitelistTable.visibilityMap.put(deviceInfo.uuid, false);
    }

    @Override
    public void onStop() {
        // if ui is up, take it down or else the old plugin will stick around.
        if (primaryPane != null && uiService.isPaneVisible(primaryPane)) {
            uiService.closePane(primaryPane);
        }
        uiService.removeToolbarItem(toolbarItem);
        whitelistTable = null;
        sensorsTable = null;
        if (btReceiver != null) {
            AtakBroadcast.getInstance().sendBroadcast(new Intent(BluetoothReceiver.ACTIONS.BLE_STOP_SCAN));
            AtakBroadcast.getInstance().unregisterReceiver(btReceiver);
            btReceiver = null;
        }
        DeviceCotListener.uninitialize();
        Log.d(TAG, "Tracking plugin stopped");
    }

    private void setupPrimaryPane() {
        mainTemplate = PluginLayoutInflater.inflate(pluginContext, R.layout.main_layout, null);
        pager = mainTemplate.findViewById(R.id.viewPager);
        tabLayout = mainTemplate.findViewById(R.id.tabLayout);
        pager.setAdapter(new TabViewPagerAdapter(pluginContext, uiService));
        // set correct height for the tabs (difference between plugin height and tabs height)
        TabLayoutMediator mediator = new TabLayoutMediator(tabLayout, pager, (tab, position) -> tab.setText(Constants.TAB_LAYOUTS.get(position).first));
        mediator.attach();
        primaryPane = new PaneBuilder(mainTemplate)
                // relative location is set to default; pane will switch location dependent on
                // current orientation of device screen
                .setMetaValue(Pane.RELATIVE_LOCATION, Pane.Location.Default)
                // pane will take up 50% of screen width in landscape mode
                .setMetaValue(Pane.PREFERRED_WIDTH_RATIO, 0.5D)
                // pane will take up 50% of screen height in portrait mode
                .setMetaValue(Pane.PREFERRED_HEIGHT_RATIO, 0.5D)
                .build();

        primaryPaneInitialized = true;
    }

    /// Pass NULL to switch to main plugin pane
    public void showPane() {
        if (!primaryPaneInitialized)
            setupPrimaryPane();
        if (!uiService.isPaneVisible(primaryPane)) {
            pager.post(() -> {
                ViewGroup.LayoutParams params = pager.getLayoutParams();
                params.height = mainTemplate.getMeasuredHeight() - tabLayout.getMeasuredHeight();
                pager.setLayoutParams(params);
            });
            uiService.showPane(primaryPane, null);
        }
    }
}
