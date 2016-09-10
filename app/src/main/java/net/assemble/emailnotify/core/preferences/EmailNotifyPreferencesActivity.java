package net.assemble.emailnotify.core.preferences;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.Manifest.permission;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;

import net.assemble.emailnotify.core.BuildConfig;
import net.assemble.emailnotify.core.EmailNotify;
import net.assemble.emailnotify.core.EmailNotifyObserveService;
import net.assemble.emailnotify.core.R;
import net.assemble.emailnotify.core.debug.MyLogReportActivity;
import net.assemble.emailnotify.core.notification.EmailNotificationManager;
import net.assemble.emailnotify.core.notification.MobileNetworkManager;
import net.assemble.emailnotify.core.notification.MobileNetworkManager.ApnInfo;

/**
 * 設定画面
 */
public class EmailNotifyPreferencesActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final int REQUEST_LAUNCH_APP = 1;
    private static final String[] SERVICES = {
        EmailNotifyPreferences.SERVICE_MOPERA, EmailNotifyPreferences.SERVICE_SPMODE,
        EmailNotifyPreferences.SERVICE_IMODE, null };

    private class PrefNotify {
         String serviceName;
         CheckBoxPreference enable;
         PreferenceScreen customScreen;
         RingtonePreference sound;
         ListPreference vibrationPattern;
         NumberSeekbarPreference vibrationLength;
         ListPreference ledColor;
         CheckBoxPreference notifyView;
         Preference launchApp;
         NumberSeekbarPreference renotifyInterval;
         NumberSeekbarPreference renotifyCount;
         NumberSeekbarPreference delay;
         CheckBoxPreference autoConnect;
         ListPreference autoConnectType;
         ListPreference autoConnectApn;
         CheckBoxPreference sms;
         EditTextPreference smsTel;
         Preference test;
    };
    private PrefNotify[] mPrefNotify;

    private NumberSeekbarPreference mPrefNotifyAutoclearInterval;
    private NumberSeekbarPreference mPrefNotifySoundLength;
    private EditTextPreference mPrefExcludeHours;

    private String[] mApnEntries;
    private String[] mApnEntryValues;
    private boolean mApnWritable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        //noinspection PointlessBooleanExpression
        if (BuildConfig.FREE_VERSION) {
            addPreferencesFromResource(R.xml.preferences_free);
        } else {
            addPreferencesFromResource(R.xml.preferences);
        }

        mApnWritable = getPackageManager().checkPermission(permission.WRITE_APN_SETTINGS, getPackageName()) == PackageManager.PERMISSION_GRANTED;
        if (mApnWritable) {
            mApnEntries = getApnEntries();
            mApnEntryValues = getApnEntryValues();
        }

        mPrefNotify = new PrefNotify[SERVICES.length];
        for (int i = 0; i < SERVICES.length; i++) {
            mPrefNotify[i] = new PrefNotify();
            mPrefNotify[i].serviceName = SERVICES[i];
            mPrefNotify[i].enable = (CheckBoxPreference) getServicePreference("service", SERVICES[i]);
            mPrefNotify[i].customScreen = (PreferenceScreen) getServicePreference("notify_custom_screen", SERVICES[i]);
            mPrefNotify[i].sound = (RingtonePreference) getServicePreference("notify_sound", SERVICES[i]);
            mPrefNotify[i].vibrationPattern = (ListPreference) getServicePreference("notify_vibration_pattern", SERVICES[i]);
            mPrefNotify[i].vibrationLength = (NumberSeekbarPreference) getServicePreference("notify_vibration_length", SERVICES[i]);
            mPrefNotify[i].ledColor = (ListPreference) getServicePreference("notify_led_color", SERVICES[i]);
            mPrefNotify[i].notifyView = (CheckBoxPreference) getServicePreference("notify_view", SERVICES[i]);
            mPrefNotify[i].launchApp = getServicePreference("notify_launch_app", SERVICES[i]);
            mPrefNotify[i].renotifyInterval = (NumberSeekbarPreference) getServicePreference("notify_renotify_interval", SERVICES[i]);
            mPrefNotify[i].renotifyCount = (NumberSeekbarPreference) getServicePreference("notify_renotify_count", SERVICES[i]);
            mPrefNotify[i].delay = (NumberSeekbarPreference) getServicePreference("notify_delay", SERVICES[i]);
            mPrefNotify[i].autoConnect = (CheckBoxPreference) getServicePreference("notify_auto_connect", SERVICES[i]);
            mPrefNotify[i].autoConnectType = (ListPreference) getServicePreference("notify_auto_connect_type", SERVICES[i]);
            mPrefNotify[i].autoConnectApn = (ListPreference) getServicePreference("notify_auto_connect_apn", SERVICES[i]);

            if (mApnWritable) {
                if (mPrefNotify[i].autoConnectApn != null && mApnEntries.length > 0) {
                    mPrefNotify[i].autoConnectApn.setEntries(mApnEntries);
                    mPrefNotify[i].autoConnectApn.setEntryValues(mApnEntryValues);
                }
            }
            mPrefNotify[i].sms = (CheckBoxPreference) getServicePreference("notify_sms", SERVICES[i]);
            mPrefNotify[i].smsTel = (EditTextPreference) getServicePreference("notify_sms_tel", SERVICES[i]);
            mPrefNotify[i].test = (Preference) getServicePreference("test_notify", SERVICES[i]);
        }

        mPrefNotifyAutoclearInterval = (NumberSeekbarPreference) findPreference("notify_autoclear_interval");
        mPrefNotifySoundLength = (NumberSeekbarPreference) findPreference("notify_sound_length");
        mPrefExcludeHours = (EditTextPreference) findPreference("exclude_hours");

        updateSummary();
    }

    private Preference getServicePreference(String key, String service) {
        if (service != null) {
            return findPreference(EmailNotifyPreferences.getServiceKey(key, service));
        } else {
            return findPreference(key);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        for (int i = 0; i < SERVICES.length; i++) {
            if (preference == mPrefNotify[i].launchApp) {
                Intent intent = new Intent();
                intent.setClass(this, EmailNotifySelectAppActivity.class);
                if (mPrefNotify[i].serviceName != null) {
                    intent.putExtra("service", mPrefNotify[i].serviceName);
                }
                startActivityForResult(intent, REQUEST_LAUNCH_APP);
            } else if (preference == mPrefNotify[i].notifyView) {
                final CheckBoxPreference checkbox = (CheckBoxPreference) preference;
                if (!checkbox.isChecked()) {
                    alertMessage(R.string.pref_notify_view_warning, null, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            checkbox.setChecked(true);
                        }
                    });
                }
            } else if (preference == mPrefNotify[i].autoConnect) {
                final CheckBoxPreference checkbox = (CheckBoxPreference) preference;
                if (checkbox.isChecked()) {
                    alertMessage(R.string.pref_notify_auto_connect_warning, null, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            checkbox.setChecked(false);
                        }
                    });
                }
            } else if (preference == mPrefNotify[i].sms) {
                final CheckBoxPreference checkbox = (CheckBoxPreference) preference;
                if (checkbox.isChecked()) {
                    alertMessage(R.string.pref_notify_sms_warning, null, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            checkbox.setChecked(false);
                        }
                    });
                }
            } else if (preference == mPrefNotify[i].test) {
                testNotificatioin(mPrefNotify[i].serviceName);
            }
        }
        if (preference == findPreference("service_imode")) {
            final CheckBoxPreference checkbox = (CheckBoxPreference) preference;
            if (checkbox.isChecked()) {
                alertMessage(R.string.pref_service_imode_warning, null, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        checkbox.setChecked(false);
                    }
                });
            }
        } else if (preference == findPreference("notify_sound_length")) {
            alertMessage(R.string.pref_notify_sound_length_warning, null, null);
        } else if (preference == findPreference("exclude_hours")) {
            alertMessage(R.string.pref_exclude_hours_warning, null, null);
        } else if (preference == findPreference("log_send")) {
            final CheckBoxPreference checkbox = (CheckBoxPreference) preference;
            if (checkbox.isChecked()) {
                alertMessage(R.string.pref_debug_log_send_warning, null, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        checkbox.setChecked(false);
                    }
                });
            }
        } else if (preference == findPreference("debug_feedback")) {
            Intent intent = new Intent(this, MyLogReportActivity.class);
            startActivity(intent);
        }
        return true;
    }

    /**
     * テスト通知
     *
     * 通知音/通知バイブレーションのシステム設定を確認し、
     * 無効であればメッセージを表示する。
     */
    private void testNotificatioin(final String serviceName) {
        final String title;
        if (serviceName != null) {
            title = "Test for " + serviceName;
        } else {
            title = "Test";
        }
        String message = "";
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (am.getStreamVolume(AudioManager.STREAM_NOTIFICATION) == 0) {
            message += getString(R.string.notification_muted_now);
        }
        if (EmailNotifyPreferences.getNotifyVibration(this, serviceName) &&
                !am.shouldVibrate(AudioManager.VIBRATE_TYPE_NOTIFICATION)) {
            message += getString(R.string.vibration_disabled_now);
        }
        if (message.length() > 0) {
            alertMessage(message + "\n" + getString(R.string.check_system_settings),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EmailNotificationManager.testNotification(EmailNotifyPreferencesActivity.this, serviceName, title);
                    }
                },
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                    }
                });
        } else {
            EmailNotificationManager.testNotification(EmailNotifyPreferencesActivity.this, serviceName, title);
        }
    }

    /**
     * 警告をダイアログ表示
     *
     * @param message 表示するメッセージ
     * @param negativeListener キャンセルされた場合のリスナ
     */
    private void alertMessage(String message, DialogInterface.OnClickListener clickListener, DialogInterface.OnCancelListener negativeListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.warning);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.ok, null);
        if (clickListener != null) {
            builder.setPositiveButton(R.string.ok, clickListener);
        }
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

    private void alertMessage(int msgResId, DialogInterface.OnClickListener clickListener, DialogInterface.OnCancelListener negativeListener) {
        alertMessage(getResources().getString(msgResId), clickListener, negativeListener);
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
        return val;
    }

    /**
     * summaryを更新
     */
    private void updateSummary() {
        String launchAppName;
        String networkType;
        String apnName;

        if (EmailNotifyPreferences.getLynxWorkaround(this)) {
            // LYNX(SH-10B)では鳴り分けは不可
            mPrefNotify[0].enable.setEnabled(false);
            mPrefNotify[1].enable.setEnabled(false);
        }

        // 通知設定
        for (int i = 0; i < SERVICES.length; i++) {
            if (mPrefNotify[i].enable != null) {
                if (!EmailNotifyPreferences.getNotifySupport(this, mPrefNotify[i].serviceName)) {
                    // サービス未サポート
                    mPrefNotify[i].enable.setSummary(R.string.service_not_supported);
                    if (mPrefNotify[i].customScreen != null) {
                        mPrefNotify[i].customScreen.setEnabled(false);
                        mPrefNotify[i].customScreen.setSummary(R.string.service_not_supported);
                    }
                } else {
                    mPrefNotify[i].enable.setSummary(null);
                }
            }
            if (mPrefNotify[i].vibrationPattern != null) {
                mPrefNotify[i].vibrationPattern.setSummary(
                    getEntryString(mPrefNotify[i].vibrationPattern.getValue(),
                        getResources().getStringArray(R.array.entries_vibration_pattern),
                        getResources().getStringArray(R.array.entryvalues_vibration_pattern)));
            }
            if (mPrefNotify[i].vibrationLength != null) {
                mPrefNotify[i].vibrationLength.setSummary(
                        mPrefNotify[i].vibrationLength.getValue() +
                        getResources().getString(R.string.pref_notify_vibration_length_unit));
            }
            if (mPrefNotify[i].ledColor != null) {
                mPrefNotify[i].ledColor.setSummary(
                    getEntryString(mPrefNotify[i].ledColor.getValue(),
                        getResources().getStringArray(R.array.entries_led_color),
                        getResources().getStringArray(R.array.entryvalues_led_color)));
            }
            if (mPrefNotify[i].launchApp != null) {
                launchAppName = EmailNotifyPreferences.getNotifyLaunchAppName(this, mPrefNotify[i].serviceName);
                if (launchAppName != null) {
                    mPrefNotify[i].launchApp.setSummary(
                            getResources().getString(R.string.pref_notify_launch_app_is) + "\n" + launchAppName);
                }
            }
            if (mPrefNotify[i].renotifyInterval != null) {
                mPrefNotify[i].renotifyInterval.setSummary(
                        mPrefNotify[i].renotifyInterval.getValue() +
                        getResources().getString(R.string.pref_notify_renotify_interval_unit));
            }
            if (mPrefNotify[i].renotifyCount != null) {
                if (mPrefNotify[i].renotifyCount.getValue() == 0) {
                    mPrefNotify[i].renotifyCount.setSummary(
                            getResources().getString(R.string.pref_notify_renotify_count_zero));
                } else {
                    mPrefNotify[i].renotifyCount.setSummary(
                            mPrefNotify[i].renotifyCount.getValue() +
                            getResources().getString(R.string.pref_notify_renotify_count_unit));
                }
            }
            if (mPrefNotify[i].delay != null) {
                if (mPrefNotify[i].delay.getValue() == 0) {
                    mPrefNotify[i].delay.setSummary(
                            getResources().getString(R.string.pref_notify_delay_summary));
                } else {
                    mPrefNotify[i].delay.setSummary(
                            mPrefNotify[i].delay.getValue() +
                            getResources().getString(R.string.pref_notify_delay_unit));
                }
            }
            if (mPrefNotify[i].autoConnectType != null) {
                networkType = EmailNotifyPreferences.getNotifyAutoConnectType(this, mPrefNotify[i].serviceName);
                if (networkType != null) {
                    mPrefNotify[i].autoConnectType.setSummary(
                            getEntryString(mPrefNotify[i].autoConnectType.getValue(),
                                getResources().getStringArray(R.array.entries_auto_connect_type),
                                getResources().getStringArray(R.array.entryvalues_auto_connect_type)));
                }
                if (mApnWritable) {
                    if (mApnEntries.length > 0 &&
                            EmailNotifyPreferences.getNotifyAutoConnect(this, mPrefNotify[i].serviceName) &&
                            networkType.equals(EmailNotifyPreferences.NETWORK_TYPE_MOBILE)) {
                        mPrefNotify[i].autoConnectApn.setEnabled(true);
                    } else {
                        mPrefNotify[i].autoConnectApn.setEnabled(false);
                    }
                    apnName = EmailNotifyPreferences.getNotifyAutoConnectApn(this, mPrefNotify[i].serviceName);
                    if (apnName != null) {
                        mPrefNotify[i].autoConnectApn.setSummary(
                            getEntryString(mPrefNotify[i].autoConnectApn.getValue(), mApnEntries, mApnEntryValues));
                    }
                } else {
                    mPrefNotify[i].autoConnectApn.setSummary(R.string.apn_not_writable);
                    mPrefNotify[i].autoConnectApn.setEnabled(false);
                }
            }
            if (mPrefNotify[i].smsTel != null) {
                mPrefNotify[i].smsTel.setSummary(mPrefNotify[i].smsTel.getText());
            }
        }

        // その他
        //noinspection PointlessBooleanExpression
        if (!BuildConfig.FREE_VERSION) {
            if (mPrefNotifyAutoclearInterval.getValue() == 0) {
                mPrefNotifyAutoclearInterval.setSummary(
                        getResources().getString(R.string.pref_notify_autoclear_interval_summary));
            } else {
                mPrefNotifyAutoclearInterval.setSummary(
                        mPrefNotifyAutoclearInterval.getValue() +
                        getResources().getString(R.string.pref_notify_autoclear_interval_unit));
            }
            if (mPrefNotifySoundLength.getValue() == 0) {
                mPrefNotifySoundLength.setSummary(
                        getResources().getString(R.string.pref_notify_sound_length_summary));
                // 通知音長が設定されていない場合は、通知音のみ選択可能。
                mPrefNotify[0].sound.setRingtoneType(RingtoneManager.TYPE_NOTIFICATION);
                mPrefNotify[1].sound.setRingtoneType(RingtoneManager.TYPE_NOTIFICATION);
                mPrefNotify[2].sound.setRingtoneType(RingtoneManager.TYPE_NOTIFICATION);
                mPrefNotify[3].sound.setRingtoneType(RingtoneManager.TYPE_NOTIFICATION);
            } else {
                mPrefNotifySoundLength.setSummary(
                        mPrefNotifySoundLength.getValue() +
                        getResources().getString(R.string.pref_notify_sound_length_unit));
                // 通知音長が設定されている場合のみ、着信音・アラーム音も選択可能とする。
                mPrefNotify[0].sound.setRingtoneType(RingtoneManager.TYPE_ALL);
                mPrefNotify[1].sound.setRingtoneType(RingtoneManager.TYPE_ALL);
                mPrefNotify[2].sound.setRingtoneType(RingtoneManager.TYPE_ALL);
                mPrefNotify[3].sound.setRingtoneType(RingtoneManager.TYPE_ALL);
            }
        }
        if (mPrefExcludeHours.getText() != null && mPrefExcludeHours.getText().length() > 0) {
            int[] hours = EmailNotifyPreferences.getExcludeHours(this, null);
            if (hours != null) {
                mPrefExcludeHours.setSummary(hours[0] +
                        getResources().getString(R.string.pref_exclude_hours_from) +
                        hours[1] + getResources().getString(R.string.pref_exclude_hours_to));
            } else {
                mPrefExcludeHours.setSummary(getResources().getString(R.string.pref_exclude_hours_invalid));
            }
        } else {
            mPrefExcludeHours.setSummary(getResources().getString(R.string.pref_exclude_hours_summary));
        }
    }

    /**
     * APN名リスト取得
     *
     * @return APN名の配列
     */
    private ArrayList<String> getApnNames() {
        // APN候補
        final String[] candidateApn = new String[] {
            "mopera.net",
            "0120.mopera.net",
            "0120.mopera.ne.jp",
            "mopera.flat.foma.ne.jp",
            "mpr.ex-pkt.net",
            "mpr.bizho.net",
            "mpr2.bizho.net",
            "open.mopera.net",
            "spmode.ne.jp",
        };

        ArrayList<String> apnnames = new ArrayList<String>();
        List<ApnInfo> apnList = new MobileNetworkManager(this).getApnList();
        if (apnList != null) {
            for (Iterator<ApnInfo> it = apnList.iterator(); it.hasNext(); ) {
                ApnInfo apn = (ApnInfo) it.next();
                int i;
                for (i = 0; i < candidateApn.length; i++) {
                    if (apn.APN_NAME.startsWith(candidateApn[i]) ||
                            apn.APN_NAME.endsWith(candidateApn[i])) {
                        // 候補のAPN文字列が含まれる(無効化されている)
                        apnnames.add(candidateApn[i]);
                        break;
                    }
                }
                if (i == candidateApn.length) {
                    // 候補になければそのまま追加
                    apnnames.add(apn.APN_NAME);
                }
            }
        }
        return apnnames;
    }

    /**
     * APN名リスト取得
     *
     * @return APN名の配列
     */
    private String[] getApnEntryValues() {
        ArrayList<String> apnnames = getApnNames();
        apnnames.add("");
        return apnnames.toArray(new String[apnnames.size()]);
    }

    /**
     * APN名リスト取得
     *
     * @return APN名の配列
     */
    private String[] getApnEntries() {
        ArrayList<String> apnnames = getApnNames();
        apnnames.add(getString(R.string.apn_not_specify));
        return apnnames.toArray(new String[apnnames.size()]);
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
            EmailNotifyObserveService.startService(this);
        } else {
            EmailNotifyObserveService.stopService(this);
        }
    }
}
