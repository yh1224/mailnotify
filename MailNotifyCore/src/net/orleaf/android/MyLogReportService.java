package net.orleaf.android;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.assemble.emailnotify.core.EmailNotify;

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
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.widget.Toast;

public class MyLogReportService extends Service {
    public static final String EXTRA_PROGRESS = "progress";
    public static final String EXTRA_REPORTER_ID = "reporter_id";
    public static final String EXTRA_INTENT = "intent";
    public static final String EXTRA_DELAY = "delay";

    private static final String REPORT_URL = "https://orleaf.net/mailnotify/index.php/report/submit";

    private static ComponentName mService;

    private PendingIntent mCallbackIntent;
    private String mReporterId;
    private boolean mProgress;
    private int mDelay;

    private Handler handler = new Handler();
    private BroadcastReceiver mConnectivityReceiver;
    private boolean mStarted = false;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        mCallbackIntent = (PendingIntent) intent.getParcelableExtra(EXTRA_INTENT);
        mProgress = intent.getBooleanExtra(EXTRA_PROGRESS, false);
        if (!mProgress) {
            mDelay = intent.getIntExtra(EXTRA_DELAY, 0);
        } else {
            mDelay = 0;
        }
        mReporterId = intent.getStringExtra(EXTRA_REPORTER_ID);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                start();
            }
        }, mDelay * 1000);
    }

    /**
     * ログ送信
     */
    private void start() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            if (mProgress) {
                Toast.makeText(MyLogReportService.this, "Pending log send...", Toast.LENGTH_SHORT).show();
            }
            registerConnectivityReceiver();
            return;
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

        /**
         * 前処理
         */
        @Override
        protected void onPreExecute() {
            if (mProgress) {
                Toast.makeText(MyLogReportService.this, "Now sending log...", Toast.LENGTH_SHORT).show();
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
            } catch (NameNotFoundException e) {}
            posts.put("reporter_id", mReporterId);
            posts.put("android_id", Secure.getString(getContentResolver(), Secure.ANDROID_ID));
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
         * @param url 送信先URL
         * @param params POSTパラメタ
         * @return エラー文字列 (null:成功)
         */
        private String postTo(String urlStr, HashMap<String, String> params) {
            StringBuffer resultBuf = new StringBuffer();
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

                urlconn.disconnect();
                return null;
            } catch (Exception e) {
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
            StringBuffer postBuf = new StringBuffer();
            for (Iterator<?> i = params.entrySet().iterator(); i.hasNext();) {
                Map.Entry entry = (Map.Entry) i.next();
                if (postBuf.length() > 0) {
                    postBuf.append("&");
                }
                String val = (String) entry.getValue();
                if (val != null) {
                    val = URLEncoder.encode(val, "UTF-8");
                }
                postBuf.append((String) entry.getKey() + "=" + val);
            }
            return postBuf.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                if (mCallbackIntent != null) {
                    try {
                        mCallbackIntent.send();
                    } catch (CanceledException e) {
                        e.printStackTrace();
                    }
                }
                MyLog.d(MyLogReportService.this, EmailNotify.TAG, "Sent log successfully.");
                if (mProgress) {
                    Toast.makeText(MyLogReportService.this, "Sent log successfully.", Toast.LENGTH_SHORT).show();
                }
            } else {
                MyLog.e(MyLogReportService.this, EmailNotify.TAG, "Send log failed: " + result);
                if (mProgress) {
                    Toast.makeText(MyLogReportService.this, "Send log failed: " + result, Toast.LENGTH_LONG).show();
                }
            }
            stopSelf();
        }
    }

    private String getPreferencesString() {
        StringBuffer strBuf = new StringBuffer();
        Map<String, ?> prefs = PreferenceManager.getDefaultSharedPreferences(this).getAll();
        for (Iterator<?> it = prefs.entrySet().iterator(); it.hasNext(); ) {
            @SuppressWarnings("unchecked")
            Map.Entry<String, ?> entry = (Entry<String, ?>) it.next();
            if (entry.getValue() == null) {
                strBuf.append(entry.getKey() + "=(null)\n");
            } else {
                strBuf.append(entry.getKey() + "=" + entry.getValue().toString() + "\n");
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

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }


    /**
     * サービス開始 (送信中を表示)
     *
     * @param ctx
     * @param reporterId
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
     * @param ctx
     * @param reporterId
     * @param callbackIntent 送信完了時に通知するインテント
     * @return サービス起動成否
     */
    public static boolean startService(Context ctx, String reporterId, PendingIntent callbackIntent, int delay) {
        Intent intent = new Intent(ctx, MyLogReportService.class);
        intent.putExtra(EXTRA_REPORTER_ID, reporterId);
        intent.putExtra(EXTRA_INTENT, callbackIntent);
        intent.putExtra(EXTRA_DELAY, delay);
        mService = ctx.startService(intent);
        return (mService != null);
    }

}
