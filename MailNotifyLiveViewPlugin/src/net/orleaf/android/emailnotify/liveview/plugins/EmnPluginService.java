package net.orleaf.android.emailnotify.liveview.plugins;

import com.sonyericsson.extras.liveview.plugins.AbstractPluginService;
import com.sonyericsson.extras.liveview.plugins.PluginConstants;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class EmnPluginService extends AbstractPluginService {

    private IEmnPluginService.Stub mBinder = new IEmnPluginService.Stub() {
        @Override
        public void sendAnnounce(String header, String body, String action) throws RemoteException {
            EmnPluginService.this.sendAnnounce(header, body, action);
        }
    };

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        // ...
        // Do plugin specifics.
        // ...
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // ...
        // Do plugin specifics.
        // ...
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // ...
        // Do plugin specifics.
        // ...
    }

    /**
     * Plugin is just sending notifications.
     */
    protected boolean isSandboxPlugin() {
        return false;
    }

    /**
     * Must be implemented. Starts plugin work, if any.
     */
    protected void startWork() {

        // Check if plugin is enabled.
        if(mSharedPreferences.getBoolean(PluginConstants.PREFERENCES_PLUGIN_ENABLED, false)) {
            // Do stuff.
        }

    }

    /**
     * Must be implemented. Stops plugin work, if any.
     */
    protected void stopWork() {

    }

    /**
     * Must be implemented.
     *
     * PluginService has done connection and registering to the LiveView Service.
     *
     * If needed, do additional actions here, e.g.
     * starting any worker that is needed.
     */
    protected void onServiceConnectedExtended(ComponentName className, IBinder service) {

    }

    /**
     * Must be implemented.
     *
     * PluginService has done disconnection from LiveView and service has been stopped.
     *
     * Do any additional actions here.
     */
    protected void onServiceDisconnectedExtended(ComponentName className) {

    }

    /**
     * Must be implemented.
     *
     * PluginService has checked if plugin has been enabled/disabled.
     *
     * The shared preferences has been changed. Take actions needed.
     */
    protected void onSharedPreferenceChangedExtended(SharedPreferences prefs, String key) {

    }

    /**
     * This method is called by the LiveView application to start the plugin.
     * For sandbox plugins, this means when the user has pressed the action button to start the plugin.
     */
    protected void startPlugin() {
        if (Emn.DEBUG) Log.d(PluginConstants.LOG_TAG, "startPlugin");

        // Check if plugin is enabled.
        if(mSharedPreferences.getBoolean(PluginConstants.PREFERENCES_PLUGIN_ENABLED, false)) {
            // Do.
        }

    }

    /**
     * This method is called by the LiveView application to stop the plugin.
     * For sandbox plugins, this means when the user has long-pressed the action button to stop the plugin.
     */
    protected void stopPlugin() {
        if (Emn.DEBUG) Log.d(PluginConstants.LOG_TAG, "stopPlugin");
        stopWork();
    }

    /**
     * Sandbox mode only. When a user presses any buttons on the LiveView device, this method will be called.
     */
    protected void button(String buttonType, boolean doublepress, boolean longpress) {
        if (Emn.DEBUG) Log.d(PluginConstants.LOG_TAG, "button - type " + buttonType + ", doublepress " + doublepress + ", longpress " + longpress);
    }

    /**
     * Called by the LiveView application to indicate the capabilites of the LiveView device.
     */
    protected void displayCaps(int displayWidthPx, int displayHeigthPx) {
        if (Emn.DEBUG) Log.d(PluginConstants.LOG_TAG, "displayCaps - width " + displayWidthPx + ", height " + displayHeigthPx);
    }

    /**
     * Called by the LiveView application when the plugin has been kicked out by the framework.
     */
    protected void onUnregistered() {
        if (Emn.DEBUG) Log.d(PluginConstants.LOG_TAG, "onUnregistered");
        stopWork();
    }

    private boolean launchInPhone(String packageName, String activityName, String service, String mailbox) {
        boolean result = true;

        Intent intent = new Intent();
        intent.putExtra("service", service);
        intent.putExtra("mailbox", mailbox);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(packageName, activityName);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // インストールされていない
            result = false;
        } catch (SecurityException e) {
            // 旧バージョン
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    /**
     * When a user presses the "open in phone" button on the LiveView device, this method is called.
     * You could e.g. open a browser and go to a specific URL, or open the music player.
     */
    protected void openInPhone(String openInPhoneAction) {
        if (Emn.DEBUG) Log.d(PluginConstants.LOG_TAG, "openInPhone: " + openInPhoneAction);

        // Open
        String[] arr = openInPhoneAction.split(" ", 2);
        if (arr.length == 2) {
            if (!launchInPhone("net.assemble.mailnotify", "net.assemble.mailnotify.EmailNotifyLaunchActivity", arr[0], arr[1])) {
                launchInPhone("net.assemble.emailnotify", "net.assemble.emailnotify.EmailNotifyLaunchActivity", arr[0], arr[1]);
            }
        }
    }

    /**
     * Sandbox mode only. Called by the LiveView application when the screen mode has changed.
     * 0 = screen is off, 1 = screen is on
     */
    protected void screenMode(int mode) {
        if (Emn.DEBUG) Log.d(PluginConstants.LOG_TAG, "screenMode: screen is now " + ((mode == 0) ? "OFF" : "ON"));
    }

    private void sendAnnounce(String header, String body, String action) {
        try {
            if(mLiveViewAdapter != null && mSharedPreferences.getBoolean(PluginConstants.PREFERENCES_PLUGIN_ENABLED, false)) {
                mLiveViewAdapter.sendAnnounce(mPluginId, mMenuIcon, header, body, System.currentTimeMillis(), action);
                if (Emn.DEBUG) Log.d(PluginConstants.LOG_TAG, "Announce sent to LiveView");
            } else {
                Log.w(PluginConstants.LOG_TAG, "LiveView not reachable");
            }
        } catch(Exception e) {
            Log.e(PluginConstants.LOG_TAG, "Failed to send announce", e);
        }
    }

}