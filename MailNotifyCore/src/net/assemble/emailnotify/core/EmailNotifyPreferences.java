package net.assemble.emailnotify.core;

import java.util.Calendar;
import java.util.Map;
import java.util.UUID;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.preference.PreferenceManager;

/**
 * 設定管理
 */
public class EmailNotifyPreferences
{
    private static final String PREF_PREFERENCE_VERSION_KEY = "preference_version";
    private static final int CURRENT_PREFERENCE_VERSION = 5;

    private static final String PREF_PREFERENCE_ID_KEY = "preference_id";

    public static final String SERVICE_MOPERA = "mopera";
    public static final String SERVICE_SPMODE = "spmode";
    public static final String SERVICE_IMODE = "imode";
    public static final String SERVICE_OTHER = "other";
    public static final String[] SERVICES = { SERVICE_MOPERA, SERVICE_SPMODE, SERVICE_IMODE };

    public static final String PREF_LICENSED_KEY = "licensed";
    public static final boolean PREF_LICENSED_DEFAULT = false;

    public static final String PREF_ENABLE_KEY = "enabled";
    public static final boolean PREF_ENABLE_DEFAULT = true;

    public static final String PREF_NOTIFICATION_ICON_KEY = "notification_icon";
    public static final boolean PREF_NOTIFICATION_ICON_DEFAULT = false;

    public static final String PREF_SERVICE_KEY = "service";
    public static final boolean PREF_SERVICE_DEFAULT = true;
    public static final boolean PREF_SERVICE_IMODE_DEFAULT = false;

    public static final String PREF_NOTIFY_CUSTOM_KEY = "notify_custom";
    public static final boolean PREF_NOTIFY_CUSTOM_DEFAULT = false;

    public static final String PREF_NOTIFY_STOP_ON_SCREEN_KEY = "notify_stop_on_screen";
    public static final boolean PREF_NOTIFY_STOP_ON_SCREEN_DEFAULT = false;

    public static final String PREF_NOTIFY_STOP_LED_ON_SCREEN_KEY = "notify_stop_led_on_screen";
    public static final boolean PREF_NOTIFY_STOP_LED_ON_SCREEN_DEFAULT = false;

    public static final String PREF_NOTIFY_STOP_RENOTIFY_ON_SCREEN_KEY = "notify_stop_renotify_on_screen";
    public static final boolean PREF_NOTIFY_STOP_RENOTIFY_ON_SCREEN_DEFAULT = true;

    public static final String PREF_NOTIFY_SOUND_KEY = "notify_sound";
    public static final String PREF_NOTIFY_SOUND_DEFAULT = "content://settings/system/notification_sound";

    public static final String PREF_NOTIFY_SOUND_LENGTH_KEY = "notify_sound_length";
    public static final int PREF_NOTIFY_SOUND_LENGTH_DEFAULT = 0;

    public static final String PREF_NOTIFY_VIBRATION_KEY = "notify_vibration";
    public static final boolean PREF_NOTIFY_VIBRATION_DEFAULT = true;

    public static final String PREF_NOTIFY_VIBRATION_PATTERN_KEY = "notify_vibration_pattern";
    public static final String PREF_NOTIFY_VIBRATION_PATTERN_DEFAULT = "0";
    public static final long[][] PREF_NOTIFY_VIBRATION_PATTERN = {
        { 250, 250, 250, 1000 },        // パターン1
        { 500, 250, 500, 1000 },        // パターン2
        { 1000, 1000, 1000, 1000 },      // パターン3
        { 2000, 500 },                  // パターン4
        { 250, 250, 1000, 1000 },       // パターン5
    };

    public static final String PREF_NOTIFY_VIBRATION_LENGTH_KEY = "notify_vibration_length";
    public static final int PREF_NOTIFY_VIBRATION_LENGTH_DEFAULT = 3;

    public static final String PREF_NOTIFY_VIBRATION_MANNERONLY_KEY = "notify_vibration_manneronly";
    public static final boolean PREF_NOTIFY_VIBRATION_MANNERONLY_DEFAULT = false;

    public static final String PREF_NOTIFY_LED_KEY = "notify_led";
    public static final boolean PREF_NOTIFY_LED_DEFAULT = true;

    public static final String PREF_NOTIFY_LED_COLOR_KEY = "notify_led_color";
    public static final String PREF_NOTIFY_LED_COLOR_DEFAULT = "ff00ff00";

    public static final String PREF_NOTIFY_VIEW_KEY = "notify_view";
    public static final boolean PREF_NOTIFY_VIEW_DEFAULT = true;

    public static final String PREF_NOTIFY_LAUNCH_APP_NAME_KEY = "notify_launch_app_name";
    public static final String PREF_NOTIFY_LAUNCH_APP_PACKAGE_KEY = "notify_launch_app_package_name";
    public static final String PREF_NOTIFY_LAUNCH_APP_CLASS_KEY = "notify_launch_app_class_name";

    public static final String PREF_NOTIFY_RENOTIFY_KEY = "notify_renotify";
    public static final boolean PREF_NOTIFY_RENOTIFY_DEFAULT = false;

    public static final String PREF_NOTIFY_RENOTIFY_INTERVAL_KEY = "notify_renotify_interval";
    public static final int PREF_NOTIFY_RENOTIFY_INTERVAL_DEFAULT = 5;

    public static final String PREF_NOTIFY_RENOTIFY_COUNT_KEY = "notify_renotify_count";
    public static final int PREF_NOTIFY_RENOTIFY_COUNT_DEFAULT = 5;

    public static final String PREF_NOTIFY_DELAY_KEY = "notify_delay";
    public static final int PREF_NOTIFY_DELAY_DEFAULT = 0;

    public static final String PREF_NOTIFY_AUTO_CONNECT_KEY = "notify_auto_connect";
    public static final boolean PREF_NOTIFY_AUTO_CONNECT_DEFAULT = false;

    public static final String PREF_EXCLUDE_HOURS_KEY = "exclude_hours";

    public static final String PREF_SEND_LOG_KEY = "log_send";
    public static final boolean PREF_SEND_LOG_DEFAULT = false;

    public static final String PREF_WORKAROUND_LYNX_KEY = "workaround_lynx";
    public static final String PREF_WORKAROUND_XPERIAARC_KEY = "workaround_xperiaarc";

    // 最終チェック日時保存用
    public static final String PREF_LAST_CHECK_KEY = "last_check";

    // 最終メール通知日時保存用
    public static final String PREF_LAST_TIMESTAMP_KEY = "last_timestamp";

    // ネットワーク復元情報保存用
    public static final String PREF_NETWORK_SAVE_KEY = "network_save";
    public static final String PREF_NETWORK_SAVE_APN_KEY_KEY = "network_save_apn_key";
    public static final String PREF_NETWORK_SAVE_APN_MODIFIER_STRING_KEY = "network_save_apn_modifier_string";
    public static final String PREF_NETWORK_SAVE_APN_MODIFIER_TYPE_KEY = "network_save_apn_modifier_type";
    public static final String PREF_NETWORK_SAVE_APN_MODIFIER_TYPE_PREFIX = "prefix";
    public static final String PREF_NETWORK_SAVE_APN_MODIFIER_TYPE_SUFFIX = "suffix";
    public static final String PREF_NETWORK_SAVE_WIFI_ENABLE_KEY = "network_save_wifi_enable";

    // 前回ログ送信日時
    public static final String PREF_LOG_SENT_KEY = "log_sent";

    /**
     * IDを取得
     */
    public static String getPreferenceId(Context ctx) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        String uniqueId = pref.getString(PREF_PREFERENCE_ID_KEY, null);
        if (uniqueId == null) {
            uniqueId = UUID.randomUUID().toString();
            Editor editor = pref.edit();
            editor.putString(PREF_PREFERENCE_ID_KEY, uniqueId);
            editor.commit();
        }
        return uniqueId;
    }

    /**
     * すべて取得
     */
    public static Map<String, ?> getAll(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getAll();
    }

    /**
     * 有効フラグを取得
     */
    public static boolean getEnable(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                PREF_ENABLE_KEY,
                PREF_ENABLE_DEFAULT);
    }

    /**
     * 有効フラグを保存
     */
    public static void setEnable(Context ctx, boolean val) {
        Editor e = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        e.putBoolean(PREF_ENABLE_KEY, val);
        e.commit();
    }

    /**
     * 購入済みフラグを取得
     */
    public static boolean getLicense(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                PREF_LICENSED_KEY,
                PREF_LICENSED_DEFAULT);
    }

    /**
     * 購入済みフラグを保存
     */
    public static void setLicense(Context ctx, boolean val) {
        Editor e = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        e.putBoolean(PREF_LICENSED_KEY, val);
        e.commit();
    }

    /**
     * 通知アイコン設定を取得
     */
    public static boolean getNotificationIcon(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                PREF_NOTIFICATION_ICON_KEY,
                PREF_NOTIFICATION_ICON_DEFAULT);
    }

    /**
     * メールサービス設定を取得
     */
    public static boolean getService(Context ctx, String service) {
        boolean def = PREF_SERVICE_DEFAULT;
        if (service.equals(SERVICE_IMODE)) {    // iモードのみ初期値変更
            def = PREF_SERVICE_IMODE_DEFAULT;
        }
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                getServiceKey(PREF_SERVICE_KEY, service), def);
    }

    /**
     * サービス毎のkeyを取得
     */
    private static String getServiceKey(String key, String service) {
        return key + "_" + service;
    }

    /**
     * サービス毎通知設定有無を取得
     */
    public static boolean isNotifyCustomized(Context ctx, String service) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                getServiceKey(PREF_NOTIFY_CUSTOM_KEY, service), false);
    }

    /**
     * 画面ONで通知を停止
     */
    public static boolean getNotifyStopOnScreen(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                PREF_NOTIFY_STOP_ON_SCREEN_KEY,
                PREF_NOTIFY_STOP_ON_SCREEN_DEFAULT);
    }

    /**
     * 画面ONでLEDも消灯
     */
    public static boolean getNotifyStopLedOnScreen(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                PREF_NOTIFY_STOP_LED_ON_SCREEN_KEY,
                PREF_NOTIFY_STOP_LED_ON_SCREEN_DEFAULT);
    }

    /**
     * 画面ONで再通知も停止
     */
    public static boolean getNotifyStopRenotifyOnScreen(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                PREF_NOTIFY_STOP_RENOTIFY_ON_SCREEN_KEY,
                PREF_NOTIFY_STOP_RENOTIFY_ON_SCREEN_DEFAULT);
    }

    /**
     * 通知音設定を取得
     */
    public static String getNotifySound(Context ctx, String service) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (service != null && isNotifyCustomized(ctx, service)) {
            return pref.getString(getServiceKey(PREF_NOTIFY_SOUND_KEY, service),
                PREF_NOTIFY_SOUND_DEFAULT);
        } else {
            return pref.getString(PREF_NOTIFY_SOUND_KEY,
                PREF_NOTIFY_SOUND_DEFAULT);
        }
    }

    /**
     * 通知音の長さ設定を取得
     */
    public static int getNotifySoundLength(Context ctx, String service) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getInt(
                PREF_NOTIFY_SOUND_LENGTH_KEY,
                PREF_NOTIFY_SOUND_LENGTH_DEFAULT);
    }

    /**
     * バイブレーション設定を取得
     */
    public static boolean getNotifyVibration(Context ctx, String service) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (service != null && isNotifyCustomized(ctx, service)) {
            return pref.getBoolean(getServiceKey(PREF_NOTIFY_VIBRATION_KEY, service),
                PREF_NOTIFY_VIBRATION_DEFAULT);
        } else {
            return pref.getBoolean(PREF_NOTIFY_VIBRATION_KEY,
                PREF_NOTIFY_VIBRATION_DEFAULT);
        }
    }

    /**
     * バイブレーションパターン設定を取得
     */
    public static long[] getNotifyVibrationPattern(Context ctx, String service) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        String val;
        if (service != null && isNotifyCustomized(ctx, service)) {
            val = pref.getString(getServiceKey(PREF_NOTIFY_VIBRATION_PATTERN_KEY, service),
                PREF_NOTIFY_VIBRATION_PATTERN_DEFAULT);
        } else {
            val = pref.getString(PREF_NOTIFY_VIBRATION_PATTERN_KEY,
                PREF_NOTIFY_VIBRATION_PATTERN_DEFAULT);
        }
        int idx =Integer.parseInt(val);
        return PREF_NOTIFY_VIBRATION_PATTERN[idx];
    }

    /**
     * バイブレーション長設定を取得
     */
    public static int getNotifyVibrationLength(Context ctx, String service) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (service != null && isNotifyCustomized(ctx, service)) {
            return pref.getInt(getServiceKey(PREF_NOTIFY_VIBRATION_LENGTH_KEY, service),
                PREF_NOTIFY_VIBRATION_LENGTH_DEFAULT);
        } else {
            return pref.getInt(PREF_NOTIFY_VIBRATION_LENGTH_KEY,
                PREF_NOTIFY_VIBRATION_LENGTH_DEFAULT);
        }
    }

    /**
     * マナーモードのみバイブレーション設定を取得
     */
    public static boolean getNotifyVibrationManneronly(Context ctx, String service) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (service != null && isNotifyCustomized(ctx, service)) {
            return pref.getBoolean(getServiceKey(PREF_NOTIFY_VIBRATION_MANNERONLY_KEY, service),
                PREF_NOTIFY_VIBRATION_MANNERONLY_DEFAULT);
        } else {
            return pref.getBoolean(PREF_NOTIFY_VIBRATION_MANNERONLY_KEY,
                PREF_NOTIFY_VIBRATION_MANNERONLY_DEFAULT);
        }
    }

    /**
     * 通知LED設定を取得
     */
    public static boolean getNotifyLed(Context ctx, String service) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (service != null && isNotifyCustomized(ctx, service)) {
            return pref.getBoolean(getServiceKey(PREF_NOTIFY_LED_KEY, service),
                PREF_NOTIFY_LED_DEFAULT);
        } else {
            return pref.getBoolean(PREF_NOTIFY_LED_KEY,
                PREF_NOTIFY_LED_DEFAULT);
        }
    }

    /**
     * 通知LED色設定を取得
     */
    public static int getNotifyLedColor(Context ctx, String service) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        String color;
        if (service != null && isNotifyCustomized(ctx, service)) {
            color = pref.getString(getServiceKey(PREF_NOTIFY_LED_COLOR_KEY, service),
                PREF_NOTIFY_LED_COLOR_DEFAULT);
        } else {
            color = pref.getString(PREF_NOTIFY_LED_COLOR_KEY,
                PREF_NOTIFY_LED_COLOR_DEFAULT);
        }
        int argb = 0;
        for(int i = 0; i < color.length(); i += 2){
            argb *= 256;
            argb += Integer.parseInt(color.substring(i, i + 2), 16);
        }
        return argb;
    }

    /**
     * 通知ビュー設定を取得
     */
    public static boolean getNotifyView(Context ctx, String service) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (service != null && isNotifyCustomized(ctx, service)) {
            return pref.getBoolean(getServiceKey(PREF_NOTIFY_VIEW_KEY, service),
                PREF_NOTIFY_VIEW_DEFAULT);
        } else {
            return pref.getBoolean(PREF_NOTIFY_VIEW_KEY,
                PREF_NOTIFY_VIEW_DEFAULT);
        }
    }

    /**
     * 起動アプリ名を取得
     */
    public static String getNotifyLaunchAppName(Context ctx, String service) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (service != null && isNotifyCustomized(ctx, service)) {
            return pref.getString(getServiceKey(PREF_NOTIFY_LAUNCH_APP_NAME_KEY, service), null);
        } else {
            return pref.getString(PREF_NOTIFY_LAUNCH_APP_NAME_KEY, null);
        }
    }

    /**
     * 起動アプリ設定を取得
     */
    public static ComponentName getNotifyLaunchAppComponent(Context ctx, String service) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        String packageName;
        String className;
        if (service != null && isNotifyCustomized(ctx, service)) {
            packageName = pref.getString(getServiceKey(PREF_NOTIFY_LAUNCH_APP_PACKAGE_KEY, service), null);
            className = pref.getString(getServiceKey(PREF_NOTIFY_LAUNCH_APP_CLASS_KEY, service), null);
        } else {
            packageName = pref.getString(PREF_NOTIFY_LAUNCH_APP_PACKAGE_KEY, null);
            className = pref.getString(PREF_NOTIFY_LAUNCH_APP_CLASS_KEY, null);
        }
        if (packageName == null || className == null) {
            return null;
        }
        return new ComponentName(packageName, className);
    }

    /**
     * 起動アプリ設定を保存
     */
    public static void setNotifyLaunchApp(Context ctx, String service, String appName, String packageName, String className) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        Editor e = pref.edit();
        if (service != null) {
            e.putString(getServiceKey(PREF_NOTIFY_LAUNCH_APP_NAME_KEY, service), appName);
            e.putString(getServiceKey(PREF_NOTIFY_LAUNCH_APP_PACKAGE_KEY, service), packageName);
            e.putString(getServiceKey(PREF_NOTIFY_LAUNCH_APP_CLASS_KEY, service), className);
        } else {
            e.putString(PREF_NOTIFY_LAUNCH_APP_NAME_KEY, appName);
            e.putString(PREF_NOTIFY_LAUNCH_APP_PACKAGE_KEY, packageName);
            e.putString(PREF_NOTIFY_LAUNCH_APP_CLASS_KEY, className);
        }
        e.commit();
    }

    /**
     * 再通知設定を取得
     */
    public static boolean getNotifyRenotify(Context ctx, String service) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (service != null && isNotifyCustomized(ctx, service)) {
            return pref.getBoolean(getServiceKey(PREF_NOTIFY_RENOTIFY_KEY, service),
                PREF_NOTIFY_RENOTIFY_DEFAULT);
        } else {
            return pref.getBoolean(
                PREF_NOTIFY_RENOTIFY_KEY,
                PREF_NOTIFY_RENOTIFY_DEFAULT);
        }
    }

    /**
     * 再通知間隔設定を取得
     */
    public static int getNotifyRenotifyInterval(Context ctx, String service) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (service != null && isNotifyCustomized(ctx, service)) {
            return pref.getInt(getServiceKey(PREF_NOTIFY_RENOTIFY_INTERVAL_KEY, service),
                PREF_NOTIFY_RENOTIFY_INTERVAL_DEFAULT);
        } else {
            return pref.getInt(PREF_NOTIFY_RENOTIFY_INTERVAL_KEY,
                PREF_NOTIFY_RENOTIFY_INTERVAL_DEFAULT);
        }
    }

    /**
     * 再通知回数設定を取得
     */
    public static int getNotifyRenotifyCount(Context ctx, String service) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (service != null && isNotifyCustomized(ctx, service)) {
            return pref.getInt(getServiceKey(PREF_NOTIFY_RENOTIFY_COUNT_KEY, service),
                PREF_NOTIFY_RENOTIFY_COUNT_DEFAULT);
        } else {
            return pref.getInt(PREF_NOTIFY_RENOTIFY_COUNT_KEY,
                PREF_NOTIFY_RENOTIFY_COUNT_DEFAULT);
        }
    }

    /**
     * 通知遅延時間設定を取得
     */
    public static int getNotifyDelay(Context ctx, String service) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (service != null && isNotifyCustomized(ctx, service)) {
            return pref.getInt(getServiceKey(PREF_NOTIFY_DELAY_KEY, service),
                PREF_NOTIFY_DELAY_DEFAULT);
        } else {
            return pref.getInt(PREF_NOTIFY_DELAY_KEY,
                PREF_NOTIFY_DELAY_DEFAULT);
        }
    }

    /**
     * 自動接続設定を取得
     */
    public static boolean getNotifyAutoConnect(Context ctx, String service) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        // TODO: 試験実装中のため、サービス毎設定の有効無効に結び付けない。
        if (service != null/* && isNotifyCustomized(ctx, service)*/) {
            return pref.getBoolean(getServiceKey(PREF_NOTIFY_AUTO_CONNECT_KEY, service),
                PREF_NOTIFY_AUTO_CONNECT_DEFAULT);
        } else {
            return pref.getBoolean(PREF_NOTIFY_AUTO_CONNECT_KEY,
                PREF_NOTIFY_AUTO_CONNECT_DEFAULT);
        }
    }

    /**
     * 通知抑止期間設定を取得
     *
     * @return 開始時、終了時 (未設定、不正の場合はnull)
     */
    public static int[] getExcludeHours(Context ctx, String service) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        // TODO: 試験実装中のため、サービス毎設定の有効無効に結び付けない。
        String val = pref.getString(PREF_EXCLUDE_HOURS_KEY, null);
        if (val != null) {
            try {
                String[] se = val.split("-");
                if (se.length == 2) {
                        int[] result = new int[2];
                        result[0] = (int) Integer.parseInt(se[0]);
                        result[1] = (int) Integer.parseInt(se[1]);
                        if (result[0] >= 0 && result[0] <= 24 &&
                                result[1] >= 0 && result[1] <= 24 &&
                                result[0] != result[1]) {
                            if (result[0] == 24) {
                                result[0] = 0;
                            }
                            if (result[1] == 24) {
                                result[1] = 0;
                            }
                            return result;
                        }
                }
            } catch (NumberFormatException e) {}
        }
        return null;
    }

    /**
     * ログ送信フラグを保存
     */
    public static boolean getSendLog(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                PREF_SEND_LOG_KEY,
                PREF_SEND_LOG_DEFAULT);
    }

    /**
     * ログ送信フラグを保存
     */
    public static void setSendLog(Context ctx, boolean val) {
        Editor e = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        e.putBoolean(PREF_SEND_LOG_KEY, val);
        e.commit();
    }

    /**
     * ログ送信フラグの設定有無確認
     */
    @SuppressWarnings("rawtypes")
    public static boolean hasSendLog(Context ctx) {
        Map map = PreferenceManager.getDefaultSharedPreferences(ctx).getAll();
        return map.containsKey(PREF_SEND_LOG_KEY);
    }

    /**
     * 現在が通知抑止期間かどうか
     */
    public static boolean inExcludeHours(Context ctx, String service) {
        int now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int[] hours = getExcludeHours(ctx, service);
        if (hours != null) {
            if (hours[0] <= hours[1]) { // start < end
                if (hours[0] <= now && now < hours[1]) {
                    return true;
                }
            } else {    // start > end
                if (hours[0] <= now || now < hours[1]) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 前回チェック日時を取得
     */
    public static long getLastCheck(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getLong(
                PREF_LAST_CHECK_KEY, 0);
    }

    /**
     * 前回チェック日時を保存
     */
    public static void setLastCheck(Context ctx, long val) {
        Editor e = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        e.putLong(PREF_LAST_CHECK_KEY, val);
        e.commit();
    }

    /**
     * ネットワーク復元情報の有無を取得
     */
    public static boolean hasNetworkSave(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                PREF_NETWORK_SAVE_KEY, false);
    }

    /**
     * ネットワーク復元情報：APNキーを取得
     */
    public static String getNetworkSaveApnKey(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getString(
                PREF_NETWORK_SAVE_APN_KEY_KEY, null);
    }

    /**
     * ネットワーク復元情報：APN付与文字列を取得
     */
    public static String getNetworkSaveApnModifierString(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getString(
                PREF_NETWORK_SAVE_APN_MODIFIER_STRING_KEY, null);
    }

    /**
     * ネットワーク復元情報：APN付与タイプを取得
     */
    public static String getNetworkSaveApnModifierType(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getString(
                PREF_NETWORK_SAVE_APN_MODIFIER_TYPE_KEY, null);
    }

    /**
     * ネットワーク復元情報：Wi-Fi状態を取得
     */
    public static boolean getNetworkSaveWifiEnable(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                PREF_NETWORK_SAVE_WIFI_ENABLE_KEY, false);
    }

    /**
     * ネットワーク復元情報有無を保存
     */
    public static void saveNetworkInfo(Context ctx) {
        Editor e = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        e.putBoolean(PREF_NETWORK_SAVE_KEY, true);
        e.commit();
    }

    /**
     * ネットワーク復元情報：APN情報を保存
     */
    public static void saveNetworkApnInfo(Context ctx, String key, String modifier, String type) {
        Editor e = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        e.putString(PREF_NETWORK_SAVE_APN_KEY_KEY, key);
        e.putString(PREF_NETWORK_SAVE_APN_MODIFIER_STRING_KEY, modifier);
        e.putString(PREF_NETWORK_SAVE_APN_MODIFIER_TYPE_KEY, type);
        e.commit();
    }

    /**
     * ネットワーク復元情報：Wi-Fi情報を保存
     */
    public static void saveNetworkWifiInfo(Context ctx, boolean enable) {
        Editor e = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        e.putBoolean(PREF_NETWORK_SAVE_WIFI_ENABLE_KEY, enable);
        e.commit();
    }

    /**
     * ネットワーク復元情報を消去
     */
    public static void unsetNetworkInfo(Context ctx) {
        Editor e = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        e.putBoolean(PREF_NETWORK_SAVE_KEY, false);
        e.remove(PREF_NETWORK_SAVE_APN_KEY_KEY);
        e.remove(PREF_NETWORK_SAVE_APN_MODIFIER_STRING_KEY);
        e.remove(PREF_NETWORK_SAVE_APN_MODIFIER_TYPE_KEY);
        e.remove(PREF_NETWORK_SAVE_WIFI_ENABLE_KEY);
        e.commit();
    }

    /**
     * 前回ログ送信日時を取得
     */
    public static long getLogSent(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getLong(
                PREF_LOG_SENT_KEY, 0);
    }

    /**
     * 前回ログ送信日時を保存
     */
    public static void setLogSent(Context ctx, long val) {
        Editor e = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        e.putLong(PREF_LOG_SENT_KEY, val);
        e.commit();
    }

    /**
     * LYNX(SH-10B)ワークアラウンドが必要かどうか
     */
    public static boolean getLynxWorkaround(Context ctx) {
        if (Build.MODEL.equals("SH-10B")) {
            return true;
        } else {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                    PREF_WORKAROUND_LYNX_KEY, false);
        }
    }

    /**
     * Xperia arc(SO-01C)ワークアラウンドが必要かどうか
     */
    public static boolean getXperiaarcWorkaround(Context ctx) {
        if (Build.MODEL.equals("SO-01C") || Build.MODEL.equals("SO-02C")) {
            return true;
        } else {
            return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                    PREF_WORKAROUND_XPERIAARC_KEY, false);
        }
    }

    /**
     * プリファレンスのアップグレード
     */
    public static void upgrade(Context ctx) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        int ver = pref.getInt(PREF_PREFERENCE_VERSION_KEY, 0);
        if (0 < ver && ver < CURRENT_PREFERENCE_VERSION) {
            Editor e = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
            if (ver == 0) {
                // キー名称を変更
                e.putBoolean(PREF_LICENSED_KEY, pref.getBoolean("licence", PREF_LICENSED_DEFAULT));
                e.putBoolean(PREF_ENABLE_KEY, pref.getBoolean("enable", PREF_ENABLE_DEFAULT));
                e.putString(PREF_NOTIFY_SOUND_KEY, pref.getString("sound", PREF_NOTIFY_SOUND_DEFAULT));
                e.putBoolean(PREF_NOTIFY_VIBRATION_KEY, pref.getBoolean("vibration", PREF_NOTIFY_VIBRATION_DEFAULT));
                e.putString(PREF_NOTIFY_VIBRATION_PATTERN_KEY, pref.getString("vibration_patter", PREF_NOTIFY_VIBRATION_PATTERN_DEFAULT));
                e.putInt(PREF_NOTIFY_VIBRATION_LENGTH_KEY, pref.getInt("vibration_length", PREF_NOTIFY_VIBRATION_LENGTH_DEFAULT));
                e.putBoolean(PREF_NOTIFY_VIBRATION_MANNERONLY_KEY, pref.getBoolean("vibration_manneronly", PREF_NOTIFY_VIBRATION_MANNERONLY_DEFAULT));
                e.putString(PREF_NOTIFY_LED_COLOR_KEY, pref.getString("led_color", PREF_NOTIFY_LED_COLOR_DEFAULT));
                e.putString(PREF_NOTIFY_LAUNCH_APP_PACKAGE_KEY, pref.getString("launch_app_package_name", null));
                e.putString(PREF_NOTIFY_LAUNCH_APP_CLASS_KEY, pref.getString("launch_app_class_name", null));
                e.putBoolean(PREF_NOTIFY_RENOTIFY_KEY, pref.getBoolean("renotify", PREF_NOTIFY_RENOTIFY_DEFAULT));
                e.putInt(PREF_NOTIFY_RENOTIFY_INTERVAL_KEY, pref.getInt("renotify_interval", PREF_NOTIFY_RENOTIFY_INTERVAL_DEFAULT));
                e.putInt(PREF_NOTIFY_RENOTIFY_COUNT_KEY, pref.getInt("renotify_count", PREF_NOTIFY_RENOTIFY_COUNT_DEFAULT));
            }
            if (ver < 2) {
                // 起動アプリ名を保存するようにした。旧設定からはパッケージ名をコピー。
                e.putString(PREF_NOTIFY_LAUNCH_APP_NAME_KEY, pref.getString(PREF_NOTIFY_LAUNCH_APP_PACKAGE_KEY, null));
                for (int i = 0; i < SERVICES.length; i++) {
                    e.putString(getServiceKey(PREF_NOTIFY_LAUNCH_APP_NAME_KEY, SERVICES[i]),
                            pref.getString(getServiceKey(PREF_NOTIFY_LAUNCH_APP_PACKAGE_KEY, SERVICES[i]), null));
                }
            }
            if (ver < 3) {
                e.putInt(PREF_NOTIFY_DELAY_KEY, Integer.parseInt(pref.getString(PREF_NOTIFY_DELAY_KEY, "0")));
                e.putInt(getServiceKey(PREF_NOTIFY_DELAY_KEY, SERVICE_MOPERA),
                        Integer.parseInt(pref.getString(getServiceKey(PREF_NOTIFY_DELAY_KEY, SERVICE_MOPERA), "0")));
                e.putInt(getServiceKey(PREF_NOTIFY_DELAY_KEY, SERVICE_SPMODE),
                        Integer.parseInt(pref.getString(getServiceKey(PREF_NOTIFY_DELAY_KEY, SERVICE_SPMODE), "0")));
                e.putInt(getServiceKey(PREF_NOTIFY_DELAY_KEY, SERVICE_IMODE),
                        Integer.parseInt(pref.getString(getServiceKey(PREF_NOTIFY_DELAY_KEY, SERVICE_IMODE), "0")));
            }
            if (ver < 5) {
                // IMoNi通知設定を削除
                e.remove("notify_to_imoni");
                e.remove("notify_to_imoni_imode");
            }
            e.putInt(PREF_PREFERENCE_VERSION_KEY, CURRENT_PREFERENCE_VERSION);
            e.commit();
        }

        if (EmailNotifyPreferences.getLynxWorkaround(ctx)) {
            // LYNXでは鳴り分けは不可
            Editor e = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
            e.putBoolean(getServiceKey(PREF_SERVICE_KEY, SERVICE_MOPERA), false);
            e.putBoolean(getServiceKey(PREF_SERVICE_KEY, SERVICE_SPMODE), false);
            e.commit();
        }

    }

}
