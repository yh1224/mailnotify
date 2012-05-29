package net.assemble.emailnotify.core;

import java.util.Set;

import net.orleaf.android.HexUtils;
import net.orleaf.android.MyLog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class EmailNotifyWapPushReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {

        if (!EmailNotifyPreferences.getEnable(ctx)) {
            return;
        }

        logIntent(ctx, intent);

        String contentType = intent.getType();
        byte[] header = intent.getByteArrayExtra("header");
        byte[] data = intent.getByteArrayExtra("data");
        if (contentType != null && data != null) {
            WapPdu pdu;
            if (header != null) {
                pdu = new WapPdu(header, data);
            } else {
                int wapAppId = intent.getIntExtra("wapAppID", 0);
                pdu = new WapPdu(contentType, wapAppId, data);
            }
            if (pdu.decode()) {
                MyLog.d(ctx, EmailNotify.TAG, "Received PDU: " + HexUtils.bytes2hex(data));
                MyLog.d(ctx, EmailNotify.TAG, "  contentType=" + pdu.getContentType() + ", wapAppID=" + pdu.getApplicationId());
                if (pdu.getTimestampDate() != null) {
                    MyLog.i(ctx, EmailNotify.TAG, "Received: " + pdu.getMailbox() + " (" + pdu.getTimestampDate().toLocaleString() + ")");
                } else {
                    MyLog.i(ctx, EmailNotify.TAG, "Received: " + pdu.getMailbox());
                }
                // 通知
                EmailNotificationService.startService(ctx, null, pdu);
            }
        }

        EmailNotifyService.startService(ctx);
    }

    private static void logIntent(Context ctx, Intent intent) {
        MyLog.d(ctx, EmailNotify.TAG, "received intent: " + intent.getAction());

        Bundle extras = intent.getExtras();
        if (extras != null) {
            Set<String> keySet = extras.keySet();
            if (keySet != null) {
                Object[] keys = keySet.toArray();
                for (int i = 0; i < keys.length; i++) {
                    Object o = extras.get((String)keys[i]);
                    String val;
                    if (o instanceof byte[]) {
                        val = HexUtils.bytes2hex((byte[])o);
                    } else {
                        val = o.toString();
                    }
                    MyLog.d(ctx, EmailNotify.TAG, " " + (String)keys[i] + " = (" + o.getClass().getName() + ") " + val);
                }
            }
        }
    }

}
