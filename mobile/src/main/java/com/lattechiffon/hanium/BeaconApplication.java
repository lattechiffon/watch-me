package com.lattechiffon.hanium;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.estimote.coresdk.observation.region.beacon.BeaconRegion;
import com.estimote.coresdk.recognition.packets.Beacon;
import com.estimote.coresdk.service.BeaconManager;

import java.util.List;
import java.util.UUID;

/**
 * Created by yongguk on 2017. 8. 18..
 */

public class BeaconApplication extends Application {

    private BeaconManager beaconManager;
    SharedPreferences pref;
    SharedPreferences.Editor editor;

    @Override
    public void onCreate() {
        super .onCreate();

        beaconManager = new BeaconManager(getApplicationContext());

        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                beaconManager.startMonitoring(new BeaconRegion(
                        "monitored region",
                        UUID.fromString("b9407f30-f5f8-466e-aff9-25556b57fe6d"),
                        34378, 4469));
            }
        });

        beaconManager.setMonitoringListener(new BeaconManager.BeaconMonitoringListener() {
            @Override
            public void onEnteredRegion(BeaconRegion beaconRegion, List<Beacon> list) {
                pref = getSharedPreferences("EmergencyData", Activity.MODE_PRIVATE);
                editor = pref.edit();

                showNotification("비콘 영역으로 들어왔습니다.",
                        "비콘 영역으로 들어왔습니다.");

                if (!pref.getBoolean("beacon", false)) {
                    editor.putBoolean("beacon", true);
                    editor.commit();
                }
            }

            @Override
            public void onExitedRegion(BeaconRegion beaconRegion) {
                pref = getSharedPreferences("EmergencyData", Activity.MODE_PRIVATE);
                editor = pref.edit();

                showNotification("비콘 영역에서 나갔습니다.",
                        "비콘 영역에서 나갔습니다.");

                if (pref.getBoolean("beacon", false)) {
                    editor.putBoolean("beacon", false);
                    editor.commit();
                }
            }
        });
    }

    public void showNotification(String title, String message) {
        Intent notifyIntent = new Intent(this, MainActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivities(this, 0,
                new Intent[] { notifyIntent }, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();
        notification.defaults |= Notification.DEFAULT_SOUND;
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }
}