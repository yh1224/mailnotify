package net.assemble.emailnotify.core;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class EmailNotifyRestoreActivity extends Activity {
    public static final String ACTION_RESTORE_NETWORK = "net.assemble.emailnotify.action.RESTORE_NETWORK";
    public static final String ACTION_KEEP_NETWORK = "net.assemble.emailnotify.action.KEEP_NETWORK";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVisible(false);

        Intent intent = getIntent();
        if (intent != null && intent.getAction().equals(ACTION_RESTORE_NETWORK)) {
            // ネットワーク復元
            new MobileNetworkManager(this).restoreNetwork();
            Toast.makeText(this, R.string.restored_network, Toast.LENGTH_LONG).show();
        }

        // ネットワーク復元情報を消去
        EmailNotifyPreferences.unsetNetworkInfo(this);

        finish();
    }

}
