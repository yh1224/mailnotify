package net.assemble.mailnotify;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 *
 */
public class EmailNotifyVibrationLengthPreference extends DialogPreference {
    private Context mCtx;
    private SeekBar mSeekBar;
    private TextView mLengthView;

    private int mVibrationLength;

    public EmailNotifyVibrationLengthPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCtx = context;

        setDialogLayoutResource(R.layout.number_seekbar_dialog);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);

        // 現在の設定値を取得
        mVibrationLength = EmailNotifyPreferences.getVibrationLength(context);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mLengthView = (TextView) view.findViewById(R.id.length);

        mSeekBar = (SeekBar) view.findViewById(R.id.seekbar);
        mSeekBar.setProgress(mVibrationLength);
        mSeekBar.setMax(EmailNotifyPreferences.PREF_VIBRATION_LENGTH_MAX);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mVibrationLength = mSeekBar.getProgress();
                if (mVibrationLength < EmailNotifyPreferences.PREF_VIBRATION_LENGTH_MIN) {
                    mVibrationLength = EmailNotifyPreferences.PREF_VIBRATION_LENGTH_MIN;
                    mSeekBar.setProgress(mVibrationLength);
                }
                mLengthView.setText(mVibrationLength +
                        mCtx.getResources().getString(R.string.pref_vibration_length_unit));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        mLengthView.setText(mVibrationLength +
                mCtx.getResources().getString(R.string.pref_vibration_length_unit));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            Editor e = getSharedPreferences().edit();
            e.putInt(EmailNotifyPreferences.PREF_KEY_VIBRATION_LENGTH, mVibrationLength);
            e.commit();
        }
    }

    public int getValue() {
        return mVibrationLength;
    }

    protected static SeekBar getSeekBar(View dialogView) {
        return (SeekBar) dialogView.findViewById(R.id.seekbar);
    }
    
}
