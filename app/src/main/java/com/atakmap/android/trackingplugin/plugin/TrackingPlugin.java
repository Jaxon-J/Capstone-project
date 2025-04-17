
package com.atakmap.android.trackingplugin.plugin;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.res.ResourcesCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.atak.plugins.impl.PluginContextProvider;
import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.trackingplugin.BluetoothReceiver;
import com.atakmap.android.trackingplugin.Constants;
import com.atakmap.android.trackingplugin.DeviceListManager;
import com.atakmap.android.trackingplugin.plugin.R;
import com.atakmap.android.trackingplugin.ui.TabViewPagerAdapter;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.google.android.material.tabs.TabLayoutMediator;

import gov.tak.api.plugin.IPlugin;
import gov.tak.api.plugin.IServiceController;
import gov.tak.api.ui.IHostUIService;
import gov.tak.api.ui.Pane;
import gov.tak.api.ui.PaneBuilder;
import gov.tak.api.ui.ToolbarItem;
import gov.tak.api.ui.ToolbarItemAdapter;
import gov.tak.platform.marshal.MarshalManager;

// FIXME: at the moment, the hot reloading is broken. working on a fix if one is available.
public class TrackingPlugin implements IPlugin {

    public static final String TAG = Constants.createTag(TrackingPlugin.class);
    IServiceController serviceController;
    Context pluginContext;
    IHostUIService uiService;
    ToolbarItem toolbarItem;
    Pane templatePane;
    BluetoothReceiver btReceiver;

    public TrackingPlugin(IServiceController serviceController) {
        this.serviceController = serviceController;
        final PluginContextProvider ctxProvider = serviceController
                .getService(PluginContextProvider.class);
        if (ctxProvider != null) {
            pluginContext = ctxProvider.getPluginContext();
            pluginContext.setTheme(R.style.ATAKPluginTheme);
        }
        DeviceListManager.initialize(pluginContext);

        // obtain the UI service
        uiService = serviceController.getService(IHostUIService.class);

        // initialize the toolbar button for the plugin

        // create the button
        toolbarItem = new ToolbarItem.Builder(
                pluginContext.getString(R.string.app_name),
                MarshalManager.marshal(
                        ResourcesCompat.getDrawable(pluginContext.getResources(), R.drawable.ic_launcher, null),
                        android.graphics.drawable.Drawable.class,
                        gov.tak.api.commons.graphics.Bitmap.class))
                .setListener(new ToolbarItemAdapter() {
                    @Override
                    public void onClick(ToolbarItem item) {
                        showPane();
                    }
                })
                .build();
    }

    @Override
    public void onStart() {
        // the plugin is starting, add the button to the toolbar
        if (uiService != null)
            uiService.addToolbarItem(toolbarItem);
        btReceiver = new BluetoothReceiver(pluginContext);
        AtakBroadcast.DocumentedIntentFilter btIntentFilter = new AtakBroadcast.DocumentedIntentFilter();
        btIntentFilter.addAction(BluetoothReceiver.ACTIONS.BLE_START_SCAN);
        btIntentFilter.addAction(BluetoothReceiver.ACTIONS.BLE_STOP_SCAN);
        btIntentFilter.addAction(BluetoothReceiver.ACTIONS.ENABLE_SCAN_WHITELIST);
        btIntentFilter.addAction(BluetoothReceiver.ACTIONS.DISABLE_SCAN_WHITELIST);
        AtakBroadcast.getInstance().registerReceiver(btReceiver, btIntentFilter);
    }

    @Override
    public void onStop() {
        // the plugin is stopping, remove the button from the toolbar
        if (uiService != null)
            uiService.removeToolbarItem(toolbarItem);
        if (btReceiver != null) {
            AtakBroadcast.getInstance().unregisterReceiver(btReceiver);
            btReceiver = null;
        }
        removeDeviceRadius();
    }

    private void showPane() {
        // instantiate the plugin view if necessary
        if (templatePane == null) {
            initUi();
        }

        // if the plugin pane is not visible, show it!
        if(!uiService.isPaneVisible(templatePane)) {
            Log.d(TAG, "Plugin pane opened");
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

    }

    // TODO: PUT BOTH FUNCTIONS BELOW SOMEWHERE ELSE WHERE IT MAKES SENSE.
    public static void displayDeviceRadius() {
        MapView mapView = MapView.getMapView();
        DrawingCircle circle = new DrawingCircle(mapView, Constants.DEVICE_RADIUS_CIRCLE_NAME);
        // set location, automatically update to follow the self marker
        circle.setCenterPoint(mapView.getSelfMarker().getGeoPointMetaData());
        mapView.getSelfMarker().addOnPointChangedListener((PointMapItem selfMarker) -> circle.setCenterPoint(selfMarker.getGeoPointMetaData()));
        circle.setRadius(10);
        circle.setMetaBoolean("archive", false);
        circle.setMetaBoolean("editable", false); // this is gross as shit.
        circle.setEditable(false);
        circle.setClickable(false);
        MapGroup radiusGroup = mapView.getRootGroup().deepFindMapGroup(Constants.RADIUS_GROUP_NAME);
        if (radiusGroup == null) {
            radiusGroup = mapView.getRootGroup().addGroup(Constants.RADIUS_GROUP_NAME);
        }
        radiusGroup.addItem(circle);
    }

    public static void removeDeviceRadius() {
        MapGroup radiusGroup = MapView.getMapView().getRootGroup().deepFindMapGroup(Constants.RADIUS_GROUP_NAME);
        MapItem radius = radiusGroup.deepFindUID(Constants.DEVICE_RADIUS_CIRCLE_NAME);
        if (radius == null)
            return;
        radiusGroup.removeItem(radius);
    }
}
