package net.assemble.emailnotify.core;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.assemble.emailnotify.core.notification.EmailNotificationManager;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class EmailNotify {
    public static final String TAG = "EmailNotify";
    public static final String MARKET_URL = "market://details?id=net.assemble.mailnotify";

    public static final String PACKAGE_NAME_FREE = "net.assemble.emailnotify";
    public static final String PACKAGE_NAME_PAID = "net.assemble.mailnotify";

    public static final String ACTION_MAIL_PUSH_RECEIVED = "net.assemble.emailnotify.MAIL_PUSH_RECEIVED";
    public static final String ACTION_LOG_SENT = "net.assemble.emailnotify.LOG_SENT";

    /**
     * デバッグ版
     */
    public static final boolean DEBUG = false;

    /**
     * 使用期限
     */
    public static final String TRIAL_EXPIRES = null;
    //public static final String TRIAL_EXPIRES = "yyyy/MM/dd";

    /**
     * 無料版チェック
     *
     * @param ctx Context
     */
    public static boolean isFreeVersion(Context ctx) {
        return ctx.getPackageName().equals(PACKAGE_NAME_FREE);
    }

    /**
     * アプリバージョン取得
     */
    public static String getAppVersion(Context ctx) {
        String ver = "";
        try {
            PackageInfo pi = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            ver = pi.versionName;
        } catch (NameNotFoundException e) {}
        if (DEBUG) {
            ver += "(DEBUG)";
        }
        return ver;
    }

    /**
     * 有効期限チェック
     *
     * @param ctx Context
     * @return true:期限内 false:期限切れ
     */
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
