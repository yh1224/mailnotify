package net.assemble.emailnotify;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import net.assemble.android.AboutActivity;
import net.assemble.android.AssetsReader;
import net.assemble.android.MyLogActivity;

public class EmailNotifyActivity extends Activity implements View.OnClickListener {
    private ToggleButton mEnableButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mEnableButton = (ToggleButton) findViewById(R.id.enable);
        mEnableButton.setOnClickListener(this);

        AssetsReader ar = new AssetsReader(this);
        try {
            String str = ar.getText("description.txt");
            TextView text = (TextView) findViewById(R.id.description);
            text.setText(Html.fromHtml(str, new Html.ImageGetter() {
                @Override
                public Drawable getDrawable(String source) {
                    Drawable d = getResources().getDrawable(R.drawable.icon);
                    d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                    return d;
                }
            }, null));
        } catch (IOException e) {}

        updateService();

        if (!EmailNotifyPreferences.isLicensed(this)) {
            checkLicense();
        }
    }

    /**
     * オプションメニューの生成
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
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
                //intent.putExtra("level", MyLog.LEVEL_VERBOSE);
                intent.putExtra("debug_menu", true);
            }
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
        if (EmailNotifyPreferences.getEnable(this)) {
            EmailObserverService.startService(this);
            mEnableButton.setChecked(true);
        } else {
            EmailObserverService.stopService(this);
            mEnableButton.setChecked(false);
        }
    }

    private void checkLicense() {
        // ライセンスチェック
        PackageManager pm = getPackageManager();
        String packageName = getPackageName() + ".license";
        Intent intent = new Intent();
        intent.setClassName(packageName, packageName + ".LicenseActivity");
        List<ResolveInfo> appList = pm.queryIntentActivities(intent, 0);
        if (appList.isEmpty()) {
            return;
        }
        startActivityForResult(intent, 2/*TODO*/);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2/*TODO */) {
            if (resultCode == RESULT_OK) {
                if (data.hasExtra("license_key")) {
                    EmailNotifyPreferences.setLicenseKey(this, data.getStringExtra("license_key"));
                    if (EmailNotifyPreferences.isLicensed(this)) {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                        alertDialogBuilder.setTitle(R.string.app_name);
                        alertDialogBuilder.setMessage(R.string.licensed_message);
                        alertDialogBuilder.setCancelable(true);
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                    }
                }
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


}
