package net.assemble.emailnotify;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

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

    public static final int NOTIFY_SOUND = 0x00000001;
    public static final int NOTIFY_LED = 0x00000002;
    public static final int NOTIFY_RENOTIFY = 0x00000004;
    public static final int NOTIFY_ALL = NOTIFY_SOUND | NOTIFY_LED | NOTIFY_RENOTIFY;

    public static final int NOTIFICATIONID_NUM = 3;
    private static final int NOTIFICATIONID_MAIN = 0;
    private static final int NOTIFICATIONID_SOUND = 1;
    private static final int NOTIFICATIONID_LED = 2;

    private Context mCtx;
    private String mMailbox;
    private String mService;
    private Date mReceiveDate;
    private long mNotifyTime;
    private int mNotificationId;
    private int mMailCount;
    private int mNotifyCount;
    private int mActiveNotify;
    private PendingIntent mRenotifyIntent;
    private PendingIntent mStopIntent;

    /**
     * Constructor
     *
     * @param ctx Context
     * @param mailbox メールボックス名
     */
    public EmailNotification(Context ctx, String service, String mailbox, Date date, int notificationId) {
        mCtx = ctx;
        mService = service;
        mMailbox = mailbox;
        if (date == null) {
            mReceiveDate = Calendar.getInstance().getTime();
        } else {
            mReceiveDate = date;
        }
        mNotificationId = notificationId;
        mMailCount = 0;
        mNotifyCount = 0;
        mActiveNotify = 0;
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
    public void doNotify() {
        NotificationManager notificationManager = (NotificationManager)
            mCtx.getSystemService(Context.NOTIFICATION_SERVICE);

        // 通知
        if (EmailNotifyPreferences.getNotifyView(mCtx, mService)) {
            Notification notification = new Notification(R.drawable.icon,
                    mCtx.getResources().getString(R.string.notify_text), mNotifyTime);
            String message = "(" + mMailCount + mCtx.getResources().getString(R.string.mail_unit) + ")";
            if (mMailbox != null) {
                message += " " + mMailbox;
            }
            notification.setLatestEventInfo(mCtx,
                    mCtx.getResources().getString(R.string.app_name),
                    message, getPendingIntent(EmailNotificationReceiver.ACTION_NOTIFY_LAUNCH));
            notification.deleteIntent =
                    getPendingIntent(EmailNotificationReceiver.ACTION_NOTIFY_CANCEL);
            if (mMailCount > 1) {
                notification.number = mMailCount;
            }
            notification.defaults = 0;
            notification.flags = Notification.FLAG_AUTO_CANCEL;
            notificationManager.notify(mNotificationId + NOTIFICATIONID_MAIN, notification);
        }

        // 通知音・バイブレーション・LEDは別途通知
        startSound();
        startLed();

        mNotifyCount++;
        MyLog.d(mCtx, TAG, "[" + mNotificationId + "] Notified: " + mMailbox + " " + mService);
        if (EmailNotify.DEBUG) Log.d(TAG, "  (mailCount=" + mMailCount +
                ", notifyCount=" + mNotifyCount + ", notificationId=" + mNotificationId + ")");

        // 通知音停止タイマ
        int soundLen = EmailNotifyPreferences.getNotifySoundLength(mCtx, mService);
        if (soundLen > 0) {
            long next = SystemClock.elapsedRealtime() + soundLen * 1000;
            mStopIntent = getPendingIntent(EmailNotificationReceiver.ACTION_STOP_SOUND);
            AlarmManager alarmManager = (AlarmManager) mCtx.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, next, mStopIntent);
            if (EmailNotify.DEBUG) Log.d(TAG, "[" + mNotificationId + "] Started stop timer for " + mMailbox);
        }

        // 再通知タイマ
        if (EmailNotifyPreferences.getNotifyView(mCtx, mService) &&
                EmailNotifyPreferences.getNotifyRenotify(mCtx, mService)) {
            int renotifyCount = EmailNotifyPreferences.getNotifyRenotifyCount(mCtx, mService);
            int interval = EmailNotifyPreferences.getNotifyRenotifyInterval(mCtx, mService) * 60;
            if (renotifyCount == 0 || mNotifyCount <= renotifyCount) {
                long next = SystemClock.elapsedRealtime() + interval * 1000;
                mRenotifyIntent = getPendingIntent(EmailNotificationReceiver.ACTION_RENOTIFY);
                AlarmManager alarmManager = (AlarmManager) mCtx.getSystemService(Context.ALARM_SERVICE);
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, next, mRenotifyIntent);
                mActiveNotify |= NOTIFY_RENOTIFY;
                if (EmailNotify.DEBUG) Log.d(TAG, "[" + mNotificationId + "] Started renotify timer for " +
                        mMailbox + " (" + interval + "sec.), active=" + mActiveNotify);
            } else {
                if (EmailNotify.DEBUG) Log.d(TAG, "[" + mNotificationId + "] Renotify count exceeded.");
            }
        }
    }

    /**
     * iMoNiでメールチェックをおこなう
     */
    public void notifyImoni() {
        if (EmailNotifyPreferences.getIntentToImoni(mCtx, mService)) {
            Intent intent = new Intent();
            intent.setAction("net.grandnature.android.imodenotifier.ACTION_CHECK");
            PendingIntent.getBroadcast(mCtx, 0, intent, 0);
            mCtx.sendBroadcast(intent);
            MyLog.d(mCtx, TAG, "Sent intent ACTION_CHECK to iMoNi.");
        }
    }

    /**
     * 通知を開始
     */
    public void start(boolean skipDelay) {
        mNotifyTime = System.currentTimeMillis();
        mMailCount++;

        // iMoNiに通知
        notifyImoni();

        // 通知遅延 (再通知タイマを使う)
        if (!skipDelay) {
            int delay = EmailNotifyPreferences.getNotifyDelay(mCtx, mService);
            if (delay > 0) {
                long next = SystemClock.elapsedRealtime() + delay * 1000;
                mRenotifyIntent = getPendingIntent(EmailNotificationReceiver.ACTION_RENOTIFY);
                AlarmManager alarmManager = (AlarmManager) mCtx.getSystemService(Context.ALARM_SERVICE);
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, next, mRenotifyIntent);
                if (EmailNotify.DEBUG) Log.d(TAG, "[" + mNotificationId +
                        "] Started notify delay timer for " + mMailbox + " (" + delay + " sec.)");
                return;
            }
        }

        doNotify();
    }

    /**
     * 通知音・バイブレーションを通知
     */
    private void startSound() {
        NotificationManager notificationManager = (NotificationManager)
            mCtx.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = new Notification();
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
        notificationManager.notify(mNotificationId + NOTIFICATIONID_SOUND, notification);
        mActiveNotify |= NOTIFY_SOUND;
        if (EmailNotify.DEBUG) Log.d(TAG, "[" + mNotificationId + "] Started sound for " + mMailbox + ", active=" + mActiveNotify);
    }

    /**
     * 通知音・バイブレーションを停止
     */
    private void stopSound() {
        if ((mActiveNotify & NOTIFY_SOUND) != 0) {
            NotificationManager notificationManager =
                (NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(mNotificationId + NOTIFICATIONID_SOUND);
            mActiveNotify &= ~NOTIFY_SOUND;
            if (EmailNotify.DEBUG) Log.d(TAG, "[" + mNotificationId + "] Stopped sound for " + mMailbox + ", active=" + mActiveNotify);
        }
    }

    /**
     * LEDを通知
     */
    private void startLed() {
        if (EmailNotifyPreferences.getNotifyLed(mCtx, mService)) {
            NotificationManager notificationManager = (NotificationManager)
                mCtx.getSystemService(Context.NOTIFICATION_SERVICE);

            Notification notification = new Notification();
            notification.flags = Notification.FLAG_SHOW_LIGHTS;
            notification.ledARGB = EmailNotifyPreferences.getNotifyLedColor(mCtx, mService);
            notification.ledOnMS = 200;
            notification.ledOffMS = 2000;
            notificationManager.notify(mNotificationId + NOTIFICATIONID_LED, notification);
            mActiveNotify |= NOTIFY_LED;
            if (EmailNotify.DEBUG) Log.d(TAG, "[" + mNotificationId + "] Started led for " + mMailbox + ", active=" + mActiveNotify);
        }
    }

    /**
     * LEDを停止
     */
    private void stopLed() {
        if ((mActiveNotify & NOTIFY_LED) != 0) {
            NotificationManager notificationManager =
                (NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(mNotificationId + NOTIFICATIONID_LED);
            mActiveNotify &= ~NOTIFY_LED;
            if (EmailNotify.DEBUG) Log.d(TAG, "[" + mNotificationId + "] Stopped led for " + mMailbox + ", active=" + mActiveNotify);
        }
    }

    /**
     * 再通知を停止
     */
    private void stopRenotify() {
        if (mRenotifyIntent != null) {
            AlarmManager alarmManager = (AlarmManager) mCtx.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(mRenotifyIntent);
            mRenotifyIntent = null;
            mActiveNotify &= ~NOTIFY_RENOTIFY;
            if (EmailNotify.DEBUG) Log.d(TAG, "[" + mNotificationId + "] Stopped renotify timer for " + mMailbox + ", active=" + mActiveNotify);
        }
    }

    /**
     * 通知音・バイブレーション・再通知を停止
     */
    public void stop(int flags) {
        if ((flags & NOTIFY_SOUND) != 0) {
            stopSound();
        }
        if ((flags & NOTIFY_LED) != 0) {
            stopLed();
        }
        if ((flags & NOTIFY_RENOTIFY) != 0) {
            stopRenotify();
        }
    }

    /**
     * 通知を消去
     */
    public void clear() {
        // 通知を消すときもiMoNiに通知
        notifyImoni();

        stop(NOTIFY_ALL);
        NotificationManager notificationManager =
            (NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(mNotificationId + NOTIFICATIONID_MAIN);
        if (EmailNotify.DEBUG) Log.d(TAG, "[" + mNotificationId + "] Cleared notification for " + mMailbox);
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
