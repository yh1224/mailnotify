package net.assemble.mailnotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class EmailNotifyScreenReceiver extends BroadcastReceiver {
    private static final String TAG = "EmailNotify";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        Log.d(TAG, "received intent: " + intent.getAction());

        if (intent.getAction() != null) {
            // Screen On
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                if (EmailNotifyPreferences.getNotifyStopOnScreen(ctx)) {
                    Log.i(TAG, "Stopping all notifications. (SCREEN ON)");
                    EmailNotificationManager.stopAllNotifications();
                }
            }
        }
    }

}
