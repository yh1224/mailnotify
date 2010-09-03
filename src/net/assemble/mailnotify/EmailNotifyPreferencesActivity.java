package net.assemble.mailnotify;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

/**
 * 設定画面
 */
public class EmailNotifyPreferencesActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private Preference mPrefLaunchApp;
    private ListPreference mPrefVibrationPattern;
    private EmailNotifyVibrationLengthPreference mPrefVibrationLength;
    private ListPreference mPrefLedColor;
    private Preference mPrefTest;
    private SharedPreferences mPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        mPrefLaunchApp = findPreference("launch_app");
        mPrefVibrationPattern = (ListPreference) findPreference("vibration_pattern");
        mPrefVibrationLength = (EmailNotifyVibrationLengthPreference) findPreference("vibration_length");
        mPrefLedColor = (ListPreference) findPreference("led_color");
        mPrefTest = (Preference) findPreference("notifytest");
        mPref = PreferenceManager.getDefaultSharedPreferences(this);

        updateSummary();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mPrefLaunchApp) {
            Intent intent = new Intent().setClass(this, EmailNotifyPreferencesLaunchAppActivity.class);
            startActivityForResult(intent, 1/*TODO*/);
        } else if (preference == mPrefTest) {
            EmailNotifyNotification.doNotify(this);
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
        if (requestCode == 1/*TODO*/) {
            if (resultCode == RESULT_OK) {
                String package_name = data.getStringExtra("package_name");
                String class_name = data.getStringExtra("class_name");

                Editor e = mPref.edit();
                e.putString(EmailNotifyPreferences.PREF_KEY_LAUNCH_APP_PACKAGE, package_name);
                e.putString(EmailNotifyPreferences.PREF_KEY_LAUNCH_APP_CLASS, class_name);
                e.commit();

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
        // 起動アプリ
        ComponentName component = EmailNotifyPreferences.getComponent(this);
        if (component != null) {
            mPrefLaunchApp.setSummary(
                    getResources().getString(R.string.pref_launch_app_is) + "\n" +
                    component.getClassName());
        }

        // バイブレーションパターン
        mPrefVibrationPattern.setSummary(
            getEntryString(mPrefVibrationPattern.getValue(),
                getResources().getStringArray(R.array.entries_vibration_pattern),
                getResources().getStringArray(R.array.entryvalues_vibration_pattern)));

        // バイブレーション時間
        mPrefVibrationLength.setSummary(
                mPrefVibrationLength.getValue() + 
                getResources().getString(R.string.pref_vibration_length_unit));

        // LED色
        mPrefLedColor.setSummary(
            getEntryString(mPrefLedColor.getValue(),
                getResources().getStringArray(R.array.entries_led_color),
                getResources().getStringArray(R.array.entryvalues_led_color)));
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
