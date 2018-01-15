/*
 *  Copyright (C) 2018 Team Gateship-One
 *  (Hendrik Borghorst & Frederik Luetkes)
 *
 *  The AUTHORS.md file contains a detailed contributors list:
 *  <https://github.com/gateship-one/malp/blob/master/AUTHORS.md>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.malp.application.background;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.support.annotation.Nullable;
import android.util.Log;

import org.gateshipone.malp.mpdservice.ConnectionManager;
import org.gateshipone.malp.mpdservice.handlers.MPDConnectionStateChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.MPDStatusChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDCommandHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDStateMonitoringHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.MPDInterface;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;
import org.gateshipone.malp.mpdservice.profilemanagement.MPDProfileManager;
import org.gateshipone.malp.mpdservice.profilemanagement.MPDServerProfile;

import java.lang.ref.WeakReference;

public class BackgroundService extends Service implements AudioManager.OnAudioFocusChangeListener {
    private static final String TAG = BackgroundService.class.getSimpleName();

    public enum STREAMING_STATUS {
        STOPPED,
        BUFFERING,
        PLAYING
    }

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
     * Extra attached to an {@link Intent} containing the {@link MPDTrack} that is playing on the server
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

    public static final String ACTION_START_MPD_STREAM_PLAYBACK = "org.gateshipone.malp.stream.play";

    /**
     * Extra attached to an {@link Intent} containing the current MPD server status
     */
    public static final String INTENT_EXTRA_STATUS = "org.gateshipone.malp.widget.extra.status";

    /**
     * Notification about a change in status of remote stream playback on the android device.
     */
    public static final String ACTION_STREAMING_STATUS_CHANGED = "org.gateshipone.malp.streaming.status_changed";

    /**
     * Contains the status as an STREAMING_STATUS enum value.
     */
    public static final String INTENT_EXTRA_STREAMING_STATUS = "org.gateshipone.malp.streaming.extra.status";

    private boolean mIsDucked = false;

    private boolean mLostAudioFocus = false;


    private BackgroundServiceBroadcastReceiver mBroadcastReceiver;

    private BackgroundMPDStateChangeListener mServerConnectionStateListener;

    private BackgroundMPDStatusChangeListener mServerStatusListener;

    private MPDCurrentStatus mLastStatus;
    private MPDTrack mLastTrack;

    /**
     * Manager helper class to handle the notification.
     */
    private NotificationManager mNotificationManager;

    /**
     * Set if currently connecting to a MPD server
     */
    private boolean mConnecting;

    /**
     * Playback manager for stream playback. Only instantiated if necessary.
     */
    private StreamPlaybackManager mPlaybackManager;

    /**
     * Set if the notification would normally not be visible but is because of streaming.
     */
    private boolean mNotificationHidden = true;

    /**
     * Set if streaming was active. This is used to automatically resume streaming after
     * stop or pause mode.
     */
    private boolean mWasStreaming = false;

    /**
     * Handler that handles requests from the binded connection. This ensures that playback/control
     * is handled in the right thread.
     */
    private BackgroundServiceHandler mHandler;

    /**
     * No bindable service.
     *
     * @param intent
     * @return Always null. No interface.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new BackgroundServiceInterface(this);
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
        filter.addAction(ACTION_START_MPD_STREAM_PLAYBACK);
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        // Register the receiver with the system
        registerReceiver(mBroadcastReceiver, filter);

        // Create handler for service binding
        mHandler = new BackgroundServiceHandler(this);

        // Create MPD callbacks
        mServerStatusListener = new BackgroundMPDStatusChangeListener(this);
        mServerConnectionStateListener = new BackgroundMPDStateChangeListener(this, getMainLooper());

        /* Register callback handlers to MPD service handlers */
        MPDInterface.mInstance.addMPDConnectionStateChangeListener(mServerConnectionStateListener);
        MPDStateMonitoringHandler.getHandler().setRefreshInterval(60 * 1000);
        MPDStateMonitoringHandler.getHandler().registerStatusListener(mServerStatusListener);

        mNotificationManager = new NotificationManager(this);

        // Create empty status
        mLastStatus = new MPDCurrentStatus();

        // Disable automatic reconnect after connection loss for the widget server
        ConnectionManager.getInstance(getApplicationContext()).setAutoconnect(false);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);

        /* Unregister MPD service handlers */
        MPDInterface.mInstance.removeMPDConnectionStateChangeListener(mServerConnectionStateListener);
        MPDStateMonitoringHandler.getHandler().unregisterStatusListener(mServerStatusListener);
        notifyDisconnected();

        mNotificationManager.hideNotification();

        Log.v(TAG, "MALP background service destroyed");
        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.v(TAG, "Null intend in onStartCommand");
            shutdownService();
            // Something wrong here.
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        Log.v(TAG, "onStartCommand: " + action);
        if (null != action) {
            handleAction(action);
        }

        Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
        return START_NOT_STICKY;
    }

    @Override
    public void onLowMemory() {
        Log.v(TAG, "onLowMemory");
        // Disconnect from server gracefully
        onMPDDisconnect();

        shutdownService();
    }

    public void onTaskRemoved(Intent rootIntent) {
        Log.v(TAG, "onTaskRemoved");
        // Disconnect from server gracefully
        onMPDDisconnect();

        shutdownService();
    }

    private void shutdownService() {
        Log.v(TAG, "shutdownService");
        notifyDisconnected();
        mNotificationManager.hideNotification();

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

    public void onStreamPlaybackStart() {
        mWasStreaming = true;
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
        MPDServerProfile profile = MPDProfileManager.getInstance(this).getAutoconnectProfile();
        ConnectionManager.getInstance(getApplicationContext()).setParameters(profile, this);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if(null == mPlaybackManager) {
            // No playback is running. Ignore!
            return;
        }

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // If we are ducked at the moment, return volume to full output
                if (mIsDucked) {
                    mPlaybackManager.setVolume(1.0f);
                    mIsDucked = false;
                } else if (mLostAudioFocus) {
                    // If we temporarily lost the audio focus we can resume playback here
                    startStreamingPlayback();
                    mLostAudioFocus = false;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Stop playback here, because we lost audio focus (not temporarily)
                if (mPlaybackManager.isPlaying()) {
                    stopStreamingPlayback();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Pause audio for the moment of focus loss, will be resumed.
                if (mPlaybackManager.isPlaying()) {
                    stopStreamingPlayback();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // We only need to duck our volume, so set it to 10%? and save that we ducked for resuming
                if (mPlaybackManager.isPlaying()) {
                    mPlaybackManager.setVolume(0.1f);
                    mIsDucked = true;
                }
                break;
            default:
                break;
        }
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
     * Sends the new {@link MPDTrack} to listening broadcast receivers
     *
     * @param track Track to broadcast
     */
    private void notifyNewTrack(MPDTrack track) {
        Intent intent = new Intent(getApplicationContext(), WidgetProvider.class);
        intent.setAction(ACTION_TRACK_CHANGED);
        intent.putExtra(INTENT_EXTRA_TRACK, track);
        sendBroadcast(intent);
    }

    /**
     * Sends the new {@link MPDCurrentStatus} to listening broadcast receivers.
     *
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
        if (!MPDInterface.mInstance.isConnected() && !mConnecting) {
            mNotificationManager.showNotification();
            mLastTrack = new MPDTrack("");
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

        MPDServerProfile profile = MPDProfileManager.getInstance(this).getAutoconnectProfile();
        ConnectionManager.getInstance(getApplicationContext()).setParameters(profile, this);

        /* Open the actual server connection */
        ConnectionManager.getInstance(getApplicationContext()).reconnectLastServer(this);
    }

    /**
     * Calls the desired action methods
     *
     * @param action Action received via an {@link Intent}
     */
    private void handleAction(String action) {
        Log.v(TAG, "Action: " + action);
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
            mNotificationHidden = false;
            checkMPDConnection();
            mNotificationManager.showNotification();
        } else if (action.equals(ACTION_HIDE_NOTIFICATION)) {
            // Only hide notification if no playback is active
            if (mPlaybackManager == null || !mPlaybackManager.isPlaying()) {
                mNotificationManager.hideNotification();
            }
            mNotificationHidden = true;
        } else if (action.equals(ACTION_QUIT_BACKGROUND_SERVICE)) {
            // Only quit service if no playback is active
            if (mPlaybackManager == null || !mPlaybackManager.isPlaying()) {
                // Just disconnect from the server. Everything else happens when the connection is disconnected.
                onMPDDisconnect();
                // FIXME timeout?
            }
            mNotificationHidden = true;
        } else if (action.equals(ACTION_START_MPD_STREAM_PLAYBACK)) {
            startStreamingPlayback();
        } else if (action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
            stopStreamingPlayback();
        }
    }

    public void startStreamingPlayback() {
        if (mPlaybackManager == null) {
            mPlaybackManager = new StreamPlaybackManager(this);
        } else if (mPlaybackManager.isPlaying()) {
            return;
        }

        // Request audio focus before doing anything
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            // Abort command if audio focus was not granted
            return;
        }

        /*
         * Make sure service is "started" so android doesn't handle it as a
         * "bound service"
         */
        Intent serviceStartIntent = new Intent(this, BackgroundService.class);
        serviceStartIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);
        startService(serviceStartIntent);

        String url = MPDProfileManager.getInstance(this).getAutoconnectProfile().getStreamingURL();
        Log.v(TAG, "Start playback of: " + url);

        // Connect to MPD server for controls
        checkMPDConnection();
        mNotificationManager.showNotification();
        mNotificationManager.setDismissible(false);


        mPlaybackManager.playURL(url);
    }

    public void stopStreamingPlayback() {
        Log.v(TAG, "Stop stream playback");
        if (mPlaybackManager != null && mPlaybackManager.isPlaying()) {
            mPlaybackManager.stop();
        }
        // Enable the notification to swipe away
        mNotificationManager.setDismissible(true);

        // Notification was only visible because of stream playback but main UI is visible, so
        // hide it again.
        if (mNotificationHidden) {
            mNotificationManager.hideNotification();
            onMPDDisconnect();
        }
    }

    public BackgroundServiceHandler getHandler() {
        return mHandler;
    }

    public int getStreamingStatus() {
        if (null == mPlaybackManager) {
            return STREAMING_STATUS.STOPPED.ordinal();
        } else {
            return mPlaybackManager.getStreamingStatus().ordinal();
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
            if (null != action) {
                handleAction(action);
            }
        }
    }

    /**
     * Private class to react to changes in MPDs connection state changes.
     */
    private static class BackgroundMPDStateChangeListener extends MPDConnectionStateChangeHandler {
        WeakReference<BackgroundService> mService;

        BackgroundMPDStateChangeListener(BackgroundService service, Looper looper) {
            super(looper);
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
            Log.v(TAG, "Disconnected");
            mService.get().mConnecting = false;
            mService.get().shutdownService();

            if (mService.get().mPlaybackManager != null && mService.get().mPlaybackManager.isPlaying()) {
                mService.get().mPlaybackManager.stop();
            }
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
            if (mService.get().mLastStatus.getPlaybackState() != status.getPlaybackState()) {
                if (status.getPlaybackState() == MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PLAYING && mService.get().mWasStreaming) {
                    mService.get().startStreamingPlayback();
                } else if (mService.get().mWasStreaming) {
                    mService.get().stopStreamingPlayback();
                }
            }

            mService.get().mLastStatus = status;
            mService.get().notifyNewStatus(status);
            mService.get().mNotificationManager.setMPDStatus(status);
        }

        @Override
        protected void onNewTrackReady(MPDTrack track) {
            mService.get().notifyNewTrack(track);
            mService.get().mLastTrack = track;
            mService.get().mNotificationManager.setMPDFile(track);
        }
    }
}
