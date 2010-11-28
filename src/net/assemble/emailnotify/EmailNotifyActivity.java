package net.assemble.emailnotify;

import com.google.ads.AdSenseSpec;
import com.google.ads.AdSenseSpec.AdType;
import com.google.ads.AdSenseSpec.ExpandDirection;
import com.google.ads.GoogleAdView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

    private static final String CLIENT_ID = "ca-mb-app-pub-6511207727081425";
    private static final String COMPANY_NAME = "Orange leaf";
    private static final String KEYWORDS = "email|docomo|mopera|spmode";
    private static final String CHANNEL_ID = "0410742134";

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

    @Override
    protected void onResume() {
        super.onResume();

        // Google AdSense
        if (EmailNotify.FREE_VERSION) {
            GoogleAdView adView = (GoogleAdView) findViewById(R.id.adview);
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                AdSenseSpec adSenseSpec =
                    new AdSenseSpec(CLIENT_ID)
                    .setCompanyName(COMPANY_NAME)
                    .setAppName(getResources().getString(R.string.app_name))
                    .setKeywords(KEYWORDS)
                    .setChannel(CHANNEL_ID)
                    .setAdType(AdType.IMAGE)
                    .setExpandDirection(ExpandDirection.BOTTOM)
                    .setAdTestEnabled(EmailNotify.DEBUG);       // Keep true while testing.
                adView.showAds(adSenseSpec);
                adView.setVisibility(View.VISIBLE);
            } else {
                // ネットワーク未接続時はエラーになるので表示しない
                adView.setVisibility(View.INVISIBLE);
            }
        }
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
