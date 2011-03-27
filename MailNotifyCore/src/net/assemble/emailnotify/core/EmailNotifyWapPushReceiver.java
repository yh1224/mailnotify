package net.assemble.emailnotify.core;

import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class EmailNotifyWapPushReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (EmailNotify.DEBUG) logIntent(intent);

        if (!EmailNotifyPreferences.getEnable(ctx)) {
            return;
        }

        EmailNotifyService.startService(ctx);
    }

    private static void logIntent(Intent intent) {
        Log.d(EmailNotify.TAG, "received intent: " + intent.getAction());

        Bundle extras = intent.getExtras();
        if (extras != null) {
            Set<String> keySet = extras.keySet();
            if (keySet != null) {
                Object[] keys = keySet.toArray();
                for (int i = 0; i < keys.length; i++) {
                    Object o = extras.get((String)keys[i]);
                    Log.d(EmailNotify.TAG, "  " + (String)keys[i] + " = (" + o.getClass().getName() + ") " + o.toString());
                }
            }
        }
    }

}
