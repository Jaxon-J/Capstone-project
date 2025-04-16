
package com.atakmap.android.trackingplugin.plugin;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.res.ResourcesCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.atak.plugins.impl.PluginContextProvider;
import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.trackingplugin.BluetoothReceiver;
import com.atakmap.android.trackingplugin.Constants;
import com.atakmap.android.trackingplugin.DeviceListManager;
import com.atakmap.android.trackingplugin.DeviceMapDisplay;
import com.atakmap.android.trackingplugin.ui.TabViewPagerAdapter;
import com.google.android.material.tabs.TabLayoutMediator;

import gov.tak.api.plugin.IPlugin;
import gov.tak.api.plugin.IServiceController;
import gov.tak.api.ui.IHostUIService;
import gov.tak.api.ui.Pane;
import gov.tak.api.ui.PaneBuilder;
import gov.tak.api.ui.ToolbarItem;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.ui.MotionEvent;

/*
Implement notes:
- Listen for other people that are actively tracking, add/remove from Sensor list accordingly.
  - Sensor list should be symmetric to whitelist

 */

public class TrackingPlugin implements IPlugin {

    public static final String TAG = Constants.createTag(TrackingPlugin.class);
    IServiceController serviceController;
    Context pluginContext;
    IHostUIService uiService;
    ToolbarItem toolbarItem;
    Pane templatePane;
    BluetoothReceiver btReceiver;
    boolean uiInitialized = false;

    public TrackingPlugin(IServiceController serviceController) {
        this.serviceController = serviceController;
        final PluginContextProvider ctxProvider = serviceController
                .getService(PluginContextProvider.class);
        uiService = serviceController.getService(IHostUIService.class);
        if (ctxProvider == null || uiService == null) {
            throw new RuntimeException("Could not retrieve services necessary to run the plugin.");
        }
        pluginContext = ctxProvider.getPluginContext();
        pluginContext.setTheme(R.style.ATAKPluginTheme);
        DeviceListManager.initialize(pluginContext);

        // create button that will be added to the toolbar in onStart
        toolbarItem = new ToolbarItem.Builder(
                pluginContext.getString(R.string.app_name),
                MarshalManager.marshal(
                        ResourcesCompat.getDrawable(pluginContext.getResources(), R.drawable.ic_launcher, null),
                        android.graphics.drawable.Drawable.class,
                        gov.tak.api.commons.graphics.Bitmap.class))
                .setListener((ToolbarItem item, MotionEvent event) -> showPane())
                .build();
    }

    @Override
    public void onStart() {
        // the plugin is starting, add the button to the toolbar
        uiService.addToolbarItem(toolbarItem);
        btReceiver = new BluetoothReceiver(pluginContext);
        AtakBroadcast.DocumentedIntentFilter btIntentFilter = new AtakBroadcast.DocumentedIntentFilter();
        btIntentFilter.addAction(BluetoothReceiver.ACTIONS.BLE_START_SCAN);
        btIntentFilter.addAction(BluetoothReceiver.ACTIONS.BLE_STOP_SCAN);
        btIntentFilter.addAction(BluetoothReceiver.ACTIONS.ENABLE_SCAN_WHITELIST);
        btIntentFilter.addAction(BluetoothReceiver.ACTIONS.DISABLE_SCAN_WHITELIST);
        AtakBroadcast.getInstance().registerReceiver(btReceiver, btIntentFilter);

        DeviceMapDisplay.start();
    }

    @Override
    public void onStop() {
        // if ui is up, take it down or else the old plugin will stick around.
        if (templatePane != null && uiService.isPaneVisible(templatePane)) {
            uiService.closePane(templatePane);
        }
        uiService.removeToolbarItem(toolbarItem);
        if (btReceiver != null) {
            AtakBroadcast.getInstance().unregisterReceiver(btReceiver);
            btReceiver = null;
        }

        DeviceMapDisplay.stop();
    }

    private void showPane() {
        // instantiate the plugin view if necessary
        if (!uiInitialized)
            initUi();

        if(!uiService.isPaneVisible(templatePane)) {
            uiService.showPane(templatePane, null);
        }
    }

    private void initUi() {
        View mainTemplate = PluginLayoutInflater.inflate(pluginContext, R.layout.main_layout, null);
        ViewPager2 pager = mainTemplate.findViewById(R.id.viewPager);
        pager.setAdapter(new TabViewPagerAdapter(pluginContext));
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
        TabLayoutMediator mediator = new TabLayoutMediator(mainTemplate.findViewById(R.id.tabLayout), pager, (tab, position) -> tab.setText(Constants.TAB_LAYOUTS.get(position).first));
        mediator.attach();
        templatePane = new PaneBuilder(mainTemplate)
                // relative location is set to default; pane will switch location dependent on
                // current orientation of device screen
                .setMetaValue(Pane.RELATIVE_LOCATION, Pane.Location.Default)
                // pane will take up 50% of screen width in landscape mode
                .setMetaValue(Pane.PREFERRED_WIDTH_RATIO, 0.5D)
                // pane will take up 50% of screen height in portrait mode
                .setMetaValue(Pane.PREFERRED_HEIGHT_RATIO, 0.5D)
                .build();

        uiInitialized = true;
    }
}
