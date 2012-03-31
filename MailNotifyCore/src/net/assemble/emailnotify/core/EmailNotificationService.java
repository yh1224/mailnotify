package net.assemble.emailnotify.core;

import java.util.Date;

import net.orleaf.android.MyLog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * メール通知サービス
 */
public class EmailNotificationService extends Service {

    private static ComponentName mService;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        if (intent == null) {
            return;
        }

        Date logdate = null;
        if (intent.hasExtra("time")) {
            long time = intent.getLongExtra("time", 0);
            logdate = new Date(time);
            Log.d(EmailNotify.TAG, "time = " + logdate.toLocaleString());
        }
        WapPdu pdu = intent.getParcelableExtra("wapPdu");
        if (pdu != null) {
            // 記録
            long historyId = EmailNotificationHistoryDao.add(this, logdate,
                    pdu.getContentType(), pdu.getApplicationId(),
                    pdu.getMailbox(), pdu.getTimestampDate(),
                    pdu.getServiceName(), pdu.getHexString());
            if (historyId < 0) {
                MyLog.w(this, EmailNotify.TAG, "Duplicated: "
                        + "mailbox=" + pdu.getMailbox() + ", timestamp=" + pdu.getTimestampString());
                return;
            }

            if (!EmailNotifyPreferences.getService(this, pdu.getServiceName())) {
                // 非通知サービス
                MyLog.d(this, EmailNotify.TAG, "Ignored: This is exclude service.");
                EmailNotificationHistoryDao.ignored(this, historyId);
                return;
            }

            if (EmailNotifyPreferences.inExcludeHours(this, pdu.getServiceName())) {
                // 非通知時間帯
                MyLog.d(this, EmailNotify.TAG, "Ignored: This is exclude hours now.");
                // PENDING: あとで通知する?
                EmailNotificationHistoryDao.ignored(this, historyId);
                return;
            }

            // 通知
            EmailNotificationManager.showNotification(this,
                    pdu.getServiceName(), pdu.getMailbox(), pdu.getTimestampDate(), false);
        }
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
     * @param pdu WAP PDU
     */
    public static void restoreNotifications(Context ctx) {
        Cursor cur = EmailNotificationHistoryDao.getActiveHistories(ctx);
        if (cur.moveToFirst()) {
            do {
                long id = cur.getLong(cur.getColumnIndex(BaseColumns._ID));
                MyLog.d(ctx, EmailNotify.TAG, "Restoring notification: " + id);
                String mailbox = cur.getString(cur.getColumnIndex("mailbox"));
                Date timestampDate = null;
                long timestamp = cur.getLong(cur.getColumnIndex("timestamp"));
                String serviceName = cur.getString(cur.getColumnIndex("service_name"));
                if (timestamp > 0) {
                    timestampDate = new Date(timestamp * 1000);
                }
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
     * @param ctx
     * @param date 検出日時
     * @param wapPdu
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
