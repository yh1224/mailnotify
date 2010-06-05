package net.assemble.emailnotify;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import net.assemble.emailnotify.R;

public class EmailNotifyActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Button btn_ok = (Button) findViewById(R.id.btn_ok);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        new EmailObserver(this);
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
        case R.id.menu_report:
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:yh1224@gmail.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "About EmailNotify");
            startActivity(intent);
            break;
        case R.id.menu_about:
            intent = new Intent().setClass(this, EmailNotifyAboutActivity.class);
            startActivity(intent);
            break;
        }
        return true;
    };

}
