package org.codeisland.aggregato.client.notification;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * Receives the notification intent from Google Play Services and gives it to the
 *  {@link org.codeisland.aggregato.client.notification.NotificationService} to handle
 *  the push message.
 * @author Lukas Knuth
 * @version 1.0
 */
public class NotificationReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ComponentName comp = new ComponentName(context, NotificationService.class);
        // Start service and keep the device awake:
        startWakefulService(context, (intent.setComponent(comp)));
        setResultCode(Activity.RESULT_OK);
    }
}
