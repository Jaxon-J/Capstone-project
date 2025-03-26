package com.atakmap.android.trackingplugin.ui;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.atakmap.android.trackingplugin.plugin.R;

import java.util.ArrayList;

public class DevicesTabHandler {

    private final View rootView;
    private final Context context;
    private EditText editText;
    private TextView pageText;
    private Button button;

    private static Boolean tabInitialized = false;

    private String TAG = "DevicesTabHandler";
    DevicesTabHandler(View rootView, Context context) {

        this.rootView = rootView;
        this.context = context;
        try {
            setup();
        } catch(Exception e) {
            Log.d(TAG,"YEETUS ERROR");
        }

    }

    public void setup() {

        if (!tabInitialized) {
            ArrayList<MockDevice> mockDevices = MockDevice.getDevices();
            Log.d(TAG,String.valueOf(mockDevices.size()));
            if (mockDevices.isEmpty()) {
                for (int i = 0; i < 5; i++) {
                    String MAC = Integer.toString(i);
                    String deviceID = "device" + MAC;
                    new MockDevice(deviceID, MAC);
                }
            }

            mockDevices = MockDevice.getDevices();

            TableLayout devicesTable = this.rootView.findViewById(R.id.devicesTableLayout);
            TableRow headerRow = new TableRow(this.context);
            TextView col1 = new TextView(this.context); // I guess i didnt need to pass in a context. Fix later.
            TextView col2 = new TextView(this.context);
            col1.setText("DEVICEID\t");
            col2.setText("MAC");
            headerRow.addView(col1);
            headerRow.addView(col2);
            devicesTable.addView(headerRow);
            Log.d(TAG,String.valueOf(mockDevices.size()));
            for (MockDevice mockDevice : mockDevices) {

                TableRow row = new TableRow(this.context);
                TextView deviceIDcol = new TextView(this.context);
                TextView MACcol = new TextView(this.context);
                deviceIDcol.setText(mockDevice.getID());
                MACcol.setText(mockDevice.getMAC());
                row.addView(deviceIDcol);
                row.addView(MACcol);
                devicesTable.addView(row);

            }

            tabInitialized = true;
        }


    }

    public void onButtonClick() {
        Log.d(TAG,"BUTTON CLICKED");

    }


}
