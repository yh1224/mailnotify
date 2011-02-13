package net.assemble.mailnotify;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.util.Log;

public class EmailNotify {
    public static final String TAG = "EmailNotify";
    public static final String MARKET_URL = "market://details?id=net.assemble.mailnotify";

    public static final String ACTION_MAIL_PUSH_RECEIVED = "net.assemble.emailnotify.MAIL_PUSH_RECEIVED";

    /**
     * デバッグ版
     */
    public static final boolean DEBUG = false;

    /**
     * 無料版 (機能制限あり)
     */
    public static final boolean FREE_VERSION = false;

    /**
     * 使用期限
     */
    public static final String TRIAL_EXPIRES = null;
    //public static final String TRIAL_EXPIRES = "yyyy/MM/dd";

    /**
     * 有効期限チェック
     *
     * @param ctx Context
     * @return true:期限内 false:期限切れ
     */
    @SuppressWarnings("unused")
    public static boolean checkExpiration(Context ctx) {
        if (TRIAL_EXPIRES != null) {
            Date today = new Date();
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
                Date expire_date = sdf.parse(EmailNotify.TRIAL_EXPIRES);
                if (today.compareTo(expire_date) > 0) {
                    EmailNotificationManager.showExpiredNotification(ctx);
                    Log.d(TAG, "Expired.");
                    return false;
                } else {
                    Log.d(TAG, "Expires on " + expire_date.toLocaleString());
                }
            } catch (ParseException e) {}
        }
        return true;
    }

}
