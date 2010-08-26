package net.assemble.android;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SimpleCursorAdapter;

import net.assemble.mailnotify.R;

public class MyLogActivity extends ListActivity {
    private static final int LEVEL_DEFAULT = MyLog.LEVEL_INFO;

    private SimpleCursorAdapter mAdapter;
    int mLevel;
    boolean mDebugMenu;
    MenuItem mMenuReport;
    MenuItem mMenuClear;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mylog);
        
        Intent intent = getIntent();
        mLevel = intent.getIntExtra("level", LEVEL_DEFAULT);
        mDebugMenu = intent.getBooleanExtra("debug_menu", false);
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
     * オプションメニューの生成
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mDebugMenu) {
            mMenuClear = menu.add("Clear");
            mMenuClear.setIcon(android.R.drawable.ic_menu_delete);
            mMenuReport = menu.add("Report");
            mMenuReport.setIcon(android.R.drawable.ic_menu_send);
        }
        return true;
    }

    /**
     * オプションメニューの選択
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item == mMenuClear) {
            MyLog.clearAll(this);
            updateList();
        } else if (item == mMenuReport) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "mailto:" + getResources().getString(R.string.feedback_to)));
            intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name));
            intent.putExtra(Intent.EXTRA_TEXT, MyLog.getLogText(this, MyLog.LEVEL_VERBOSE));
            startActivity(intent);
        }
        return true;
    }

    /**
     * ログ一覧表示を更新
     */
    private void updateList() {
        Cursor c = MyLog.getDb(this).query(MyLogOpenHelper.TABLE_LOG,
                null, "level <= " + mLevel, null,
                null, null, "created_at desc", null);
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
