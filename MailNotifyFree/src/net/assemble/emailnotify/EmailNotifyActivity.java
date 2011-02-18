package net.assemble.emailnotify;

import net.assemble.emailnotify.core.R;
import net.assemble.emailnotify.core.EmailNotify;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;

import com.admob.android.ads.AdView;

public class EmailNotifyActivity extends
        net.assemble.emailnotify.core.EmailNotifyActivity {

    @Override
    protected void onResume() {
        super.onResume();

        // AdMob
        if (EmailNotify.isFreeVersion(this)) {
            AdView adView = (AdView) findViewById(R.id.ad);
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                //AdManager.setTestDevices( new String[] { "388141A967D061433E2A0DD6CD552C68" });
                adView.requestFreshAd();
                adView.setVisibility(View.VISIBLE);
            } else {
                // ネットワーク未接続時はエラーになるので表示しない
                adView.setVisibility(View.INVISIBLE);
            }
        }
    }

}
