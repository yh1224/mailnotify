package net.assemble.mailnotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * メール着信通知からのインテント受信
 */
public class EmailNotificationReceiver extends BroadcastReceiver {
    public static final String ACTION_NOTIFY_LAUNCH = "net.assemble.emailnotify.action.NOTIFY_CLICK";
    public static final String ACTION_NOTIFY_CANCEL = "net.assemble.emailnotify.action.NOTIFY_CANCEL";
    public static final String ACTION_STOP_SOUND = "net.assemble.emailnotify.action.NOTIFY_STOP_SOUND";
    public static final String ACTION_RENOTIFY = "net.assemble.emailnotify.action.RENOTIFY";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "received intent: " + intent.getAction());

        String action = intent.getAction();
        if (action != null) {
            String service = intent.getStringExtra("service");
            String mailbox = intent.getStringExtra("mailbox");
            if (action.startsWith(ACTION_NOTIFY_LAUNCH)) {
                // 通知停止
                EmailNotificationManager.clearNotification(mailbox);
                Intent launchIntent = new Intent().setClass(ctx, EmailNotifyLaunchActivity.class);
                launchIntent.putExtra("service", service);
                launchIntent.putExtra("mailbox", mailbox);
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(launchIntent);
            } else if (action.startsWith(ACTION_NOTIFY_CANCEL)) {
                // 通知停止
                EmailNotificationManager.clearNotification(mailbox);
            } else if (action.startsWith(ACTION_STOP_SOUND)) {
                // 通知停止
                EmailNotificationManager.stopNotificationSound(mailbox, EmailNotification.NOTIFY_SOUND);
            } else if (action.startsWith(ACTION_RENOTIFY)) {
                // 再通知
                EmailNotificationManager.renotifyNotification(mailbox);
            }
        }
    }

}
