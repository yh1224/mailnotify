package net.assemble.emailnotify;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;

public class EmailNotificationSound {
    private static final String TAG = "EmailNotify";

    private ContentResolver mContentResolver;
    private Uri mUri;
    private boolean mIsNotification;
    private boolean mIsRingtone;
    private String mTitle;
    private int mDuration;

    public EmailNotificationSound(ContentResolver cr) {
        mContentResolver = cr;
    }

    /**
     * サウンド情報のカーソルを取得
     *
     * @param uri URI
     * @return カーソル
     */
    public EmailNotificationSound setUri(Uri uri) {
        // 変化なし
        if (uri.equals(mUri)) {
            return this;
        }

        mUri = uri;
        mIsRingtone = false;
        mTitle = null;
        mDuration = 0;

        // プリセット
        if (uri.toString().startsWith(Settings.System.CONTENT_URI.toString())) {
            // 設定から取得
            String sound = Settings.System.getString(mContentResolver, uri.getLastPathSegment());
            if (sound != null) {
                uri = Uri.parse(sound);
            }
        }
        String columns[] = {
                MediaStore.Audio.AudioColumns._ID,
                MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.AudioColumns.IS_NOTIFICATION,
                MediaStore.Audio.AudioColumns.IS_RINGTONE,
                MediaStore.Audio.AudioColumns.DURATION
        };
        Cursor c = null;
        try {
            c = mContentResolver.query(uri, columns, null, null, null);
            if (c != null) {
                c.moveToFirst();

                mTitle = c.getString(c.getColumnIndex(MediaStore.Audio.AudioColumns.TITLE));
                if (c.getInt(c.getColumnIndex(MediaStore.Audio.AudioColumns.IS_NOTIFICATION)) != 0) {
                    mIsNotification = true;
                }
                if (c.getInt(c.getColumnIndex(MediaStore.Audio.AudioColumns.IS_RINGTONE)) != 0) {
                    mIsRingtone = true;
                }
                mDuration = c.getInt(c.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION));
                c.close();
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to fetch " + uri + ": " + e.toString());
        }
        return this;
    }

    /**
     * サウンドのタイトルを取得
     */
    public String getTitle() {
        //return mTitle;
        return mTitle + " (" + (mDuration / 1000 + 1) + "秒)\n" + mUri;
    }

    /**
     * サウンドが通知音かどうか
     */
    public boolean isNotification() {
        return mIsNotification;
    }

    /**
     * サウンドが着信音かどうか
     */
    public boolean isRingtone() {
        return mIsRingtone;
    }

    /**
     * サウンドの長さを取得(ms)
     */
    public int getDuration() {
        return mDuration;
    }

}
