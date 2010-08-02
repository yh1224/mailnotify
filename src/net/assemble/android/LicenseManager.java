package net.assemble.android;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.provider.Settings.Secure;

public class LicenseManager {
    public static String generateLicenseKey(Context ctx, String name) throws NoSuchAlgorithmException {
        String android_id = Secure.getString(ctx.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        String key = name + android_id;
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.update(key.getBytes());
        return hexString(md.digest());
    }

    private static String hexString(byte[] bytes) {
        StringBuffer strBuf = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            int d = bytes[i];
            if (d < 0) {  // byte 128-255
                d += 256;
            }
            if (d < 16) { //0-15 16
                strBuf.append("0");
            }
            strBuf.append(Integer.toString(d, 16));
        }
        return strBuf.toString();
    }
}
