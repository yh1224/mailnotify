package net.assemble.emailnotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class EmailNotifyReceiver extends BroadcastReceiver {
    private static final String TAG = "EmailNotify";
    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "received intent: " + intent.getAction());

        if (intent.getAction() != null) {
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                Log.i(TAG, "EmailNotify restarted.");
                EmailObserverService.startService(context);
            }
        }
    }
}
