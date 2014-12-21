package org.codeisland.aggregato.client.notification;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import org.codeisland.aggregato.client.R;
import org.codeisland.aggregato.client.Watchlist;

/**
 * Does the actual work of handling the received push-message.
 * @author Lukas Knuth
 * @version 1.0
 */
public class NotificationService extends IntentService {

    private static final int NOTIFICATION_ID = 17;
    private static final String MESSAGE_TYPE_KEY = "type";
    private static final String MESSAGE_TYPE_WATCHLIST_UPDATE = "watchlist_update";

    public NotificationService() {
        super("GCMNotificationService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);
        if (!extras.isEmpty()){
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                if (extras.containsKey(MESSAGE_TYPE_KEY)){
                    String type = extras.getString(MESSAGE_TYPE_KEY);
                    if (MESSAGE_TYPE_WATCHLIST_UPDATE.equals(type)){
                        putWatchlistNotification();
                    }
                } else {
                    Log.e("Aggregato GCM", "No 'type' in the extras Bundle!");
                }
            }
        }
        // Done, give back the wake-lock
        NotificationReceiver.completeWakefulIntent(intent);
    }

    private void putWatchlistNotification(){
        PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(this, Watchlist.class), 0);
        String title = getString(R.string.notification_watchlist_title);
        String text = getString(R.string.notification_watchlist_text);
        this.putNotification(intent, title, text);
    }

    private void putNotification(PendingIntent intent, String title, String text){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean(getString(R.string.settings_key_notifications), false)){
            return;
        }
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(intent);
        // Set rest according to settings:
        if (preferences.getBoolean(getString(R.string.settings_key_notifications_sound), true)){
            builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI);
        }
        if (preferences.getBoolean(getString(R.string.settings_key_notifications_flash), true)){
            builder.setDefaults(Notification.DEFAULT_LIGHTS);
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
