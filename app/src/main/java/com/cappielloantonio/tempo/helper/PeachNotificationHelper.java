package com.cappielloantonio.tempo.helper;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media3.common.util.UnstableApi;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.ui.activity.MainActivity;
import com.cappielloantonio.tempo.util.PeachUpdateManager;
import com.cappielloantonio.tempo.util.Preferences;

@UnstableApi
public class PeachNotificationHelper {

    public static final String CHANNEL_UPDATES = "peach_updates";
    public static final String CHANNEL_ALERTS = "peach_service_alerts";

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            NotificationChannel updatesChannel = new NotificationChannel(
                    CHANNEL_UPDATES,
                    "Mises à jour Peach",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            updatesChannel.setDescription("Notifications pour les nouvelles versions de l'application");

            NotificationChannel alertsChannel = new NotificationChannel(
                    CHANNEL_ALERTS,
                    "Alertes de service Tropikeau",
                    NotificationManager.IMPORTANCE_HIGH
            );
            alertsChannel.setDescription("Maintenances et incidents réseau Tropikeau");

            manager.createNotificationChannel(updatesChannel);
            manager.createNotificationChannel(alertsChannel);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    public static void postNotification(Context context, String channelId, int id, String title, String message, String url, String notificationKey) {
        if (!Preferences.isPeachNotificationEnabled()) return;

        if (notificationKey != null && !notificationKey.isEmpty()) {
            if (Preferences.getNotifiedKeys().contains(notificationKey)) {
                return;
            }
        }

        Intent intent;
        if (url != null && !url.isEmpty() && url.startsWith("https://tropikeau.fr/")) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            if (CHANNEL_UPDATES.equals(channelId)) {
                intent.putExtra(
                        PeachUpdateManager.EXTRA_OPEN_UPDATE,
                        true
                );
            }
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_splash_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(CHANNEL_ALERTS.equals(channelId) ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (CHANNEL_UPDATES.equals(channelId)) {
            builder.addAction(
                    R.drawable.ic_splash_logo,
                    "Mettre à jour",
                    pendingIntent
            );
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(id, builder.build());
            if (notificationKey != null && !notificationKey.isEmpty()) {
                Preferences.addNotifiedKey(notificationKey);
            }
        } catch (SecurityException e) {
            // POST_NOTIFICATIONS permission not granted
        }
    }
}
