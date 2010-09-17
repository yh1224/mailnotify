package net.assemble.mailnotify;

import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class EmailNotificationManager {
    public static final int NOTIFICATIONID_ICON = 1;
    public static final int NOTIFICATIONID_EXPIRED = 2;
    public static final int NOTIFICATIONID_EMAIL_START = 3;

    private static boolean mNotificationIcon = false;
    private static ArrayList<EmailNotification> mNotifications = new ArrayList<EmailNotification>();

    /**
     * メール着信通知
     */
    public static void showNotification(Context ctx, String service, String mailbox) {
        EmailNotification emn = getNotification(mailbox, false);
        if (emn != null) {
            emn.stop();
        } else {
            emn = new EmailNotification(ctx, service, mailbox, getNextNotificationId());
            mNotifications.add(emn);
        }
        emn.start();
    }

    /**
     * 通知検索
     *
     * @param mbox メールボックス名
     * @param remove true:リストから削除する false:しない
     * @return 通知
     */
    private static EmailNotification getNotification(String mbox, boolean remove) {
        for (int i = 0; i < mNotifications.size(); i++) {
            EmailNotification emn = mNotifications.get(i);
            String mailbox = emn.getMailbox();
            if (mailbox != null && mailbox.equals(mbox)) {
                if (remove) {
                    mNotifications.remove(i);
                }
                return emn;
            }
        }
        return null;
    }

    /**
     * 通知ID取得
     *
     * @return 通知ID
     */
    private static int getNextNotificationId() {
        int id = NOTIFICATIONID_EMAIL_START;
        for (int i = 0; i < mNotifications.size(); i++) {
            EmailNotification emn = mNotifications.get(i);
            if (id <= emn.getNotificationId()) {
                id = emn.getNotificationId() + 1;
            }
        }
        return id;
    }

    /**
     * メール着信音停止
     *
     * @param mailbox メールボックス名
     */
    public static void stopNotification(String mailbox) {
        EmailNotification emn = getNotification(mailbox, false);
        if (emn != null) {
            // 音なしで再通知
            emn.doNotify(false);
        }
    }

    /**
     * メール着信音停止(すべて)
     */
    public static void stopAllNotifications() {
        for (int i = 0; i < mNotifications.size(); i++) {
            EmailNotification emn = mNotifications.get(i);
            emn.doNotify(false);
        }
    }

    /**
     * メール着信再通知
     *
     * @param mailbox メールボックス名
     */
    public static void renotifyNotification(String mailbox) {
        EmailNotification emn = getNotification(mailbox, false);
        if (emn != null) {
            emn.doNotify(true);
        }
    }

    /**
     * メール着信通知を消去
     */
    public static void clearNotification(String mailbox) {
        EmailNotification emn = getNotification(mailbox, true);
        if (emn != null) {
            emn.stop();
        }
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

    /**
     * 通知
     */
    public static void showExpiredNotification(Context ctx) {
        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.icon,
                      ctx.getResources().getString(R.string.app_name),
                      System.currentTimeMillis());
        Intent intent = new Intent();
        intent.setClass(ctx, EmailNotifyActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, intent, 0);
        String message = ctx.getResources().getString(R.string.notify_expired);
        notification.setLatestEventInfo(ctx,
                ctx.getResources().getString(R.string.app_name),
                message, contentIntent);
        notification.defaults = Notification.DEFAULT_ALL;
        notification.flags = Notification.FLAG_SHOW_LIGHTS;
        notificationManager.notify(NOTIFICATIONID_EXPIRED, notification);
    }

}
