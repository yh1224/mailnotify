package net.assemble.mailnotify;

import java.util.Set;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class EmailNotifyLaunchActivity extends Activity {
    private static final String TAG = "EmailNotify";
    private static final String SPMODE_APN = "spmode.ne.jp";

    private ConnectivityManager mConnManager;
    private WifiManager mWifiManager;
    private TelephonyManager mTelManager;
    private ProgressDialog mProgressDialog = null;
    private BroadcastReceiver mReceiver = null;
    private PhoneStateListener mStateListener = null;

    private String mService;
    private String mMailbox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVisible(false);

        mConnManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mTelManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        Intent intent = getIntent();
        mService = intent.getStringExtra("service");
        mMailbox = intent.getStringExtra("mailbox");

        // 通知停止
        EmailNotificationManager.clearNotification(mMailbox);

        // 自動接続設定
        if (EmailNotifyPreferences.getNotifyAutoConnect(this, mService)) {
            NetworkInfo mobileNetworkInfo = mConnManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if (!mobileNetworkInfo.isConnected()) {
                if (mWifiManager.getWifiState() != WifiManager.WIFI_STATE_DISABLED) {
                    // Wi-Fiが有効であれば無効化
                    showProgress(getResources().getText(R.string.disabling_wifi));
                    disableWifi();
                } else {
                    showProgress(getResources().getText(R.string.enabling_3g));
                }
                // APNを有効化する (現状spモードのみ)
                enableAPN(SPMODE_APN);

                // 3G状態リスナ登録
                mStateListener = new PhoneStateListener() {
                    @Override
                    public void onDataConnectionStateChanged(int state) {
                        super.onDataConnectionStateChanged(state);
                        if (EmailNotify.DEBUG) Log.d(TAG, "DataConnectionState changed: " + state);
                        if (state == TelephonyManager.DATA_CONNECTED) {
                            if (EmailNotify.DEBUG) Log.d(TAG, "Mobile network connected.");
                            launchApp();
                            finish();
                        }
                    }
                };
                mTelManager.listen(mStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
                return;
            }
        }

        launchApp();
        finish();
    }

    @Override
    protected void onDestroy() {
        // 3G状態リスナ登録解除
        if (mStateListener != null) {
            mTelManager.listen(mStateListener, PhoneStateListener.LISTEN_NONE);
            mStateListener = null;
        }

        // Wi-Fiレシーバ登録解除
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }

        // 進捗ダイアログ消去
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.cancel();
            mProgressDialog = null;
        }
        super.onDestroy();
    }

    /**
     * Wi-Fi無効化
     */
    private void disableWifi() {
        if (EmailNotify.DEBUG) Log.d(TAG, "Disabling Wi-Fi...");
        // Wi-Fiレシーバ登録
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                if (EmailNotify.DEBUG) Log.d(TAG, "received intent: " + intent.getAction());
                if (EmailNotify.DEBUG) logIntent(intent);
                if (intent.getAction().equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                    int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
                    if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                        if (EmailNotify.DEBUG) Log.d(TAG, "Wi-Fi disabled.");
                        unregisterReceiver(mReceiver);
                        mReceiver = null;
                        showProgress(getResources().getText(R.string.enabling_3g));
                    }
                } else if (intent.getAction().equals("android.net.wifi.STATE_CHANGE")) {
                    NetworkInfo networkInfo = (NetworkInfo) intent.getExtras().get(WifiManager.EXTRA_NETWORK_INFO);
                    if (!networkInfo.isConnected()) {
                        if (EmailNotify.DEBUG) Log.d(TAG, "Wi-Fi disconnected.");
                        // ??
                    }
                }
            }
        };
        registerReceiver(mReceiver, new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED"));
        registerReceiver(mReceiver, new IntentFilter("android.net.wifi.STATE_CHANGE"));
        mWifiManager.setWifiEnabled(false);
    }

    /**
     * TODO: APN有効化
     */
    private void enableAPN(String target_apn) {
        if (EmailNotify.DEBUG) Log.d(TAG, "Enabling spmode...");
        ContentResolver resolver = getContentResolver();
        ContentValues values;
        Cursor cursor = resolver.query(Uri.parse("content://telephony/carriers"),
                new String[] {"_id", "apn", "type"}, null, null, null);

        // APNのkeyと付加文字列検索 (ex:apndroid)
        String apnKey = null;
        String extraStr = null;
        if (cursor.moveToFirst()) {
            do {
                String key = cursor.getString(0);
                String apn = cursor.getString(1);
                String type = cursor.getString(2);
                if (EmailNotify.DEBUG) Log.d(TAG, "key=" + key + ", apn=" + apn + ", type=" + type);

                if (apn.equals(target_apn)) {
                    if (EmailNotify.DEBUG) Log.d(TAG, "Found valid APN.");
                    apnKey = key;
                    break;
                }
                if (apn.startsWith(target_apn)) {
                    String ex = apn.substring(target_apn.length());
                    if (type.endsWith(ex)) {
                        apnKey = key;
                        extraStr = ex;
                        if (EmailNotify.DEBUG) Log.d(TAG, "Found extra suffix: " + extraStr);
                    }
                } else if (apn.endsWith(target_apn)) {
                    String ex = apn.substring(0, apn.length() - target_apn.length());
                    if (type.startsWith(ex)) {
                        apnKey = key;
                        extraStr = ex;
                        if (EmailNotify.DEBUG) Log.d(TAG, "Found extra prefix: " + extraStr);
                    }
                }
            } while (cursor.moveToNext());
        }

        if (extraStr != null) {
            // Activate APNs
            if (cursor.moveToFirst()) {
                do {
                    String key = cursor.getString(0);
                    String apn = cursor.getString(1);
                    String type = cursor.getString(2);

                    String new_apn;
                    String new_type;
                    if (apn.startsWith(extraStr) && type.startsWith(extraStr)) {
                        new_apn = apn.substring(extraStr.length());
                        new_type = type.substring(extraStr.length());
                    } else if (apn.endsWith(extraStr) && type.endsWith(extraStr)) {
                        new_apn = apn.substring(0, apn.length() - extraStr.length());
                        new_type = type.substring(0, type.length() - extraStr.length());
                    } else {
                        if (EmailNotify.DEBUG) Log.d(TAG, "Skipping APN: apn=" + apn + ", type=" + type);
                        continue;
                    }

                    // APN設定をもとに戻す
                    values = new ContentValues();
                    values.put("apn", new_apn);
                    values.put("type",  new_type);
                    Uri url = ContentUris.withAppendedId(Uri.parse("content://telephony/carriers"), Integer.parseInt(key));
                    resolver.update(url, values, null, null);
                    if (EmailNotify.DEBUG) Log.d(TAG, "Updated APN setting: apn=" + new_apn + ", type=" + new_type);
                } while (cursor.moveToNext());
            }
        }
        if (apnKey != null) {
            // 接続先APNを設定
            values = new ContentValues();
            values.put("apn_id", apnKey);
            resolver.update(Uri.parse("content://telephony/carriers/preferapn"), values, null, null);
            if (EmailNotify.DEBUG) Log.d(TAG, "Set default APN key: " + apnKey);
        }

        cursor.close();
    }

    /**
     * 進捗ダイアログ表示
     */
    private void showProgress(CharSequence message) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            // すでに表示中の場合はメッセージを更新する。
            mProgressDialog.setMessage(message);
            return;
        }
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(message);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                getResources().getString(R.string.launch_app),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        launchApp();
                        finish();
                    }
                }
            );
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                getResources().getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }
            );
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        mProgressDialog.show();
    }

    // アプリ起動
    private void launchApp() {
        Intent intent = new Intent();
        ComponentName component = EmailNotifyPreferences.getNotifyLaunchAppComponent(this, mService);
        if (component != null) {
            intent.setComponent(component);
        } else {
            intent.setClass(this, EmailNotifyActivity.class);
        }
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private static void logIntent(Intent intent) {
        Log.d(TAG, "received intent: " + intent.getAction());

        Bundle extras = intent.getExtras();
        if (extras != null) {
            Set<String> keySet = extras.keySet();
            if (keySet != null) {
                Object[] keys = keySet.toArray();
                for (int i = 0; i < keys.length; i++) {
                    String val;
                    Object o = extras.get((String)keys[i]);
                    if (o instanceof Integer) {
                        val = "Integer:" + ((Integer)o).toString();
                    } else if (o instanceof Boolean) {
                        val = "Boolean:" + ((Boolean)o).toString();
                    } else if (o instanceof SupplicantState) {
                        val = "SupplicantState:" + ((SupplicantState)o).toString();
                    } else if (o instanceof NetworkInfo) {
                        val = "NetworkInfo:" + ((NetworkInfo)o).toString();
                    } else if (o instanceof String) {
                        val = "String:" + (String)o;
                    } else {
                        val = o.getClass().getName() + ":?";
                    }
                    Log.d(TAG, "  " + (String)keys[i] + " = " + val);
                }
            }
        }
    }

}
