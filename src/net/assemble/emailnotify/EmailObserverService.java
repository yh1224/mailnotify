package net.assemble.emailnotify;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * メール監視サービス
 */
public class EmailObserverService extends Service {
    private static final String TAG = "EmailNotify";

    private static ComponentName mService;
    private EmObserver mObserver;
    private Calendar mLastCheck;
    private Calendar mLastNotify;

    @Override
    public void onCreate() {
        super.onCreate();

        mLastCheck = Calendar.getInstance();

        mObserver = new EmObserver(new Handler());
        getContentResolver().registerContentObserver(
                Uri.parse("content://com.android.email/update"), true, mObserver);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    public void onDestroy() {
        getContentResolver().unregisterContentObserver(mObserver);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private boolean checkLog() {
        boolean result = false;
//        Log.d(TAG, "Checking email notification.");
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
                //Log.d(TAG, line);
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

                    if (mLastNotify == null || ccal.getTimeInMillis() > mLastNotify.getTimeInMillis()) {
                        // 未通知であれば通知する
                        Log.d(TAG, "[" + ccal.getTimeInMillis() + "] " + line);
                        mLastNotify = ccal;
                        result = true;
                    }
                }
                line = bufferedReader.readLine();
            }
            bufferedReader.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to check log.");
            // 例外処理
        }
        mLastCheck = Calendar.getInstance();
        return result;
    }

    /**
     * 通知
     */
    private void doNotify() {
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.icon,
                getResources().getString(R.string.app_name),
                System.currentTimeMillis());

        Intent intent = new Intent();
        ComponentName component = EmailNotifyPreferences.getComponent(this);
        if (component != null) {
            intent.setComponent(component);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            intent.setClass(this, EmailNotifyActivity.class);
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
        String message = getResources().getString(R.string.notify_text);
//        Calendar cal = Calendar.getInstance();
//        message += " (" + cal.get(Calendar.HOUR_OF_DAY) + ":"
//                + cal.get(Calendar.MINUTE) + ")";
        notification.setLatestEventInfo(this,
                getResources().getString(R.string.app_name),
                message, contentIntent);
        notification.defaults = 0;
        String soundUri = EmailNotifyPreferences.getSound(this);
        if (soundUri.startsWith("content:")) {
            notification.sound = Uri.parse(soundUri);
        }
        if (EmailNotifyPreferences.getVibration(this)) {
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_SHOW_LIGHTS;
        notification.ledARGB = 0xff00ff00;
        notification.ledOnMS = 200;
        notification.ledOffMS = 2000;
        notificationManager.notify(1, notification);
    }

    /**
     * アプリ起動
     */
    private void doLaunch() {
        Intent intent = new Intent();
        ComponentName component = EmailNotifyPreferences.getComponent(this);
        if (component != null) {
            intent.setComponent(component);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            intent.setClass(this, EmailNotifyActivity.class);
        }
        startActivity(intent);
    }

    /**
     * Eメールアプリを殺す
     */
    private void killEmailApp() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        manager.restartPackage("com.android.email");
    }

    /**
     * メールが来たかどうかをチェックし、アクションを起こす
     */
    public void checkEmail() {
        Calendar cal = Calendar.getInstance();
        if (cal.getTimeInMillis() - mLastCheck.getTimeInMillis() < 200) {
            /* 最低でも200msあけてチェック */
            return;
        }

        if (checkLog()) {
            if (EmailNotifyPreferences.getNotify(this)) {
                doNotify();
            }
            if (EmailNotifyPreferences.getLaunch(this)) {
                doLaunch();
            }
            if (EmailNotifyPreferences.getKillEmail(this)) {
                killEmailApp();
            }
        }
    }

    private class EmObserver extends ContentObserver {
        public EmObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            checkEmail();
        }
    }


    /**
     * サービス動作有無取得
     */
    public static boolean isActive() {
        if (mService != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * サービス開始
     */
    public static boolean startService(Context ctx) {
        boolean result;
        boolean restart = EmailObserverService.isActive();
        mService = ctx.startService(new Intent(ctx, EmailObserverService.class));
        if (mService == null) {
            Log.e(TAG, "EmailObserverService could not start!");
            result = false;
        } else {
            Log.d(TAG, "EmailObserverService started: " + mService);
            result = true;
        }
        if (!restart && result) {
            Toast.makeText(ctx, R.string.service_started, Toast.LENGTH_SHORT).show();
        }
        return result;
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
                Log.e(TAG, "EmailObserverService could not stop!");
            } else {
                Log.d(TAG, "EmailObserverService stopped: " + mService);
                Toast.makeText(ctx, R.string.service_stopped, Toast.LENGTH_SHORT).show();
                mService = null;
            }
        }
    }
}
