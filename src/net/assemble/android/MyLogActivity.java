package net.assemble.android;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import net.assemble.mailnotify.R;

public class MyLogActivity extends ListActivity {
    private static final int LEVEL_DEFAULT = MyLog.LEVEL_INFO;

    private MyAdapter mAdapter;
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
                null, null, "created_at desc, _id desc", null);
        if (c.moveToFirst()) {
            startManagingCursor(c);
            mAdapter = new MyAdapter(this, c);
            setListAdapter(mAdapter);
        } else {
            setListAdapter(null);
        }
    }

    /**
     * DB→リスト表示
     */
    private class MyAdapter extends ResourceCursorAdapter {

        public MyAdapter(Context context, Cursor cur) {
            super(context, R.layout.mylog, cur);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater li = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            return li.inflate(R.layout.mylog_entry, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cur) {
            TextView createdDate = (TextView)view.findViewById(R.id.created_date);
            TextView logText = (TextView)view.findViewById(R.id.log_text);

            createdDate.setText(cur.getString(cur.getColumnIndex("created_date")));
            logText.setText(cur.getString(cur.getColumnIndex("log_text")));
            int level = cur.getInt(cur.getColumnIndex("level"));
            if (level == MyLog.LEVEL_ERROR) {
                logText.setTextColor(Color.rgb(255, 128, 128));
            } else if (level == MyLog.LEVEL_WARN) {
                logText.setTextColor(Color.rgb(255, 224, 128));
            } else if (level == MyLog.LEVEL_INFO) {
                logText.setTextColor(Color.rgb(192, 192, 255));
            } else {
                logText.setTextColor(getResources().getColor(android.R.color.primary_text_dark));
            }
        }
    }

}
