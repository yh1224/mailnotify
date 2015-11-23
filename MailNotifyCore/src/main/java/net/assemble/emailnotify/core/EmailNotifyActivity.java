package net.assemble.emailnotify.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ToggleButton;

import net.assemble.emailnotify.core.debug.EmailNotifyDebugActivity;
import net.assemble.emailnotify.core.notification.EmailNotificationHistoryActivity;
import net.assemble.emailnotify.core.preferences.EmailNotifyPreferences;
import net.assemble.emailnotify.core.preferences.EmailNotifyPreferencesActivity;
import net.orleaf.android.AboutActivity;
import net.orleaf.android.MyLog;
import net.orleaf.android.MyLogActivity;

/**
 * メイン画面 Activity
 */
public class EmailNotifyActivity extends Activity implements View.OnClickListener {
    private ToggleButton mEnableButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (EmailNotify.isFreeVersion(this)) {
            setContentView(R.layout.main_free);
        } else {
            setContentView(R.layout.main);
        }

        mEnableButton = (ToggleButton) findViewById(R.id.enable);
        mEnableButton.setOnClickListener(this);

        // ライセンスフラグ設定
        //  有料版を使ったことがある場合は購入メニューを表示させない
        if (!EmailNotify.isFreeVersion(this)) {
            EmailNotifyPreferences.setLicense(this, true);
        }

        // 有効期限チェック
        if (!EmailNotify.checkExpiration(this)) {
            EmailNotifyPreferences.setEnable(this, false);
        }

        updateService();

        if (EmailNotify.isFreeVersion(this)) {
            confirmReport();
        }
    }

    /**
     * レポート送信可否確認
     */
    private void confirmReport() {
        if (!EmailNotifyPreferences.hasSendLog(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.warning);
            builder.setMessage(getResources().getString(R.string.pref_debug_log_send_warning));
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EmailNotifyPreferences.setSendLog(EmailNotifyActivity.this, true);
                }
            });
            builder.setNegativeButton(R.string.disallow, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    EmailNotifyPreferences.setSendLog(EmailNotifyActivity.this, false);
                }
            });
            builder.setCancelable(true);
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    /**
     * オプションメニューの生成
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        // デバッグ用メニュー追加
        if (EmailNotify.DEBUG) {
            MenuItem menuReport = menu.add("Log");
            menuReport.setIcon(android.R.drawable.ic_menu_view);
            menuReport.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Intent intent = new Intent().setClass(EmailNotifyActivity.this, MyLogActivity.class);
                    intent.putExtra(MyLogActivity.EXTRA_REPORTER_ID,
                            EmailNotifyPreferences.getPreferenceId(EmailNotifyActivity.this));
                    if (EmailNotify.DEBUG) {
                        intent.putExtra(MyLogActivity.EXTRA_LEVEL, MyLog.LEVEL_VERBOSE);
                        intent.putExtra(MyLogActivity.EXTRA_DEBUG_MENU, true);
                    }
                    startActivity(intent);
                    return true;
                }
            });

            MenuItem menuDebug = menu.add("Debug");
            menuDebug.setIcon(android.R.drawable.ic_menu_manage);
            menuDebug.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Intent intent = new Intent().setClass(EmailNotifyActivity.this,
                            EmailNotifyDebugActivity.class);
                    startActivity(intent);
                    return true;
                }
            });
        }

        // 購入メニュー (FREE/TRIAL版)
        if (EmailNotify.isFreeVersion(this) || EmailNotify.TRIAL_EXPIRES != null) {
            if (!EmailNotifyPreferences.getLicense(this)) {
                MenuItem menuBuy = menu.add(R.string.buy);
                menuBuy.setIcon(android.R.drawable.ic_menu_more);
                menuBuy.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(EmailNotify.MARKET_URL));
                        startActivity(intent);
                        return true;
                    }
                });
            }
        }

        return true;
    }

    /**
     * オプションメニューの選択
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        Intent intent;
        if (itemId == R.id.menu_preferences) {
            intent = new Intent().setClass(this, EmailNotifyPreferencesActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.menu_history) {
            intent = new Intent().setClass(EmailNotifyActivity.this,
                    EmailNotificationHistoryActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.menu_about) {
            intent = new Intent().setClass(this, AboutActivity.class);
            intent.putExtra("body_asset", "about.txt");
            startActivity(intent);
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        EmailNotifyPreferences.setEnable(this, mEnableButton.isChecked());
        updateService();
    }

    private void updateService() {
        ImageView image = (ImageView) findViewById(R.id.main_image);
        if (EmailNotifyPreferences.getEnable(this)) {
            EmailNotifyObserveService.startService(this);
            mEnableButton.setChecked(true);
            image.setImageResource(R.drawable.main);
        } else {
            EmailNotifyObserveService.stopService(this);
            mEnableButton.setChecked(false);
            image.setImageResource(R.drawable.disable);
        }
    }

}
