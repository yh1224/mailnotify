package net.assemble.emailnotify;

import net.assemble.android.MyLog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class EmailNotifyNotification {
    private static final String TAG = "EmailNotify";
    private static final int NOTIFICATIONID_EMAIL = 2;

    /**
     * 通知
     */
    public static void doNotify(Context ctx) {
        doNotify(ctx, null);
    }

    /**
     * 通知
     */
    public static void doNotify(Context ctx, String mailbox) {
        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification;

        notification = new Notification(R.drawable.icon,
                ctx.getResources().getString(R.string.app_name),
                System.currentTimeMillis());

        Intent intent = new Intent();
        ComponentName component = EmailNotifyPreferences.getComponent(ctx);
        if (component != null) {
            intent.setComponent(component);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            intent.setClass(ctx, EmailNotifyActivity.class);
        }
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, intent, 0);
        String message = ctx.getResources().getString(R.string.notify_text);
        if (mailbox != null) {
            message += "\n" + mailbox;
        }
//        Calendar cal = Calendar.getInstance();
//        message += " (" + cal.get(Calendar.HOUR_OF_DAY) + ":"
//                + cal.get(Calendar.MINUTE) + ")";
        notification.setLatestEventInfo(ctx,
                ctx.getResources().getString(R.string.app_name),
                message, contentIntent);
        notification.defaults = 0;
        String soundUri = EmailNotifyPreferences.getSound(ctx);
        if (soundUri.startsWith("content:")) {
            notification.sound = Uri.parse(soundUri);
        }
        if (EmailNotifyPreferences.getVibration(ctx)) {
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
        notification.ledARGB = 0xff00ff00;
        notification.ledOnMS = 200;
        notification.ledOffMS = 2000;
        notificationManager.notify(NOTIFICATIONID_EMAIL, notification);
        MyLog.d(ctx, TAG, "Notify: " + mailbox);
    }

}
