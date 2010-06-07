package net.assemble.emailnotify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/**
 * メール監視サービス
 */
public class EmailObserverService extends Service {
    private static ComponentName mService;
    private EmObserver mObserver;
    private ContentResolver mContentResolver;
    private Calendar mLastCheck;
    private Calendar mLastNotify;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        mObserver = new EmObserver(new Handler());
        mContentResolver = getContentResolver();
        mContentResolver.registerContentObserver(Uri
                .parse("content://com.android.email/update"), true, mObserver);
        mLastCheck = Calendar.getInstance();
    }

    public void onDestroy() {
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private boolean checkLog() {
        boolean result = false;
        //Log.d(getClass().getName(), "Checking log...");
        try {
            ArrayList<String> commandLine = new ArrayList<String>();
            commandLine.add("logcat");
            commandLine.add("-d");
            commandLine.add("-v");
            commandLine.add("time");
            commandLine.add("-s");
            commandLine.add("MailPushFactory:D");
            Process process = Runtime.getRuntime().exec(
                    commandLine.toArray(new String[commandLine.size()]));
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()), 1024);
            String line = bufferedReader.readLine();
            // Sample:
            // 05-28 00:20:53.021 D/MailPushFactory( 6694): Wsp Transaction ID : 0
            // 05-28 00:20:53.021 D/MailPushFactory( 6694): Wsp Type : 6
            // 05-28 00:20:53.021 D/MailPushFactory( 6694): Wap Application ID : 9
            // 05-28 00:20:53.021 D/MailPushFactory( 6694): Sms pdu : 0891180945123410f26412d04e2a15447c0...
            // 05-28 00:20:53.021 D/MailPushFactory( 6694): Wap data : 030d6a00850703796831323234406d6f70657261008705c3072010052715205301
            // 05-28 00:20:53.021 D/MailPushFactory( 6694): getEmnMailbox : mailat:yh1224@mopera.net
            // 05-28 00:20:53.021 D/MailPushFactory( 6694): getEmnTimestamp : 1274973653000
            while (line != null) {
                //Log.d(getClass().getName(), line);
                if (line.contains("getEmnMailbox")) {
                    // ログ出力日時を取得
                    String[] days = line.split(" ")[0].split("-");
                    String[] times = line.split(" ")[1].split(":");
                    Calendar ccal = Calendar.getInstance();
                    ccal.set(Calendar.MONTH, Integer.valueOf(days[0]) - 1);
                    ccal.set(Calendar.DAY_OF_MONTH, Integer.valueOf(days[1]));
                    ccal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(times[0]));
                    ccal.set(Calendar.MINUTE, Integer.valueOf(times[1]));
                    ccal.set(Calendar.SECOND, Integer.valueOf(times[2].substring(0, 2)));
                    ccal.set(Calendar.MILLISECOND, 0);
                    //Log.d(getClass().getName(), "ccal = " + ccal.getTimeInMillis());

                    if (mLastNotify == null || ccal.getTimeInMillis() > mLastNotify.getTimeInMillis()) {
                        // 未通知であれば通知する
                        mLastNotify = ccal;
                        result = true;
                        break;
                    }
                }
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
        } catch (IOException e) {
            Log.e(getClass().getName(), "Failed to check log.");
            // 例外処理
        }
        mLastCheck = Calendar.getInstance();
        return result;
    }

    public void doNotify() {
        Calendar cal = Calendar.getInstance();
        long diff = cal.getTimeInMillis() - mLastCheck.getTimeInMillis();
        if (diff > 10000/*10秒以内は再チェックしない*/ && checkLog()) {
            NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = new Notification(R.drawable.icon, 
                    getResources().getString(R.string.app_name),
                    System.currentTimeMillis());
            Intent intent = new Intent(this, EmailNotifyActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
            String message = getResources().getString(R.string.notify_text);
            message += " (" + cal.get(Calendar.HOUR_OF_DAY) + ":"
                    + cal.get(Calendar.MINUTE) + ")";
            notification.setLatestEventInfo(this,
                    getResources().getString(R.string.app_name),
                    message, contentIntent);
            notification.defaults = Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE;
            notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
            notification.ledARGB = 0xff00ff00;
            notification.ledOnMS = 200;
            notification.ledOffMS = 2000;
            notificationManager.notify(1, notification);
        }
    }

    private class EmObserver extends ContentObserver {
        public EmObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            doNotify();
        }
    }

    
    /**
     * サービス開始
     */
    public static void startService(Context ctx) {
        mService = ctx.startService(new Intent(ctx, EmailObserverService.class));
        if (mService == null) {
            Log.e(ctx.getClass().getName(), "EmailObserverService could not start!");
        } else {
            Log.d(ctx.getClass().getName(), "EmailObserverService started: " + mService);
        }
    }

    /**
     * サービス停止
     */
    public static void stopService(Context ctx) {
        if (mService != null) {
            Intent i = new Intent();
            i.setComponent(mService);
            boolean res = ctx.stopService(i);
            if (res == false) {
                Log.e(ctx.getClass().getName(), "EmailObserverService could not stop!");
            } else {
                Log.d(ctx.getClass().getName(), "EmailObserverService stopped: " + mService);
                mService = null;
            }
        }
    }
}
