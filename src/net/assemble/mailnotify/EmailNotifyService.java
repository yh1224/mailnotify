package net.assemble.mailnotify;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;

import net.assemble.android.MyLog;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.WspTypeDecoder;

/**
 * メール監視サービス
 */
public class EmailNotifyService extends Service {
    private static final String TAG = "EmailNotify";

    private static ComponentName mService;
    private static boolean mActive = false;

    private EmailNotifyScreenReceiver mScreenReceiver;
    private LogCheckThread mLogCheckThread;
    private boolean mStopLogCheckThread;
    private long mLastCheck;
    private Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        if (EmailNotify.DEBUG) Log.d(TAG, "EmailNotifyService.onCreate()");
        super.onCreate();

        // プリファレンスのバージョンアップ
        EmailNotifyPreferences.upgrade(this);

        // ネットワーク復元情報を消去
        EmailNotifyPreferences.unsetNetworkInfo(this);

        // ACTION_SCREEN_ON レシーバの登録
        mScreenReceiver = new EmailNotifyScreenReceiver();
        registerReceiver(mScreenReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));

        // すでに通知したものは通知しないようにする
        mLastCheck = EmailNotifyPreferences.getLastCheck(this);
        if (EmailNotify.DEBUG) Log.d(TAG, "Last notify: " + mLastCheck);
        if (mLastCheck == 0) {
            // 前回通知日時が存在しない場合、サービス開始以前を通知しない。
            mLastCheck = Calendar.getInstance().getTimeInMillis();
            EmailNotifyPreferences.setLastCheck(this, mLastCheck);
        }
        startCheck();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (EmailNotify.DEBUG) Log.d(TAG, "EmailNotifyService.onStart()");
        super.onStart(intent, startId);
        startCheck();
    }

    public void startCheck() {
        mActive = true;

        // 常駐アイコン
        if (EmailNotifyPreferences.getNotificationIcon(this)) {
            EmailNotificationManager.showNotificationIcon(this);
        } else {
            EmailNotificationManager.clearNotificationIcon(this);
        }

        // リアルタイムログ監視開始
        startLogCheckThread();
    }

    public void onDestroy() {
        if (EmailNotify.DEBUG) Log.d(TAG, "EmailNotifyService.onDestroy()");
        super.onDestroy();
        mActive = false;

        // 通知アイコン
        EmailNotificationManager.clearNotificationIcon(this);

        // レシーバ解除
        unregisterReceiver(mScreenReceiver);

        // リアルタイムログ監視停止
        stopLogCheckThread();

        // 正常に停止した場合は、次に開始するまでに受信した通知は
        // 通知しないようにするため、前回通知日時をクリアする。
        EmailNotifyPreferences.setLastCheck(this, 0);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    /**
     * ログ日時解析
     * @param line ログ行
     * @return 日時
     */
    private Calendar getLogDate(String line) {
        // 日付
        Calendar cal = Calendar.getInstance();
        String[] days = line.split(" ")[0].split("-");
        String[] times = line.split(" ")[1].split(":");
        cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, Integer.valueOf(days[0]) - 1);
        cal.set(Calendar.DAY_OF_MONTH, Integer.valueOf(days[1]));
        cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(times[0]));
        cal.set(Calendar.MINUTE, Integer.valueOf(times[1]));
        cal.set(Calendar.SECOND, Integer.valueOf(times[2].substring(0, 2)));
        cal.set(Calendar.MILLISECOND, 0);

        return cal;
    }

    /**
     * ログ行をチェック
     *
     * @param line ログ文字列
     * @return WapPdu WAP PDU (null:メール通知ではない)
     */
    private WapPdu checkLogLine(String line) {
        //if (EmailNotify.DEBUG) Log.v(TAG, "> " + line);
        if (line.length() >= 19 && line.substring(19).startsWith("D/WAP PUSH")/* && line.contains(": Rx: ")*/) {
            Calendar ccal = getLogDate(line);
            if (ccal.getTimeInMillis() <= mLastCheck) {
                // チェック済
                if (EmailNotify.DEBUG) Log.d(TAG, "Already checked (" + ccal.getTimeInMillis() + " <= " + mLastCheck + " )");
                return null;
            }

            // LYNX(SH-01B)対応
            if (line.endsWith(": Receive EMN")) {
                MyLog.i(this, TAG, "Received EMN");
                return new WapPdu(0x030a);
            }

            if (line.contains(": Rx: ")) {
                String data = line.split(": Rx: ")[1];

                // データ解析
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                for (int i = 0; i < data.length(); i += 2){
                    int b = Integer.parseInt(data.substring(i, i + 2), 16);
                    baos.write(b);
                }
                WapPdu pdu = new WapPdu(baos.toByteArray());
                if (!pdu.decode()) {
                    MyLog.w(this, TAG, "Unexpected PDU: " + data);
                    return null;
                }
                if (pdu.getBinaryContentType() == WspTypeDecoder.CONTENT_TYPE_B_PUSH_SL) {
                    // application/vnd.wap.slc は、Receiverで受信する ので無視
                    if (EmailNotify.DEBUG) {
                        Log.d(TAG, "Received PDU: " + data);
                        Log.i(TAG, "Received: " + pdu.getMailbox());
                    }
                    return null;
                }
                MyLog.d(this, TAG, "Received PDU: " + data);
                MyLog.i(this, TAG, "Received: " + pdu.getMailbox());
                mLastCheck = ccal.getTimeInMillis();
                return pdu;
            }
        }
        return null;
    }

    /**
     * リアルタイムログ監視スレッド
     */
    private class LogCheckThread extends Thread {
        private Context mCtx;

        @Override
        public void run() {
            mCtx = EmailNotifyService.this;
            MyLog.d(mCtx, TAG, "Starting log check thread.");
            try {
                ArrayList<String> commandLine = new ArrayList<String>();
                commandLine.add("logcat");
                commandLine.add("-v");
                commandLine.add("time");
                commandLine.add("-s");
                //commandLine.add("MailPushFactory:D");
                // ほんとうは「WAP PUSH」でフィルタしたいんだけどスペースがあるとうまくいかない…
                commandLine.add("*:D");
                Process process = Runtime.getRuntime().exec(
                        commandLine.toArray(new String[commandLine.size()]));
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()), 1024);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (mStopLogCheckThread) {
                        break;
                    }
                    final WapPdu pdu = checkLogLine(line);
                    if (pdu != null) {
                        // 最後に通知した日時を保持しておく
                        EmailNotifyPreferences.setLastCheck(mCtx, mLastCheck);
                        int type = pdu.getBinaryContentType();
                        if (type == 0x030a && pdu.getMailbox() != null && pdu.getMailbox().endsWith("docomo.ne.jp")) {
                            if (EmailNotifyPreferences.getServiceSpmode(mCtx)) {
                                String prev = EmailNotifyPreferences.getLastTimestamp(mCtx, EmailNotifyPreferences.SERVICE_SPMODE); 
                                if (prev != null && pdu.getTimestampString() != null && prev.equals(pdu.getTimestampString())) {
                                    // 既に通知済み
                                    MyLog.w(EmailNotifyService.this, TAG, "Duplicated: " + pdu.getTimestampString());
                                } /*TODO: まずはチェックのみ。 else*/ {
                                    notify(pdu, EmailNotifyPreferences.SERVICE_SPMODE);
                                    EmailNotifyPreferences.setLastTimestamp(mCtx, EmailNotifyPreferences.SERVICE_SPMODE, pdu.getTimestampString());
                                }
                            }
                        } else if (type == 0x030a && pdu.getMailbox() != null  && pdu.getMailbox().endsWith("mopera.net")) {
                            if (EmailNotifyPreferences.getServiceMopera(mCtx)) {
                                String prev = EmailNotifyPreferences.getLastTimestamp(mCtx, EmailNotifyPreferences.SERVICE_MOPERA); 
                                if (prev != null && pdu.getTimestampString() != null && prev.equals(pdu.getTimestampString())) {
                                    // 既に通知済み
                                    MyLog.w(EmailNotifyService.this, TAG, "Duplicated: " + pdu.getTimestampString());
                                } /*TODO: まずはチェックのみ。 else*/ {
                                    notify(pdu, EmailNotifyPreferences.SERVICE_MOPERA);
                                    EmailNotifyPreferences.setLastTimestamp(mCtx, EmailNotifyPreferences.SERVICE_MOPERA, pdu.getTimestampString());
                                }
                            }
                        } else if (EmailNotifyPreferences.getServiceOther(mCtx)) {
                            notify(pdu, EmailNotifyPreferences.SERVICE_OTHER);
                        }
                    }
                }
                bufferedReader.close();
                process.destroy();
                MyLog.d(mCtx, TAG, "Exiting log check thread.");
                mLogCheckThread = null;
            } catch (IOException e) {
                MyLog.e(mCtx, TAG, "Unexpected error on log checking.");
                stopSelf();
            }
        }

        /**
         * メール着信通知
         *
         * 通知後にタイマをかけるので、元スレッドで実行する。
         *
         * @param pdu PDU
         */
        private void notify(final WapPdu pdu, final String service) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    EmailNotificationManager.showNotification(mCtx, service, pdu.getMailbox(), pdu.getTimestampDate());
                }
            });
        }
    }

    /**
     * リアルタイムログ監視スレッド開始
     */
    private void startLogCheckThread() {
        if (mLogCheckThread != null) {
            if (EmailNotify.DEBUG) Log.d(TAG, "Log check thread already running.");
            return;
        }
        mStopLogCheckThread = false;
        mLogCheckThread = new LogCheckThread();
        mLogCheckThread.start();
    }

    /**
     * リアルタイムログ監視スレッド停止指示
     */
    private void stopLogCheckThread() {
        if (mLogCheckThread == null) {
            if (EmailNotify.DEBUG) Log.d(TAG, "Log check thread not running.");
            return;
        }
        mStopLogCheckThread = true;
    }


    /**
     * サービス開始
     */
    public static boolean startService(Context ctx) {
        boolean result;
        boolean restart = mActive;
        mService = ctx.startService(new Intent(ctx, EmailNotifyService.class));
        if (mService == null) {
            MyLog.e(ctx, TAG, "Service start failed!");
            result = false;
        } else {
            if (EmailNotify.DEBUG) Log.d(TAG, "EmailNotifyService started: " + mService);
            result = true;
        }
        if (!restart && result) {
            Toast.makeText(ctx, R.string.service_started, Toast.LENGTH_SHORT).show();
            MyLog.i(ctx, TAG, "Service started.");
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
                Log.e(TAG, "EmailNotifyService could not stop!");
            } else {
                if (EmailNotify.DEBUG) Log.d(TAG, "EmailNotifyService stopped: " + mService);
                Toast.makeText(ctx, R.string.service_stopped, Toast.LENGTH_SHORT).show();
                MyLog.i(ctx, TAG, "Service stopped.");
                mService = null;
            }
        }
    }
}
