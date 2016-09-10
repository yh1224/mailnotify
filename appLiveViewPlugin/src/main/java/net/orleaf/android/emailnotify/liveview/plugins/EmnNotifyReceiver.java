package net.orleaf.android.emailnotify.liveview.plugins;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class EmnNotifyReceiver extends BroadcastReceiver {
    private static final String TAG = "EmailNotify";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (Emn.DEBUG) Log.d(TAG, "received intent: " + intent.getAction());

        if (intent.getAction() != null) {
            if (intent.getAction().equals(Emn.ACTION_MAIL_PUSH_RECEIVED)) {
                 Intent launchIntent = new Intent(ctx, EmnNotifyService.class);
                 launchIntent.putExtras(intent.getExtras());
//                 launchIntent.putExtra("service", intent.getStringExtra("service"));
//                 launchIntent.putExtra("mailbox", intent.getStringExtra("mailbox"));
//                 launchIntent.putExtra("received", (Date)intent.getSerializableExtra("received"));
//                 launchIntent.putExtra("count", intent.getIntExtra("count", 0));
                 ctx.startService(launchIntent);
            }
        }
        return;
    }

}
