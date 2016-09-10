package net.assemble.emailnotify.core.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.assemble.emailnotify.core.BuildConfig;
import net.assemble.emailnotify.core.EmailNotify;
import net.assemble.emailnotify.core.preferences.EmailNotifyPreferences;

/**
 * 画面ONイベント処理
 *
 * 画面ONで通知を停止する。
 */
public class EmailNotifyScreenReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "received intent: " + intent.getAction());

        if (intent.getAction() != null) {
            // Screen On
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                if (EmailNotifyPreferences.getNotifyStopOnScreen(ctx)) {
                    Log.i(EmailNotify.TAG, "Stopping all notifications. (SCREEN ON)");
                    int flags = EmailNotification.NOTIFY_SOUND;
                    if (EmailNotifyPreferences.getNotifyStopLedOnScreen(ctx)) {
                        flags |= EmailNotification.NOTIFY_LED;
                    }
                    if (EmailNotifyPreferences.getNotifyStopRenotifyOnScreen(ctx)) {
                        flags |= EmailNotification.NOTIFY_RENOTIFY;
                    }
                    EmailNotificationManager.stopAllNotifications(flags);
                }
            }
        }
    }

}
