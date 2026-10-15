package com.openflux.app.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.openflux.app.R;
import com.openflux.app.ui.MainActivity;

/** Создаёт канал и уведомление для foreground-сервиса. */
public final class VpnNotifFactory {
    public static final String CHANNEL_ID = "openflux_vpn";
    public static final int NOTIFICATION_ID = 1;

    private VpnNotifFactory() {}

    public static void ensureChannel(Context ctx) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, ctx.getString(R.string.notif_channel),
                    NotificationManager.IMPORTANCE_LOW));
        }
    }

    public static Notification build(Context ctx, String text) {
        PendingIntent pi = PendingIntent.getActivity(ctx, 0,
                new Intent(ctx, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(ctx, CHANNEL_ID)
                .setContentTitle(ctx.getString(R.string.notif_title))
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }
}
