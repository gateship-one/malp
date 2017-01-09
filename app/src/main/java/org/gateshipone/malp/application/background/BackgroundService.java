/*
 * Copyright (C) 2016 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/malp/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gateshipone.malp.application.background;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.gateshipone.malp.mpdservice.ConnectionManager;
import org.gateshipone.malp.mpdservice.handlers.MPDConnectionStateChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.MPDStatusChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDCommandHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDStateMonitoringHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.MPDConnection;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFile;
import org.gateshipone.malp.mpdservice.profilemanagement.MPDProfileManager;
import org.gateshipone.malp.mpdservice.profilemanagement.MPDServerProfile;

import java.lang.ref.WeakReference;

public class BackgroundService extends Service {
    private static final String TAG = BackgroundService.class.getSimpleName();
    /**
     * Control actions for the MPD service
     */
    public static final String ACTION_PLAY = "org.gateshipone.malp.widget.play";
    public static final String ACTION_PAUSE = "org.gateshipone.malp.widget.pause";
    public static final String ACTION_STOP = "org.gateshipone.malp.widget.stop";
    public static final String ACTION_NEXT = "org.gateshipone.malp.widget.next";
    public static final String ACTION_PREVIOUS = "org.gateshipone.malp.widget.previous";

    /**
     * Management actions for the MPD service
     */
    /**
     * Requests the service to connect to the MPD server
     */
    public static final String ACTION_CONNECT = "org.gateshipone.malp.widget.connect";
    /**
     * Requests the service to disconnect from the MPD server
     */
    public static final String ACTION_DISCONNECT = "org.gateshipone.malp.widget.disconnect";
    /**
     * Notifies the service that the default profile changed. It will reread the profile
     * database and try to reconnect to the new default profile.
     */
    public static final String ACTION_PROFILE_CHANGED = "org.gateshipone.malp.widget.profile_changed";

    /**
     * Sent if a new song is played by the MPD server. This needs to be catched by the widget provider
     * to show new information.
     */
    public static final String ACTION_TRACK_CHANGED = "org.gateshipone.malp.widget.track_changed";

    /**
     * Sent if the MPD server status changes. This needs to be catched by the widget provider to show
     * correct information.
     */
    public static final String ACTION_STATUS_CHANGED = "org.gateshipone.malp.widget.status_changed";

    /**
     * Sent if the MPD server is disconnected. This needs to be catched by the widget provider to show
     * correct information.
     */
    public static final String ACTION_SERVER_DISCONNECTED = "org.gateshipone.malp.widget.server_disconnected";

    /**
     * Extra attached to an {@link Intent} containing the {@link MPDFile} that is playing on the server
     */
    public static final String INTENT_EXTRA_TRACK = "org.gateshipone.malp.widget.extra.track";

    /**
     * Shows the notification
     */
    public static final String ACTION_SHOW_NOTIFICATION = "org.gateshipone.malp.notification.show";

    /**
     * Hides the notification if it is currently visible
     */
    public static final String ACTION_HIDE_NOTIFICATION = "org.gateshipone.malp.notification.hide";

    /**
     * Notifies the service that the user has dismissed the notification
     */
    public static final String ACTION_QUIT_BACKGROUND_SERVICE = "org.gateshipone.malp.background.quit";

    /**
     * Extra attached to an {@link Intent} containing the current MPD server status
     */
    public static final String INTENT_EXTRA_STATUS = "org.gateshipone.malp.widget.extra.status";

    /**
     * Profile manage instance to get the last used profile out of the SQLite database.
     */
    private MPDProfileManager mProfileManager;

    private BackgroundServiceBroadcastReceiver mBroadcastReceiver;

    private BackgroundMPDStateChangeListener mServerConnectionStateListener;

    private BackgroundMPDStatusChangeListener mServerStatusListener;

    private MPDCurrentStatus mLastStatus;
    private MPDFile mLastTrack;

    /**
     * Manager helper class to handle the notification.
     */
    private NotificationManager mNotificationManager;

    private boolean mConnecting;

    /**
     * No bindable service.
     *
     * @param intent
     * @return Always null. No interface.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        /* Create the broadcast receiver to react on incoming Intent */
        mBroadcastReceiver = new BackgroundServiceBroadcastReceiver();

        /* Only react to certain Actions defined in this service */
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAY);
        filter.addAction(ACTION_PAUSE);
        filter.addAction(ACTION_STOP);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_PREVIOUS);
        filter.addAction(ACTION_CONNECT);
        filter.addAction(ACTION_DISCONNECT);
        filter.addAction(ACTION_PROFILE_CHANGED);
        filter.addAction(ACTION_SHOW_NOTIFICATION);
        filter.addAction(ACTION_HIDE_NOTIFICATION);
        filter.addAction(ACTION_QUIT_BACKGROUND_SERVICE);

        // Register the receiver with the system
        registerReceiver(mBroadcastReceiver, filter);

        // Create MPD callbacks
        mServerStatusListener = new BackgroundMPDStatusChangeListener(this);
        mServerConnectionStateListener = new BackgroundMPDStateChangeListener(this);

        /* Register callback handlers to MPD service handlers */
        MPDCommandHandler.registerConnectionStateListener(mServerConnectionStateListener);
        MPDStateMonitoringHandler.setRefreshInterval(60 * 1000);
        MPDStateMonitoringHandler.registerStatusListener(mServerStatusListener);

        mNotificationManager = new NotificationManager(this);

        // Initialize an ProfileManager to get the default profile.
        mProfileManager = new MPDProfileManager(this);
        // Disable automatic reconnect after connection loss for the widget server
        ConnectionManager.setAutoconnect(false);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);

        /* Unregister MPD service handlers */
        MPDCommandHandler.unregisterConnectionStateListener(mServerConnectionStateListener);
        MPDStateMonitoringHandler.unregisterStatusListener(mServerStatusListener);
        notifyDisconnected();

        Log.v(TAG,"MALP background service destroyed");
        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            // Something wrong here.
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        if ( null != action ) {
            handleAction(action);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onLowMemory () {
        // Disconnect from server gracefully
        onMPDDisconnect();

        // Notify the widgets that the service is disconnected now
        notifyDisconnected();

        stopSelf();
    }

    public void onTaskRemoved(Intent rootIntent) {
        // Disconnect from server gracefully
        onMPDDisconnect();

        // Notify the widgets that the service is disconnected now
        notifyDisconnected();

        //stop service
        stopSelf();
    }

    /**
     * Simple command handling methods
     */


    /**
     * Toggles pause/play
     */
    private void onPlay() {
        checkMPDConnection();
        MPDCommandHandler.togglePause();
    }

    /**
     * Toggles pause/play
     */
    private void onPause() {
        checkMPDConnection();
        MPDCommandHandler.togglePause();
    }

    /**
     * Stops playback
     */
    private void onStop() {
        checkMPDConnection();
        MPDCommandHandler.stop();
    }

    /**
     * Jumps to next song
     */
    private void onNext() {
        checkMPDConnection();
        MPDCommandHandler.nextSong();
    }

    /**
     * Jumps to previous song
     */
    private void onPrevious() {
        checkMPDConnection();
        MPDCommandHandler.previousSong();
    }

    /**
     * Tries to connect to last used server profile
     */
    private void onMPDConnect() {
        // This should open the connection if it is not already open
        checkMPDConnection();

        // Notify about new dummy tracks
        notifyNewStatus(mLastStatus);
        notifyNewTrack(mLastTrack);
    }

    /**
     * Disconnects from MPD server
     */
    private void onMPDDisconnect() {
        MPDCommandHandler.disconnectFromMPDServer();
    }

    /**
     * Disconnects from the current server and rereads the default profile.
     */
    private void onProfileChanged() {
        onMPDDisconnect();
        MPDServerProfile profile = mProfileManager.getAutoconnectProfile();
        ConnectionManager.setParameters(profile, this);
    }

    /**
     * Notifies the widgets that the server is disconnected now.
     */
    private void notifyDisconnected() {
        Intent intent = new Intent(getApplicationContext(), WidgetProvider.class);
        intent.setAction(ACTION_SERVER_DISCONNECTED);
        sendBroadcast(intent);

        // Dismiss the notification on disconnects
        mNotificationManager.hideNotification();
    }

    /**
     * Sends the new {@link MPDFile} to listening broadcast receivers
     * @param track Track to broadcast
     */
    private void notifyNewTrack(MPDFile track) {
        Intent intent = new Intent(getApplicationContext(), WidgetProvider.class);
        intent.setAction(ACTION_TRACK_CHANGED);
        intent.putExtra(INTENT_EXTRA_TRACK, track);
        sendBroadcast(intent);
    }

    /**
     * Sends the new {@link MPDCurrentStatus} to listening broadcast receivers.
     * @param status Status to broadcast
     */
    private void notifyNewStatus(MPDCurrentStatus status) {
        Intent intent = new Intent(getApplicationContext(), WidgetProvider.class);
        intent.setAction(ACTION_STATUS_CHANGED);
        intent.putExtra(INTENT_EXTRA_STATUS, status);
        sendBroadcast(intent);
    }

    /**
     * Ensures an MPD server is connected before performing an action.
     */
    private void checkMPDConnection() {
        if (!MPDConnection.getInstance().isConnected() && !mConnecting) {
            mLastTrack = new MPDFile("");
            mLastStatus = new MPDCurrentStatus();
            connectMPDServer();
        }
    }

    /**
     * Gets last used server profile and then tries to connect to it.
     * As SQLite should be safe to call from different processes, this process
     * should be able to see changes to the profile database instantaneously.
     */
    private void connectMPDServer() {
        mConnecting = true;

        MPDServerProfile profile = mProfileManager.getAutoconnectProfile();
        ConnectionManager.setParameters(profile, this);

        /* Open the actual server connection */
        ConnectionManager.reconnectLastServer(this);
    }

    /**
     * Calls the desired action methods
     * @param action Action received via an {@link Intent}
     */
    private void handleAction(String action) {
        Log.v(TAG,"Action: " + action);
        if (action.equals(ACTION_PLAY)) {
            onPlay();
        } else if (action.equals(ACTION_PAUSE)) {
            onPause();
        } else if (action.equals(ACTION_STOP)) {
            onStop();
        } else if (action.equals(ACTION_PREVIOUS)) {
            onPrevious();
        } else if (action.equals(ACTION_NEXT)) {
            onNext();
        } else if (action.equals(ACTION_CONNECT)) {
            onMPDConnect();
        } else if (action.equals(ACTION_DISCONNECT)) {
            onMPDDisconnect();
        } else if (action.equals(ACTION_PROFILE_CHANGED)) {
            onProfileChanged();
        } else if (action.equals(ACTION_SHOW_NOTIFICATION)) {
            checkMPDConnection();
            mNotificationManager.showNotification();
        } else if (action.equals(ACTION_HIDE_NOTIFICATION)) {
            mNotificationManager.hideNotification();
        } else if (action.equals(ACTION_QUIT_BACKGROUND_SERVICE)) {
            // Just disconnect from the server. Everything else happens when the connection is disconnected.
            onMPDDisconnect();
            mNotificationManager.hideNotification();
        }
    }

    /**
     * Broadcast receiver subclass to handle broadcasts emitted from the widgetprovider to control
     * MPD and this service.
     */
    private class BackgroundServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (null == intent) {
                return;
            }
            String action = intent.getAction();
            if ( null != action ) {
                handleAction(action);
            }
        }
    }

    /**
     * Private class to react to changes in MPDs connection state changes.
     */
    private static class BackgroundMPDStateChangeListener extends MPDConnectionStateChangeHandler {
        WeakReference<BackgroundService> mService;

        BackgroundMPDStateChangeListener(BackgroundService service) {
            mService = new WeakReference<BackgroundService>(service);
        }

        @Override
        public void onConnected() {
            mService.get().mConnecting = false;
        }

        /**
         * Hide notification on disconnect and stop the service.
         */
        @Override
        public void onDisconnected() {
            Log.v(TAG,"Disconnected");
            mService.get().mConnecting = false;
            mService.get().notifyDisconnected();
            mService.get().mNotificationManager.hideNotification();
            mService.get().stopSelf();
        }
    }

    /**
     * Private class to handle changes in MPDs status or playing track.
     */
    private static class BackgroundMPDStatusChangeListener extends MPDStatusChangeHandler {
        WeakReference<BackgroundService> mService;

        BackgroundMPDStatusChangeListener(BackgroundService service) {
            mService = new WeakReference<BackgroundService>(service);
        }

        @Override
        protected void onNewStatusReady(MPDCurrentStatus status) {
            mService.get().mLastStatus = status;
            mService.get().notifyNewStatus(status);
            mService.get().mNotificationManager.setMPDStatus(status);
        }

        @Override
        protected void onNewTrackReady(MPDFile track) {
            mService.get().notifyNewTrack(track);
            mService.get().mLastTrack = track;
            mService.get().mNotificationManager.setMPDFile(track);
        }
    }
}
