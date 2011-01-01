package net.assemble.emailnotify;

import java.util.ArrayList;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class EmailNotificationManager {
    public static final int NOTIFICATIONID_ICON = 1;
    public static final int NOTIFICATIONID_EXPIRED = 2;
    public static final int NOTIFICATIONID_SUSPENDED = 3;
    public static final int NOTIFICATIONID_RESTORE_NETWORK = 4;
    public static final int NOTIFICATIONID_EMAIL_START = 10;

    private static boolean mNotificationIcon = false;
    private static ArrayList<EmailNotification> mNotifications = new ArrayList<EmailNotification>();

    /**
     * メール着信通知
     */
    public static void showNotification(Context ctx, String service, String mailbox, Date date) {
        EmailNotification emn = getNotification(mailbox, false);
        if (emn != null) {
            emn.clear();
        } else {
            emn = new EmailNotification(ctx, service, mailbox, date, getNextNotificationId());
            mNotifications.add(emn);
        }
        emn.start(false);
    }

    /**
     * メール着信通知テスト
     */
    public static void testNotification(Context ctx, String service, String mailbox) {
        EmailNotification emn = getNotification(mailbox, false);
        if (emn != null) {
            emn.clear();
        } else {
            emn = new EmailNotification(ctx, service, mailbox, null, getNextNotificationId());
            mNotifications.add(emn);
        }
        emn.start(true);
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
                id = emn.getNotificationId() + EmailNotification.NOTIFICATIONID_NUM;
            }
        }
        return id;
    }

    /**
     * メール着信音停止
     *
     * @param mailbox メールボックス名
     */
    public static void stopNotificationSound(String mailbox, int flags) {
        EmailNotification emn = getNotification(mailbox, false);
        if (emn != null) {
            emn.stop(flags);
        }
    }

    /**
     * メール通知停止(すべて)
     */
    public static void stopAllNotifications(int flags) {
        for (int i = 0; i < mNotifications.size(); i++) {
            EmailNotification emn = mNotifications.get(i);
            emn.stop(flags);
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
            emn.doNotify();
        }
    }

    /**
     * メール着信通知を消去
     */
    public static void clearNotification(String mailbox) {
        EmailNotification emn = getNotification(mailbox, true);
        if (emn != null) {
            emn.clear();
        }
    }

    /**
     * ネットワーク復元アイコンを表示
     */
    public static void showRestoreNetworkIcon(Context ctx) {
        NotificationManager notificationManager = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.restore,
                ctx.getResources().getString(R.string.app_name), System.currentTimeMillis());
        Intent restoreIntent = new Intent(ctx, EmailNotifyLaunchActivity.class);
        restoreIntent.setAction(EmailNotifyLaunchActivity.ACTION_RESTORE_NETWORK);
        restoreIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notification.setLatestEventInfo(ctx, ctx.getResources().getString(R.string.restore_network),
                ctx.getResources().getString(R.string.restore_network_message),
                PendingIntent.getActivity(ctx, 0, restoreIntent, 0));
        Intent deleteIntent = new Intent(ctx, EmailNotifyLaunchActivity.class);
        deleteIntent.setAction(EmailNotifyLaunchActivity.ACTION_KEEP_NETWORK);
        deleteIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notification.deleteIntent = PendingIntent.getActivity(ctx, 0, deleteIntent, 0);
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(NOTIFICATIONID_RESTORE_NETWORK, notification);
    }

    /**
     * 常駐アイコンを表示
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
        notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        notificationManager.notify(NOTIFICATIONID_ICON, notification);
        mNotificationIcon = true;
    }

    /**
     * 常駐アイコンを消去
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
     * 期限超過通知
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

    /**
     * 監視停止通知
     */
    public static void showSuspendedNotification(Context ctx) {
        NotificationManager notificationManager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.icon,
                      ctx.getResources().getString(R.string.app_name),
                      System.currentTimeMillis());
        Intent intent = new Intent();
        intent.setClass(ctx, EmailNotifyActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, intent, 0);
        String message = ctx.getResources().getString(R.string.notify_suspended);
        notification.setLatestEventInfo(ctx,
                ctx.getResources().getString(R.string.app_name),
                message, contentIntent);
        notification.defaults = Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE;
        notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONLY_ALERT_ONCE;
        notificationManager.notify(NOTIFICATIONID_SUSPENDED, notification);
    }

    /**
     * 監視停止通知を消去
     */
    public static void clearSuspendedNotification(Context ctx) {
        NotificationManager notificationManager =
                (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(NOTIFICATIONID_SUSPENDED);
    }

}
