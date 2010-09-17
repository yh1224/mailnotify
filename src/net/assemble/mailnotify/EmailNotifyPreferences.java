package net.assemble.mailnotify;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

/**
 * 設定管理
 */
public class EmailNotifyPreferences
{
    private static final String PREF_PREFERENCE_VERSION_KEY = "preference_version";
    private static final int CURRENT_PREFERENCE_VERSION = 1;

    public static final String SERVICE_MOPERA = "mopera";
    public static final String SERVICE_SPMODE = "spmode";
    public static final String SERVICE_IMODE = "imode";
    public static final String SERVICE_OTHER = "other";

    public static final String PREF_LICENSED_KEY = "licensed";
    public static final boolean PREF_LICENSED_DEFAULT = false;

    public static final String PREF_ENABLE_KEY = "enabled";
    public static final boolean PREF_ENABLE_DEFAULT = true;

    public static final String PREF_NOTIFICATION_ICON_KEY = "notification_icon";
    public static final boolean PREF_NOTIFICATION_ICON_DEFAULT = false;

    public static final String PREF_SERVICE_MOPERA_KEY = "service_mopera";
    public static final boolean PREF_SERVICE_MOPERA_DEFAULT = true;

    public static final String PREF_SERVICE_SPMODE_KEY = "service_spmode";
    public static final boolean PREF_SERVICE_SPMODE_DEFAULT = true;

    public static final String PREF_SERVICE_IMODE_KEY = "service_imode";
    public static final boolean PREF_SERVICE_IMODE_DEFAULT = false;

    public static final String PREF_SERVICE_OTHER_KEY = "service_other";
    public static final boolean PREF_SERVICE_OTHER_DEFAULT = true;

    public static final String PREF_NOTIFY_CUSTOM_KEY = "notify_custom";
    public static final boolean PREF_NOTIFY_CUSTOM_DEFAULT = false;

    public static final String PREF_NOTIFY_STOP_ON_SCREEN_KEY = "notify_stop_on_screen";
    public static final boolean PREF_NOTIFY_STOP_ON_SCREEN_DEFAULT = false;

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

    public static final String PREF_NOTIFY_LAUNCH_APP_PACKAGE_KEY = "notify_launch_app_package_name";
    public static final String PREF_NOTIFY_LAUNCH_APP_CLASS_KEY = "notify_launch_app_class_name";

    public static final String PREF_NOTIFY_RENOTIFY_KEY = "notify_renotify";
    public static final boolean PREF_NOTIFY_RENOTIFY_DEFAULT = false;

    public static final String PREF_NOTIFY_RENOTIFY_INTERVAL_KEY = "notify_renotify_interval";
    public static final int PREF_NOTIFY_RENOTIFY_INTERVAL_DEFAULT = 5;

    public static final String PREF_NOTIFY_RENOTIFY_COUNT_KEY = "notify_renotify_count";
    public static final int PREF_NOTIFY_RENOTIFY_COUNT_DEFAULT = 5;

    public static final String PREF_NOTIFY_DISABLE_WIFI_KEY = "notify_disable_wifi";
    public static final boolean PREF_NOTIFY_DISABLE_WIFI_DEFAULT = false;

    public static final String PREF_LAST_CHECK_KEY = "last_check";
    public static final long PREF_LAST_CHECK_DEFAULT = 0;

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
     * mopera Uメール有効設定を取得
     */
    public static boolean getServiceMopera(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                PREF_SERVICE_MOPERA_KEY,
                PREF_SERVICE_MOPERA_DEFAULT);
    }

    /**
     * spモード有効設定を取得
     */
    public static boolean getServiceSpmode(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                PREF_SERVICE_SPMODE_KEY,
                PREF_SERVICE_SPMODE_DEFAULT);
    }

    /**
     * iモード有効設定を取得
     */
    public static boolean getServiceImode(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                PREF_SERVICE_IMODE_KEY,
                PREF_SERVICE_IMODE_DEFAULT);
    }

    /**
     * その他サービス有効設定を取得
     */
    public static boolean getServiceOther(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(
                PREF_SERVICE_OTHER_KEY,
                PREF_SERVICE_OTHER_DEFAULT);
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
     * 起動アプリ設定を取得
     */
    public static ComponentName getNotifyLaunchApp(Context ctx, String service) {
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
    public static void setNotifyLaunchApp(Context ctx, String service, String packageName, String className) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        Editor e = pref.edit();
        if (service != null) {
            e.putString(getServiceKey(PREF_NOTIFY_LAUNCH_APP_PACKAGE_KEY, service), packageName);
            e.putString(getServiceKey(PREF_NOTIFY_LAUNCH_APP_CLASS_KEY, service), className);
        } else {
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
     * 前回通知日時を取得
     */
    public static long getLastCheck(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getLong(
                PREF_LAST_CHECK_KEY,
                PREF_LAST_CHECK_DEFAULT);
    }

    /**
     * WiFi無効化設定を取得
     */
    public static boolean getNotifyDisableWifi(Context ctx, String service) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (service != null && isNotifyCustomized(ctx, service)) {
            return pref.getBoolean(getServiceKey(PREF_NOTIFY_DISABLE_WIFI_KEY, service),
                PREF_NOTIFY_DISABLE_WIFI_DEFAULT);
        } else {
            return pref.getBoolean(PREF_NOTIFY_DISABLE_WIFI_KEY,
                PREF_NOTIFY_DISABLE_WIFI_DEFAULT);
        }
    }

    /**
     * 前回通知日時を保存
     */
    public static void setLastCheck(Context ctx, long val) {
        Editor e = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        e.putLong(PREF_LAST_CHECK_KEY, val);
        e.commit();
    }

    /**
     * プリファレンスのアップグレード
     */
    public static void upgarde(Context ctx) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        int ver = pref.getInt(PREF_PREFERENCE_VERSION_KEY, 0);
        if (ver < CURRENT_PREFERENCE_VERSION) {
            Editor e = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
            if (ver == 0) {
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
            e.putInt(PREF_PREFERENCE_VERSION_KEY, CURRENT_PREFERENCE_VERSION);
            e.commit();
        }
    }

}
