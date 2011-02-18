package net.orleaf.android.emailnotify.liveview.plugins;

import java.util.Date;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class EmnNotifyService extends Service
{
    private IEmnPluginService mPluginService;
    private String mHeader;
    private String mBody;
    private String mAction;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPluginService = IEmnPluginService.Stub.asInterface(service);
            try {
                mPluginService.sendAnnounce(mHeader, mBody, mAction);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            stopSelf();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mPluginService = null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        mHeader = getString(R.string.announce_header);
        String service = intent.getStringExtra("service");
        String mailbox = intent.getStringExtra("mailbox");
        mAction = service + " " + mailbox;
        Date received = (Date)intent.getSerializableExtra("received");
        int count = intent.getIntExtra("count", 0);
        mBody = mailbox;
        if (received != null) {
            mBody += " at " + received.toLocaleString();   
        }
        if (count > 0) {
            mBody += " (" + count + ")";
        }
        Log.i(Emn.TAG, "Mail Received: " + mBody);

        // Bind EmnPluginService
        Intent serviceIntent = new Intent(EmnNotifyService.this, EmnPluginService.class);
        bindService(serviceIntent, mConnection, 0);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }

}
