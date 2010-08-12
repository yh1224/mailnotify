package net.assemble.emailnotify;

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
    private SeekBar mSeekBar;
    private TextView mLengthView;

    private int mVibrationLength;

    public EmailNotifyVibrationLengthPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

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
        mSeekBar.setMax(30);
        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mVibrationLength = mSeekBar.getProgress();
                mLengthView.setText(mVibrationLength + "sec");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        mLengthView.setText(mVibrationLength + "sec");
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

    protected static SeekBar getSeekBar(View dialogView) {
        return (SeekBar) dialogView.findViewById(R.id.seekbar);
    }
}
