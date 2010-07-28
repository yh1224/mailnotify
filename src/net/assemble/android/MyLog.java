package net.assemble.android;

import java.util.Calendar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class MyLog {
    public static final int LEVEL_ERROR = 1;
    public static final int LEVEL_WARN = 2;
    public static final int LEVEL_INFO = 3;
    public static final int LEVEL_DEBUG = 4;
    public static final int LEVEL_VERBOSE = 5;

    /**
     * ログ保持期間
     */
    private static final long LOG_ROTATE_LIMIT_SEC = 8 * 60 * 60; // 8 hours

    private static SQLiteDatabase mDb;

    /**
     * データベースを取得
     */
    public static SQLiteDatabase getDb(Context ctx) {
        if (mDb == null) {
            MyLogOpenHelper h = new MyLogOpenHelper(ctx);
            mDb = h.getWritableDatabase();
        }
        return mDb;
    }

    /**
     * 古いログを削除する
     */
    private static void rotate(Context ctx) {
        Calendar cal = Calendar.getInstance();
        long t = cal.getTimeInMillis() / 1000 - LOG_ROTATE_LIMIT_SEC;
        getDb(ctx).delete(MyLogOpenHelper.TABLE_LOG, "created_at < " + t, null);
    }

    /**
     * ログ採取
     *
     * @param text ログ文字列
     */
    private static void add(Context ctx, int level, String text) {
        ContentValues values = new ContentValues();
        Calendar cal = Calendar.getInstance();
        values.put("created_at", cal.getTimeInMillis() / 1000);
        values.put("created_date", cal.getTime().toLocaleString());
        values.put("level", level);
        values.put("log_text", text);
        getDb(ctx).insert(MyLogOpenHelper.TABLE_LOG, null, values);
        rotate(ctx);
    }

    public static void e(Context ctx, String text) {
        Log.e(ctx.getClass().getName(), text);
        add(ctx, LEVEL_ERROR, text);
    }
    public static void w(Context ctx, String text) {
        Log.w(ctx.getClass().getName(), text);
        add(ctx, LEVEL_WARN, text);
    }
    public static void i(Context ctx, String text) {
        Log.i(ctx.getClass().getName(), text);
        add(ctx, LEVEL_INFO, text);
    }
    public static void d(Context ctx, String text) {
        Log.d(ctx.getClass().getName(), text);
        add(ctx, LEVEL_DEBUG, text);
    }
    public static void v(Context ctx, String text) {
        Log.v(ctx.getClass().getName(), text);
        add(ctx, LEVEL_VERBOSE, text);
    }

    /**
     * ログを取得
     *
     * @return カーソル
     */
    public static Cursor getLogCursor(Context ctx) { 
        Cursor c = getDb(ctx).query(MyLogOpenHelper.TABLE_LOG,
                null, null, null, null, null, "created_at", null);
        return c;
    }

}
