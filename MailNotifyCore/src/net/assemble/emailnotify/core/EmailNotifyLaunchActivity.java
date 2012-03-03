package net.assemble.emailnotify.core;

import java.util.Set;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class EmailNotifyLaunchActivity extends Activity {
    private static final String SPMODE_APN = "spmode.ne.jp";

    private ConnectivityManager mConnManager;
    private WifiManager mWifiManager;
    private TelephonyManager mTelManager;
    private BroadcastReceiver mConnectivityReceiver = null;
    private PhoneStateListener mStateListener = null;

    private TextView mMessageText;
    private ProgressBar mProgressBar;
    private Button mLaunchButton;
    private Button mCancelButton;

    private String mService;
    private String mMailbox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);

        mConnManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mTelManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        Intent intent = getIntent();
        mService = intent.getStringExtra("service");
        mMailbox = intent.getStringExtra("mailbox");
        if (mService == null || mMailbox == null) {
            finish();
            return;
        }

        // 再生停止
        EmailNotificationManager.stopAllNotifications(EmailNotification.NOTIFY_ALL);

        setContentView(R.layout.launch);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
                R.drawable.icon);
        mMessageText = (TextView) findViewById(R.id.message);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);

        // アプリ起動ボタン
        mLaunchButton = (Button) findViewById(R.id.launch);
        mLaunchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EmailNotificationManager.clearNotification(mMailbox);
                launchApp();
                finish();
            }
        });

        // キャンセルボタン
        mCancelButton = (Button) findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EmailNotificationManager.clearNotification(mMailbox);
                finish();
            }
        });

        // spモード自動接続設定
        if (EmailNotifyPreferences.getNotifyAutoConnect(this, mService) &&
                !EmailNotifyPreferences.hasNetworkSave(this)) {
            new AsyncTask<Object, Object, Boolean>() {
                @Override
                protected Boolean doInBackground(Object... params) {
                    // APN有効化 (現状spモードのみ)
                    boolean changed = new MobileNetworkManager(EmailNotifyLaunchActivity.this).connectAPN(SPMODE_APN);
                    if (changed) {
                        // 復元用の通知を表示
                        EmailNotificationManager.showRestoreNetworkIcon(EmailNotifyLaunchActivity.this);
                        return true;
                    } else {
                        if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "No need to modify network configuration.");
                        return false;
                    }
                }

                protected void onPostExecute(Boolean result) {
                    if (result) {
                        Toast.makeText(EmailNotifyLaunchActivity.this,
                                R.string.autoconnected_network, Toast.LENGTH_LONG).show();
                    }
                }
            }.execute();
        }

        // 3G状態リスナ登録
        mStateListener = new PhoneStateListener() {
            @Override
            public void onDataConnectionStateChanged(int state) {
                super.onDataConnectionStateChanged(state);
                if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "DataConnectionState changed: " + state);
                checkAndLaunch();
            }
        };
        mTelManager.listen(mStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);

        // ネットワーク接続監視
        mConnectivityReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "received intent: " + intent.getAction());
                logIntent(intent);
                checkAndLaunch();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectivityReceiver, filter);

        checkAndLaunch();
    }

    @Override
    protected void onDestroy() {
        // 3G状態リスナ登録解除
        if (mStateListener != null) {
            mTelManager.listen(mStateListener, PhoneStateListener.LISTEN_NONE);
            mStateListener = null;
        }

        // ネットワーク接続監視解除
        if (mConnectivityReceiver != null) {
            unregisterReceiver(mConnectivityReceiver);
            mConnectivityReceiver = null;
        }

        super.onDestroy();
    }

    /**
     * ネットワークに接続済みかどうか。
     *
     */
    private boolean isNetworkAvailable() {
        // spモード自動接続設定時は、spモードAPNのみを対象
        if (EmailNotifyPreferences.getNotifyAutoConnect(this, mService)) {
            NetworkInfo dataNetworkInfo = mConnManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if (dataNetworkInfo.isConnected()) {
                String apnname = getPreferApnName();
                if (apnname != null && apnname.equals(SPMODE_APN)) {
                    if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "connected to spmode");
                    return true;
                } else {
                    if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "connected but not spmode");
                    return false;
                }
            }
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "not connected to spmode");
            return false;
        }

        NetworkInfo networkInfo = mConnManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "connected to " + networkInfo.getType());
            return true;
        }

        if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "not connected to any network");
        return false;
    }

    /**
     * ネットワーク接続完了時にアプリ起動して終了
     */
    private boolean checkAndLaunch() {
        if (isNetworkAvailable()) {
            // ネットワーク接続完了時はアプリを起動して終了
            EmailNotificationManager.clearNotification(mMailbox);
            launchApp();
            finish();
            return true;
        }

        if (mWifiManager.getWifiState() != WifiManager.WIFI_STATE_DISABLED ||
                mTelManager.getDataState() != TelephonyManager.DATA_DISCONNECTED) {
            // TODO: 3G無効かどうかはAPNまで見て判断する?
            // 接続処理中
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "connecting to network");
            mProgressBar.setVisibility(View.VISIBLE);
            mMessageText.setText(R.string.network_connecting);
        } else {
            // ネットワーク無効
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "network disabled");
            mProgressBar.setVisibility(View.INVISIBLE);
            mMessageText.setText(getResources().getString(R.string.network_disabled));
        }
        return false;
    }

    // アプリ起動
    private void launchApp() {
        Intent intent = new Intent();
        ComponentName component = EmailNotifyPreferences.getNotifyLaunchAppComponent(this, mService);
        if (component != null) {
            intent.setComponent(component);
        } else {
            intent.setClassName(getPackageName(), getPackageName() + ".EmailNotifyActivity");
        }
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.application_not_found, Toast.LENGTH_LONG).show();
            Log.d(EmailNotify.TAG, e.getMessage());
        }
    }

    /**
     * 現在の接続先を取得
     */
    private String getPreferApnKey() {
        String apnKey = null;
        ContentResolver resolver = getContentResolver();
        Cursor cur = resolver.query(Uri.parse("content://telephony/carriers/preferapn"),
                new String[] { BaseColumns._ID }, null, null, null);
        if (cur.moveToFirst()) {
            apnKey = cur.getString(0);
        }
        cur.close();
        return apnKey;
    }

    /**
     * 現在の接続先名を取得
     */
    private String getPreferApnName() {
        String apnName = null;
        ContentResolver resolver = getContentResolver();
        String apnKey = getPreferApnKey();
        if (apnKey != null) {
            Cursor cur = resolver.query(Uri.parse("content://telephony/carriers"),
                    new String[] { "apn" }, BaseColumns._ID + " = " + apnKey, null, null);
            if (cur.moveToFirst()) {
                apnName = cur.getString(0);
            }
            cur.close();
        }
        if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "preferapn key=" + apnKey + " name=" + apnName);
        return apnName;
    }

    private static void logIntent(Intent intent) {
        Log.d(EmailNotify.TAG, "received intent: " + intent.getAction());

        Bundle extras = intent.getExtras();
        if (extras != null) {
            Set<String> keySet = extras.keySet();
            if (keySet != null) {
                Object[] keys = keySet.toArray();
                for (int i = 0; i < keys.length; i++) {
                    Object o = extras.get((String)keys[i]);
                    Log.d(EmailNotify.TAG, "  " + (String)keys[i] + " = (" + o.getClass().getName() + ") " + o.toString());
                }
            }
        }
    }

}
