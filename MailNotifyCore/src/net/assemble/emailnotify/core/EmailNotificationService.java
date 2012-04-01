package net.assemble.emailnotify.core;

import net.orleaf.android.HexUtils;
import net.orleaf.android.MyLog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * メール通知サービス
 */
public class EmailNotificationService extends Service {

    private static ComponentName mService;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        if (intent == null) {
            return;
        }

        WapPdu pdu = null;
        String contentType = intent.getStringExtra("contentType");
        int wapAppId = intent.getIntExtra("wapAppId", 0);
        byte[] data = intent.getByteArrayExtra("data");
        if (contentType == null) {
            // Content-Typeなしの場合はヘッダ込み
            pdu = new WapPdu(data);
        } else {
            // Content-Typeありの場合はボディのみ
            pdu = new WapPdu(contentType, wapAppId, data);
        }
        if (pdu != null) {
            if (pdu.decode()) {
                MyLog.d(this, EmailNotify.TAG, "Received WAP data: " + HexUtils.bytes2hex(data) + " (wapAppId=" + wapAppId + ")");
                if (pdu.getTimestampDate() != null) {
                    MyLog.i(this, EmailNotify.TAG, "Received: " + pdu.getMailbox() + " (" + pdu.getTimestampDate().toLocaleString() + ")");
                } else {
                    MyLog.i(this, EmailNotify.TAG, "Received: " + pdu.getMailbox());
                }

                // 記録
                long historyId = EmailNotificationHistoryDao.add(this, null,
                        pdu.getContentType(), pdu.getApplicationId(),
                        pdu.getMailbox(), pdu.getTimestampDate(),
                        pdu.getServiceName(), pdu.getHexString());
                if (historyId < 0) {
                    MyLog.w(this, EmailNotify.TAG, "Duplicated: "
                            + "mailbox=" + pdu.getMailbox() + ", timestamp=" + pdu.getTimestampString());
                    return;
                }

                EmailNotifyService.doNotify(this, contentType, pdu.getMailbox(), pdu.getTimestampDate(),
                        pdu.getServiceName(), historyId, false);
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }


    /**
     * サービス開始
     */
    private static boolean startService(Context ctx, Intent intent) {
        boolean result;
        mService = ctx.startService(intent);
        if (mService == null) {
            Log.e(EmailNotify.TAG, "EmailNotificationService start failed!");
            result = false;
        } else {
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "EmailNotificationService started: " + mService);
            result = true;
        }
        return result;
    }

    /**
     * メール通知
     */
    public static boolean notify(Context ctx, byte[] wapPdu) {
        Intent intent = new Intent(ctx, EmailNotificationService.class);
        intent.putExtra("data", wapPdu);
        return startService(ctx, intent);
    }

    /**
     * メール通知
     */
    public static boolean notify(Context ctx, String contentType, int wapAppId, byte[] wapBody) {
        Intent intent = new Intent(ctx, EmailNotificationService.class);
        intent.putExtra("contentType", contentType);
        intent.putExtra("wapAppId", wapAppId);
        intent.putExtra("data", wapBody);
        return startService(ctx, intent);
    }

}
