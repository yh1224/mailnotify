package net.assemble.emailnotify;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.telephony.WspTypeDecoder;

/**
 * メール監視サービス
 */
public class EmailObserverService extends Service {
    private static final String TAG = "EmailNotify";
    private static final int DELAY_CHECK = 200; /* ms */

    private static ComponentName mService;
    private EmObserver mObserver;
    private SQLiteDatabase mDb;
    private Calendar mLastCheck;
    private Handler mHandler = new Handler();
    boolean pending = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // 起動前のものは通知しないようにする
        mLastCheck = Calendar.getInstance();

        // Eメールアプリの監視を開始
        mObserver = new EmObserver(new Handler());
        getContentResolver().registerContentObserver(
                Uri.parse("content://com.android.email/update"), true, mObserver);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        checkEmail();
    }

    public void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(mObserver);
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
     * メール受信通知確認
     *
     * ログから、メール受信通知(WAP PUSH)を受信したかどうかチェックする
     *
     * @return true:メール受信した
     */
    private boolean checkIfMailReceived() {
        boolean result = false;
        //Log.d(TAG, "Checking log...");
        try {
            ArrayList<String> commandLine = new ArrayList<String>();
            commandLine.add("logcat");
            commandLine.add("-d");
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
            String line = bufferedReader.readLine();
            // Sample:
            // 07-23 22:46:53.610 D/WAP PUSH( 1038): Rx: 0006060302030aaf89030d6a00850703796831323234406d6f70657261008705c3072010072313465401
            while (line != null) {
                //Log.d(TAG, line);
                //if (line.contains("getEmnMailbox")) {
                if (line.substring(19).startsWith("D/WAP PUSH") && line.contains(": Rx: ")) {
                    // ログ出力日時を取得
                    String[] days = line.split(" ")[0].split("-");
                    String[] times = line.split(" ")[1].split(":");
                    String data = line.split(": Rx: ")[1];
                    Calendar ccal = Calendar.getInstance();
                    ccal.set(Calendar.MONTH, Integer.valueOf(days[0]) - 1);
                    ccal.set(Calendar.DAY_OF_MONTH, Integer.valueOf(days[1]));
                    ccal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(times[0]));
                    ccal.set(Calendar.MINUTE, Integer.valueOf(times[1]));
                    ccal.set(Calendar.SECOND, Integer.valueOf(times[2].substring(0, 2)));
                    ccal.set(Calendar.MILLISECOND, 0);

                    if (ccal.getTimeInMillis() > mLastCheck.getTimeInMillis()) {
                        // 未チェック
                        mLastCheck = ccal;

                        // データ解析
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        for (int i = 0; i < data.length(); i += 2){
                            int b = Integer.parseInt(data.substring(i, i + 2), 16);
                            baos.write(b);
                        }
                        if (!checkWapPdu(baos.toByteArray())) {
                            // メール受信ではなかった
                            continue;
                        }

                        Log.d(TAG, "[" + ccal.getTimeInMillis() + "] " + data);
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
        startPolling();
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
            //Log.d(TAG, "Launching app: " + component);
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
        //Log.d(TAG, "Killing email app.");
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        manager.restartPackage("com.android.email");
    }

    /**
     * メールが来たかどうかをチェックし、アクションを起こす
     */
    public void checkEmail() {
        pending = false;
        if (checkIfMailReceived()) {
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

    /**
     * Eメールアプリの動作を監視
     */
    private class EmObserver extends ContentObserver {
        public EmObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            //Log.d(TAG, "onChange() called.");

            // DELAY_CHECK(ms)後にcheckEmail()を実行する。
            if (!pending) {
                pending = true;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkEmail();
                    }
                }, DELAY_CHECK);
            }
        }
    }

    /**
     * ポーリングチェックを開始する
     */
    public void startPolling() {
        final Runnable checkEmailHandler = new Runnable() {
            @Override
            public void run() {
                checkEmail();
            }
        };

        int interval = EmailNotifyPreferences.getPollingInterval(this);
        if (interval > 0) {
            Log.d(TAG, "Start polling. interval=" + interval + "sec.");
            mHandler.postDelayed(checkEmailHandler, interval * 1000);
        } else {
            //Log.d(TAG, "Stop polling.");
            mHandler.removeCallbacks(checkEmailHandler);
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
