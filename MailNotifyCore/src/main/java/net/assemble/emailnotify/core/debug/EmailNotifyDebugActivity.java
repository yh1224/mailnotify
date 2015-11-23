package net.assemble.emailnotify.core.debug;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import net.assemble.emailnotify.core.R;
import net.orleaf.android.HexUtils;

/**
 * デバッグメニュー
 */
public class EmailNotifyDebugActivity extends PreferenceActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.debug_preferences);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {

        if (preference == findPreference("show_preferences")) {
            showDialogMessage("Preferences", getPreferencesString());
        } else
        if (preference == findPreference("test_mopera")) {
            Log.d("WAP PUSH", "Rx: 0006060302030aaf89030d6a00850703796831323234406d6f70657261008705c307"
                    + getTimestamp() + "01");
            Log.d("WAP PUSH", "call startService : Intent { act=android.provider.Telephony.WAP_PUSH_RECEIVED typ=application/vnd.wap.emn+wbxml cmp=jp.co.nttdocomo.carriermail/.SMSService (has extras) }");
        } else if (preference == findPreference("test_spmode")) {
            Log.d("WAP PUSH", "Rx: 0006080302030aaf02905c030d6a008507036869726f73652d79313232342d737040646f636f6d6f2e6e652e6a700005c307"
                    + getTimestamp() + "01");
            Log.d("WAP PUSH", "call startService : Intent { act=android.provider.Telephony.WAP_PUSH_RECEIVED typ=application/vnd.wap.emn+wbxml cmp=jp.co.nttdocomo.carriermail/.SMSService (has extras) }");
        } else if (preference == findPreference("test_imode")) {
            Log.d("WAP PUSH", "Rx: 000605b0af02800202066a00850903646f636f6d6f2e6e652e6a703f50493d30360001");
        } else if (preference == findPreference("test_mopera_intent")) {
            Intent intent = new Intent("android.provider.Telephony.WAP_PUSH_RECEIVED");
            intent.setType("application/vnd.wap.emn+wbxml");
            intent.putExtra("data", HexUtils.hex2bytes("030d6a00850703796831323234406d6f70657261008705c307" + getTimestamp() + "01"));
            intent.putExtra("transactionId", 0);
            intent.putExtra("pduType", 6);
            intent.putExtra("contentTypeParameters", new HashMap<Void, Void>());
            intent.putExtra("mps", "+8190542143014");
            intent.putExtra("ppg", "NTT DoCoMo");
            intent.putExtra("wapAppID", 9);
            sendBroadcast(intent);
        } else if (preference == findPreference("test_spmode_intent")) {
            Intent intent = new Intent("android.provider.Telephony.WAP_PUSH_RECEIVED");
            intent.setType("application/vnd.wap.emn+wbxml");
            intent.putExtra("header", HexUtils.hex2bytes("b0af029062"));
            intent.putExtra("data", HexUtils.hex2bytes("02066a00850b0373706d6f64652e6e652e6a703f44493d30370001"));
            intent.putExtra("pduType", 6);
            intent.putExtra("contentTypeParameters", new HashMap<Void, Void>());
            intent.putExtra("transactionId", 0);
            sendBroadcast(intent);
        } else if (preference == findPreference("test_imode_intent")) {
            Intent intent = new Intent("android.provider.Telephony.WAP_PUSH_RECEIVED");
            intent.setType("application/vnd.wap.slc");
            intent.putExtra("data", HexUtils.hex2bytes("02066a00850903646f636f6d6f2e6e652e6a703f50493d30360001"));
            intent.putExtra("pduType", 6);
            intent.putExtra("contentTypeParameters", new HashMap<Void, Void>());
            intent.putExtra("transactionId", 0);
            sendBroadcast(intent);
        } else if (preference == findPreference("test_unknown_intent")) {
            Intent intent = new Intent("android.provider.Telephony.WAP_PUSH_RECEIVED");
            intent.setType("application/vnd.wap.emn+wbxml");
            intent.putExtra("data", HexUtils.hex2bytes("01"));
            intent.putExtra("transactionId", 0);
            intent.putExtra("pduType", 6);
            intent.putExtra("header", HexUtils.hex2bytes("b0af029061"));
            intent.putExtra("contentTypeParameters", new HashMap<Void, Void>());
            sendBroadcast(intent);
        } else if (preference == findPreference("numberformatexception")) {
            Log.d("WAP PUSH", "Rx: 0g");
        } else if (preference == findPreference("stringindexoutofboundsexception")) {
            Log.d("WAP PUSH", "Rx: 0");
        } else if (preference == findPreference("test_spmode_xperiaarc")) {
            Log.d("WAP PUSH", "call startService : Intent { act=android.provider.Telephony.WAP_PUSH_RECEIVED typ=application/vnd.wap.emn+wbxml cmp=jp.co.nttdocomo.carriermail/.SMSService (has extras) }");
        } else if (preference == findPreference("test_processmsg")) {
            Log.d("WAP PUSH", "wpman processMsg 36956:application/vnd.wap.emn+wbxml");
        } else if (preference == findPreference("test_lynx")) {
            Log.d("WAP PUSH", "Receive EMN");
        }
        return true;
    }

    private String getTimestamp() {
        // TODO: 本当はGMTでフォーマットする
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(date);
    }

    private String getPreferencesString() {
        StringBuffer strBuf = new StringBuffer();
        Map<String, ?> prefs = PreferenceManager.getDefaultSharedPreferences(this).getAll();
        for (Iterator<?> it = prefs.entrySet().iterator(); it.hasNext(); ) {
            @SuppressWarnings("unchecked")
            Map.Entry<String, ?> entry = (Entry<String, ?>) it.next();
            if (entry.getValue() == null) {
                strBuf.append(entry.getKey() + "=(null)\n");
            } else {
                strBuf.append(entry.getKey() + "=" + entry.getValue().toString() + "\n");
            }
        }
        return strBuf.toString();
    }

    /**
     * ダイアログでメッセージを表示
     *
     * @param title タイトル
     * @param message メッセージ
     */
    private void showDialogMessage(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message.toString());
        builder.setPositiveButton(R.string.ok, null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

}
