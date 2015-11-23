package net.assemble.emailnotify.core;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import net.assemble.emailnotify.core.preferences.EmailNotifyPreferences;
import net.orleaf.android.MyLog;

public class EmailNotifyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "received intent: " + intent.getAction());

        if (!EmailNotifyPreferences.getEnable(ctx)) {
            return;
        }

        // 有効期限チェック
        if (!EmailNotify.checkExpiration(ctx)) {
            EmailNotifyPreferences.setEnable(ctx, false);
            return;
        }

        if (intent.getAction() != null) {
            // Restart
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                Log.i(EmailNotify.TAG, "EmailNotify restarted. (at boot)");
                EmailNotifyObserveService.startService(ctx);
            } else if (intent.getAction().equals("android.intent.action.PACKAGE_REPLACED"/*Intent.ACTION_PACKAGE_REPLACED*/)) {
                if (intent.getData() != null &&
                    intent.getData().equals(Uri.fromParts("package", ctx.getPackageName(), null))) {
                    // Restart service
                    Log.i(EmailNotify.TAG, "EmailNotify restarted. (package replaced)");
                    MyLog.clearAll(ctx);
                    MyLog.i(ctx, EmailNotify.TAG, "Package replaced to " + EmailNotify.getAppVersion(ctx));
                    EmailNotifyObserveService.startService(ctx);
                    EmailNotifyPreferences.resetSendLog(ctx);
                }
            } else if (intent.getAction().equals(Intent.ACTION_TIME_CHANGED)
                    || intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                EmailNotifyObserveService.startService(ctx);
            } else if (intent.getAction().equals(EmailNotify.ACTION_LOG_SENT)) {
                String result = intent.getStringExtra("result");
                if (result == null) {
                    MyLog.d(ctx, EmailNotify.TAG, "Sent report successfully.");
                    EmailNotifyPreferences.setLogSent(ctx, Calendar.getInstance().getTimeInMillis());
                } else {
                    MyLog.d(ctx, EmailNotify.TAG, "Failed to send report: " + result);
                }
            }
            return;
        }

        EmailNotifyObserveService.startService(ctx);
    }

}
