package net.assemble.emailnotify;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ToggleButton;

import net.assemble.emailnotify.R;

public class EmailNotifyActivity extends Activity implements View.OnClickListener {
    private ToggleButton mEnableButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mEnableButton = (ToggleButton) findViewById(R.id.enable);
        mEnableButton.setOnClickListener(this);

        updateService();
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
        case R.id.menu_about:
            intent = new Intent().setClass(this, EmailNotifyAboutActivity.class);
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
    
}
