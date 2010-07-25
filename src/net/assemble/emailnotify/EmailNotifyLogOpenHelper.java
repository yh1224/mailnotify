package net.assemble.emailnotify;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class EmailNotifyLogOpenHelper extends SQLiteOpenHelper {
    public static final String TABLE_LOG = "emailnotify_log";

    private static final String DB_NAME = "emailnotify.db";
    private static final int DB_VERSION = 1;
    private static final String DB_CREATE_SQL = "create table log ("
            + "_id integer primary key autoincrement, "
            + "received_at timestamp,"
            + "wap_pdu blob);";
    private static final String DB_DROP_SQL = "drop table log";

    public EmailNotifyLogOpenHelper(Context context) {
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
