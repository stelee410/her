package com.linkyun.her;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

public class HerForegroundService extends Service {
    public static final String ACTION_START = "com.linkyun.her.action.START";
    public static final String ACTION_MICROPHONE_MODE = "com.linkyun.her.action.MICROPHONE_MODE";
    public static final String ACTION_IDLE_MODE = "com.linkyun.her.action.IDLE_MODE";

    private static final String CHANNEL_ID = "her_foreground";
    private static final int NOTIFICATION_ID = 1024;

    @Override
    public void onCreate() {
        super.onCreate();
        ensureChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        boolean microphoneMode = ACTION_MICROPHONE_MODE.equals(action);
        startForegroundCompat(microphoneMode);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startForegroundCompat(boolean microphoneMode) {
        Notification notification = buildNotification(microphoneMode);
        if (Build.VERSION.SDK_INT >= 29) {
            int type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
            if (microphoneMode) type |= ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
            startForeground(NOTIFICATION_ID, notification, type);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification(boolean microphoneMode) {
        Intent openIntent = new Intent(this, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Doris is awake")
                .setContentText(microphoneMode ? "Listening can continue in the background." : "Ready to continue when you return.")
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setShowWhen(false)
                .build();
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Doris background",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Keeps Doris available while the app is in the background.");
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }
}
