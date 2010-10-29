package net.assemble.emailnotify;

import android.app.LauncherActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class EmailNotifyPreferencesLaunchAppActivity extends LauncherActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.select_app);
    }

    @Override
    protected Intent getTargetIntent() {
        // TODO: できればメール受信アプリだけ取得したいが…
        //Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        return intent;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent app_intent = intentForPosition(position);
        Intent intent = new Intent();
        intent.putExtra("package_name", app_intent.getComponent().getPackageName());
        intent.putExtra("class_name", app_intent.getComponent().getClassName());
        setResult(RESULT_OK, intent);
        finish();
    }

}
