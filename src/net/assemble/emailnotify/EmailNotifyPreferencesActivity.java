package net.assemble.emailnotify;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
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

    // 基本通知設定
    private RingtonePreference mPrefNotifySound;
    private Preference mPrefNotifyLaunchApp;
    private Preference mPrefTestNotify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        // 基本通知設定
        mPrefNotifySound = (RingtonePreference) findPreference("notify_sound");
        mPrefNotifyLaunchApp = findPreference("notify_launch_app");
        mPrefTestNotify = (Preference) findPreference("test_notify");

        updateSummary();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mPrefNotifyLaunchApp) {
            Intent intent = new Intent();
            intent.setClass(this, EmailNotifySelectAppActivity.class);
            startActivityForResult(intent, REQUEST_LAUNCH_APP);
        } else if (preference == findPreference("service_imode")) {
            final CheckBoxPreference checkbox = (CheckBoxPreference) preference;
            if (checkbox.isChecked()) {
                alertMessage(R.string.pref_service_imode_warning, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        checkbox.setChecked(false);
                    }
                });
            }
        } else if (preference == findPreference("notify_auto_connect_spmode")) {
            final CheckBoxPreference checkbox = (CheckBoxPreference) preference;
            if (checkbox.isChecked()) {
                alertMessage(R.string.pref_notify_auto_connect_spmode_warning, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        checkbox.setChecked(false);
                    }
                });
            }
        } else if (preference == mPrefTestNotify) {
            EmailNotificationManager.testNotification(this, EmailNotifyPreferences.SERVICE_OTHER, "Test");
        }
        return true;
    }

    /**
     * 警告をダイアログ表示
     *
     * @param message 表示するメッセージのリソースID
     * @param negativeListener キャンセルされた場合のリスナ
     */
    private void alertMessage(int msgResId, DialogInterface.OnCancelListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.warning);
        builder.setMessage(getResources().getString(msgResId));
        builder.setPositiveButton(R.string.ok, null);
        if (negativeListener != null) {
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.setCancelable(true);
            builder.setOnCancelListener(negativeListener);
        }
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
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

        // 基本通知設定
        launchAppName = EmailNotifyPreferences.getNotifyLaunchAppName(this, null);
        if (launchAppName != null) {
            mPrefNotifyLaunchApp.setSummary(
                    getResources().getString(R.string.pref_notify_launch_app_is) + "\n" + launchAppName);
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
