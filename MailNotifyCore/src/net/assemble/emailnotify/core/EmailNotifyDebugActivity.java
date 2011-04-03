package net.assemble.emailnotify.core;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
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
            intent.putExtra("wapAppId", 9);
            sendBroadcast(intent);
        } else if (preference == findPreference("test_imode_intent")) {
            Intent intent = new Intent("android.provider.Telephony.WAP_PUSH_RECEIVED");
            intent.setType("application/vnd.wap.slc");
            intent.putExtra("data", HexUtils.hex2bytes("02066a00850903646f636f6d6f2e6e652e6a703f50493d30360001"));
            intent.putExtra("pduType", 6);
            intent.putExtra("contentTypeParameters", new HashMap<Void, Void>());
            intent.putExtra("transactionId", 0);
            sendBroadcast(intent);
        } else if (preference == findPreference("test_spmode_xperiaarc")) {
            Log.d("WAP PUSH", "call startService : Intent { act=android.provider.Telephony.WAP_PUSH_RECEIVED typ=application/vnd.wap.emn+wbxml cmp=jp.co.nttdocomo.carriermail/.SMSService (has extras) }");
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

}
