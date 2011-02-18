package net.assemble.emailnotify.core;

import java.util.Calendar;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

public class EmailNotificationHistoryDao {
    /**
     * ログ保持数
     */
    private static final long LOG_ROTATE_LIMIT = 100;

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
        Cursor c = getDb(ctx).query(EmailNotificationHistoryOpenHelper.TABLE_NAME,
                null, null, null,
                null, null, "created_at desc, _id desc", null);
        return c;
    }

    /**
     * 未消去のメール受信履歴を取得
     *
     * @param ctx Context
     * @return Cursor
     */
    public static Cursor getActiveHistories(Context ctx) {
        Cursor c = getDb(ctx).query(EmailNotificationHistoryOpenHelper.TABLE_NAME,
                null, "cleared_at IS NULL", null, null, null, null);
        return c;
    }

    /**
     * メール受信履歴を1件取得
     *
     * @param ctx Context
     * @param id ID
     * @return Cursor
     */
    public static Cursor getHistory(Context ctx, long id) {
        Cursor c = getDb(ctx).query(EmailNotificationHistoryOpenHelper.TABLE_NAME,
                null, BaseColumns._ID + " = " + id, null,
                null, null, null, null);
        return c;
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
     * @param mailbox メールボックス名
     * @param timestamp タイムスタンプ
     * @param wap_data WAP data
     * @return ID
     */
    public static long add(Context ctx, Date logdate, String contentType, String mailbox, Date timestamp, String wap_data) {
        ContentValues values = new ContentValues();
        values.put("created_at", Calendar.getInstance().getTimeInMillis() / 1000);
        values.put("content_type", contentType);
        if (logdate != null) {
            values.put("logged_at", logdate.getTime() / 1000);
        }
        if (mailbox != null) {
            values.put("mailbox", mailbox);
        }
        if (timestamp != null) {
            values.put("timestamp", timestamp.getTime() / 1000);
        }
        values.put("wap_data", wap_data);
        long id = getDb(ctx).insert(EmailNotificationHistoryOpenHelper.TABLE_NAME, null, values);
        rotate(ctx, id);
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
        return;
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
        return;
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
        return;
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
        if (mailbox == null || timestamp == null) {
            // 材料が揃っていない場合は判定不可
            return false;
        }

        boolean result = false;
        Cursor c = getDb(ctx).query(EmailNotificationHistoryOpenHelper.TABLE_NAME,
                null, "mailbox = ? AND timestamp = ?",
                new String[] { mailbox, String.valueOf(timestamp.getTime() / 1000) },
                null, null, null, null);
        if (c.moveToFirst()) {
            result = true;
        }
        c.close();
        return result;
    }

}
