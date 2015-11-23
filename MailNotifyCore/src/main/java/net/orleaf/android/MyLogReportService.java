package net.orleaf.android;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.widget.Toast;

public class MyLogReportService extends Service {
    public static final String EXTRA_PROGRESS = "progress";
    public static final String EXTRA_REPORTER_ID = "reporter_id";
    public static final String EXTRA_INTENT = "intent";
    public static final String EXTRA_DELAY = "delay";
    public static final String EXTRA_WAIT_CONNECT = "wait_connect";
    public static final int WAIT_CONNECT_DISABLE = 0;
    public static final int WAIT_CONNECT_ENABLE = 1;
    public static final int WAIT_CONNECT_WIFIONLY = 2;

    private static final String REPORT_URL = "https://orleaf.net/mailnotify/index.php/report/submit";

    private static ComponentName mService;

    private PendingIntent mCallbackIntent;
    private String mReporterId;
    private boolean mProgress;
    private int mDelay;
    private int mWaitConnect;

    private Handler handler = new Handler();
    private BroadcastReceiver mConnectivityReceiver;
    private boolean mStarted = false;

    private RemoteCallbackList<IMyLogReportListener> listeners = new RemoteCallbackList<IMyLogReportListener>();
    private final IMyLogReportService.Stub binder = new IMyLogReportService.Stub() {
        public void addListener(IMyLogReportListener listener) {
            listeners.register(listener);
        }
        public void removeListener(IMyLogReportListener listener) {
            listeners.unregister(listener);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    // This is the old onStart method that will be called on the pre-2.0
    // platform.  On 2.0 or later we override onStartCommand() so this
    // method will not be called.
    @SuppressWarnings("deprecation")
    @Override
    public void onStart(Intent intent, int startId) {
        handleCommand(intent);
    }

    @TargetApi(5)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        handleCommand(intent);
        return binder;
    }

    /**
     * ログ送信
     */
    public void handleCommand(Intent intent) {
        if (intent == null) {
            return;
        }

        mCallbackIntent = intent.getParcelableExtra(EXTRA_INTENT);
        mProgress = intent.getBooleanExtra(EXTRA_PROGRESS, false);
        if (!mProgress) {
            mDelay = intent.getIntExtra(EXTRA_DELAY, 0);
        } else {
            mDelay = 0;
        }
        mReporterId = intent.getStringExtra(EXTRA_REPORTER_ID);
        mWaitConnect = intent.getIntExtra(EXTRA_WAIT_CONNECT, WAIT_CONNECT_DISABLE);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                start();
            }
        }, mDelay * 1000);
    }

    /**
     * レポート送信
     */
    private void start() {
        if (mWaitConnect != WAIT_CONNECT_DISABLE) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isConnected()) {
                if (mProgress) {
                    Toast.makeText(MyLogReportService.this, "Pending report send...", Toast.LENGTH_SHORT).show();
                }
                registerConnectivityReceiver();
                return;
            }
            if (mWaitConnect == WAIT_CONNECT_WIFIONLY &&
                    networkInfo.getType() != ConnectivityManager.TYPE_WIFI) {
                registerConnectivityReceiver();
                return;
            }
        }
        startSendReport();
    }

    /**
     * ログ送信実行
     */
    private void startSendReport() {
        if (!mStarted) {
            mStarted = true;
            new ReportTask().execute();
        }
    }


    /**
     * ログ送信タスク
     */
    private class ReportTask extends AsyncTask<Object, Integer, String> {

        private String mResult = null;

        /**
         * 前処理
         */
        @Override
        protected void onPreExecute() {
            if (mProgress) {
                Toast.makeText(MyLogReportService.this, "Now sending report...", Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * ログ送信処理居
         */
        @Override
        protected String doInBackground(Object... params) {
            //if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Executing ReportTask.");
            HashMap<String, String> posts = new HashMap<String, String>();

            try {
                PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
                posts.put("app_name", getResources().getString(pi.applicationInfo.labelRes));
                posts.put("app_version", pi.versionName);
            } catch (NameNotFoundException ignored) {}
            posts.put("reporter_id", mReporterId);
            posts.put("android_id", Secure.getString(getContentResolver(), Secure.ANDROID_ID));
            posts.put("build_version_release", Build.VERSION.RELEASE);
            posts.put("build_brand", Build.BRAND);
            posts.put("build_model", Build.MODEL);
            posts.put("build_id", Build.ID);
            posts.put("build_fingerprint", Build.FINGERPRINT);
            //posts.put("Build.VERSION.CODENAMET", Build.VERSION.CODENAME);
            //posts.put("Build.VERSION.INCREMENTAL", Build.VERSION.INCREMENTAL);
            //posts.put("Build.VERSION.RELEASE", Build.VERSION.RELEASE);
            posts.put("log", MyLog.getLogText(MyLogReportService.this));
            posts.put("preferences", getPreferencesString());
            return postTo(REPORT_URL, posts);
        }

        /**
         * POST
         *
         * @param urlStr 送信先URL
         * @param params POSTパラメタ
         * @return エラー文字列 (null:成功)
         */
        private String postTo(String urlStr, HashMap<String, String> params) {
            StringBuilder resultBuf = new StringBuilder();
            try {
                URL url = new URL(urlStr);
                HttpURLConnection urlconn = (HttpURLConnection)url.openConnection();
                urlconn.setDoOutput(true);

                PrintStream ps = new PrintStream(urlconn.getOutputStream());
                ps.print(getQueryString(params));
                ps.close();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(urlconn.getInputStream()));
                String s;
                while ((s = reader.readLine()) != null) {
                    resultBuf.append(s);
                }
                reader.close();
                mResult = resultBuf.toString();

                urlconn.disconnect();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return e.toString();
            }
        }

        /**
         * クエリ文字列へ変換
         *
         * @param params パラメタ
         * @return クエリ文字列
         */
        @SuppressWarnings("rawtypes")
        private String getQueryString(HashMap<String, String> params) throws UnsupportedEncodingException {
            StringBuilder postBuf = new StringBuilder();
            for (Entry<String, String> entry : params.entrySet()) {
                if (postBuf.length() > 0) {
                    postBuf.append("&");
                }
                String val = entry.getValue();
                if (val != null) {
                    val = URLEncoder.encode(val, "UTF-8");
                }
                postBuf.append(entry.getKey()).append("=").append(val);
            }
            return postBuf.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            // インテントで結果通知
            if (mCallbackIntent != null) {
                try {
                    Intent intent = new Intent();
                    intent.putExtra("result", result);
                    mCallbackIntent.send(getApplicationContext(), 0, intent);
                } catch (CanceledException e) {
                    e.printStackTrace();
                }
            }

            // 結果表示
            if (result == null) {
                if (mProgress) {
                    Toast.makeText(MyLogReportService.this, "Sent report successfully.", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (mProgress) {
                    Toast.makeText(MyLogReportService.this, "Failed to send report: " + result, Toast.LENGTH_LONG).show();
                }
            }

            // コールバック onComplete()
            int numListeners = listeners.beginBroadcast();
            for (int i = 0; i < numListeners; i++) {
                try {
                    listeners.getBroadcastItem(i).onComplete(mResult);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            listeners.finishBroadcast();

            stopSelf();
        }
    }

    private String getPreferencesString() {
        StringBuilder strBuf = new StringBuilder();
        Map<String, ?> prefs = PreferenceManager.getDefaultSharedPreferences(this).getAll();
        for (Entry<String, ?> entry : prefs.entrySet()) {
            if (entry.getValue() == null) {
                strBuf.append(entry.getKey()).append("=(null)\n");
            } else {
                strBuf.append(entry.getKey()).append("=").append(entry.getValue().toString()).append("\n");
            }
        }
        return strBuf.toString();
    }

    /**
     * ネットワーク接続監視開始
     */
    private void registerConnectivityReceiver() {
        if (mConnectivityReceiver == null) {
            mConnectivityReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    start();
                }
            };
            registerReceiver(mConnectivityReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    /**
     * ネットワーク接続監視停止
     */
    private void unregisterConnectivityReceiver() {
        if (mConnectivityReceiver != null) {
            unregisterReceiver(mConnectivityReceiver);
            mConnectivityReceiver = null;
        }
    }

    public void onDestroy() {
        super.onDestroy();
        unregisterConnectivityReceiver();
    }


    /**
     * サービス開始 (送信中を表示)
     *
     * @param ctx Context
     * @param reporterId Reporter ID
     * @return サービス起動成否
     */
    public static boolean startServiceWithProgress(Context ctx, String reporterId) {
        Intent intent = new Intent(ctx, MyLogReportService.class);
        intent.putExtra(EXTRA_REPORTER_ID, reporterId);
        intent.putExtra(EXTRA_PROGRESS, true);
        mService = ctx.startService(intent);
        return (mService != null);
    }

    /**
     * サービス開始 (送信完了時インテントを通知)
     *
     * @param ctx Context
     * @param reporterId Reporter ID
     * @param callbackIntent 送信完了時に通知するインテント
     * @return サービス起動成否
     */
    public static boolean startService(Context ctx, String reporterId, PendingIntent callbackIntent, int delay, int waitconn) {
        Intent intent = new Intent(ctx, MyLogReportService.class);
        intent.putExtra(EXTRA_REPORTER_ID, reporterId);
        intent.putExtra(EXTRA_INTENT, callbackIntent);
        intent.putExtra(EXTRA_DELAY, delay);
        intent.putExtra(EXTRA_WAIT_CONNECT, waitconn);
        mService = ctx.startService(intent);
        return (mService != null);
    }

}
