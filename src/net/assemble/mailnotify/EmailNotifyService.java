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

    private LogCheckThread mLogCheckThread;
    private boolean mStopLogCheckThread;
    private Calendar mLastCheck;

    @Override
    public void onCreate() {
        super.onCreate();

        // 起動前のものは通知しないようにする
        mLastCheck = Calendar.getInstance();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        // 通知アイコン
        if (EmailNotifyPreferences.getNotificationIcon(this)) {
            EmailNotifyNotification.showNotificationIcon(this);
        } else {
            EmailNotifyNotification.clearNotificationIcon(this);
        }

        // リアルタイムログ監視開始
        startLogCheckThread();
    }

    public void onDestroy() {
        super.onDestroy();

        // 通知アイコン
        EmailNotifyNotification.clearNotificationIcon(this);

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
        if (EmailNotify.DEBUG) Log.v(TAG, "> " + line);
        if (line.substring(19).startsWith("D/WAP PUSH")/* && line.contains(": Rx: ")*/) {
            Calendar ccal = getLogDate(line);
            if (ccal.getTimeInMillis() <= mLastCheck.getTimeInMillis()) {
                // チェック済
                return null;
            }
            mLastCheck = ccal;

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
                return pdu;
            }
        }
        return null;
    }

    /**
     * リアルタイムログ監視スレッド
     */
    private class LogCheckThread extends Thread {
        @Override
        public void run() {
            if (EmailNotify.DEBUG) Log.d(TAG, "Log check thread started.");
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
                    WapPdu pdu = checkLogLine(line);
                    if (pdu != null) {
                        int type = pdu.getBinaryContentType();
                        if (type == 0x030a && pdu.getMailbox().endsWith("docomo.ne.jp")) {
                            if (EmailNotifyPreferences.getServiceSpmode(EmailNotifyService.this)) {
                                EmailNotifyNotification.doNotify(EmailNotifyService.this, pdu.getMailbox());
                            }
                        } else if (type == 0x030a && pdu.getMailbox().endsWith("mopera.net")) {
                            if (EmailNotifyPreferences.getServiceMopera(EmailNotifyService.this)) {
                                EmailNotifyNotification.doNotify(EmailNotifyService.this, pdu.getMailbox());
                            }
                        } else if (EmailNotifyPreferences.getServiceAny(EmailNotifyService.this)) {
                            EmailNotifyNotification.doNotify(EmailNotifyService.this, pdu.getMailbox());
                        }
                    }
                }
                bufferedReader.close();
                if (EmailNotify.DEBUG) Log.d(TAG, "Log check thread stopped.");
                mLogCheckThread = null;
            } catch (IOException e) {
                MyLog.e(EmailNotifyService.this, TAG, "Unexpected error on check.");
                stopSelf();
            }
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
        boolean restart = EmailNotifyService.isActive();
        mService = ctx.startService(new Intent(ctx, EmailNotifyService.class));
        if (mService == null) {
            Log.e(TAG, "EmailNotifyService could not start!");
            result = false;
        } else {
            Log.d(TAG, "EmailNotifyService started: " + mService);
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
                Log.d(TAG, "EmailNotifyService stopped: " + mService);
                Toast.makeText(ctx, R.string.service_stopped, Toast.LENGTH_SHORT).show();
                MyLog.i(ctx, TAG, "Service stopped.");
                mService = null;
            }
        }
    }
}
