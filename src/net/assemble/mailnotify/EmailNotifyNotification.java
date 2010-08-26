package net.assemble.mailnotify;

import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class EmailNotifyNotification {
    private static final int NOTIFICATIONID_ICON = 1;
    private static final int NOTIFICATIONID_EMAIL = 2;

    private static boolean mNotificationIcon = false;

    /**
     * バイブレーションパターン取得
     * 
     * @param pattern パターン
     * @param length 継続時間(秒)
     * @return バイブレーションパターン
     */
    private static long[] getVibrate(long[] pattern, long length) {
        ArrayList<Long> arr = new ArrayList<Long>();
        arr.add(new Long(0));
        long rest = length * 1000;
        while (rest > 0) {
            for (int j = 0; j < pattern.length; j++) {
                arr.add(new Long(pattern[j]));
                rest -= pattern[j];
            }
        }
        long[] vibrate = new long[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            vibrate[i] = arr.get(i);
        }
        return vibrate;
    }

    /**
     * 通知
     */
    public static void doNotify(Context ctx) {
        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.icon,
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
           notification.vibrate = getVibrate(
                   EmailNotifyPreferences.getVibrationPattern(ctx),
                   EmailNotifyPreferences.getVibrationLength(ctx));
        }
        notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
        notification.ledARGB = 0xff00ff00;
        notification.ledOnMS = 200;
        notification.ledOffMS = 2000;
        notificationManager.notify(NOTIFICATIONID_EMAIL, notification);
    }

    /**
     * 通知バーにアイコンを表示
     */
    public static void showNotificationIcon(Context ctx) {
        if (mNotificationIcon != false) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) 
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.notification_icon,
                ctx.getResources().getString(R.string.app_name), System.currentTimeMillis());
        Intent intent = new Intent(ctx, EmailNotifyActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, intent, 0);
        notification.setLatestEventInfo(ctx, ctx.getResources().getString(R.string.app_name),
                ctx.getResources().getString(R.string.notification_message), contentIntent);
        notification.flags |= Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        notificationManager.notify(NOTIFICATIONID_ICON, notification);
        mNotificationIcon = true;
    }

    /**
     * ノーティフィケーションバーのアイコンを消去
     */
    public static void clearNotificationIcon(Context ctx) {
        if (mNotificationIcon == false) {
            return;
        }
        NotificationManager notificationManager =
            (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATIONID_ICON);
        mNotificationIcon = false;
    }

}
