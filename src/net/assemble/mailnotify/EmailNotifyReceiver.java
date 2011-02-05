package net.assemble.mailnotify;

import net.assemble.android.MyLog;

import com.android.internal.telephony.WspTypeDecoder;

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
            // WAP PUSH
            if (intent.getAction().equals("android.provider.Telephony.WAP_PUSH_RECEIVED")) {
                byte[] data = intent.getByteArrayExtra("data");
                WapPdu pdu = new WapPdu(WspTypeDecoder.CONTENT_TYPE_B_PUSH_SL, data);
                if (!pdu.decode()) {
                    MyLog.w(ctx, EmailNotify.TAG, "Unexpected PDU: " + pdu.getHexString());
                    return;
                }
                MyLog.d(ctx, EmailNotify.TAG ,"Received PDU: " + pdu.getHexString());
                MyLog.i(ctx, EmailNotify.TAG ,"Received: " + pdu.getMailbox());
                if (pdu.getMailbox() != null && pdu.getMailbox().contains("docomo.ne.jp")) {
                    if (EmailNotifyPreferences.getServiceImode(ctx)) {
                        EmailNotificationManager.showNotification(ctx, EmailNotifyPreferences.SERVICE_IMODE, "docomo.ne.jp", null);
                    }
                } else {
                    if (EmailNotifyPreferences.getServiceOther(ctx)) {
                        EmailNotificationManager.showNotification(ctx, EmailNotifyPreferences.SERVICE_OTHER, pdu.getMailbox(), null);
                    }
                }
            }
            // Restart
            else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                Log.i(EmailNotify.TAG, "EmailNotify restarted. (at boot)");
                EmailNotifyService.startService(ctx);
            } else if (intent.getAction().equals("android.intent.action.PACKAGE_REPLACED"/*Intent.ACTION_PACKAGE_REPLACED*/)) {
                if (intent.getData() != null &&
                    intent.getData().equals(Uri.fromParts("package", ctx.getPackageName(), null))) {
                    // Restart service
                    Log.i(EmailNotify.TAG, "EmailNotify restarted. (package replaced)");
                    EmailNotifyService.startService(ctx);
                }
            } else if (intent.getAction().equals(Intent.ACTION_TIME_CHANGED)
                    || intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                EmailNotifyService.startService(ctx);
            }
            return;
        }

        EmailNotifyService.startService(ctx);
    }

}
