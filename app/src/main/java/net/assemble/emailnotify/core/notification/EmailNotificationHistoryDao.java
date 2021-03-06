package net.assemble.emailnotify.core.notification;

import java.util.Calendar;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

/**
 * メール着信通知履歴データベース操作
 */
public class EmailNotificationHistoryDao {
    /**
     * ログ保持数
     */
    private static final long LOG_ROTATE_LIMIT = 100;

    /**
     * 重複判定閾値(秒)
     * *timestampが存在しない場合*の重複チェックでは、
     * 保存日時がこの時間内であれば重複とみなす。
     */
    private static final int DUPLICATE_CHECK_THRESHOLD_SEC = 15;

    private static SQLiteDatabase mDb;

    /**
     * データベースを取得
     */
    public static SQLiteDatabase getDb(Context ctx) {
        if (mDb == null) {
            EmailNotificationHistoryOpenHelper h = new EmailNotificationHistoryOpenHelper(ctx);
            mDb = h.getWritableDatabase();
        }
        return mDb;
    }

    /**
     * メール受信履歴を取得
     *
     * @param ctx Context
     * @return Cursor
     */
    public static Cursor getHistories(Context ctx) {
        return getDb(ctx).query(EmailNotificationHistoryOpenHelper.TABLE_NAME,
                null, null, null,
                null, null, "created_at desc, _id desc", null);
    }

    /**
     * 未消去のメール受信履歴を取得
     *
     * @param ctx Context
     * @return Cursor
     */
    public static Cursor getActiveHistories(Context ctx) {
        return getDb(ctx).query(EmailNotificationHistoryOpenHelper.TABLE_NAME,
                null, "cleared_at IS NULL", null, null, null, null);
    }

    /**
     * メール受信履歴を1件取得
     *
     * @param ctx Context
     * @param id ID
     * @return Cursor
     */
    public static Cursor getHistory(Context ctx, long id) {
        return getDb(ctx).query(EmailNotificationHistoryOpenHelper.TABLE_NAME,
                null, BaseColumns._ID + " = " + id, null,
                null, null, null, null);
    }

    /**
     * 古いログを消去する
     */
    private static void rotate(Context ctx, long id) {
        if (id > LOG_ROTATE_LIMIT) {
            id -= LOG_ROTATE_LIMIT;
            // 消去済みに限る
            getDb(ctx).delete(EmailNotificationHistoryOpenHelper.TABLE_NAME,
                    BaseColumns._ID + " <= " + id + " AND cleared_at IS NOT NULL", null);
        }
    }

    /**
     * メール受信を記録
     *
     * @param ctx Context
     * @param logdate ログチェック日時
     * @param contentType Content-Type
     * @param applicationId X-Wap-Application-Id
     * @param mailbox メールボックス名
     * @param timestamp タイムスタンプ
     * @param serviceName サービス名
     * @param wap_data WAP data
     * @return ID (<0:同じ通知がすでに存在するため記録せず)
     */
    public static long add(Context ctx, Date logdate, String contentType, String applicationId, String mailbox, Date timestamp, String serviceName, String wap_data) {
        long id = -1;
        getDb(ctx).beginTransaction();
        try {
            if (!exists(ctx, mailbox, timestamp)) {
                ContentValues values = new ContentValues();
                values.put("created_at", Calendar.getInstance().getTimeInMillis() / 1000);
                values.put("content_type", contentType);
                values.put("application_id", applicationId);
                if (logdate != null) {
                    values.put("logged_at", logdate.getTime() / 1000);
                }
                if (mailbox != null) {
                    values.put("mailbox", mailbox);
                }
                if (timestamp != null) {
                    values.put("timestamp", timestamp.getTime() / 1000);
                }
                if (serviceName != null) {
                    values.put("service_name", serviceName);
                }
                values.put("wap_data", wap_data);
                id = getDb(ctx).insert(EmailNotificationHistoryOpenHelper.TABLE_NAME, null, values);
                rotate(ctx, id);
                getDb(ctx).setTransactionSuccessful();
            }
        } finally {
            getDb(ctx).endTransaction();
        }
        return id;
    }

    /**
     * メール受信を通知したことを記録
     *
     * @param ctx Context
     * @param mailbox メールボックス名
     */
    public static void notified(Context ctx, String mailbox) {
        ContentValues values = new ContentValues();
        values.put("notified_at", Calendar.getInstance().getTimeInMillis() / 1000);
        getDb(ctx).update(EmailNotificationHistoryOpenHelper.TABLE_NAME,
                values, "mailbox = ? AND notified_at IS NULL", new String[] { mailbox });
    }

    /**
     * メール受信通知を消去したことを記録
     *
     * @param ctx Context
     * @param mailbox メールボックス名
     */
    public static void cleared(Context ctx, String mailbox) {
        ContentValues values = new ContentValues();
        values.put("cleared_at", Calendar.getInstance().getTimeInMillis() / 1000);
        // そのメールボックスに対する通知済み、かつ未消去の通知すべて
        getDb(ctx).update(EmailNotificationHistoryOpenHelper.TABLE_NAME,
                values, "mailbox = ? AND notified_at IS NOT NULL AND cleared_at IS NULL",
                new String[] { mailbox });
    }

    /**
     * メール受信通知を無視したことを記録
     *
     * @param ctx Context
     * @param id ID
     */
    public static void ignored(Context ctx, long id) {
        ContentValues values = new ContentValues();
        values.put("cleared_at", Calendar.getInstance().getTimeInMillis() / 1000);
        getDb(ctx).update(EmailNotificationHistoryOpenHelper.TABLE_NAME,
                values, BaseColumns._ID + " = " + id, null);
    }

    /**
     * 重複チェック
     *
     * @param ctx Context
     * @param mailbox メールボックス名
     * @param timestamp タイムスタンプ
     * @return true:すでに存在する
     */
    public static boolean exists(Context ctx, String mailbox, Date timestamp) {
        if (mailbox == null/* || timestamp == null*/) {
            // 材料が揃っていない場合は判定不可
            return false;
        }

        boolean result = false;
        Cursor c;
        if (timestamp != null) {
            // mailboxとtimestampが一致するものを検索
            c = getDb(ctx).query(EmailNotificationHistoryOpenHelper.TABLE_NAME,
                    null, "mailbox = ? AND timestamp = ?",
                    new String[] { mailbox, String.valueOf(timestamp.getTime() / 1000) },
                    null, null, null, null);
        } else {
            // timestampがない場合、保存日時が閾値以内かどうかで判断する
            long cur = Calendar.getInstance().getTimeInMillis() / 1000;
            c = getDb(ctx).query(EmailNotificationHistoryOpenHelper.TABLE_NAME,
                    null, "mailbox = ? AND created_at > ? AND created_at <= ?",
                    new String[] { mailbox, String.valueOf(cur - DUPLICATE_CHECK_THRESHOLD_SEC), String.valueOf(cur) },
                    null, null, null, null);
        }
        if (c.moveToFirst()) {
            result = true;
        }
        c.close();
        return result;
    }

}
