package net.assemble.emailnotify.core;

import java.util.Calendar;

import net.orleaf.android.MyLog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

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
                EmailNotifyService.startService(ctx);
            } else if (intent.getAction().equals("android.intent.action.PACKAGE_REPLACED"/*Intent.ACTION_PACKAGE_REPLACED*/)) {
                if (intent.getData() != null &&
                    intent.getData().equals(Uri.fromParts("package", ctx.getPackageName(), null))) {
                    // Restart service
                    Log.i(EmailNotify.TAG, "EmailNotify restarted. (package replaced)");
                    MyLog.clearAll(ctx);
                    MyLog.i(ctx, EmailNotify.TAG, "Package replaced to " + EmailNotify.getAppVersion(ctx));
                    EmailNotifyService.startService(ctx);
                    EmailNotifyPreferences.resetSendLog(ctx);
                }
            } else if (intent.getAction().equals(Intent.ACTION_TIME_CHANGED)
                    || intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                EmailNotifyService.startService(ctx);
            } else if (intent.getAction().equals(EmailNotify.ACTION_LOG_SENT)) {
                MyLog.i(ctx, EmailNotify.TAG, "Log sent.");
                EmailNotifyPreferences.setLogSent(ctx, Calendar.getInstance().getTimeInMillis());
            }
            return;
        }

        EmailNotifyService.startService(ctx);
    }

}
