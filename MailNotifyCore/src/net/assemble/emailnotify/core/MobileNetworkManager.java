package net.assemble.emailnotify.core;

import java.util.ArrayList;
import java.util.List;

import net.orleaf.android.MyLog;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.provider.BaseColumns;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MobileNetworkManager {

    private TelephonyManager mTelManager;

    public class ApnInfo {
        long ID;
        public String KEY;
        public String TYPE;
        public String APN_NAME;
    }

    private Context mCtx;
    private WifiManager mWifiManager;
    private ContentResolver mResolver;

    public MobileNetworkManager(Context ctx) {
        mCtx = ctx;
        mTelManager = (TelephonyManager) mCtx.getSystemService(Context.TELEPHONY_SERVICE);
        mWifiManager = (WifiManager) mCtx.getSystemService(Context.WIFI_SERVICE);
        mResolver = mCtx.getContentResolver();
    }

    /**
     * APNリスト取得
     */
    public List<ApnInfo> getApnList() {
        ArrayList<ApnInfo> apnList = new ArrayList<ApnInfo>();
        String whereCaluse = null;
        String simOp = mTelManager.getSimOperator();
        if (simOp != null) {
            whereCaluse = "numeric = " + simOp;
        }
        Cursor cursor = mResolver.query(Uri.parse("content://telephony/carriers"),
                new String[] { BaseColumns._ID, "apn", "type", "numeric" },
                whereCaluse, null, BaseColumns._ID);

        if (cursor.moveToFirst()) {
            do {
                if (EmailNotify.DEBUG) {
                    String key = cursor.getString(cursor.getColumnIndex(BaseColumns._ID));
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        Log.d(EmailNotify.TAG, " [" + key + "] " + cursor.getColumnName(i) + " = " + cursor.getString(i));
                    }
                }

                ApnInfo info = new ApnInfo();
                info.KEY = cursor.getString(cursor.getColumnIndex(BaseColumns._ID));
                info.ID = Long.parseLong(info.KEY);
                info.APN_NAME = cursor.getString(cursor.getColumnIndex("apn"));
                info.TYPE = cursor.getString(cursor.getColumnIndex("type"));
                apnList.add(info);
                if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "key=" + info.KEY + ", apn=" + info.APN_NAME + ", type=" + info.TYPE);
            } while (cursor.moveToNext());
        }
        return (List<ApnInfo>) apnList;
    }

    /**
     * APN有効化
     *
     * @return true:設定を変更した false:何もしなかった
     */
    public boolean enableAPN(String target_apn) {
        boolean changed = disableWifi();
        ContentResolver resolver = mCtx.getContentResolver();
        ContentValues values;
        Cursor cursor = resolver.query(Uri.parse("content://telephony/carriers"),
                new String[] { BaseColumns._ID, "apn", "type" },
                "numeric = " + mTelManager.getSimOperator(), null, null);

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
                if (EmailNotify.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "key=" + key + ", apn=" + apn + ", type=" + type);

                if (apn.equals(target_apn)) {
                    if (EmailNotify.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "Found valid APN.");
                    apnKey = key;
                    break;
                }
                if (apn.startsWith(target_apn)) {
                    String ex = apn.substring(target_apn.length());
                    if (type.endsWith(ex)) {
                        apnKey = key;
                        modifierStr = ex;
                        modifierType = EmailNotifyPreferences.PREF_NETWORK_SAVE_APN_MODIFIER_TYPE_SUFFIX;
                        if (EmailNotify.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "Found modifier " + modifierType + ": " + modifierStr);
                    }
                } else if (apn.endsWith(target_apn)) {
                    String ex = apn.substring(0, apn.length() - target_apn.length());
                    if (type.startsWith(ex)) {
                        apnKey = key;
                        modifierStr = ex;
                        modifierType = EmailNotifyPreferences.PREF_NETWORK_SAVE_APN_MODIFIER_TYPE_PREFIX;
                        if (EmailNotify.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "Found modifier " + modifierType + ": " + modifierStr);
                    }
                }
            } while (cursor.moveToNext());
        }

        if (modifierStr != null) {
            if (EmailNotify.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "Enabling APNs...");
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
                        if (EmailNotify.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "Skipping APN: apn=" + apn + ", type=" + type);
                        continue;
                    }

                    // APN設定をもとに戻す
                    values = new ContentValues();
                    values.put("apn", new_apn);
                    values.put("type", new_type);
                    Uri url = ContentUris.withAppendedId(Uri.parse("content://telephony/carriers"), Integer.parseInt(key));
                    resolver.update(url, values, null, null);
                    changed = true;
                    MyLog.d(mCtx, EmailNotify.TAG, "APN enabled: key=" + key + ", apn=" + new_apn + ", type=" + new_type);
                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        String prevApnKey = null;
        if (apnKey != null) {
            if (EmailNotify.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "Activating APN...");

            // 現在の接続先を取得
            cursor = resolver.query(Uri.parse("content://telephony/carriers/preferapn"),
                    new String[] {"_id"}, null, null, null);
            if (cursor.moveToFirst()) {
                prevApnKey = cursor.getString(0);
                if (EmailNotify.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "Current APN ID: " + prevApnKey);
            }
            cursor.close();

            if (prevApnKey == null || !prevApnKey.equals(apnKey)) {
                // 接続先APNを変更
                values = new ContentValues();
                values.put("apn_id", apnKey);
                resolver.update(Uri.parse("content://telephony/carriers/preferapn"), values, null, null);
                changed = true;
                MyLog.d(mCtx, EmailNotify.TAG, "APN activated: " + prevApnKey + " -> " + apnKey);
            }
        }

        if (changed) {
            // ネットワーク復元情報(APN)を保存
            EmailNotifyPreferences.saveNetworkApnInfo(mCtx, prevApnKey, modifierStr, modifierType);
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Saved network: apnKey=" + prevApnKey +
                    ", modifier=" + modifierStr + ", type=" + modifierType);
        } else {
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "APN is already active.");
        }
        if (changed) {
            // 復元用に変更前のネットワーク情報を保存(有効フラグを立てる)
            EmailNotifyPreferences.saveNetworkInfo(mCtx);
        }
        return changed;
    }

    /**
     * Wi-Fi無効化
     *
     * @return true:無効化した false:何もしてない(すでに無効)
     */
    public boolean disableWifi() {
        boolean wifiEnabled = mWifiManager.getWifiState() != WifiManager.WIFI_STATE_DISABLED;

        // Wi-Fiが有効であれば無効化
        if (wifiEnabled) {
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Disabling Wi-Fi...");
            mWifiManager.setWifiEnabled(false);
        } else {
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Wi-Fi is already disabled.");
        }

        // ネットワーク復元情報(Wi-Fi)を保存
        EmailNotifyPreferences.saveNetworkWifiInfo(mCtx, wifiEnabled);

        return wifiEnabled;
    }

    /**
     * ネットワークを復元する
     */
    public void restoreNetwork() {
        if (EmailNotifyPreferences.hasNetworkSave(mCtx)) {
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Restoring network...");

            String apnKey = EmailNotifyPreferences.getNetworkSaveApnKey(mCtx);
            String modifierStr = EmailNotifyPreferences.getNetworkSaveApnModifierString(mCtx);
            String modifierType = EmailNotifyPreferences.getNetworkSaveApnModifierType(mCtx);
            boolean wifiEnable = EmailNotifyPreferences.getNetworkSaveWifiEnable(mCtx);
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Restoring network: apnKey=" + apnKey + ", modifier=" + modifierStr +
                    ", type=" + modifierType + ", wifiEnable=" + wifiEnable);

            ContentValues values;

            if (modifierStr != null) {
                Cursor cursor = mResolver.query(Uri.parse("content://telephony/carriers"),
                        new String[] { BaseColumns._ID, "apn", "type" },
                        "numeric = " + mTelManager.getSimOperator(), null, null);

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
                            if (EmailNotify.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "Skipping APN: apn=" + apn + ", type=" + type);
                            continue;
                        }

                        values = new ContentValues();
                        values.put("apn", new_apn);
                        values.put("type", new_type);
                        Uri url = ContentUris.withAppendedId(Uri.parse("content://telephony/carriers"), Integer.parseInt(key));
                        mResolver.update(url, values, null, null);
                        MyLog.d(mCtx, EmailNotify.TAG, "APN disabled: key=" + key + ", apn=" + new_apn + ", type=" + new_type);
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }

            // 接続先APNを復元
            if (apnKey != null) {
                values = new ContentValues();
                values.put("apn_id", apnKey);
                mResolver.update(Uri.parse("content://telephony/carriers/preferapn"), values, null, null);
                MyLog.d(mCtx, EmailNotify.TAG, "APN restored: -> " + apnKey);
            }

            // Wi-Fi状態を復元
            if (wifiEnable) {
                if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Enabling Wi-Fi...");
                mWifiManager.setWifiEnabled(true);
                MyLog.d(mCtx, EmailNotify.TAG, "Wi-Fi enabled.");
            }
        }
    }

}
