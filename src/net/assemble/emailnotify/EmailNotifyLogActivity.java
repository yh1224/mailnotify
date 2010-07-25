package net.assemble.emailnotify;

import android.app.ListActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;

public class EmailNotifyLogActivity extends ListActivity {
    private SQLiteDatabase mDb;
    private SimpleCursorAdapter mAdapter;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_list);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EmailNotifyLogOpenHelper h = new EmailNotifyLogOpenHelper(this);
        mDb = h.getReadableDatabase();
        updateList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDb.close();
    }

    /**
     * ログ覧表示を更新
     */
    private void updateList() {
        Cursor c = mDb.query(EmailNotifyLogOpenHelper.TABLE_LOG,
                null, null, null, null, null, "created_at", null);
        if (c.moveToFirst()) {
            startManagingCursor(c);
            String[] from = new String[] { "created_date", "received_date", "wap_pdu"};
            int[] to = new int[] { R.id.created_date, R.id.received_date, R.id.wap_pdu };
            mAdapter = new SimpleCursorAdapter(this, R.layout.log_list_entry, c, from, to);
            setListAdapter(mAdapter);
        } else {
            setListAdapter(null);
        }
    }

}
