package net.assemble.emailnotify;

import net.assemble.android.MyLog;

import com.android.internal.telephony.WspTypeDecoder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class EmailNotifyReceiver extends BroadcastReceiver {
    private static final String TAG = "EmailNotify";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (EmailNotify.DEBUG) Log.d(TAG, "received intent: " + intent.getAction());

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
                    MyLog.w(ctx, TAG, "Unexpected PDU: " + pdu.getHexString());
                    return;
                }
                MyLog.d(ctx, TAG ,"Received PDU: " + pdu.getHexString());
                MyLog.i(ctx, TAG ,"Received: " + pdu.getMailbox());
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
                Log.i(TAG, "EmailNotify restarted.");
                EmailNotifyService.startService(ctx);
            } else if (intent.getAction().equals(Intent.ACTION_TIME_CHANGED)
                    || intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                EmailNotifyService.startService(ctx);
            }
            return;
        }

        EmailNotifyService.startService(ctx);
    }

}
