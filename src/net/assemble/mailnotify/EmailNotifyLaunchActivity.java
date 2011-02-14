package net.assemble.mailnotify;

import java.util.Set;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
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
import android.widget.Toast;

import net.orleaf.android.MyLog;

public class EmailNotifyLaunchActivity extends Activity {
    private static final String SPMODE_APN = "spmode.ne.jp";

    public static final String ACTION_RESTORE_NETWORK = "net.assemble.emailnotify.action.RESTORE_NETWORK";
    public static final String ACTION_KEEP_NETWORK = "net.assemble.emailnotify.action.KEEP_NETWORK";

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
        if (intent.getAction() != null) {
            // ネットワーク復元
            if (intent.getAction().equals(ACTION_RESTORE_NETWORK)) {
                restoreNetwork();
                Toast.makeText(this, R.string.restored_network, Toast.LENGTH_LONG).show();
            }

            // ネットワーク復元情報を消去
            EmailNotifyPreferences.unsetNetworkInfo(this);
            finish();
            return;
        }

        mService = intent.getStringExtra("service");
        mMailbox = intent.getStringExtra("mailbox");
        if (mService == null || mMailbox == null) {
            finish();
            return;
        }

        // 通知停止
        EmailNotificationManager.clearNotification(mMailbox);

        // 自動接続設定
        if (EmailNotifyPreferences.getNotifyAutoConnect(this, mService) &&
                !EmailNotifyPreferences.hasNetworkSave(this)) {
            // Wi-Fi無効化
            boolean wifiChanged = disableWifi();

            // APN有効化 (現状spモードのみ)
            boolean apnChanged = enableAPN(SPMODE_APN);

            if (wifiChanged || apnChanged) {
                // プログレスダイアログ表示
                if (wifiChanged) {
                    showProgress(getResources().getText(R.string.disabling_wifi));
                } else {
                    showProgress(getResources().getText(R.string.enabling_3g));
                }

                // 復元用に変更前のネットワーク情報を保存(有効フラグを立てる)
                EmailNotifyPreferences.saveNetworkInfo(this);

                // 復元用の通知を表示
                EmailNotificationManager.showRestoreNetworkIcon(this);

                // 3G状態リスナ登録
                mStateListener = new PhoneStateListener() {
                    @Override
                    public void onDataConnectionStateChanged(int state) {
                        super.onDataConnectionStateChanged(state);
                        if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "DataConnectionState changed: " + state);
                        if (state == TelephonyManager.DATA_CONNECTED) {
                            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Mobile network connected.");
                            Toast.makeText(EmailNotifyLaunchActivity.this,
                                    R.string.connected_network, Toast.LENGTH_LONG).show();
                            launchApp();
                            finish();
                        }
                    }
                };
                mTelManager.listen(mStateListener, PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
                return;
            } else {
                if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "No need to modify network configuration.");
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
     *
     * @return true:無効化した false:何もしてない(すでに無効)
     */
    private boolean disableWifi() {
        boolean wifiEnabled = mWifiManager.getWifiState() != WifiManager.WIFI_STATE_DISABLED;

        // Wi-Fiが有効であれば無効化
        if (wifiEnabled) {
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Disabling Wi-Fi...");

            // Wi-Fiレシーバ登録
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "received intent: " + intent.getAction());
                    if (EmailNotify.DEBUG) logIntent(intent);
                    if (intent.getAction().equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                        int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
                        if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Wi-Fi disabled.");
                            if (mReceiver != null) {
                                unregisterReceiver(mReceiver);
                                mReceiver = null;
                                showProgress(getResources().getText(R.string.enabling_3g));
                            }
                        }
                    }
                }
            };
            registerReceiver(mReceiver, new IntentFilter("android.net.wifi.WIFI_STATE_CHANGED"));
            mWifiManager.setWifiEnabled(false);
            MyLog.d(this, EmailNotify.TAG, "Wi-Fi disabled.");
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
                new String[] {"_id", "apn", "type"}, null, null, null);

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
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.application_not_found, Toast.LENGTH_LONG).show();
            Log.d(EmailNotify.TAG, e.getMessage());
        }
    }

    /**
     * ネットワークを復元する
     */
    private void restoreNetwork() {
        if (EmailNotifyPreferences.hasNetworkSave(this)) {
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Restoring network...");

            String apnKey = EmailNotifyPreferences.getNetworkSaveApnKey(this);
            String modifierStr = EmailNotifyPreferences.getNetworkSaveApnModifierString(this);
            String modifierType = EmailNotifyPreferences.getNetworkSaveApnModifierType(this);
            boolean wifiEnable = EmailNotifyPreferences.getNetworkSaveWifiEnable(this);
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Restoring network: apnKey=" + apnKey + ", modifier=" + modifierStr +
                    ", type=" + modifierType + ", wifiEnable=" + wifiEnable);

            ContentResolver resolver = getContentResolver();
            ContentValues values;

            if (modifierStr != null) {
                Cursor cursor = resolver.query(Uri.parse("content://telephony/carriers"),
                        new String[] {"_id", "apn", "type"}, null, null, null);

                // APN設定を復元
                if (cursor.moveToFirst()) {
                    do {
                        String key = cursor.getString(0);
                        String apn = cursor.getString(1);
                        String type = cursor.getString(2);

                        String new_apn;
                        String new_type;
                        if (modifierType.equals(EmailNotifyPreferences.PREF_NETWORK_SAVE_APN_MODIFIER_TYPE_PREFIX) &&
                                !(apn.startsWith(modifierStr) || type.startsWith(modifierStr))) {
                            new_apn = modifierStr + apn;
                            new_type = modifierStr + type;
                        } else if (modifierType.equals(EmailNotifyPreferences.PREF_NETWORK_SAVE_APN_MODIFIER_TYPE_SUFFIX) &&
                                !(apn.endsWith(modifierStr) || type.endsWith(modifierStr))) {
                            new_apn = apn + modifierStr;
                            new_type = type + modifierStr;
                        } else {
                            // すでに無効化されているので何もしない
                            if (EmailNotify.DEBUG) MyLog.d(this, EmailNotify.TAG, "Skipping APN: apn=" + apn + ", type=" + type);
                            continue;
                        }

                        values = new ContentValues();
                        values.put("apn", new_apn);
                        values.put("type", new_type);
                        Uri url = ContentUris.withAppendedId(Uri.parse("content://telephony/carriers"), Integer.parseInt(key));
                        resolver.update(url, values, null, null);
                        MyLog.d(this, EmailNotify.TAG, "APN disabled: key=" + key + ", apn=" + new_apn + ", type=" + new_type);
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }

            // 接続先APNを復元
            if (apnKey != null) {
                values = new ContentValues();
                values.put("apn_id", apnKey);
                resolver.update(Uri.parse("content://telephony/carriers/preferapn"), values, null, null);
                MyLog.d(this, EmailNotify.TAG, "APN restored: -> " + apnKey);
            }

            // Wi-Fi状態を復元
            if (wifiEnable) {
                if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Enabling Wi-Fi...");
                mWifiManager.setWifiEnabled(true);
                MyLog.d(this, EmailNotify.TAG, "Wi-Fi enabled.");
            }
        }
    }

    private static void logIntent(Intent intent) {
        Log.d(EmailNotify.TAG, "received intent: " + intent.getAction());

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
                    Log.d(EmailNotify.TAG, "  " + (String)keys[i] + " = " + val);
                }
            }
        }
    }

}
