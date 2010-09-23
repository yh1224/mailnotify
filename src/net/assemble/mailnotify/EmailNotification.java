package net.assemble.mailnotify;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import net.assemble.android.MyLog;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

/**
 * メール着信通知
 */
public class EmailNotification {
    private static final String TAG = "EmailNotify";

    private Context mCtx;
    private Calendar mCal;
    private String mMailbox;
    private String mService;
    private int mNotificationId;
    private int mMailCount;
    private int mNotifyCount;
    private PendingIntent mRenotifyIntent;
    private PendingIntent mStopIntent;

    /**
     * Constructor
     *
     * @param ctx Context
     * @param mailbox メールボックス名
     */
    public EmailNotification(Context ctx, String service, String mailbox, int notificationId) {
        mCtx = ctx;
        mService = service;
        mMailbox = mailbox;
        mNotificationId = notificationId;
        mMailCount = 0;
        mNotifyCount = 0;
    }

    /**
     * メールボックス名を取得
     */
    public String getMailbox() {
        return mMailbox;
    }

    public int getNotificationId() {
        return mNotificationId;
    }

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
    public void doNotify(boolean sound) {
        NotificationManager notificationManager = (NotificationManager)
            mCtx.getSystemService(Context.NOTIFICATION_SERVICE);

        if (!sound) {
            // TODO: 音停止時は一旦通知を消す
            notificationManager.cancel(mNotificationId);
        }

        Notification notification;
        if (EmailNotifyPreferences.getNotifyView(mCtx, mService)) {
            notification = new Notification(R.drawable.icon,
                    mCtx.getResources().getString(R.string.app_name),
                    System.currentTimeMillis());
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm ");
            String message = sdf.format(mCal.getTime()) +
                mCtx.getResources().getString(R.string.notify_text) + " (" +
                mMailCount + mCtx.getResources().getString(R.string.mail_unit) + ")";
            if (mMailbox != null) {
                message += "\n" + mMailbox;
            }
            notification.setLatestEventInfo(mCtx,
                    mCtx.getResources().getString(R.string.app_name),
                    message, getPendingIntent(EmailNotificationReceiver.ACTION_NOTIFY_LAUNCH));
            notification.deleteIntent =
                    getPendingIntent(EmailNotificationReceiver.ACTION_NOTIFY_CANCEL);
        } else {
            notification = new Notification();
        }
        notification.defaults = 0;
        notification.flags = Notification.FLAG_AUTO_CANCEL;
        if (sound) {
            String soundUri = EmailNotifyPreferences.getNotifySound(mCtx, mService);
            if (soundUri.startsWith("content:")) {
                notification.sound = Uri.parse(soundUri);
            }
            if (EmailNotifyPreferences.getNotifyVibration(mCtx, mService)) {
                AudioManager audio = (AudioManager) mCtx.getSystemService(Context.AUDIO_SERVICE);
                if (!(EmailNotifyPreferences.getNotifyVibrationManneronly(mCtx, mService) &&
                        audio.getRingerMode() == AudioManager.RINGER_MODE_NORMAL)) {
                    notification.vibrate = getVibrate(
                            EmailNotifyPreferences.getNotifyVibrationPattern(mCtx, mService),
                            EmailNotifyPreferences.getNotifyVibrationLength(mCtx, mService));
                }
            }
            if (EmailNotifyPreferences.getNotifyLed(mCtx, mService)) {
                notification.flags |= Notification.FLAG_SHOW_LIGHTS;
                notification.ledARGB = EmailNotifyPreferences.getNotifyLedColor(mCtx, mService);
                notification.ledOnMS = 200;
                notification.ledOffMS = 2000;
            }
        }
        if (mMailCount > 1) {
            notification.number = mMailCount;
        }
        notificationManager.notify(mNotificationId, notification);

        if (!sound) {
            // 音停止時はここまで
            if (EmailNotify.DEBUG) Log.d(TAG, "Notify sound stopped. (notificationId=" + mNotificationId + ")");
            return;
        }

        mNotifyCount++;
        MyLog.d(mCtx, TAG, "Notified: " + mMailbox);
        if (EmailNotify.DEBUG) Log.d(TAG, "  (mailCount=" + mMailCount +
                ", notifyCount=" + mNotifyCount + ", notificationId=" + mNotificationId + ")");

        // 通知音停止タイマ
        int soundLen = EmailNotifyPreferences.getNotifySoundLength(mCtx, mService);
        if (soundLen > 0) {
            long next = SystemClock.elapsedRealtime() + soundLen * 1000;
            mStopIntent = getPendingIntent(EmailNotificationReceiver.ACTION_STOP);
            AlarmManager alarmManager = (AlarmManager) mCtx.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, next, mStopIntent);
            if (EmailNotify.DEBUG) Log.d(TAG, "Started stop timer for " + mMailbox);
        }

        // 再通知タイマ
        if (EmailNotifyPreferences.getNotifyView(mCtx, mService) &&
                EmailNotifyPreferences.getNotifyRenotify(mCtx, mService)) {
            int renotifyCount = EmailNotifyPreferences.getNotifyRenotifyCount(mCtx, mService);
            if (renotifyCount == 0 || mNotifyCount <= renotifyCount) {
                long next = SystemClock.elapsedRealtime();
                next += EmailNotifyPreferences.getNotifyRenotifyInterval(mCtx, mService) * 60 * 1000;
                mRenotifyIntent = getPendingIntent(EmailNotificationReceiver.ACTION_RENOTIFY);
                AlarmManager alarmManager = (AlarmManager) mCtx.getSystemService(Context.ALARM_SERVICE);
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, next, mRenotifyIntent);
                if (EmailNotify.DEBUG) Log.d(TAG, "Started renotify timer for " + mMailbox);
            } else {
                if (EmailNotify.DEBUG) Log.d(TAG, "Renotify count exceeded.");
            }
        }
    }

    /**
     * 通知を開始
     */
    public void start() {
        mCal = Calendar.getInstance();
        mMailCount++;
        doNotify(true);
    }

    /**
     * 通知を停止
     */
    public void stop() {
        if (mRenotifyIntent != null) {
            AlarmManager alarmManager = (AlarmManager) mCtx.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(mRenotifyIntent);
            mRenotifyIntent = null;
            if (EmailNotify.DEBUG) Log.d(TAG, "Canceled renotify timer for " + mMailbox);
        }
        NotificationManager notificationManager =
            (NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(mNotificationId);
    }

    /**
     * PendingIntentを生成
     */
    public PendingIntent getPendingIntent(String action) {
        Intent intent = new Intent(mCtx, EmailNotificationReceiver.class);
        intent.setAction(action + "." + mMailbox);
        intent.putExtra("service", mService);
        intent.putExtra("mailbox", mMailbox);
        return PendingIntent.getBroadcast(mCtx, 0, intent, 0);
    }

}
