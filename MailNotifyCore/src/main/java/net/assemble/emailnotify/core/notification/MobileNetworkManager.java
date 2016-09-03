package net.assemble.emailnotify.core.notification;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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

import net.assemble.emailnotify.core.BuildConfig;
import net.assemble.emailnotify.core.EmailNotify;
import net.assemble.emailnotify.core.preferences.EmailNotifyPreferences;
import net.orleaf.android.MyLog;

/**
 * モバイルネットワーク管理
 */
public class MobileNetworkManager {

    public class ApnInfo {
        long ID;
        public String KEY;
        public String TYPE;
        public String APN_NAME;
    }

    private final Context mCtx;

    public MobileNetworkManager(Context ctx) {
        mCtx = ctx;
    }

    /**
     * 現在の接続先APNを取得
     */
    private String getPreferApnKey() {
        String apnKey = null;
        ContentResolver resolver = mCtx.getContentResolver();
        Cursor cur = resolver.query(Uri.parse("content://telephony/carriers/preferapn"),
                new String[] { BaseColumns._ID }, null, null, null);
        assert cur != null;
        if (cur.moveToFirst()) {
            apnKey = cur.getString(0);
        }
        cur.close();
        return apnKey;
    }

    /**
     * 現在の接続先APN名を取得
     */
    @SuppressWarnings("unused")
    public String getPreferApnName() {
        String apnName = null;
        ContentResolver resolver = mCtx.getContentResolver();
        String apnKey = getPreferApnKey();
        if (apnKey != null) {
            Cursor cur = resolver.query(Uri.parse("content://telephony/carriers"),
                    new String[] { "apn" }, BaseColumns._ID + " = " + apnKey, null, null);
            assert cur != null;
            if (cur.moveToFirst()) {
                apnName = cur.getString(0);
            }
            cur.close();
        }
        if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "preferapn key=" + apnKey + " name=" + apnName);
        return apnName;
    }

    /**
     * APNリスト取得
     *
     * @return APNリスト (null:取得失敗)
     */
    public List<ApnInfo> getApnList() {
        TelephonyManager tm = (TelephonyManager) mCtx.getSystemService(Context.TELEPHONY_SERVICE);

        ArrayList<ApnInfo> apnList = new ArrayList<ApnInfo>();
        String simOp = tm.getSimOperator();
        if (simOp == null || simOp.length() == 0) {
            return null;
        }

        ContentResolver resolver = mCtx.getContentResolver();
        Cursor cursor = resolver.query(Uri.parse("content://telephony/carriers"),
                new String[] { BaseColumns._ID, "apn", "type", "numeric" },
                "numeric = ?", new String[] { simOp }, BaseColumns._ID);

        assert cursor != null;
        if (cursor.moveToFirst()) {
            do {
                if (BuildConfig.DEBUG) {
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
                if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "key=" + info.KEY + ", apn=" + info.APN_NAME + ", type=" + info.TYPE);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return apnList;
    }

    /**
     * データ通信の有効/無効を取得
     *
     * @return true:有効 false:無効
     */
    private boolean getMobileDataEnabled()
            throws ClassNotFoundException, SecurityException, NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        ConnectivityManager cm = (ConnectivityManager) mCtx.getSystemService(Context.CONNECTIVITY_SERVICE);

        final Class<?> conmanClass = Class.forName(cm.getClass().getName());
        final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
        iConnectivityManagerField.setAccessible(true);
        final Object iConnectivityManager = iConnectivityManagerField.get(cm);
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
        ConnectivityManager cm = (ConnectivityManager) mCtx.getSystemService(Context.CONNECTIVITY_SERVICE);

        try {
            if (getMobileDataEnabled() != enable) {
                final Class<?> conmanClass = Class.forName(cm.getClass().getName());
                final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
                iConnectivityManagerField.setAccessible(true);
                final Object iConnectivityManager = iConnectivityManagerField.get(cm);
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
        TelephonyManager tm = (TelephonyManager) mCtx.getSystemService(Context.TELEPHONY_SERVICE);
        ContentResolver resolver = mCtx.getContentResolver();
        boolean changed = false;

        String simOp = tm.getSimOperator();
        if (simOp == null || simOp.length() == 0) {
            MyLog.d(mCtx, EmailNotify.TAG, "simOp not set.");
            return false;
        }
        if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "simOp = " + simOp);
        Cursor cursor = resolver.query(Uri.parse("content://telephony/carriers"),
                new String[] { BaseColumns._ID, "apn", "type" },
                "numeric = ?", new String[] { simOp }, null);
        assert cursor != null;

        ContentValues values;

        // APNのkeyと付加文字列検索 (ex:apndroid)
        if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "Checking for enable APN...");
        String apnKey = null;
        String modifierStr = null;
        String modifierType = null;
        if (cursor.moveToFirst()) {
            do {
                String key = cursor.getString(0);
                String apn = cursor.getString(1);
                String type = cursor.getString(2);
                if (BuildConfig.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "key=" + key + ", apn=" + apn + ", type=" + type);

                if (apn.equals(target_apn)) {
                    if (BuildConfig.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "Found valid APN.");
                    apnKey = key;
                    break;
                }
                if (apn.startsWith(target_apn)) {
                    String ex = apn.substring(target_apn.length());
                    if (type.endsWith(ex)) {
                        apnKey = key;
                        modifierStr = ex;
                        modifierType = EmailNotifyPreferences.PREF_NETWORK_SAVE_APN_MODIFIER_TYPE_SUFFIX;
                        if (BuildConfig.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "Found modifier " + modifierType + ": " + modifierStr);
                    }
                } else if (apn.endsWith(target_apn)) {
                    String ex = apn.substring(0, apn.length() - target_apn.length());
                    if (type.startsWith(ex)) {
                        apnKey = key;
                        modifierStr = ex;
                        modifierType = EmailNotifyPreferences.PREF_NETWORK_SAVE_APN_MODIFIER_TYPE_PREFIX;
                        if (BuildConfig.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "Found modifier " + modifierType + ": " + modifierStr);
                    }
                }
            } while (cursor.moveToNext());
        }

        if (modifierStr != null) {
            if (BuildConfig.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "Enabling APNs...");
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
                        if (BuildConfig.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "Skipping APN: apn=" + apn + ", type=" + type);
                        continue;
                    }

                    // APN設定をもとに戻す
                    values = new ContentValues();
                    values.put("apn", new_apn);
                    values.put("type", new_type);
                    Uri url = ContentUris.withAppendedId(Uri.parse("content://telephony/carriers"), Integer.parseInt(key));
                    try {
                        resolver.update(url, values, null, null);
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
            if (BuildConfig.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "Activating APN...");

            // 現在の接続先を取得
            cursor = resolver.query(Uri.parse("content://telephony/carriers/preferapn"),
                    new String[] {"_id"}, null, null, null);
            assert cursor != null;
            if (cursor.moveToFirst()) {
                prevApnKey = cursor.getString(0);
                if (BuildConfig.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "Current APN ID: " + prevApnKey);
            }
            cursor.close();

            if (prevApnKey == null || !prevApnKey.equals(apnKey)) {
                // 接続先APNを変更
                values = new ContentValues();
                values.put("apn_id", apnKey);
                try {
                    resolver.update(Uri.parse("content://telephony/carriers/preferapn"), values, null, null);
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
            if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "Saved network: apnKey=" + prevApnKey +
                    ", modifier=" + modifierStr + ", type=" + modifierType);
        } else {
            if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "APN is already active.");
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
        WifiManager wm = (WifiManager) mCtx.getSystemService(Context.WIFI_SERVICE);
        boolean changed = false;

        if (wm.getWifiState() != WifiManager.WIFI_STATE_DISABLED) {
            if (!enable) {
                // 有効→無効化
                if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "Disabling Wi-Fi...");
                wm.setWifiEnabled(false);
                changed = true;
            } else {
                if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "Wi-Fi is already enabled.");
            }
        } else {
            if (enable) {
                // 無効→有効化
                if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "Enabling Wi-Fi...");
                wm.setWifiEnabled(true);
                changed = true;
            } else {
                if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "Wi-Fi is already disabled.");
            }
        }

        return changed;
    }

    /**
     * APNを復元する
     */
    private void restoreAPN() {
        TelephonyManager tm = (TelephonyManager) mCtx.getSystemService(Context.TELEPHONY_SERVICE);
        ContentResolver resolver = mCtx.getContentResolver();

        String apnKey = EmailNotifyPreferences.getNetworkSaveApnKey(mCtx);
        String modifierStr = EmailNotifyPreferences.getNetworkSaveApnModifierString(mCtx);
        String modifierType = EmailNotifyPreferences.getNetworkSaveApnModifierType(mCtx);
        if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "Restoring APN: apnKey=" + apnKey + ", modifier=" + modifierStr +
                ", type=" + modifierType);

        ContentValues values;

        if (modifierStr != null) {
            Cursor cursor = resolver.query(Uri.parse("content://telephony/carriers"),
                    new String[] { BaseColumns._ID, "apn", "type" },
                    "numeric = ?", new String[] { tm.getSimOperator() }, null);
            assert cursor != null;

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
                        if (BuildConfig.DEBUG) MyLog.d(mCtx, EmailNotify.TAG, "Skipping APN: apn=" + apn + ", type=" + type);
                        continue;
                    }

                    values = new ContentValues();
                    values.put("apn", new_apn);
                    values.put("type", new_type);
                    Uri url = ContentUris.withAppendedId(Uri.parse("content://telephony/carriers"), Integer.parseInt(key));
                    try {
                        resolver.update(url, values, null, null);
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
                resolver.update(Uri.parse("content://telephony/carriers/preferapn"), values, null, null);
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
            if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "Saved network information.");
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

        if (wifiChanged) {
            // 復元用に変更前のネットワーク情報を保存(有効フラグを立てる)
            EmailNotifyPreferences.saveNetworkInfo(mCtx);
            if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "Saved network information.");
        }
        return wifiChanged;
    }

    /**
     * ネットワーク設定を復元する
     */
    public void restoreNetwork() {
        WifiManager wm = (WifiManager) mCtx.getSystemService(Context.WIFI_SERVICE);

        if (EmailNotifyPreferences.hasNetworkSave(mCtx)) {
            if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "Restoring network...");

            // APN設定を復元
            restoreAPN();

            // データ通信設定を復元
            if (EmailNotifyPreferences.hasNetworkSaveMobileDataEnable(mCtx)) {
                if (EmailNotifyPreferences.getNetworkSaveMobileDataEnable(mCtx)) {
                    if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "Enabling Mobile Data...");
                    setMobileDataEnabled(true);
                    MyLog.d(mCtx, EmailNotify.TAG, "Mobile Data enabled.");
                } else {
                    if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "Disabling Mobile Data...");
                    setMobileDataEnabled(false);
                    MyLog.d(mCtx, EmailNotify.TAG, "Mobile Data disabled.");
                }
            }

            // Wi-Fi設定を復元
            if (EmailNotifyPreferences.hasNetworkSaveWifiEnable(mCtx)) {
                if (EmailNotifyPreferences.getNetworkSaveWifiEnable(mCtx)) {
                    if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "Enabling Wi-Fi...");
                    wm.setWifiEnabled(true);
                    MyLog.d(mCtx, EmailNotify.TAG, "Wi-Fi enabled.");
                } else {
                    if (BuildConfig.DEBUG) Log.d(EmailNotify.TAG, "Disabling Wi-Fi...");
                    wm.setWifiEnabled(false);
                    MyLog.d(mCtx, EmailNotify.TAG, "Wi-Fi disabled.");
                }
            }
        }
    }

}
