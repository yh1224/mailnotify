package net.assemble.mailnotify;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * メール着信通知からのインテント受信
 */
public class EmailNotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "EmailNotify";

    public static final String ACTION_NOTIFY_LAUNCH = "net.assemble.mailnotify.action.NOTIFY_CLICK";
    public static final String ACTION_NOTIFY_CANCEL = "net.assemble.mailnotify.action.NOTIFY_CANCEL";
    public static final String ACTION_STOP = "net.assemble.mailnotify.action.NOTIFY_STOP";
    public static final String ACTION_RENOTIFY = "net.assemble.mailnotify.action.RENOTIFY";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (EmailNotify.DEBUG) Log.d(TAG, "received intent: " + intent.getAction());

        String action = intent.getAction();
        if (action != null) {
            String service = intent.getStringExtra("service");
            String mailbox = intent.getStringExtra("mailbox");
            if (action.startsWith(ACTION_NOTIFY_LAUNCH)) {
                // 通知停止
                EmailNotificationManager.clearNotification(mailbox);

                if (EmailNotifyPreferences.getNotifyDisableWifi(ctx, service)) {
                    // Wi-Fi無効化
                    WifiManager wifi = (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
                    wifi.setWifiEnabled(false);
                }

                // アプリ起動
                Intent launchIntent = new Intent();
                ComponentName component = EmailNotifyPreferences.getNotifyLaunchAppComponent(ctx, service);
                if (component != null) {
                    launchIntent.setComponent(component);
                } else {
                    launchIntent.setClass(ctx, EmailNotifyActivity.class);
                }
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(launchIntent);
            } else if (action.startsWith(ACTION_NOTIFY_CANCEL)) {
                // 通知停止
                EmailNotificationManager.clearNotification(mailbox);
            } else if (action.startsWith(ACTION_STOP)) {
                // 通知停止
                EmailNotificationManager.stopNotification(mailbox);
            } else if (action.startsWith(ACTION_RENOTIFY)) {
                // 再通知
                EmailNotificationManager.renotifyNotification(mailbox);
            }
        }
    }

}
