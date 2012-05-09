package net.assemble.emailnotify.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import net.orleaf.android.MyLog;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

/**
 * メール着信通知
 */
public class EmailNotification {
    private static final String PACKAGE_NAME_IMONI = "net.grandnature.android.imodenotifier";
    private static final String ACTION_IMONI_CHECK = "net.grandnature.android.imodenotifier.ACTION_CHECK";

    public static final int NOTIFY_SOUND = 0x00000001;
    public static final int NOTIFY_LED = 0x00000002;
    public static final int NOTIFY_RENOTIFY = 0x00000004;
    public static final int NOTIFY_ALL = NOTIFY_SOUND | NOTIFY_LED | NOTIFY_RENOTIFY;

    public static final int NOTIFICATIONID_NUM = 3;
    private static final int NOTIFICATIONID_ICON = 0;
    private static final int NOTIFICATIONID_SOUND = 1;
    private static final int NOTIFICATIONID_LED = 2;

    private Context mCtx;
    private ImoniNotifier mImoniNotifier = null;

    private String mMailbox;        // メールボックス名
    private String mService;        // メールサービス名
    private Date mTimestamp;        // タイムスタンプ
    private long mNotifyTime;       // 通知時刻
    private int mNotificationId;    // 通知ID

    private int mMailCount;         // メール数
    private int mNotifyCount;       // 通知回数
    private int mActiveNotify;      // 現在アクティブな通知種別 (MAIN/SOUND/LED)

    private PendingIntent mRenotifyIntent;
    private PendingIntent mStopIntent;

    /**
     * Constructor
     *
     * @param ctx Context
     * @param mailbox メールボックス名
     * @param timestamp タイムスタンプ
     * @param notificationId 通知ID
     */
    public EmailNotification(Context ctx, String service, String mailbox, Date timestamp, int notificationId) {
        mCtx = ctx;
        mService = service;
        mMailbox = mailbox;
        mTimestamp = timestamp;
        if (timestamp == null) {
            mNotifyTime = Calendar.getInstance().getTimeInMillis();
        } else {
            mNotifyTime = timestamp.getTime();
        }
        mNotificationId = notificationId;
        mMailCount = 1;
        mNotifyCount = 0;
        mActiveNotify = 0;
    }

    /**
     * 通知情報を更新
     *
     * @param timestamp タイムスタンプ
     */
    public void update(Date timestamp) {
        mTimestamp = timestamp;
        if (timestamp == null) {
            mNotifyTime = Calendar.getInstance().getTimeInMillis();
        } else {
            mNotifyTime = timestamp.getTime();
        }
        mMailCount++;
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
        EmailNotificationHistoryDao.notified(mCtx, mMailbox);

        // 通知
        startIcon();
        startSound();
        startLed();

        mNotifyCount++;
        MyLog.d(mCtx, EmailNotify.TAG, "[" + mNotificationId + "] Notified: " + mMailbox +
                " (service=" + mService + ", mail=" + mMailCount + ", notify=" + mNotifyCount + ")");

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
                if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "[" + mNotificationId + "] Started renotify timer for " +
                        mMailbox + " (" + interval + "sec.), active=" + mActiveNotify);
            } else {
                if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "[" + mNotificationId + "] Renotify count exceeded.");
            }
        }
    }

    /**
     * 受信通知をブロードキャスト
     */
    public void broadcastNotify() {
        Intent intent = new Intent();
        intent.setAction(EmailNotify.ACTION_MAIL_PUSH_RECEIVED);
        intent.putExtra("service", mService);
        intent.putExtra("mailbox", mMailbox);
        intent.putExtra("timestamp", mTimestamp);
        intent.putExtra("count", mMailCount);
        mCtx.sendBroadcast(intent);
        if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Sent broadcast MAIL_PUSH_RECEIVED");
    }

    /**
     * 通知を開始
     */
    public void start(boolean skipDelay) {
        mNotifyCount = 0;

        if (mActiveNotify > 0) {
            stop(NOTIFY_ALL);
        }

        // 通知をブロードキャスト
        broadcastNotify();

        // iMoNiに通知
        ComponentName app = EmailNotifyPreferences.getNotifyLaunchAppComponent(mCtx, mService);
        if (app != null && app.getPackageName().equals(PACKAGE_NAME_IMONI)) {
            mImoniNotifier = new ImoniNotifier();
            mImoniNotifier.start();
         }

        // SMSで通知
        if (EmailNotifyPreferences.getNotifySms(mCtx, mService)) {
            String destAddr =  EmailNotifyPreferences.getNotifySmsTel(mCtx, mService);
            try {
                PendingIntent sentNotify = getPendingIntent(EmailNotificationReceiver.ACTION_SMS_SENT);
                SmsManager.getDefault().sendTextMessage(destAddr, null,
                        mCtx.getString(R.string.app_name) + "\n" + mMailbox, sentNotify, null);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(mCtx, R.string.notify_sms_failed, Toast.LENGTH_LONG).show();
            }
        }

        // 通知遅延 (再通知タイマを使う)
        if (!skipDelay) {
            int delay = EmailNotifyPreferences.getNotifyDelay(mCtx, mService);
            if (delay > 0) {
                long next = SystemClock.elapsedRealtime() + delay * 1000;
                mRenotifyIntent = getPendingIntent(EmailNotificationReceiver.ACTION_RENOTIFY);
                AlarmManager alarmManager = (AlarmManager) mCtx.getSystemService(Context.ALARM_SERVICE);
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, next, mRenotifyIntent);
                if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "[" + mNotificationId +
                        "] Started notify delay timer for " + mMailbox + " (" + delay + " sec.)");
                MyLog.d(mCtx, EmailNotify.TAG, "[" + mNotificationId +
                        "] Delayed: " + mMailbox + " (" + delay + " sec.)");
                return;
            }
        }

        doNotify();
    }

    /**
     * 通知を復元
     */
    public void restore() {
        startIcon();
        MyLog.d(mCtx, EmailNotify.TAG, "[" + mNotificationId + "] Restored: " + mMailbox +
                " (service=" + mService + ", mail=" + mMailCount + ")");
    }

    /**
     * 通知アイコン表示
     */
    private void startIcon() {
        if (!EmailNotifyPreferences.getNotifyView(mCtx, mService)) {
            // 消すタイミングがないので消去済みとみなす
            EmailNotificationHistoryDao.cleared(mCtx, mMailbox);
            return;
        }

        NotificationManager notificationManager = (NotificationManager)
            mCtx.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = new Notification(R.drawable.icon,
                mCtx.getResources().getString(R.string.notify_text), mNotifyTime);
        String message = "(" + mMailCount + mCtx.getResources().getString(R.string.mail_unit) + ")";
        if (mMailbox.startsWith("imap://docomo.ne.jp")) {
            // iモードの場合は表示を変更 (imap://docomo.ne.jp?PI=06 とか意味不明なので)
            message += " docomo.ne.jp";
        } else if (mMailbox != null) {
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
        notification.flags = 0;
        notificationManager.notify(mNotificationId + NOTIFICATIONID_ICON, notification);
        if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "[" + (mNotificationId + NOTIFICATIONID_ICON) +
                "] Started icon for " + mMailbox + ", active=" + mActiveNotify);
    }

    /**
     * 通知音・バイブレーションを開始
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
        if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "[" + (mNotificationId + NOTIFICATIONID_SOUND) +
                "] Started sound for " + mMailbox + ", active=" + mActiveNotify);

        // 通知音停止タイマ
        int soundLen = EmailNotifyPreferences.getNotifySoundLength(mCtx, mService);
        if (soundLen > 0) {
            long next = SystemClock.elapsedRealtime() + soundLen * 1000;
            mStopIntent = getPendingIntent(EmailNotificationReceiver.ACTION_STOP_SOUND);
            AlarmManager alarmManager = (AlarmManager) mCtx.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, next, mStopIntent);
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "[" + (mNotificationId + NOTIFICATIONID_SOUND) +
                    "] Started stop timer for " + mMailbox);
        }
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
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "[" + (mNotificationId + NOTIFICATIONID_SOUND) +
                    "] Stopped sound for " + mMailbox + ", active=" + mActiveNotify);
        }
    }

    /**
     * LEDを開始
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
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "[" + (mNotificationId + NOTIFICATIONID_LED) +
                    "] Started led for " + mMailbox + ", active=" + mActiveNotify);
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
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "[" + (mNotificationId + NOTIFICATIONID_LED) +
                    "] Stopped led for " + mMailbox + ", active=" + mActiveNotify);
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
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "[" + mNotificationId +
                    "] Stopped renotify timer for " + mMailbox + ", active=" + mActiveNotify);
        }
    }

    /**
     * 通知音・バイブレーション・再通知を停止
     */
    public void stop(int flags) {
        if (mNotifyCount == 0) {
            // 未通知は除外
            return;
        }

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
        if (mImoniNotifier != null) {
            mImoniNotifier.stop();
        }

        stop(NOTIFY_ALL);
        NotificationManager notificationManager =
            (NotificationManager) mCtx.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(mNotificationId + NOTIFICATIONID_ICON);
        if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "[" + (mNotificationId + NOTIFICATIONID_ICON) + 
                "] Stopped icon for " + mMailbox);

        MyLog.d(mCtx, EmailNotify.TAG, "[" + mNotificationId + "] Cleared: " + mMailbox);

        // 消去の記録
        EmailNotificationHistoryDao.cleared(mCtx, mMailbox);
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

    /**
     * IMoNi で新着メールをチェック
     * ネットワーク未接続時は接続完了を待って通知する。
     */
    private class ImoniNotifier {
        private ConnectivityManager mConnManager;
        private BroadcastReceiver mConnectivityReceiver = null;

        public ImoniNotifier() {
            mConnManager = (ConnectivityManager) mCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        /**
         * 開始
         */
        public void start() {
            if (checkAndNotify()) {
                // 通知完了
                return;
            }

            // 接続状態監視
            mConnectivityReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "received intent: " + intent.getAction());
                    checkAndNotify();
                }
            };
            mCtx.registerReceiver(mConnectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Started observing connection by ImoniNotifier");
        }

        /**
         * 接続中であれば通知
         *
         * @return true:通知完了 false:未通知
         */
        private boolean checkAndNotify() {
            NetworkInfo networkInfo = mConnManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                mCtx.sendBroadcast(new Intent(ACTION_IMONI_CHECK));
                MyLog.d(mCtx, EmailNotify.TAG, "Sent intent ACTION_CHECK to IMoNi.");
                stop();
                return true;
            }
            return false;
        }

        /**
         * 停止
         */
        public void stop() {
            if (mConnectivityReceiver != null) {
                mCtx.unregisterReceiver(mConnectivityReceiver);
                mConnectivityReceiver = null;
                if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Stopped observing connection by ImoniNotifier");
            }
        }
    }

}
