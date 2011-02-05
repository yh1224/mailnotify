package net.assemble.android;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MailHistoryOpenHelper extends SQLiteOpenHelper {
    public static final String TABLE_LOG = "log";

    private static final String DB_NAME = "mylog.db";
    private static final int DB_VERSION = 4;
    private static final String DB_CREATE_SQL = "create table log ("
            + "_id integer primary key autoincrement, "
            + "created_at timestamp,"
            + "created_date text,"
            + "level int,"
            + "log_text text);";
    private static final String DB_DROP_SQL = "drop table log";

    public MailHistoryOpenHelper(Context context) {
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
