package net.assemble.android;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;

import net.assemble.emailnotify.R;

public class MyLogActivity extends ListActivity {
    private SimpleCursorAdapter mAdapter;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mylog);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateList();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    /**
     * ログ覧表示を更新
     */
    private void updateList() {
        Cursor c = MyLog.getDb(this).query(MyLogOpenHelper.TABLE_LOG,
                null, null, null, null, null, "created_at desc", null);
        if (c.moveToFirst()) {
            startManagingCursor(c);
            String[] from = new String[] { "created_date", "log_text"};
            int[] to = new int[] { R.id.created_date, R.id.log_text };
            mAdapter = new SimpleCursorAdapter(this, R.layout.mylog_entry, c, from, to);
            setListAdapter(mAdapter);
        } else {
            setListAdapter(null);
        }
    }

}
