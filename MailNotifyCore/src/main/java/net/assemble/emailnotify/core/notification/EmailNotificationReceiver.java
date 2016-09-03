package net.assemble.emailnotify.core.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import net.assemble.emailnotify.core.BuildConfig;
import net.assemble.emailnotify.core.EmailNotify;
import net.assemble.emailnotify.core.R;

/**
 * メール着信通知からのインテント受信
 */
public class EmailNotificationReceiver extends BroadcastReceiver {
    public static final String ACTION_NOTIFY_LAUNCH = "net.assemble.emailnotify.action.NOTIFY_CLICK";
    public static final String ACTION_NOTIFY_CANCEL = "net.assemble.emailnotify.action.NOTIFY_CANCEL";
    public static final String ACTION_STOP_SOUND = "net.assemble.emailnotify.action.NOTIFY_STOP_SOUND";
    public static final String ACTION_RENOTIFY = "net.assemble.emailnotify.action.RENOTIFY";
    public static final String ACTION_SMS_SENT = "net.assemble.emailnotify.action.SMS_SENT";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "received intent: " + intent.getAction());

        String action = intent.getAction();
        if (action != null) {
            String service = intent.getStringExtra("service");
            String mailbox = intent.getStringExtra("mailbox");
            if (action.startsWith(ACTION_NOTIFY_LAUNCH)) {
                Intent launchIntent = new Intent();
                launchIntent.setClassName(ctx.getPackageName(), ctx.getPackageName() + ".EmailNotifyLaunchActivity");
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
            } else if (action.startsWith(ACTION_SMS_SENT)) {
                // 再通知
                if (getResultCode() != 0) {
                    Toast.makeText(ctx, R.string.notify_sms_failed, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

}
