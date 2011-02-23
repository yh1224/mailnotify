package net.assemble.emailnotify.core;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import net.orleaf.android.MyLog;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.util.Log;
import android.widget.Toast;

/**
 * メール監視サービス
 */
public class EmailNotifyService extends Service {
    private static ComponentName mService;
    private static boolean mActive = false;

    private EmailNotifyScreenReceiver mScreenReceiver;
    private LogCheckThread mLogCheckThread;
    private boolean mStopLogCheckThread;
    private long mLastCheck;
    private Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        MyLog.d(this, EmailNotify.TAG, "EmailNotifyService.onCreate()");
        super.onCreate();

        // プリファレンスのバージョンアップ
        EmailNotifyPreferences.upgrade(this);

        // ネットワーク復元情報を消去
        EmailNotifyPreferences.unsetNetworkInfo(this);

        // ACTION_SCREEN_ON レシーバの登録
        mScreenReceiver = new EmailNotifyScreenReceiver();
        registerReceiver(mScreenReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));

        // 異常終了チェック
        mLastCheck = EmailNotifyPreferences.getLastCheck(this);
        if (mLastCheck != 0) {
            Date d = new Date(mLastCheck);
            MyLog.w(this, EmailNotify.TAG, "Service restarted unexpectedly. Last checked at " + d.toLocaleString());
            // 未消去の通知を復元する
            restoreNotifications();
            // ネットワーク復元情報を消去
            EmailNotifyPreferences.unsetNetworkInfo(this);
        } else {
            // 正常に終了していた場合、サービス開始以前のものを通知しない。
            mLastCheck = Calendar.getInstance().getTimeInMillis();
            EmailNotifyPreferences.setLastCheck(this, mLastCheck);
        }

        // 定期的に再startを仕掛けておく
        PendingIntent restartIntent = PendingIntent.getService(this, 0, 
                new Intent(this, EmailNotifyService.class), 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC, 0, 30 * 60 * 1000, restartIntent);

        startCheck();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        MyLog.d(this, EmailNotify.TAG, "EmailNotifyService.onStart()");
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
        MyLog.d(this, EmailNotify.TAG, "EmailNotifyService.onDestroy()");
        super.onDestroy();
        mActive = false;

        // 通知アイコン
        EmailNotificationManager.clearNotificationIcon(this);

        // レシーバ解除
        unregisterReceiver(mScreenReceiver);

        // リアルタイムログ監視停止
        stopLogCheckThread();
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
        String logdate = line.substring(0, 18);
        Calendar cal = Calendar.getInstance();
        Date date;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            date = sdf.parse(cal.get(Calendar.YEAR) + "-" + logdate);
        } catch (ParseException e) {
            Log.w(EmailNotify.TAG, "Unexpected log date: " + logdate);
            return null;
        }
        cal.setTime(date);
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
            if (ccal == null) {
                return null;
            }
            if (ccal.getTimeInMillis() <= mLastCheck) {
                // チェック済
                if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Already checked (" + ccal.getTimeInMillis() + " <= " + mLastCheck + ")");
                return null;
            }

            // LYNX(SH-01B)対応
            if (EmailNotifyPreferences.getLynxWorkaround(this) && line.endsWith(": Receive EMN")) {
                MyLog.i(this, EmailNotify.TAG, "Received EMN");
                mLastCheck = ccal.getTimeInMillis();
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
                    MyLog.w(this, EmailNotify.TAG, "Unexpected PDU: " + data);
                    return null;
                }
                MyLog.d(this, EmailNotify.TAG, "Received PDU: " + data);
                if (pdu.getTimestampDate() != null) {
                    MyLog.i(this, EmailNotify.TAG, "Received: " + pdu.getMailbox() + " (" + pdu.getTimestampDate().toLocaleString() + ")");
                } else {
                    MyLog.i(this, EmailNotify.TAG, "Received: " + pdu.getMailbox());
                }
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

        /**
         * Constructor
         * @param ctx Context
         */
        public LogCheckThread(Context ctx) {
            super();
            mCtx = ctx;
        }

        /**
         * logcatクリア
         */
        private void clearLog() {
            try {
                Process process = Runtime.getRuntime().exec(new String[] {"logcat", "-c" });
                process.waitFor();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            MyLog.d(mCtx, EmailNotify.TAG, "Logcat cleared.");
        }

        /**
         * エラー出力を取得する
         */
        private String getErrorMessage(Process process) throws IOException {
            String line;
            BufferedReader errReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()), 1024);
            StringBuffer errMsg = new StringBuffer();
            while ((line = errReader.readLine()) != null) {
                errMsg.append(line + "\n");
            }
            return errMsg.toString().trim();
        }

        @Override
        public void run() {
            MyLog.d(mCtx, EmailNotify.TAG, "Starting log check thread.");
            EmailNotificationManager.clearSuspendedNotification(mCtx);

            String[] command = new String[] {
                "logcat",
                "-v", "time",
                "-s", "*:D"
                 // ほんとうは「WAP PUSH」でフィルタしたいんだけどスペースがあるとうまくいかない…
            };
            int errCount = 0;
            while (true) {
                try {
                    Process process = Runtime.getRuntime().exec(command);
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()), 1024);
                    int readCount = 0;
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        if (mStopLogCheckThread) {
                            break;
                        }
                        readCount++;
                        WapPdu pdu = checkLogLine(line);
                        if (pdu != null) {
                            // 最終チェック日時を更新
                            EmailNotifyPreferences.setLastCheck(mCtx, mLastCheck);
                            notify(getLogDate(line).getTime(), pdu);
                        }
                    }
                    bufferedReader.close();
                    String errMsg = getErrorMessage(process);
                    process.destroy();
                    if (!mStopLogCheckThread) {
                        // 不正終了
                        MyLog.w(mCtx, EmailNotify.TAG, "Unexpectedly suspended. read=" + readCount);
                        process.waitFor();
                        MyLog.d(mCtx, EmailNotify.TAG, "exitValue=" + process.exitValue()+ "\n" + errMsg);

                        // 5回連続して全く読めなかった場合は通知を出して停止
                        if (readCount == 0) {
                            errCount++;
                            if (errCount >= 5) {
                                break;
                            }
                        } else {
                            errCount = 0;
                        }

                        // ログをクリアして再試行する。
                        clearLog();
                        Thread.sleep(5000);
                        continue;
                    }
                } catch (IOException e) {
                    MyLog.e(mCtx, EmailNotify.TAG, "Unexpected error on log checking.");
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    MyLog.e(mCtx, EmailNotify.TAG, "Interrupted on log checking.");
                    e.printStackTrace();
                }
                break;
            }

            if (!mStopLogCheckThread) {
                EmailNotificationManager.showSuspendedNotification(mCtx);
            }
            MyLog.d(mCtx, EmailNotify.TAG, "Exiting log check thread.");
            mLogCheckThread = null;
            stopSelf();
        }

        /**
         * メール着信通知
         *
         * @param logdate
         * @param pdu
         */
        private void notify(Date logdate, final WapPdu pdu) {
            // 重複通知チェック
            if (EmailNotificationHistoryDao.exists(mCtx, pdu.getMailbox(), pdu.getTimestampDate())) {
                MyLog.w(EmailNotifyService.this, EmailNotify.TAG, "Duplicated: " + pdu.getTimestampString());
                return;
            }

            // 記録
            final long historyId = EmailNotificationHistoryDao.add(mCtx, logdate,
                    pdu.getContentType(), pdu.getMailbox(), pdu.getTimestampDate(), pdu.getHexString());

            // 通知
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    doNotify(pdu, historyId, false);
                }
            });
        }
    }

    /**
     * リアルタイムログ監視スレッド開始
     */
    private void startLogCheckThread() {
        if (mLogCheckThread != null) {
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Log check thread already running.");
            return;
        }
        mStopLogCheckThread = false;
        mLogCheckThread = new LogCheckThread(this);
        mLogCheckThread.start();
    }

    /**
     * リアルタイムログ監視スレッド停止指示
     */
    private void stopLogCheckThread() {
        if (mLogCheckThread == null) {
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Log check thread not running.");
            return;
        }
        mStopLogCheckThread = true;
    }

    /**
     * メール着信通知
     *
     * @param pdu WAP PDU
     * @param restore 復元
     */
    private void doNotify(WapPdu pdu, long historyId, boolean restore) {
        String service = null;
        String mailbox = pdu.getMailbox();

        // メールサービス別通知
        int type = pdu.getBinaryContentType();
        if (type == 0x030a && mailbox != null && mailbox.endsWith("docomo.ne.jp")) {
            // spモードメール
            if (EmailNotifyPreferences.getServiceSpmode(this)) {
                service = EmailNotifyPreferences.SERVICE_SPMODE;
            }
        } else if (type == 0x030a && mailbox != null  && mailbox.endsWith("mopera.net")) {
            // mopera Uメール
            if (EmailNotifyPreferences.getServiceMopera(this)) {
                service = EmailNotifyPreferences.SERVICE_MOPERA;
            }
        } else if (type == 0x30 && mailbox != null && mailbox.contains("docomo.ne.jp")) {
            // iモードメール
            if (EmailNotifyPreferences.getServiceImode(this)) {
                service = EmailNotifyPreferences.SERVICE_IMODE;
            }
        } else if (EmailNotifyPreferences.getServiceOther(this)) {
            // その他
            service = EmailNotifyPreferences.SERVICE_OTHER;
        }

        if (service != null) {
            if (EmailNotifyPreferences.inExcludeHours(this, service)) {
                MyLog.d(this, EmailNotify.TAG, "This is exclude hours now.");
                // PENDING: あとで通知する?
                EmailNotificationHistoryDao.ignored(this, historyId);
                return;
            }
            EmailNotificationManager.showNotification(this,
                    service, mailbox, pdu.getTimestampDate(), restore);
        } else {
            EmailNotificationHistoryDao.ignored(this, historyId);
        }
    }

    /**
     * 未消去の通知を復元する
     *
     * @param pdu WAP PDU
     */
    private void restoreNotifications() {
        Cursor cur = EmailNotificationHistoryDao.getActiveHistories(this);
        if (cur.moveToFirst()) {
            do {
                long id = cur.getLong(cur.getColumnIndex(BaseColumns._ID));
                MyLog.d(this, EmailNotify.TAG, "Restoring notification: " + id);
                String wap_data = cur.getString(cur.getColumnIndex("wap_data"));
                WapPdu pdu = new WapPdu(wap_data);
                pdu.decode();
                if ((cur.getLong(cur.getColumnIndex("notified_at"))) == 0) {
                    // 未通知なら改めて通知
                    doNotify(pdu, id, false);
                } else {
                    // 通知済みなら表示のみ
                    doNotify(pdu, id, true);
                }
            } while (cur.moveToNext());
        }
    }


    /**
     * サービス開始
     */
    public static boolean startService(Context ctx) {
        boolean result;
        boolean restart = mActive;
        mService = ctx.startService(new Intent(ctx, EmailNotifyService.class));
        if (mService == null) {
            MyLog.e(ctx, EmailNotify.TAG, "Service start failed!");
            result = false;
        } else {
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "EmailNotifyService started: " + mService);
            result = true;
        }
        if (!restart && result) {
            Toast.makeText(ctx, R.string.service_started, Toast.LENGTH_SHORT).show();
            MyLog.i(ctx, EmailNotify.TAG, "Service started.");
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
                Log.e(EmailNotify.TAG, "EmailNotifyService could not stop!");
            } else {
                if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "EmailNotifyService stopped: " + mService);
                Toast.makeText(ctx, R.string.service_stopped, Toast.LENGTH_SHORT).show();
                MyLog.i(ctx, EmailNotify.TAG, "Service stopped.");
                mService = null;

                // 正常に停止した場合は、次に開始するまでに受信した通知を
                // 通知しないようにするため、最終チェック日時をクリアする。
                EmailNotifyPreferences.setLastCheck(ctx, 0);
            }
        }
    }
}
