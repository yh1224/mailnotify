package net.assemble.mailnotify;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ToggleButton;

import net.assemble.android.AboutActivity;
import net.assemble.android.MyLog;
import net.assemble.android.MyLogActivity;

public class EmailNotifyActivity extends Activity implements View.OnClickListener {
    private ToggleButton mEnableButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mEnableButton = (ToggleButton) findViewById(R.id.enable);
        mEnableButton.setOnClickListener(this);

        // ライセンスフラグ設定
        //  有料版を使ったことがある場合は購入メニューを表示させない
        if (!EmailNotify.FREE_VERSION) {
            EmailNotifyPreferences.setLicense(this, true);
        }

        // 有効期限チェック
        if (!EmailNotify.checkExpiration(this)) {
            EmailNotifyPreferences.setEnable(this, false);
        }

        updateService();
    }

    /**
     * オプションメニューの生成
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        // 購入メニュー (FREE版のみ)
        if (EmailNotify.FREE_VERSION) {
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
        switch (itemId) {
        case R.id.menu_preferences:
            intent = new Intent().setClass(this, EmailNotifyPreferencesActivity.class);
            startActivity(intent);
            break;
        case R.id.menu_log:
            intent = new Intent().setClass(this, MyLogActivity.class);
            if (EmailNotify.DEBUG) {
                intent.putExtra("level", MyLog.LEVEL_VERBOSE);
            }
            intent.putExtra("debug_menu", true);
            startActivity(intent);
            break;
        case R.id.menu_help:
            intent = new Intent().setClass(this, EmailNotifyHelpActivity.class);
            startActivity(intent);
            break;
        case R.id.menu_about:
            intent = new Intent().setClass(this, AboutActivity.class);
            startActivity(intent);
            break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        EmailNotifyPreferences.setEnable(this, mEnableButton.isChecked());
        updateService();
    };

    private void updateService() {
        ImageView image = (ImageView) findViewById(R.id.main_image);
        if (EmailNotifyPreferences.getEnable(this)) {
            EmailNotifyService.startService(this);
            mEnableButton.setChecked(true);
            image.setImageResource(R.drawable.main);
        } else {
            EmailNotifyService.stopService(this);
            mEnableButton.setChecked(false);
            image.setImageResource(R.drawable.disable);
        }
    }

}
