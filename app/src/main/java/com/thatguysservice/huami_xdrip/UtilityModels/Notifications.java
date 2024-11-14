package com.thatguysservice.huami_xdrip.UtilityModels;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.thatguysservice.huami_xdrip.HuamiXdrip;
import com.thatguysservice.huami_xdrip.MainActivity;
import com.thatguysservice.huami_xdrip.R;
import com.thatguysservice.huami_xdrip.models.Helper;

import static com.thatguysservice.huami_xdrip.models.Helper.isRunningOreoOrLater;

public class Notifications {
    final static int ongoingNotificationId = 8811;

    public static final String NOTIFICATION_CHANNEL_ID = "ongoingChannel";
    private static boolean notificationChannelsCreated;

    public static void createNotificationChannels(Context context) {
        if (notificationChannelsCreated) return;

        if (isRunningOreoOrLater()) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            NotificationChannel channelGeneral = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW);
            channelGeneral.setShowBadge(false);
            notificationManager.createNotificationChannel(channelGeneral);
        }
        notificationChannelsCreated = true;
    }

    public static Notification createNotification(String text, Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        builder.setTicker(HuamiXdrip.getAppContext().getString(R.string.app_name))
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setContentIntent(getContentIntent(context))
                .setColor(ContextCompat.getColor(context, R.color.white))
                .setShowWhen(false)
                .setOngoing(true);
        if (Helper.isRunningLollipopOrLater()) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        }
        builder.setPriority(Notification.PRIORITY_MIN);
        return builder.build();
    }

    private static PendingIntent getContentIntent(Context context) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return pendingIntent;
    }
}
