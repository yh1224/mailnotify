package net.assemble.emailnotify;

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

        // リアルタイムログ監視停止
        stopLogCheckThread();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    /**
     * WAP PDU解析 (基本ぱくり)
     *
     * frameworks/base/telephony/java/com/android/internal/telephony/WapPushOverSms.java
     * WapPushOverSms#dispatchWapPdu()
     *
     * @param pdu WAP PDU
     * @return true:メールを受信した
     */
    private boolean checkWapPdu(byte[] pdu) {
        WspTypeDecoder pduDecoder = new WspTypeDecoder(pdu);

        int index = 0;
        int transactionId = pdu[index++] & 0xFF;
        int pduType = pdu[index++] & 0xFF;
        int headerLength = 0;

        if ((pduType != WspTypeDecoder.PDU_TYPE_PUSH) &&
                (pduType != WspTypeDecoder.PDU_TYPE_CONFIRMED_PUSH)) {
            Log.w(TAG, "Received non-PUSH WAP PDU. Type = " + pduType);
            return false;
        }

        pduDecoder = new WspTypeDecoder(pdu);

        /**
         * Parse HeaderLen(unsigned integer).
         * From wap-230-wsp-20010705-a section 8.1.2
         * The maximum size of a uintvar is 32 bits.
         * So it will be encoded in no more than 5 octets.
         */
        if (pduDecoder.decodeUintvarInteger(index) == false) {
            Log.w(TAG, "Received PDU. Header Length error.");
            return false;
        }
        headerLength = (int)pduDecoder.getValue32();
        index += pduDecoder.getDecodedDataLength();

        int headerStartIndex = index;

        /**
         * Parse Content-Type.
         * From wap-230-wsp-20010705-a section 8.4.2.24
         *
         * Content-type-value = Constrained-media | Content-general-form
         * Content-general-form = Value-length Media-type
         * Media-type = (Well-known-media | Extension-Media) *(Parameter)
         * Value-length = Short-length | (Length-quote Length)
         * Short-length = <Any octet 0-30>   (octet <= WAP_PDU_SHORT_LENGTH_MAX)
         * Length-quote = <Octet 31>         (WAP_PDU_LENGTH_QUOTE)
         * Length = Uintvar-integer
         */
        if (pduDecoder.decodeContentType(index) == false) {
            Log.w(TAG, "Received PDU. Header Content-Type error.");
            return false;
        }
        int binaryContentType;
        String mimeType = pduDecoder.getValueString();
        if (mimeType == null) {
            binaryContentType = (int)pduDecoder.getValue32();
            switch (binaryContentType) {
                case WspTypeDecoder.CONTENT_TYPE_B_DRM_RIGHTS_XML:
                    mimeType = WspTypeDecoder.CONTENT_MIME_TYPE_B_DRM_RIGHTS_XML;
                    break;
                case WspTypeDecoder.CONTENT_TYPE_B_DRM_RIGHTS_WBXML:
                    mimeType = WspTypeDecoder.CONTENT_MIME_TYPE_B_DRM_RIGHTS_WBXML;
                    break;
                case WspTypeDecoder.CONTENT_TYPE_B_PUSH_SI:
                    mimeType = WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_SI;
                    break;
                case WspTypeDecoder.CONTENT_TYPE_B_PUSH_SL:
                    mimeType = WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_SL;
                    break;
                case WspTypeDecoder.CONTENT_TYPE_B_PUSH_CO:
                    mimeType = WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_CO;
                    break;
                case WspTypeDecoder.CONTENT_TYPE_B_MMS:
                    mimeType = WspTypeDecoder.CONTENT_MIME_TYPE_B_MMS;
                    break;
                case 0x030a:
                    mimeType = "application/vnd.wap.emn+wbxml";
                    break;
                default:
                    Log.w(TAG, "Received PDU. Unsupported Content-Type = " + binaryContentType);
                    return false;
            }
        } else {
            if (mimeType.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_DRM_RIGHTS_XML)) {
                binaryContentType = WspTypeDecoder.CONTENT_TYPE_B_DRM_RIGHTS_XML;
            } else if (mimeType.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_DRM_RIGHTS_WBXML)) {
                binaryContentType = WspTypeDecoder.CONTENT_TYPE_B_DRM_RIGHTS_WBXML;
            } else if (mimeType.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_SI)) {
                binaryContentType = WspTypeDecoder.CONTENT_TYPE_B_PUSH_SI;
            } else if (mimeType.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_SL)) {
                binaryContentType = WspTypeDecoder.CONTENT_TYPE_B_PUSH_SL;
            } else if (mimeType.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_PUSH_CO)) {
                binaryContentType = WspTypeDecoder.CONTENT_TYPE_B_PUSH_CO;
            } else if (mimeType.equals(WspTypeDecoder.CONTENT_MIME_TYPE_B_MMS)) {
                binaryContentType = WspTypeDecoder.CONTENT_TYPE_B_MMS;
            } else if (mimeType.equals("application/vnd.wap.emn+wbxml")) {
                binaryContentType = 0x030a;
            } else {
                Log.w(TAG, "Received PDU. Unknown Content-Type = " + mimeType);
                return false;
            }
        }
        index += pduDecoder.getDecodedDataLength();
        int dataIndex = headerStartIndex + headerLength;

        Log.d(TAG ,"Receieved WAP PDU. transactionId=" + transactionId + ", pduType=" + pduType +
                ", contentType=" + mimeType + "(" + binaryContentType + ")" +
                ", dataIndex=" + dataIndex);

        if (binaryContentType != 0x030a) {
            return false;
        }

        return true;
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
     * @return true:新規メール受信通知
     */
    private boolean checkLogLine(String line) {
        if (EmailNotify.DEBUG) Log.v(TAG, "> " + line);
        if (line.substring(19).startsWith("D/WAP PUSH") && line.contains(": Rx: ")) {
            Calendar ccal = getLogDate(line);
            if (ccal.getTimeInMillis() <= mLastCheck.getTimeInMillis()) {
                // チェック済
                return false;
            }
            mLastCheck = ccal;

            String data = line.split(": Rx: ")[1];

            // データ解析
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (int i = 0; i < data.length(); i += 2){
                int b = Integer.parseInt(data.substring(i, i + 2), 16);
                baos.write(b);
            }
            boolean ret = false;
            try {
                ret = checkWapPdu(baos.toByteArray());
            } catch (IndexOutOfBoundsException e) {}
            if (!ret) {
                // メール受信ではなかった
                MyLog.w(this, TAG, "Unexpected PDU: " + data);
                return false;
            }

            MyLog.i(this, TAG, "Received: " + data);
            return true;
        }
        return false;
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
                    if (checkLogLine(line)) {
                        EmailNotifyNotification.doNotify(EmailNotifyService.this);
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
            Log.e(TAG, "EmailObserverService could not start!");
            result = false;
        } else {
            Log.d(TAG, "EmailObserverService started: " + mService);
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
                Log.e(TAG, "EmailObserverService could not stop!");
            } else {
                Log.d(TAG, "EmailObserverService stopped: " + mService);
                Toast.makeText(ctx, R.string.service_stopped, Toast.LENGTH_SHORT).show();
                MyLog.i(ctx, TAG, "Service stopped.");
                mService = null;
            }
        }
    }
}
