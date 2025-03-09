package com.atakmap.android.trackingplugin;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.atakmap.android.trackingplugin.plugin.R;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.maps.MapView;

public class TrackingPluginMapComponent extends DropDownMapComponent {
    private static final String TAG = Constants.createTag(TrackingPluginMapComponent.class);
    private Context pluginContext;
    private TrackingPluginDropDownReceiver mainDdr;

    // ENTRY POINT
    @Override
    public void onCreate(Context context, Intent intent, MapView view) {
        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        pluginContext = context;
        try {
            Class.forName("dalvik.system.CloseGuard")
                    .getMethod("setEnabled", boolean.class)
                    .invoke(null, true);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        mainDdr = new TrackingPluginDropDownReceiver(view, context);
        Log.d(TAG, "Registering TrackingPluginDropDownReceiver");
        DocumentedIntentFilter filter = new DocumentedIntentFilter();
        filter.addAction(TrackingPluginDropDownReceiver.ACTIONS.SHOW_PLUGIN);
        registerDropDownReceiver(mainDdr, filter);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        // not sure what cleanup code goes here shrug
        super.onDestroyImpl(context, view);
    }
}
