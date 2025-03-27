package com.atakmap.android.trackingplugin.ui;

import static android.app.PendingIntent.getActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

import com.atakmap.android.trackingplugin.plugin.R;

import java.util.ArrayList;

public class DevicesTabHandler extends DialogFragment{

    private final View rootView;
    private final Context context;
    private static Boolean tabInitialized = false;

    private static TableLayout devicesTable;
    private String TAG = "DevicesTabHandler";

    DevicesTabHandler(View rootView, Context context) {

        this.rootView = rootView;
        this.context = context;
        Log.d(TAG,"LOADING");
        try {
            String invokeCrashString = rootView.findViewById(R.id.devicesTableLayout).toString();
            setup();
        } catch (Exception e){
            Log.d(TAG, "ERROR" + e.getMessage());
        }

    }

    public void setup() {

        if (!tabInitialized) {

            Log.d(TAG,"Creating mock devices");
            ArrayList<MockDevice> mockDevices = MockDevice.getDevices();
            //Log.d(TAG,String.valueOf(mockDevices.size()));
            if (mockDevices.isEmpty()) {
                for (int i = 0; i < 20; i++) {
                    String MAC = "place_holder_MAC_" + Integer.toString(i);
                    String deviceID = "device_" + Integer.toString(i);
                    new MockDevice(deviceID, MAC);
                }
            }



            devicesTable = this.rootView.findViewById(R.id.devicesTableLayout);
            loadTable(devicesTable);

            Button addDeviceButton = this.rootView.findViewById(R.id.addDeviceButton);
            Log.d(TAG,"Setting button listener");
            addDeviceButton.setOnClickListener(v -> {

                //String invokeCrashString = rootView.findViewById(R.id.devicesTableLayout).toString();
                showInputPopup();


            });


            tabInitialized = true;
        }


    }

    public void loadTable(TableLayout tableLayout) {

        Log.d(TAG,"Loading device table");
        tableLayout.removeAllViews();
        tableLayout.setStretchAllColumns(true);
        TableRow headerRow = new TableRow(this.context);
        TextView headerCol1 = new TextView(this.context); // I guess i didnt need to pass in a context. Fix later.
        TextView headerCol2 = new TextView(this.context);
        headerCol1.setText("DEVICE_ID\t");
        headerCol2.setText("MAC");
        headerRow.addView(headerCol1);
        headerRow.addView(headerCol2);
        headerRow.setBackgroundColor(Color.BLUE);
        tableLayout.addView(headerRow);
        ArrayList<MockDevice> mockDevices = MockDevice.getDevices();
        //Log.d(TAG,String.valueOf(mockDevices.size()));


        int i = 0;
        for (MockDevice mockDevice : mockDevices) {

            TableRow row = new TableRow(this.context); // Create the table row

            TextView deviceIDcol = new TextView(this.context);
            TextView MACcol = new TextView(this.context);

            deviceIDcol.setTextColor(Color.BLACK);
            MACcol.setTextColor(Color.BLACK);

            deviceIDcol.setText(mockDevice.getID());
            MACcol.setText(mockDevice.getMAC());

            row.addView(deviceIDcol);
            row.addView(MACcol);

            row.setBackgroundColor((i % 2 == 0) ? Color.LTGRAY : Color.WHITE);
            i++;

            tableLayout.addView(row);


        }

    }
    public void showInputPopup() {

        LayoutInflater inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customView = inflater.inflate(R.layout.add_device_popup, null);
        PopupWindow popupWindow = new PopupWindow(customView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        popupWindow.setFocusable(true);  // Allows interaction with the popup
        popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.WHITE));

        EditText deviceIDEntry = customView.findViewById(R.id.deviceIDEntry);
        deviceIDEntry.setHint("Enter device ID");
        EditText MACEntry = customView.findViewById(R.id.MACEntry);
        MACEntry.setHint("Enter MAC address");

        // Step 3: Get a reference to a button or other UI elements in the popup
        Button enterButton = customView.findViewById(R.id.EnterButton);
        enterButton.setOnClickListener(v -> {
            // Dismiss the PopupWindow when the button is clicked
            String deviceID = deviceIDEntry.getText().toString();
            String MAC = MACEntry.getText().toString();

            new MockDevice(deviceID, MAC);

            popupWindow.dismiss();
            loadTable(devicesTable);

        });

        Button cancelButton = customView.findViewById(R.id.CancelButton);
        cancelButton.setOnClickListener(v -> {

            popupWindow.dismiss();
        });



        // Step 4: Show the PopupWindow at the desired location
        popupWindow.showAtLocation(this.rootView, Gravity.CENTER, 0, 0);




    }

    public void showInputDialog() {

            if (!(context instanceof Activity)) {
                Log.e(TAG, "Context is not a valid Activity.");

                return;
            }

            Activity activity = (Activity) context;
            if (activity.isFinishing()) {
                Log.e(TAG, "Activity is finishing, cannot show dialog.");
                return;
            }

            if (activity.isDestroyed()) {
                Log.e(TAG, "Activity is destroyed, cannot show dialog.");
                return;
            }

            Log.d(TAG, "Showing input dialog");
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Add a new device.");

            LinearLayout layout = new LinearLayout(this.context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 20, 50, 20);

            final EditText deviceIDInput = new EditText(this.context);
            final EditText MACInput = new EditText(this.context);

            deviceIDInput.setHint("Enter Device ID");
            MACInput.setHint("Enter MAC address");

            deviceIDInput.setInputType(InputType.TYPE_CLASS_TEXT);
            MACInput.setInputType(InputType.TYPE_CLASS_TEXT);

            layout.addView(deviceIDInput);
            layout.addView(MACInput);

            builder.setView(layout);


            builder.setPositiveButton("ENTER", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Log.d(TAG, "USER ENTERED DEVICE ID: " + deviceIDInput.getText().toString() + " MAC: " + MACInput.getText().toString());
                }
            }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Log.d(TAG, "CANCELLED INTERACTION");
                }
            });

            builder.show();

    }

}
