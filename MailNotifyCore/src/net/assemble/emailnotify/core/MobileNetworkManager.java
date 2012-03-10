package net.assemble.emailnotify.core;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.orleaf.android.MyLog;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.provider.BaseColumns;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MobileNetworkManager {

    public class ApnInfo {
        long ID;
        public String KEY;
        public String TYPE;
        public String APN_NAME;
    }

    private final Context mCtx;
    private TelephonyManager mTelManager;
    private final WifiManager mWifiManager;
    private final ConnectivityManager mConnManager;
    private final ContentResolver mResolver;

    public MobileNetworkManager(Context ctx) {
        mCtx = ctx;
        mTelManager = (TelephonyManager) mCtx.getSystemService(Context.TELEPHONY_SERVICE);
        mWifiManager = (WifiManager) mCtx.getSystemService(Context.WIFI_SERVICE);
        mConnManager = (ConnectivityManager) mCtx.getSystemService(Context.CONNECTIVITY_SERVICE);
        mResolver = mCtx.getContentResolver();
    }

    /**
     * 現在の接続先APNを取得
     */
    private String getPreferApnKey() {
        String apnKey = null;
        ContentResolver resolver = mCtx.getContentResolver();
        Cursor cur = resolver.query(Uri.parse("content://telephony/carriers/preferapn"),
                new String[] { BaseColumns._ID }, null, null, null);
        if (cur.moveToFirst()) {
            apnKey = cur.getString(0);
        }
        cur.close();
        return apnKey;
    }

    /**
     * 現在の接続先APN名を取得
     */
    public String getPreferApnName() {
        String apnName = null;
        ContentResolver resolver = mCtx.getContentResolver();
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

    /**
     * APNリスト取得
     *
     * @return APNリスト (null:取得失敗)
     */
    public List<ApnInfo> getApnList() {
        ArrayList<ApnInfo> apnList = new ArrayList<ApnInfo>();
        String simOp = mTelManager.getSimOperator();
        if (simOp == null || simOp.length() == 0) {
            return null;
        }

        Cursor cursor = mResolver.query(Uri.parse("content://telephony/carriers"),
                new String[] { BaseColumns._ID, "apn", "type", "numeric" },
                "numeric = ?", new String[] { simOp }, BaseColumns._ID);

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
     * データ通信の有効/無効を取得
     *
     * @return true:有効 false:無効
     */
    private boolean getMobileDataEnabled()
            throws ClassNotFoundException, SecurityException, NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final Class<?> conmanClass = Class.forName(mConnManager.getClass().getName());
        final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
        iConnectivityManagerField.setAccessible(true);
        final Object iConnectivityManager = iConnectivityManagerField.get(mConnManager);
        final Class<?> iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
        final Method getMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("getMobileDataEnabled");
        getMobileDataEnabledMethod.setAccessible(true);
        return (Boolean) getMobileDataEnabledMethod.invoke(iConnectivityManager);
    }

    /**
     * データ通信の有効化/無効化
     *
     * @param enable true:有効化 false:無効化
     * @return true:設定を変更した false:何もしなかった
     */
    private boolean setMobileDataEnabled(boolean enable) {
        try {
            if (getMobileDataEnabled() != enable) {
                final Class<?> conmanClass = Class.forName(mConnManager.getClass().getName());
                final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
                iConnectivityManagerField.setAccessible(true);
                final Object iConnectivityManager = iConnectivityManagerField.get(mConnManager);
                final Class<?> iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
                final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
                setMobileDataEnabledMethod.setAccessible(true);
                setMobileDataEnabledMethod.invoke(iConnectivityManager, enable);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * APN有効化
     *
     * android.permission.WRITE_APN_SETTINGS が必要
     *
     * @param target_apn APN
     * @return true:設定を変更した false:何もしなかった
     */
    private boolean activateAPN(String target_apn) {
        boolean changed = false;

        String simOp = mTelManager.getSimOperator();
        if (simOp == null || simOp.length() == 0) {
            MyLog.d(mCtx, EmailNotify.TAG, "simOp not set.");
            return false;
        }
        if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "simOp = " + simOp);
        Cursor cursor = mResolver.query(Uri.parse("content://telephony/carriers"),
                new String[] { BaseColumns._ID, "apn", "type" },
                "numeric = ?", new String[] { simOp }, null);

        ContentValues values;

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
                    try {
                        mResolver.update(url, values, null, null);
                        changed = true;
                        MyLog.d(mCtx, EmailNotify.TAG, "APN enabled: key=" + key + ", apn=" + new_apn + ", type=" + new_type);
                    } catch (SecurityException e) {
                        MyLog.e(mCtx, EmailNotify.TAG, "Unable to update APN settings for security reasons.");
                    }
                } while (cursor.moveToNext());
            }
        }
        cursor.close();

        String prevApnKey = null;
        if (apnKey != null) {
            if (EmailNotify.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "Activating APN...");

            // 現在の接続先を取得
            cursor = mResolver.query(Uri.parse("content://telephony/carriers/preferapn"),
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
                try {
                    mResolver.update(Uri.parse("content://telephony/carriers/preferapn"), values, null, null);
                    changed = true;
                    MyLog.d(mCtx, EmailNotify.TAG, "APN activated: " + prevApnKey + " -> " + apnKey);
                } catch (SecurityException e) {
                    MyLog.e(mCtx, EmailNotify.TAG, "Unable to update APN settings for security reasons.");
                }
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
        return changed;
    }

    /**
     * Wi-Fi有効化/無効化
     *
     * @param enable true:有効化 false:無効化
     * @return true:設定を変更した false:何もしなかった
     */
    private boolean setWifiEnabled(boolean enable) {
        boolean changed = false;

        if (mWifiManager.getWifiState() != WifiManager.WIFI_STATE_DISABLED) {
            if (!enable) {
                // 有効→無効化
                if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Disabling Wi-Fi...");
                mWifiManager.setWifiEnabled(false);
                changed = true;
            } else {
                if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Wi-Fi is already enabled.");
            }
        } else {
            if (enable) {
                // 無効→有効化
                if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Enabling Wi-Fi...");
                mWifiManager.setWifiEnabled(true);
                changed = true;
            } else {
                if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Wi-Fi is already disabled.");
            }
        }

        return changed;
    }

    /**
     * APNを復元する
     */
    private void restoreAPN() {
        String apnKey = EmailNotifyPreferences.getNetworkSaveApnKey(mCtx);
        String modifierStr = EmailNotifyPreferences.getNetworkSaveApnModifierString(mCtx);
        String modifierType = EmailNotifyPreferences.getNetworkSaveApnModifierType(mCtx);
        if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Restoring APN: apnKey=" + apnKey + ", modifier=" + modifierStr +
                ", type=" + modifierType);

        ContentValues values;

        if (modifierStr != null) {
            Cursor cursor = mResolver.query(Uri.parse("content://telephony/carriers"),
                    new String[] { BaseColumns._ID, "apn", "type" },
                    "numeric = ?", new String[] { mTelManager.getSimOperator() }, null);

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
                    try {
                        mResolver.update(url, values, null, null);
                        MyLog.d(mCtx, EmailNotify.TAG, "APN disabled: key=" + key + ", apn=" + new_apn + ", type=" + new_type);
                    } catch (SecurityException e) {
                        MyLog.e(mCtx, EmailNotify.TAG, "Unable to update APN settings for security reasons.");
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        // 接続先APNを復元
        if (apnKey != null) {
            values = new ContentValues();
            values.put("apn_id", apnKey);
            try {
                mResolver.update(Uri.parse("content://telephony/carriers/preferapn"), values, null, null);
                MyLog.d(mCtx, EmailNotify.TAG, "APN restored: -> " + apnKey);
            } catch (SecurityException e) {
                MyLog.e(mCtx, EmailNotify.TAG, "Unable to update APN settings for security reasons.");
            }
        }
    }

    /**
     * 指定したAPNへ接続
     *
     * @param apn APN (null:変更しない)
     * @return true:設定を変更した false:何もしなかった
     */
    public boolean connectApn(String apn) {
        // Wi-Fi無効化
        boolean wifiChanged = setWifiEnabled(false);
        if (wifiChanged) {
            // ネットワーク復元情報(Wi-Fi)を保存
            EmailNotifyPreferences.saveNetworkWifiInfo(mCtx, true);
        }

        // データ通信有効化
        boolean mobileChanged = setMobileDataEnabled(true);
        if (mobileChanged) {
         // ネットワーク復元情報(データ通信)を保存
            EmailNotifyPreferences.saveNetworkMobileDataEnable(mCtx, false);
        }

        // APN設定を有効化
        boolean apnChanged = false;
        if (apn != null && apn.length() > 0) {
            apnChanged = activateAPN(apn);
        }

        boolean changed = wifiChanged || mobileChanged || apnChanged;
        if (changed) {
            // 復元用に変更前のネットワーク情報を保存(有効フラグを立てる)
            EmailNotifyPreferences.saveNetworkInfo(mCtx);
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Saved network information.");
        }

        return changed;
    }

    /**
     * Wi-Fiへ接続
     *
     * @return true:設定を変更した false:何もしなかった
     */
    public boolean connectWifi() {
        // Wi-Fi有効化
        boolean wifiChanged = setWifiEnabled(true);
        if (wifiChanged) {
            EmailNotifyPreferences.saveNetworkWifiInfo(mCtx, false);
        }

        boolean changed = wifiChanged;
        if (changed) {
            // 復元用に変更前のネットワーク情報を保存(有効フラグを立てる)
            EmailNotifyPreferences.saveNetworkInfo(mCtx);
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Saved network information.");
        }

        return changed;
    }

    /**
     * ネットワーク設定を復元する
     */
    public void restoreNetwork() {
        if (EmailNotifyPreferences.hasNetworkSave(mCtx)) {
            if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Restoring network...");

            // APN設定を復元
            restoreAPN();

            // データ通信設定を復元
            if (EmailNotifyPreferences.hasNetworkSaveMobileDataEnable(mCtx)) {
                if (EmailNotifyPreferences.getNetworkSaveMobileDataEnable(mCtx)) {
                    if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Enabling Mobile Data...");
                    setMobileDataEnabled(true);
                    MyLog.d(mCtx, EmailNotify.TAG, "Mobile Data enabled.");
                } else {
                    if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Disabling Mobile Data...");
                    setMobileDataEnabled(false);
                    MyLog.d(mCtx, EmailNotify.TAG, "Mobile Data disabled.");
                }
            }

            // Wi-Fi設定を復元
            if (EmailNotifyPreferences.hasNetworkSaveWifiEnable(mCtx)) {
                if (EmailNotifyPreferences.getNetworkSaveWifiEnable(mCtx)) {
                    if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Enabling Wi-Fi...");
                    mWifiManager.setWifiEnabled(true);
                    MyLog.d(mCtx, EmailNotify.TAG, "Wi-Fi enabled.");
                } else {
                    if (EmailNotify.DEBUG) Log.d(EmailNotify.TAG, "Disabling Wi-Fi...");
                    mWifiManager.setWifiEnabled(false);
                    MyLog.d(mCtx, EmailNotify.TAG, "Wi-Fi disabled.");
                }
            }
        }
    }

}
