package com.atakmap.android.capstoneplugin.plugin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.atak.plugins.impl.PluginContextProvider;
import com.atak.plugins.impl.PluginLayoutInflater;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import gov.tak.api.plugin.IPlugin;
import gov.tak.api.plugin.IServiceController;
import gov.tak.api.ui.IHostUIService;
import gov.tak.api.ui.Pane;
import gov.tak.api.ui.PaneBuilder;
import gov.tak.api.ui.ToolbarItem;
import gov.tak.api.ui.ToolbarItemAdapter;
import gov.tak.platform.marshal.MarshalManager;

public class TrackingPlugin implements IPlugin {

    public static final String TAG = Constants.TAG_PREFIX + "Main";
    private static int DEBUG_PIN_COUNT;
    IServiceController serviceController;
    Context pluginContext;
    IHostUIService uiService;
    ToolbarItem toolbarItem;
    Pane pluginViewPane;
    private boolean debug_scanning = false;
    private boolean testServiceRunning = false;

    public TrackingPlugin(IServiceController serviceController) {
        this.serviceController = serviceController;
        final PluginContextProvider ctxProvider =
                serviceController.getService(PluginContextProvider.class);
        OpaqueClassInspector inspector = new OpaqueClassInspector();
        inspector.inspectObject(serviceController, "ServiceController");
        inspector.inspectObject(ctxProvider, "PluginContextProvider");
        if (ctxProvider != null) {
            Log.d(TAG, "CONTEXT WAS NULL");
            pluginContext = ctxProvider.getPluginContext();
            pluginContext.setTheme(R.style.ATAKPluginTheme);
        }

        // obtain the UI service
        uiService = serviceController.getService(IHostUIService.class);

        // initialize the toolbar button for the plugin

        // create the button
        toolbarItem = new ToolbarItem.Builder(pluginContext.getString(R.string.app_name),
                MarshalManager.marshal(pluginContext.getResources()
                .getDrawable(R.drawable.ic_launcher, null),
                        android.graphics.drawable.Drawable.class,
                        gov.tak.api.commons.graphics.Bitmap.class)).setListener(new ToolbarItemAdapter() {
            @Override
            public void onClick(ToolbarItem item) {
                showPane();
            }
        }).build();
    }

    @Override
    public void onStart() {
        // the plugin is starting, add the button to the toolbar
        if (uiService == null) return;

        uiService.addToolbarItem(toolbarItem);
    }

    @Override
    public void onStop() {
        // the plugin is stopping, remove the button from the toolbar
        if (uiService == null) return;

        uiService.removeToolbarItem(toolbarItem);
    }

    private void showPane() {
        // instantiate the plugin view if necessary
        if (pluginViewPane == null) {
            // Remember to use the PluginLayoutInflator if you are actually inflating a custom view
            // In this case, using it is not necessary - but I am putting it here to remind
            // developers to look at this Inflator

            pluginViewPane = new PaneBuilder(PluginLayoutInflater.inflate(pluginContext,
                    R.layout.main_layout, null))
                    // relative location is set to default; pane will switch location dependent on
                    // current orientation of device screen
                    .setMetaValue(Pane.RELATIVE_LOCATION, Pane.Location.Default)
                    // pane will take up 50% of screen width in landscape mode
                    .setMetaValue(Pane.PREFERRED_WIDTH_RATIO, 0.5D)
                    // pane will take up 50% of screen height in portrait mode
                    .setMetaValue(Pane.PREFERRED_HEIGHT_RATIO, 0.5D).build();
        }

        // if the plugin pane is not visible, show it!
        if (!uiService.isPaneVisible(pluginViewPane)) {
            Log.d(TAG, "Plugin pane opened");
            uiService.showPane(pluginViewPane, null);
        }

        View pluginView = MarshalManager.marshal(pluginViewPane, Pane.class, View.class);
        pluginView.findViewById(R.id.grantPermissionsDebugButton)
                .setOnClickListener(this::onGetPermsButtonClick);
        pluginView.findViewById(R.id.trackDebugButton)
                .setOnClickListener(this::onTrackDebugButtonClick);
        pluginView.findViewById(R.id.startTestForegroundButton).setOnClickListener((View v) -> {
            Button btn = (Button) v;
            Intent testServiceIntent = new Intent(pluginContext, TestForegroundService.class);
            if (testServiceRunning) {
                pluginContext.stopService(testServiceIntent);
                btn.setText("Start Test Service");
                testServiceRunning = false;
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                pluginContext.startForegroundService(testServiceIntent);
            } else {
                pluginContext.startService(testServiceIntent);
            }
            btn.setText("Stop Test Service");
            testServiceRunning = true;
        });
        pluginView.findViewById(R.id.incrementButton)
                .setOnClickListener((View v) -> pluginContext.sendBroadcast(new Intent(TestForegroundService.INCREMENT)));
        pluginView.findViewById(R.id.decrementButton)
                .setOnClickListener((View v) -> pluginContext.sendBroadcast(new Intent(TestForegroundService.DECREMENT)));

    }

    private void onTrackDebugButtonClick(View v) {
        if (!getPerms()) return;
        Log.d(TAG, "Debug tracking button pressed");
        Button trackBtn = (Button) v;
        Intent btServiceIntent = new Intent(pluginContext, BluetoothTrackerService.class);
        if (debug_scanning) {
            trackBtn.setText("Start Tracking");
            debug_scanning = false;
            pluginContext.stopService(btServiceIntent);
            return;
        }
        trackBtn.setText("Stop Tracking");
        debug_scanning = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pluginContext.startForegroundService(btServiceIntent);
        } else {
            pluginContext.startService(btServiceIntent);
        }
        Intent startScanning = new Intent(BluetoothTrackerService.ACTION_START_SCANNING);
        pluginContext.sendBroadcast(startScanning);
    }

    private boolean getPerms() {
        // probably refactoring this later into PermissionsHandler or something
        boolean hasPerms = true;
        Set<String> missingPerms = new HashSet<>();
        Set<String> neededPerms;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            neededPerms = Set.of(Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.WAKE_LOCK);
        } else {
            neededPerms = Set.of(Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION
                    , Manifest.permission.WAKE_LOCK);
        }
        for (String perm : neededPerms) {
            if (pluginContext.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED)
                continue;
            missingPerms.add(perm);
            hasPerms = false;
        }
        if (!hasPerms) {

            MarshalManager.marshal(pluginViewPane, Pane.class, View.class);
            Log.e(TAG, "Cannot start service. Missing permissions: " + String.join(",",
                    missingPerms));
        }
        return hasPerms;
    }


    private void onGetPermsButtonClick(View v) {
        // request permissions here? might need a PermissionsHandler or something
    }


    // something claude spit out to help me out with understanding classes.
    // pass in any "[Class].class" to inspectObject and it will spit everything out in logs.
    static class OpaqueClassInspector {
        /**
         * Logs comprehensive information about an object of unknown type
         *
         * @param obj   The object to inspect
         * @param label A descriptive label for the log output
         */
        public void inspectObject(Object obj, String label) {
            if (obj == null) {
                Log.i(TAG, label + ": Object is null");
                return;
            }

            Class<?> clazz = obj.getClass();

            // Basic information
            Log.i(TAG, "======== " + label + " (" + clazz.getName() + ") ========");
            Log.i(TAG, "toString(): " + obj);
            Log.i(TAG, "hashCode(): " + obj.hashCode());

            // Class hierarchy
            logClassHierarchy(clazz);

            // Fields and their values
            logFields(obj);

            // Available methods
            logMethods(clazz);

            // Try common getter methods
            logCommonGetters(obj);

            Log.i(TAG, "======== End of " + label + " inspection ========");
        }

        /**
         * Logs the complete class hierarchy
         */
        private void logClassHierarchy(Class<?> clazz) {
            StringBuilder hierarchy = new StringBuilder("Class hierarchy: ");
            Class<?> current = clazz;

            while (current != null) {
                hierarchy.append(current.getName());
                current = current.getSuperclass();
                if (current != null) {
                    hierarchy.append(" -> ");
                }
            }

            Log.i(TAG, hierarchy.toString());

            // Log interfaces
            Class<?>[] interfaces = clazz.getInterfaces();
            if (interfaces.length > 0) {
                Log.i(TAG, "Implemented interfaces: " + Arrays.toString(interfaces));
            }
        }

        /**
         * Logs all fields and their values using reflection
         */
        private void logFields(Object obj) {
            Class<?> clazz = obj.getClass();
            Log.i(TAG, "--- Fields ---");

            try {
                // Get all fields including private/protected ones from the class and its
                // superclasses
                Map<String, Field> fieldMap = new HashMap<>();
                Class<?> currentClass = clazz;

                while (currentClass != null) {
                    Field[] fields = currentClass.getDeclaredFields();
                    for (Field field : fields) {
                        if (!fieldMap.containsKey(field.getName())) {
                            fieldMap.put(field.getName(), field);
                        }
                    }
                    currentClass = currentClass.getSuperclass();
                }

                for (Field field : fieldMap.values()) {
                    field.setAccessible(true);
                    String modifiers = Modifier.toString(field.getModifiers());
                    try {
                        Object value = field.get(obj);
                        String valueStr = (value != null) ? (value.getClass()
                                .isArray() ? Arrays.deepToString((Object[]) value) :
                                value.toString()) : "null";
                        Log.i(TAG, modifiers + " " + field.getType()
                                .getName() + " " + field.getName() + " " + "= " + valueStr);
                    } catch (Exception e) {
                        Log.i(TAG, modifiers + " " + field.getType()
                                .getName() + " " + field.getName() + " " + "= [Could not access " +
                                "value: " + e.getMessage() + "]");
                    }
                }
            } catch (SecurityException e) {
                Log.w(TAG, "Security manager prevented access to fields");
                Log.w(TAG, e.toString());
            }
        }

        /**
         * Logs all available methods
         */
        private void logMethods(Class<?> clazz) {
            Log.i(TAG, "--- Methods ---");
            Method[] methods = clazz.getMethods();

            for (Method method : methods) {
                String params = Arrays.toString(method.getParameterTypes())
                        .replace("[", "(")
                        .replace("]", ")")
                        .replace("class ", "");

                Log.i(TAG, Modifier.toString(method.getModifiers()) + " " + method.getReturnType()
                        .getSimpleName() + " " + method.getName() + params);
            }
        }

        /**
         * Attempts to call common getter methods
         */
        private void logCommonGetters(Object obj) {
            Log.i(TAG, "--- Common Getter Results ---");
            Method[] methods = obj.getClass().getMethods();

            for (Method method : methods) {
                // Only try no-arg methods that start with "get" or "is" and aren't getClass()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (method.getParameterCount() == 0 && !method.getName()
                            .equals("getClass") && (method.getName()
                            .startsWith("get") || method.getName().startsWith("is"))) {
                        try {
                            method.setAccessible(true);
                            Object result = method.invoke(obj);
                            Log.i(TAG, method.getName() + "() = " + (result != null ?
                                    result.toString() : "null"));
                        } catch (Exception e) {
                            // Just log and continue
                            Log.i(TAG, method.getName() + "() = [Exception: " + e.getMessage() +
                                    "]");
                        }
                    }
                }
            }
        }
    }
}
