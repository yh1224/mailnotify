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
    private final long RESTART_INTERVAL = 30 * 60 * 1000;

    private AlarmManager mAlarmManager;

    private static ComponentName mService;
    private static boolean mActive = false;

    private EmailNotifyScreenReceiver mScreenReceiver;
    private LogCheckThread mLogCheckThread;
    private boolean mStopLogCheckThread;
    private long mLastCheck;
    private int mSaveApplicationId;
    private Handler mHandler = new Handler();
    private PendingIntent mRestartIntent = null;

    @Override
    public void onCreate() {
        MyLog.d(this, EmailNotify.TAG, "EmailNotifyService.onCreate()");
        super.onCreate();

        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

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
        mRestartIntent  = PendingIntent.getService(this, 0,
                new Intent(this, EmailNotifyService.class), 0);
        mAlarmManager.setRepeating(AlarmManager.RTC, 0, RESTART_INTERVAL, mRestartIntent);

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

        if (mRestartIntent != null) {
            mAlarmManager.cancel(mRestartIntent);
            mRestartIntent = null;
        }

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
        //if (EmailNotify.DEBUG) Log.v(EmailNotify.TAG, "> " + line);
        if (line.length() >= 19 &&
                (line.substring(19).startsWith("D/WAP PUSH") || line.substring(19).startsWith("D/EmailPushNotification"))) {
            String[] lines =line.split(": ", 2);
            String tag = lines[0].substring(19);
            String log = lines[1];

            Calendar ccal = getLogDate(line);
            if (ccal == null) {
                return null;
            }
            if (ccal.getTimeInMillis() <= mLastCheck) {
                // チェック済
                if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Already checked (" + ccal.getTimeInMillis() + " <= " + mLastCheck + ")");
                return null;
            }

            String data = null;
            WapPdu pdu = null;

            // LYNX(SH-01B)対応
            if (EmailNotifyPreferences.getLynxWorkaround(this) &&
                    tag.startsWith("D/WAP PUSH") && log.equals("Receive EMN")) {
                MyLog.i(this, EmailNotify.TAG, "Received EMN");
                mLastCheck = ccal.getTimeInMillis();
                return new WapPdu();
            }

            // Xperia arc(SO-01C)対応
            if (EmailNotifyPreferences.getXperiaarcWorkaround(this)) {
                // spモードメール
                if (tag.startsWith("D/WAP PUSH") && log.equals("call startService : Intent { act=android.provider.Telephony.WAP_PUSH_RECEIVED typ=application/vnd.wap.emn+wbxml cmp=jp.co.nttdocomo.carriermail/.SMSService (has extras) }")) {
                    MyLog.i(this, EmailNotify.TAG, "Detected broadcast WAP_PUSH_RECEIVED for sp-mode");
                    mLastCheck = ccal.getTimeInMillis();
                    return new WapPdu(EmailNotifyPreferences.SERVICE_SPMODE, "spmode");
                }
                // mopera Uメール
                if (tag.startsWith("D/EmailPushNotification") && log.startsWith("Wap data : ")) {
                    if (mSaveApplicationId == 0x09) {
                        // Content-Type: application/vnd.wap.emn+wbxml と仮定する
                        data = log.split("Wap data : ")[1];
                        if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Found Wap data : " + data);
                        try {
                            pdu = new WapPdu("application/vnd.wap.emn+wbxml", 0x09, hex2bytes(data));
                        } catch (Exception e) {
                            MyLog.w(this, EmailNotify.TAG, "Invalid PDU: " + data);
                            e.printStackTrace();
                        }
                    }
                }
            }

            if (tag.startsWith("D/WAP PUSH") && log.contains("Rx: ")) {
                data = log.split("Rx: ")[1];
                try {
                    pdu = new WapPdu(hex2bytes(data));
                } catch (Exception e) {
                    MyLog.w(this, EmailNotify.TAG, "Invalid PDU: " + data);
                    e.printStackTrace();
                }
            }

            if (pdu != null) {
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
     * 16進数文字列をバイト配列に変換
     *
     * @param hex 16進数文字列
     * @return バイト配列
     */
    private byte[] hex2bytes(String hex) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < hex.length(); i += 2){
            int b = Integer.parseInt(hex.substring(i, i + 2), 16);
            baos.write(b);
        }
        return baos.toByteArray();
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
        private void notify(Date logdate, WapPdu pdu) {
            final String contentType = pdu.getContentType();
            final Date timestamp = pdu.getTimestampDate();
            final String mailbox = pdu.getMailbox();
            final String serviceName = pdu.getServiceName();

            // 記録
            final long historyId = EmailNotificationHistoryDao.add(mCtx, logdate,
                    contentType, pdu.getApplicationId(), mailbox, timestamp, serviceName, pdu.getHexString());
            if (historyId < 0) {
                MyLog.w(EmailNotifyService.this, EmailNotify.TAG, "Duplicated: "
                        + "mailbox=" + mailbox + ", timestamp=" + pdu.getTimestampString());
                return;
            }

            // 通知
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    doNotify(EmailNotifyService.this, contentType, mailbox, timestamp,
                            serviceName, historyId, false);
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
     * @param ctx Context
     * @param contentType Content-Type
     * @param timestampDate タイムスタンプ
     * @param mailbox メールボックス名
     * @param serviceName サービス名
     * @param historyId 履歴ID
     * @param restore 復元
     */
    public static void doNotify(Context ctx, String contentType, String mailbox, Date timestampDate, String serviceName, long historyId, boolean restore) {
        if (EmailNotifyPreferences.getService(ctx, serviceName)) {
            if (EmailNotifyPreferences.inExcludeHours(ctx, serviceName)) {
                MyLog.d(ctx, EmailNotify.TAG, "Ignored: This is exclude hours now.");
                // PENDING: あとで通知する?
                EmailNotificationHistoryDao.ignored(ctx, historyId);
                return;
            }
            EmailNotificationManager.showNotification(ctx,
                    serviceName, mailbox, timestampDate, restore);
        } else {
            MyLog.d(ctx, EmailNotify.TAG, "Ignored: This is exclude service.");
            EmailNotificationHistoryDao.ignored(ctx, historyId);
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
                String contentType = cur.getString(cur.getColumnIndex("content_type"));
                String mailbox = cur.getString(cur.getColumnIndex("mailbox"));
                Date timestampDate = null;
                long timestamp = cur.getLong(cur.getColumnIndex("timestamp"));
                String serviceName = cur.getString(cur.getColumnIndex("service_name"));
                if (timestamp > 0) {
                    timestampDate = new Date(timestamp * 1000);
                }
                if ((cur.getLong(cur.getColumnIndex("notified_at"))) == 0) {
                    // 未通知なら改めて通知
                    doNotify(this, contentType, mailbox, timestampDate, serviceName, id, false);
                } else {
                    // 通知済みなら表示のみ
                    doNotify(this, contentType, mailbox, timestampDate, serviceName, id, true);
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
