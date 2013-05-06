package net.assemble.emailnotify;

import net.assemble.emailnotify.core.R;
import net.assemble.emailnotify.core.EmailNotify;
import android.view.View;
import android.widget.LinearLayout;

import com.google.ads.*;

/**
 * メイン画面 Activity
 */
public class EmailNotifyActivity extends
        net.assemble.emailnotify.core.EmailNotifyActivity {
    private static final String MY_AD_UNIT_ID = "a14d316803d0e29";

    @Override
    protected void onResume() {
        super.onResume();

        // AdMob
        if (EmailNotify.isFreeVersion(this)) {
            AdView adView = new AdView(this, AdSize.BANNER, MY_AD_UNIT_ID);
            LinearLayout adLayout = (LinearLayout) findViewById(R.id.ad);
            adLayout.addView(adView);
            adView.loadAd(new AdRequest());
            adView.setVisibility(View.VISIBLE);
        }
    }

}
