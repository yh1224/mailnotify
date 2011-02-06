package net.assemble.mailnotify;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class EmailNotificationHistoryOpenHelper extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "email_history";

    private static final String DB_NAME = "emailnotify.db";
    private static final int DB_VERSION = 8;
    private static final String DB_CREATE_SQL = "create table " + TABLE_NAME + " ("
            + "_id integer primary key autoincrement, "
            + "created_at timestamp,"   // レコード追加日時
            + "logged_at timestamp,"    // ログ日時
            + "notified_at timestamp,"  // 通知日時 (null:未通知)
            + "cleared_at timestamp,"   // 消去日時 (null:未消去)
            + "content_type text,"      // Content-Type
            + "mailbox text,"           // mailbox属性
            + "timestamp timestamp,"    // timestamp属性
            + "wap_data text);";
    private static final String DB_DROP_SQL = "drop table " + TABLE_NAME;

    public EmailNotificationHistoryOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DB_CREATE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // とりあえず作り直し
        db.execSQL(DB_DROP_SQL);
        onCreate(db);
    }
}
