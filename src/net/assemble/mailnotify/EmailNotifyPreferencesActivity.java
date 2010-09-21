package net.assemble.mailnotify;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;

/**
 * 設定画面
 */
public class EmailNotifyPreferencesActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private final int REQUEST_LAUNCH_APP = 1;

    // mopera Uメール通知設定
    private RingtonePreference mPrefNotifySoundMopera;
    private ListPreference mPrefNotifyVibrationPatternMopera;
    private NumberSeekbarPreference mPrefNotifyVibrationLengthMopera;
    private ListPreference mPrefNotifyLedColorMopera;
    private Preference mPrefNotifyLaunchAppMopera;
    private NumberSeekbarPreference mPrefNotifyRenotifyIntervalMopera;
    private NumberSeekbarPreference mPrefNotifyRenotifyCountMopera;
    private Preference mPrefTestNotifyMopera;

    // spモードメール通知設定
    private RingtonePreference mPrefNotifySoundSpmode;
    private ListPreference mPrefNotifyVibrationPatternSpmode;
    private NumberSeekbarPreference mPrefNotifyVibrationLengthSpmode;
    private ListPreference mPrefNotifyLedColorSpmode;
    private Preference mPrefNotifyLaunchAppSpmode;
    private NumberSeekbarPreference mPrefNotifyRenotifyIntervalSpmode;
    private NumberSeekbarPreference mPrefNotifyRenotifyCountSpmode;
    private Preference mPrefTestNotifySpmode;

    // iモードメール通知設定
    private RingtonePreference mPrefNotifySoundImode;
    private ListPreference mPrefNotifyVibrationPatternImode;
    private NumberSeekbarPreference mPrefNotifyVibrationLengthImode;
    private ListPreference mPrefNotifyLedColorImode;
    private Preference mPrefNotifyLaunchAppImode;
    private NumberSeekbarPreference mPrefNotifyRenotifyIntervalImode;
    private NumberSeekbarPreference mPrefNotifyRenotifyCountImode;
    private Preference mPrefTestNotifyImode;

    // 基本通知設定
    private RingtonePreference mPrefNotifySound;
    private NumberSeekbarPreference mPrefNotifySoundLength;
    private ListPreference mPrefNotifyVibrationPattern;
    private NumberSeekbarPreference mPrefNotifyVibrationLength;
    private ListPreference mPrefNotifyLedColor;
    private Preference mPrefNotifyLaunchApp;
    private NumberSeekbarPreference mPrefNotifyRenotifyInterval;
    private NumberSeekbarPreference mPrefNotifyRenotifyCount;
    private Preference mPrefTestNotify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // mopera Uメール通知設定
        mPrefNotifySoundMopera = (RingtonePreference) findPreference("notify_sound_mopera");
        mPrefNotifyVibrationPatternMopera = (ListPreference) findPreference("notify_vibration_pattern_mopera");
        mPrefNotifyVibrationLengthMopera = (NumberSeekbarPreference) findPreference("notify_vibration_length_mopera");
        mPrefNotifyLedColorMopera = (ListPreference) findPreference("notify_led_color_mopera");
        mPrefNotifyLaunchAppMopera = findPreference("notify_launch_app_mopera");
        mPrefNotifyRenotifyIntervalMopera = (NumberSeekbarPreference) findPreference("notify_renotify_interval_mopera");
        mPrefNotifyRenotifyCountMopera = (NumberSeekbarPreference) findPreference("notify_renotify_count_mopera");
        mPrefTestNotifyMopera = (Preference) findPreference("test_notify_mopera");

        // spモードメール通知設定
        mPrefNotifySoundSpmode = (RingtonePreference) findPreference("notify_sound_spmode");
        mPrefNotifyVibrationPatternSpmode = (ListPreference) findPreference("notify_vibration_pattern_spmode");
        mPrefNotifyVibrationLengthSpmode = (NumberSeekbarPreference) findPreference("notify_vibration_length_spmode");
        mPrefNotifyLedColorSpmode = (ListPreference) findPreference("notify_led_color_spmode");
        mPrefNotifyLaunchAppSpmode = findPreference("notify_launch_app_spmode");
        mPrefNotifyRenotifyIntervalSpmode = (NumberSeekbarPreference) findPreference("notify_renotify_interval_spmode");
        mPrefNotifyRenotifyCountSpmode = (NumberSeekbarPreference) findPreference("notify_renotify_count_spmode");
        mPrefTestNotifySpmode = (Preference) findPreference("test_notify_spmode");

        // iモードメール通知設定
        mPrefNotifySoundImode = (RingtonePreference) findPreference("notify_sound_imode");
        mPrefNotifyVibrationPatternImode = (ListPreference) findPreference("notify_vibration_pattern_imode");
        mPrefNotifyVibrationLengthImode = (NumberSeekbarPreference) findPreference("notify_vibration_length_imode");
        mPrefNotifyLedColorImode = (ListPreference) findPreference("notify_led_color_imode");
        mPrefNotifyLaunchAppImode = findPreference("notify_launch_app_imode");
        mPrefNotifyRenotifyIntervalImode = (NumberSeekbarPreference) findPreference("notify_renotify_interval_imode");
        mPrefNotifyRenotifyCountImode = (NumberSeekbarPreference) findPreference("notify_renotify_count_imode");
        mPrefTestNotifyImode = (Preference) findPreference("test_notify_imode");

        // 基本通知設定
        mPrefNotifySound = (RingtonePreference) findPreference("notify_sound");
        mPrefNotifySoundLength = (NumberSeekbarPreference) findPreference("notify_sound_length");
        mPrefNotifyVibrationPattern = (ListPreference) findPreference("notify_vibration_pattern");
        mPrefNotifyVibrationLength = (NumberSeekbarPreference) findPreference("notify_vibration_length");
        mPrefNotifyLedColor = (ListPreference) findPreference("notify_led_color");
        mPrefNotifyLaunchApp = findPreference("notify_launch_app");
        mPrefNotifyRenotifyInterval = (NumberSeekbarPreference) findPreference("notify_renotify_interval");
        mPrefNotifyRenotifyCount = (NumberSeekbarPreference) findPreference("notify_renotify_count");
        mPrefTestNotify = (Preference) findPreference("test_notify");

        updateSummary();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mPrefNotifyLaunchAppMopera ||
                preference == mPrefNotifyLaunchAppSpmode ||
                preference == mPrefNotifyLaunchAppImode ||
                preference == mPrefNotifyLaunchApp) {
            Intent intent = new Intent();
            intent.setClass(this, EmailNotifySelectAppActivity.class);
            if (preference == mPrefNotifyLaunchAppMopera) {
                intent.putExtra("service", EmailNotifyPreferences.SERVICE_MOPERA);
            } else if (preference == mPrefNotifyLaunchAppSpmode) {
                intent.putExtra("service", EmailNotifyPreferences.SERVICE_SPMODE);
            } else if (preference == mPrefNotifyLaunchAppImode) {
                intent.putExtra("service", EmailNotifyPreferences.SERVICE_IMODE);
            }
            startActivityForResult(intent, REQUEST_LAUNCH_APP);
        } else if (preference == findPreference("service_imode")) {
            final CheckBoxPreference checkbox = (CheckBoxPreference) preference;
            if (checkbox.isChecked()) {
                // iモードメール警告
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.warning);
                builder.setMessage(getResources().getString(R.string.pref_service_imode_warning));
                builder.setPositiveButton(R.string.ok, null);
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        checkbox.setChecked(false);
                    }
                });
                builder.setCancelable(true);
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        } else if (preference == mPrefTestNotifyMopera) {
            EmailNotificationManager.showNotification(this, EmailNotifyPreferences.SERVICE_MOPERA, "Test for mopera U");
        } else if (preference == mPrefTestNotifySpmode) {
            EmailNotificationManager.showNotification(this, EmailNotifyPreferences.SERVICE_SPMODE, "Test for sp-mode");
        } else if (preference == mPrefTestNotifyImode) {
            EmailNotificationManager.showNotification(this, EmailNotifyPreferences.SERVICE_IMODE, "Test for i-mode");
        } else if (preference == mPrefTestNotify) {
            EmailNotificationManager.showNotification(this, EmailNotifyPreferences.SERVICE_OTHER, "Test");
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        updateService();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LAUNCH_APP) {
            if (resultCode == RESULT_OK) {
                String appName = data.getStringExtra("app_name");
                String packageName = data.getStringExtra("package_name");
                String className = data.getStringExtra("class_name");
                String service = data.getStringExtra("service");
                EmailNotifyPreferences.setNotifyLaunchApp(EmailNotifyPreferencesActivity.this,
                        service, appName, packageName, className);
                updateSummary();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * 設定値から表示文字列を取得
     *
     * @param val 設定値
     * @param entries　表示文字列の配列
     * @param entryvalues　設定値の配列
     * @return
     */
    private String getEntryString(String val, String[] entries, String[] entryvalues) {
        for (int i = 0; i < entries.length; i++) {
            if (val.equals(entryvalues[i])) {
                return entries[i];
            }
        }
        return null;
    }

    /**
     * summaryを更新
     */
    private void updateSummary() {
        String launchAppName;

        // mopera Uメール通知設定
        mPrefNotifyVibrationPatternMopera.setSummary(
            getEntryString(mPrefNotifyVibrationPatternMopera.getValue(),
                getResources().getStringArray(R.array.entries_vibration_pattern),
                getResources().getStringArray(R.array.entryvalues_vibration_pattern)));
        mPrefNotifyVibrationLengthMopera.setSummary(
                mPrefNotifyVibrationLengthMopera.getValue() +
                getResources().getString(R.string.pref_notify_vibration_length_unit));
        mPrefNotifyLedColorMopera.setSummary(
            getEntryString(mPrefNotifyLedColorMopera.getValue(),
                getResources().getStringArray(R.array.entries_led_color),
                getResources().getStringArray(R.array.entryvalues_led_color)));
        launchAppName = EmailNotifyPreferences.getNotifyLaunchAppName(this, EmailNotifyPreferences.SERVICE_MOPERA);
        if (launchAppName != null) {
            mPrefNotifyLaunchAppMopera.setSummary(
                    getResources().getString(R.string.pref_notify_launch_app_is) + "\n" + launchAppName);
        }
        mPrefNotifyRenotifyIntervalMopera.setSummary(
                mPrefNotifyRenotifyIntervalMopera.getValue() +
                getResources().getString(R.string.pref_notify_renotify_interval_unit));
        if (mPrefNotifyRenotifyCountMopera.getValue() == 0) {
            mPrefNotifyRenotifyCountMopera.setSummary(
                    getResources().getString(R.string.pref_notify_renotify_count_zero));
        } else {
            mPrefNotifyRenotifyCountMopera.setSummary(
                    mPrefNotifyRenotifyCountMopera.getValue() +
                    getResources().getString(R.string.pref_notify_renotify_count_unit));
        }

        // spモードメール通知設定
        mPrefNotifyVibrationPatternSpmode.setSummary(
            getEntryString(mPrefNotifyVibrationPatternSpmode.getValue(),
                getResources().getStringArray(R.array.entries_vibration_pattern),
                getResources().getStringArray(R.array.entryvalues_vibration_pattern)));
        mPrefNotifyVibrationLengthSpmode.setSummary(
                mPrefNotifyVibrationLengthSpmode.getValue() +
                getResources().getString(R.string.pref_notify_vibration_length_unit));
        mPrefNotifyLedColorSpmode.setSummary(
            getEntryString(mPrefNotifyLedColorSpmode.getValue(),
                getResources().getStringArray(R.array.entries_led_color),
                getResources().getStringArray(R.array.entryvalues_led_color)));
        launchAppName = EmailNotifyPreferences.getNotifyLaunchAppName(this, EmailNotifyPreferences.SERVICE_SPMODE);
        if (launchAppName != null) {
            mPrefNotifyLaunchAppSpmode.setSummary(
                    getResources().getString(R.string.pref_notify_launch_app_is) + "\n" + launchAppName);
        }
        mPrefNotifyRenotifyIntervalSpmode.setSummary(
                mPrefNotifyRenotifyIntervalSpmode.getValue() +
                getResources().getString(R.string.pref_notify_renotify_interval_unit));
        if (mPrefNotifyRenotifyCountSpmode.getValue() == 0) {
            mPrefNotifyRenotifyCountSpmode.setSummary(
                    getResources().getString(R.string.pref_notify_renotify_count_zero));
        } else {
            mPrefNotifyRenotifyCountSpmode.setSummary(
                    mPrefNotifyRenotifyCountSpmode.getValue() +
                    getResources().getString(R.string.pref_notify_renotify_count_unit));
        }

        // iモードメール通知設定
        mPrefNotifyVibrationPatternImode.setSummary(
            getEntryString(mPrefNotifyVibrationPatternImode.getValue(),
                getResources().getStringArray(R.array.entries_vibration_pattern),
                getResources().getStringArray(R.array.entryvalues_vibration_pattern)));
        mPrefNotifyVibrationLengthImode.setSummary(
                mPrefNotifyVibrationLengthImode.getValue() +
                getResources().getString(R.string.pref_notify_vibration_length_unit));
        mPrefNotifyLedColorImode.setSummary(
            getEntryString(mPrefNotifyLedColorImode.getValue(),
                getResources().getStringArray(R.array.entries_led_color),
                getResources().getStringArray(R.array.entryvalues_led_color)));
        launchAppName = EmailNotifyPreferences.getNotifyLaunchAppName(this, EmailNotifyPreferences.SERVICE_IMODE);
        if (launchAppName != null) {
            mPrefNotifyLaunchAppImode.setSummary(
                    getResources().getString(R.string.pref_notify_launch_app_is) + "\n" + launchAppName);
        }
        mPrefNotifyRenotifyIntervalImode.setSummary(
                mPrefNotifyRenotifyIntervalImode.getValue() +
                getResources().getString(R.string.pref_notify_renotify_interval_unit));
        if (mPrefNotifyRenotifyCountImode.getValue() == 0) {
            mPrefNotifyRenotifyCountImode.setSummary(
                    getResources().getString(R.string.pref_notify_renotify_count_zero));
        } else {
            mPrefNotifyRenotifyCountImode.setSummary(
                    mPrefNotifyRenotifyCountImode.getValue() +
                    getResources().getString(R.string.pref_notify_renotify_count_unit));
        }

        // 基本通知設定
        mPrefNotifyVibrationPattern.setSummary(
            getEntryString(mPrefNotifyVibrationPattern.getValue(),
                getResources().getStringArray(R.array.entries_vibration_pattern),
                getResources().getStringArray(R.array.entryvalues_vibration_pattern)));
        mPrefNotifyVibrationLength.setSummary(
                mPrefNotifyVibrationLength.getValue() +
                getResources().getString(R.string.pref_notify_vibration_length_unit));
        mPrefNotifyLedColor.setSummary(
            getEntryString(mPrefNotifyLedColor.getValue(),
                getResources().getStringArray(R.array.entries_led_color),
                getResources().getStringArray(R.array.entryvalues_led_color)));
        launchAppName = EmailNotifyPreferences.getNotifyLaunchAppName(this, null);
        if (launchAppName != null) {
            mPrefNotifyLaunchApp.setSummary(
                    getResources().getString(R.string.pref_notify_launch_app_is) + "\n" + launchAppName);
        }
        mPrefNotifyRenotifyInterval.setSummary(
                mPrefNotifyRenotifyInterval.getValue() +
                getResources().getString(R.string.pref_notify_renotify_interval_unit));
        if (mPrefNotifyRenotifyCount.getValue() == 0) {
            mPrefNotifyRenotifyCount.setSummary(
                    getResources().getString(R.string.pref_notify_renotify_count_zero));
        } else {
            mPrefNotifyRenotifyCount.setSummary(
                    mPrefNotifyRenotifyCount.getValue() +
                    getResources().getString(R.string.pref_notify_renotify_count_unit));
        }

        // その他
        if (mPrefNotifySoundLength.getValue() == 0) {
            mPrefNotifySoundLength.setSummary(
                    getResources().getString(R.string.pref_notify_sound_length_summary));
            // 通知音長が設定されていない場合は、通知音のみ選択可能。
            mPrefNotifySoundMopera.setRingtoneType(RingtoneManager.TYPE_NOTIFICATION);
            mPrefNotifySoundSpmode.setRingtoneType(RingtoneManager.TYPE_NOTIFICATION);
            mPrefNotifySoundImode.setRingtoneType(RingtoneManager.TYPE_NOTIFICATION);
            mPrefNotifySound.setRingtoneType(RingtoneManager.TYPE_NOTIFICATION);
        } else {
            mPrefNotifySoundLength.setSummary(
                    mPrefNotifySoundLength.getValue() +
                    getResources().getString(R.string.pref_notify_sound_length_unit));
            // 通知音長が設定されている場合のみ、着信音・アラーム音も選択可能とする。
            mPrefNotifySoundMopera.setRingtoneType(RingtoneManager.TYPE_ALL);
            mPrefNotifySoundSpmode.setRingtoneType(RingtoneManager.TYPE_ALL);
            mPrefNotifySoundImode.setRingtoneType(RingtoneManager.TYPE_ALL);
            mPrefNotifySound.setRingtoneType(RingtoneManager.TYPE_ALL);
        }
    }

    /**
     * 設定変更時の処理
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // summaryを更新
        updateSummary();
    }

    /**
     * 設定を反映
     */
    private void updateService() {
        if (EmailNotifyPreferences.getEnable(this)) {
            EmailNotifyService.startService(this);
        } else {
            EmailNotifyService.stopService(this);
        }
    }

}
