package net.assemble.emailnotify.core;

import java.util.Set;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
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

import net.orleaf.android.MyLog;

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
                    // Wi-Fi無効化
                    boolean wifiChanged = disableWifi();

                    // APN有効化 (現状spモードのみ)
                    boolean apnChanged = enableAPN(SPMODE_APN);

                    if (wifiChanged || apnChanged) {
                        // 復元用に変更前のネットワーク情報を保存(有効フラグを立てる)
                        EmailNotifyPreferences.saveNetworkInfo(EmailNotifyLaunchActivity.this);

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

    /**
     * Wi-Fi無効化
     *
     * @return true:無効化した false:何もしてない(すでに無効)
     */
    private boolean disableWifi() {
        boolean wifiEnabled = mWifiManager.getWifiState() != WifiManager.WIFI_STATE_DISABLED;

        // Wi-Fiが有効であれば無効化
        if (wifiEnabled) {
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Disabling Wi-Fi...");
            mWifiManager.setWifiEnabled(false);
        } else {
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Wi-Fi is already disabled.");
        }

        // ネットワーク復元情報(Wi-Fi)を保存
        EmailNotifyPreferences.saveNetworkWifiInfo(this, wifiEnabled);

        return wifiEnabled;
    }

    /**
     * APN有効化
     *
     * @return true:設定を変更した false:何もしなかった
     */
    private boolean enableAPN(String target_apn) {
        boolean changed = false;
        ContentResolver resolver = getContentResolver();
        ContentValues values;
        Cursor cursor = resolver.query(Uri.parse("content://telephony/carriers"),
                new String[] { BaseColumns._ID, "apn", "type" }, null, null, null);

        // APNのkeyと付加文字列検索 (ex:apndroid)
        if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Checking for enable APN...");
        String apnKey = null;
        String modifierStr = null;
        String modifierType = null;
        if (cursor.moveToFirst()) {
            do {
                String key = cursor.getString(0);
                String apn = cursor.getString(1);
                String type = cursor.getString(2);
                if (EmailNotify.DEBUG) MyLog.d(this, EmailNotify.TAG, "key=" + key + ", apn=" + apn + ", type=" + type);

                if (apn.equals(target_apn)) {
                    if (EmailNotify.DEBUG) MyLog.d(this, EmailNotify.TAG, "Found valid APN.");
                    apnKey = key;
                    break;
                }
                if (apn.startsWith(target_apn)) {
                    String ex = apn.substring(target_apn.length());
                    if (type.endsWith(ex)) {
                        apnKey = key;
                        modifierStr = ex;
                        modifierType = EmailNotifyPreferences.PREF_NETWORK_SAVE_APN_MODIFIER_TYPE_SUFFIX;
                        if (EmailNotify.DEBUG) MyLog.d(this, EmailNotify.TAG, "Found modifier " + modifierType + ": " + modifierStr);
                    }
                } else if (apn.endsWith(target_apn)) {
                    String ex = apn.substring(0, apn.length() - target_apn.length());
                    if (type.startsWith(ex)) {
                        apnKey = key;
                        modifierStr = ex;
                        modifierType = EmailNotifyPreferences.PREF_NETWORK_SAVE_APN_MODIFIER_TYPE_PREFIX;
                        if (EmailNotify.DEBUG) MyLog.d(this, EmailNotify.TAG, "Found modifier " + modifierType + ": " + modifierStr);
                    }
                }
            } while (cursor.moveToNext());
        }

        if (modifierStr != null) {
            if (EmailNotify.DEBUG) MyLog.d(this, EmailNotify.TAG, "Enabling APNs...");
            // Activate APNs
            if (cursor.moveToFirst()) {
                do {
                    String key = cursor.getString(0);
                    String apn = cursor.getString(1);
                    String type = cursor.getString(2);

                    String new_apn;
                    String new_type;
                    if (apn.startsWith(modifierStr) && type.startsWith(modifierStr)) {
                        new_apn = apn.substring(modifierStr.length());
                        new_type = type.substring(modifierStr.length());
                    } else if (apn.endsWith(modifierStr) && type.endsWith(modifierStr)) {
                        new_apn = apn.substring(0, apn.length() - modifierStr.length());
                        new_type = type.substring(0, type.length() - modifierStr.length());
                    } else {
                        if (EmailNotify.DEBUG) MyLog.d(this, EmailNotify.TAG, "Skipping APN: apn=" + apn + ", type=" + type);
                        continue;
                    }

                    // APN設定をもとに戻す
                    values = new ContentValues();
                    values.put("apn", new_apn);
                    values.put("type", new_type);
                    Uri url = ContentUris.withAppendedId(Uri.parse("content://telephony/carriers"), Integer.parseInt(key));
                    resolver.update(url, values, null, null);
                    changed = true;
                    MyLog.d(this, EmailNotify.TAG, "APN enabled: key=" + key + ", apn=" + new_apn + ", type=" + new_type);
                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        String prevApnKey = null;
        if (apnKey != null) {
            if (EmailNotify.DEBUG) MyLog.d(this, EmailNotify.TAG, "Activating APN...");

            // 現在の接続先を取得
            cursor = resolver.query(Uri.parse("content://telephony/carriers/preferapn"),
                    new String[] {"_id"}, null, null, null);
            if (cursor.moveToFirst()) {
                prevApnKey = cursor.getString(0);
                if (EmailNotify.DEBUG) MyLog.d(this, EmailNotify.TAG, "Current APN ID: " + prevApnKey);
            }
            cursor.close();

            if (prevApnKey == null || !prevApnKey.equals(apnKey)) {
                // 接続先APNを変更
                values = new ContentValues();
                values.put("apn_id", apnKey);
                resolver.update(Uri.parse("content://telephony/carriers/preferapn"), values, null, null);
                changed = true;
                MyLog.d(this, EmailNotify.TAG, "APN activated: " + prevApnKey + " -> " + apnKey);
            }
        }

        if (changed) {
            // ネットワーク復元情報(APN)を保存
            EmailNotifyPreferences.saveNetworkApnInfo(this, prevApnKey, modifierStr, modifierType);
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Saved network: apnKey=" + prevApnKey +
                    ", modifier=" + modifierStr + ", type=" + modifierType);
        } else {
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "APN is already active.");
        }
        return changed;
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
