package net.assemble.emailnotify.core.debug;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;

import net.assemble.emailnotify.core.R;
import net.assemble.emailnotify.core.preferences.EmailNotifyPreferences;
import net.orleaf.android.IMyLogReportListener;
import net.orleaf.android.IMyLogReportService;
import net.orleaf.android.MyLogReportService;

public class MyLogReportActivity extends ListActivity {
    private IMyLogReportService mReportService;
    private ProgressDialog mProgressDialog;
    private IMyLogReportListener.Stub mListener;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mReportService = IMyLogReportService.Stub.asInterface(binder);
            try {
                mListener = new IMyLogReportListener.Stub() {
                    @Override
                    public void onComplete(String result) throws RemoteException {
                        if (result == null) {
                            Toast.makeText(MyLogReportActivity.this, R.string.send_log_failed, Toast.LENGTH_SHORT).show();
                        } else {
                            sendMail(result);
                        }
                        finish();
                    }
                };
                mReportService.addListener(mListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mReportService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent serviceIntent = new Intent(this, MyLogReportService.class);
        serviceIntent.putExtra(MyLogReportService.EXTRA_REPORTER_ID,
                EmailNotifyPreferences.getPreferenceId(this));
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        mProgressDialog = new ProgressDialog(MyLogReportActivity.this);
        mProgressDialog.setMessage(getString(R.string.sending_log));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        mProgressDialog.show();

        setVisible(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        if (mReportService != null) {
            try {
                mReportService.removeListener(mListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            unbindService(serviceConnection);
        }
        super.onDestroy();
    }

    private void sendMail(String result) {
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getString(R.string.feedback_to)));
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " (" + result + ")");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

}
