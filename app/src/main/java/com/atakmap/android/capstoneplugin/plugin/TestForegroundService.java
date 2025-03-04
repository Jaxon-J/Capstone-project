package com.atakmap.android.capstoneplugin.plugin;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class TestForegroundService extends Service {
    public static final String INCREMENT = "com.atakmap.testforegroundservice.INC";
    public static final String DECREMENT = "com.atakmap.testforegroundservice.DEC";
    private static final String TAG = Constants.TAG_PREFIX + "TestForeground";
    private static final String NOTIF_CHANNEL_ID = "atak_track_test_notif_channel";
    private static final int NOTIF_ID = 8266;
    BroadcastReceiver broadcastReceiver;
    private int displayNum = 0;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) return;

                switch (action) {
                    case INCREMENT:
                        displayNum += 1;
                        Log.d(TAG, "" + displayNum);
                        break;
                    case DECREMENT:
                        displayNum -= 1;
                        Log.d(TAG, "" + displayNum);
                        break;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(INCREMENT);
        filter.addAction(DECREMENT);
        registerReceiver(broadcastReceiver, filter, RECEIVER_NOT_EXPORTED);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        String action = intent.getAction();

        // INITIALIZE COUNTER?

        // FOREGROUND STUFF
        NotificationChannelCompat notifChannel = // 4 = IMPORTANCE_HIGH
                new NotificationChannelCompat.Builder(NOTIF_CHANNEL_ID, 4).setName("Test " +
                                "Notification Channel")
                        .setDescription("Channel for testing foreground service.")
                        .build();
        NotificationManagerCompat.from(this).createNotificationChannel(notifChannel);
        Notification notif =
                new NotificationCompat.Builder(this, NOTIF_CHANNEL_ID).setContentTitle("ATAK " +
                                "Track Test Service")
                .setContentText("This is a running foreground service!")
                .setSmallIcon(R.drawable.ic_launcher)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        else startForeground(NOTIF_ID, notif);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        broadcastReceiver = null;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
