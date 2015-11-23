package net.assemble.emailnotify.core.notification;

import java.util.Date;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.util.Log;

import net.assemble.emailnotify.core.EmailNotify;
import net.assemble.emailnotify.core.WapPdu;
import net.assemble.emailnotify.core.preferences.EmailNotifyPreferences;
import net.orleaf.android.MyLog;

/**
 * メール着信通知サービス
 */
public class EmailNotificationService extends Service {

    private static ComponentName mService;

    private Handler handler = new Handler();
    private boolean mPending;

    @Override
    public void onCreate() {
        super.onCreate();
        mPending = false;
    }

    // This is the old onStart method that will be called on the pre-2.0
    // platform.  On 2.0 or later we override onStartCommand() so this
    // method will not be called.
    @SuppressWarnings("deprecation")
    @Override
    public void onStart(Intent intent, int startId) {
        handleCommand(intent);
    }

    @TargetApi(5)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    private void handleCommand(Intent intent) {
        if (intent == null) {
            return;
        }

        final Date logdate;
        if (intent.hasExtra("time")) {
            long time = intent.getLongExtra("time", 0);
            logdate = new Date(time);
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "time = " + logdate.toLocaleString());
        } else {
            logdate = null;
        }
        final WapPdu pdu = intent.getParcelableExtra("wapPdu");
        if (pdu != null) {
            // サポートフラグ記録
            EmailNotifyPreferences.setNotifySupport(this, pdu.getServiceName());
            EmailNotifyPreferences.incrementNotifyCount(this, pdu.getServiceName());

            if (pdu.getServiceName().equals(EmailNotifyPreferences.SERVICE_UNSPEC)) {
                // サービス不明の場合、ちょっと待ってから通知
                mPending = true;
                MyLog.d(EmailNotificationService.this, EmailNotify.TAG, "Pending notification.");
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mPending) {
                            doNotify(logdate, pdu);
                        } else {
                            MyLog.w(EmailNotificationService.this, EmailNotify.TAG, "Pending notification dropped.");
                        }
                    }
                }, 4000);
            } else {
                doNotify(logdate, pdu);
            }
        }
    }

    /**
     * 記録して通知
     */
    private void doNotify(Date logdate, WapPdu pdu) {
        mPending = false;
        String mbox = pdu.getMailbox() + " (" + pdu.getTimestampString() + ")";

        // 履歴に記録
        long historyId = EmailNotificationHistoryDao.add(this, logdate,
                pdu.getContentType(), pdu.getApplicationId(),
                pdu.getMailbox(), pdu.getTimestampDate(),
                pdu.getServiceName(), pdu.getHexString());
        if (historyId < 0) {
            MyLog.w(this, EmailNotify.TAG, "Duplicated: " + mbox);
            return;
        }

        if (!EmailNotifyPreferences.getService(this, pdu.getServiceName())) {
            // 非通知サービス
            MyLog.d(this, EmailNotify.TAG, "<" + historyId + "> Ignored(service): " + mbox);
            EmailNotificationHistoryDao.ignored(this, historyId);
            return;
        }

        if (EmailNotifyPreferences.inExcludeHours(this, pdu.getServiceName())) {
            // 非通知時間帯
            MyLog.d(this, EmailNotify.TAG, "<" + historyId + "> Ignored(hours): " + mbox);
            // PENDING: あとで通知する?
            EmailNotificationHistoryDao.ignored(this, historyId);
            return;
        }

        MyLog.d(this, EmailNotify.TAG, "<" + historyId + "> " + mbox);

        // 通知
        EmailNotificationManager.showNotification(this,
                pdu.getServiceName(), pdu.getMailbox(), pdu.getTimestampDate(), false);
    }

    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    /**
     * 未消去の通知を復元する
     *
     * @param ctx Context
     */
    public static void restoreNotifications(Context ctx) {
        Cursor cur = EmailNotificationHistoryDao.getActiveHistories(ctx);
        if (cur.moveToFirst()) {
            do {
                long id = cur.getLong(cur.getColumnIndex(BaseColumns._ID));
                String mailbox = cur.getString(cur.getColumnIndex("mailbox"));
                Date timestampDate = null;
                long timestamp = cur.getLong(cur.getColumnIndex("timestamp"));
                String serviceName = cur.getString(cur.getColumnIndex("service_name"));
                if (timestamp > 0) {
                    timestampDate = new Date(timestamp * 1000);
                }
                MyLog.d(ctx, EmailNotify.TAG, "<" + id + "> Restore: " + mailbox);
                if ((cur.getLong(cur.getColumnIndex("notified_at"))) == 0) {
                    // 未通知なら改めて通知
                    EmailNotificationManager.showNotification(ctx, serviceName, mailbox, timestampDate, false);
                } else {
                    // 通知済みなら表示のみ
                    EmailNotificationManager.showNotification(ctx, serviceName, mailbox, timestampDate, true);
                }
            } while (cur.moveToNext());
        }
    }


    /**
     * サービス開始
     *
     * @param ctx Context
     * @param date 検出日時
     * @param wapPdu WAP PDU
     */
    public static boolean startService(Context ctx, Date date, WapPdu wapPdu) {
        boolean result;
        Intent intent = new Intent(ctx, EmailNotificationService.class);
        if (date != null) {
            intent.putExtra("time", date.getTime());
        }
        intent.putExtra("wapPdu", wapPdu);
        mService = ctx.startService(intent);
        if (mService == null) {
            Log.e(EmailNotify.TAG, "EmailNotificationService start failed!");
            result = false;
        } else {
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "EmailNotificationService started: " + mService);
            result = true;
        }
        return result;
    }

}
