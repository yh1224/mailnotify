package net.assemble.emailnotify.core;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import net.orleaf.android.MyLog;

public class EmailNotifyRestoreActivity extends Activity {
    public static final String ACTION_RESTORE_NETWORK = "net.assemble.emailnotify.action.RESTORE_NETWORK";
    public static final String ACTION_KEEP_NETWORK = "net.assemble.emailnotify.action.KEEP_NETWORK";

    private WifiManager mWifiManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVisible(false);

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        Intent intent = getIntent();
        if (intent.getAction().equals(ACTION_RESTORE_NETWORK)) {
            // ネットワーク復元
            restoreNetwork();
            Toast.makeText(this, R.string.restored_network, Toast.LENGTH_LONG).show();
        }

        // ネットワーク復元情報を消去
        EmailNotifyPreferences.unsetNetworkInfo(this);

        finish();
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

}
