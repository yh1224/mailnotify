package net.assemble.emailnotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class EmailNotifyReceiver extends BroadcastReceiver {
    static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

    public void onReceiveIntent(Context context, Intent intent) {
        Log.d(this.getClass().getName(), "received intent: "
                + intent.getAction());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(this.getClass().getName(), "received intent: "
                + intent.getAction());

        if (intent.getAction() != null) {
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                Log.i(this.getClass().getName(), "EmailNotify restarted.");
                new EmailObserver(context);
            }
        }
    }
}
