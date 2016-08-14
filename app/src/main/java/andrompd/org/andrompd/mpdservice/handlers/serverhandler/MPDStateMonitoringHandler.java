/*
 * Copyright (C) 2016  Hendrik Borghorst
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package andrompd.org.andrompd.mpdservice.handlers.serverhandler;


import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import andrompd.org.andrompd.mpdservice.handlers.MPDConnectionStateChangeHandler;
import andrompd.org.andrompd.mpdservice.handlers.MPDStatusChangeHandler;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseHandler;
import andrompd.org.andrompd.mpdservice.mpdprotocol.MPDConnection;
import andrompd.org.andrompd.mpdservice.mpdprotocol.MPDCurrentStatus;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDFile;

public class MPDStateMonitoringHandler extends MPDGenericHandler implements MPDConnection.MPDConnectionIdleChangeListener {
    private static final String THREAD_NAME = "MPDStatusHandler";
    private static final String TAG = "StateMonitoring";


    /**
     * Time to idle before resyncing the state with the MPD host (30 seconds).
     */
    private static final int IDLE_TIME = 30 * 1000;

    /**
     * Time used to send interpolated states to the listener (1 second)
     */
    private static final int INTERPOLATE_INTERVAL = 1 * 1000;


    private static HandlerThread mHandlerThread = null;
    private static MPDStateMonitoringHandler mHandlerSingleton = null;

    /**
     * Callback handler for the GUI to get notified on updates
     */
    private static ArrayList<MPDStatusChangeHandler> mStatusListeners;

    /**
     * Timer used to periodically resync the state with the mpd server between interpolating
     * the time values.
     */
    private Timer mResyncTimer = null;

    private Timer mInterpolateTimer = null;

    /**
     * Last unix time used to interpolate the time as accurate as possible
     */
    private Long mLastTimeBase;

    /**
     * Used to check if a new file is playing
     */
    private MPDFile mLastFile;

    private MPDCurrentStatus mLastStatus;

    /**
     * Private constructor for use in singleton.
     *
     * @param looper Looper of a HandlerThread (that is NOT the UI thread)
     */
    private MPDStateMonitoringHandler(Looper looper) {
        super(looper);
    }

    /**
     * Private method to ensure that the singleton runs in a separate thread.
     * Otherwise android will deny network access because of UI blocks.
     *
     * @return
     */
    private synchronized static MPDStateMonitoringHandler getHandler() {
        if (null == mHandlerSingleton) {
            Log.v(TAG, "Creating singleton");
            mHandlerThread = new HandlerThread(THREAD_NAME);
            mHandlerThread.start();
            mHandlerSingleton = new MPDStateMonitoringHandler(mHandlerThread.getLooper());

            mStatusListeners = new ArrayList<>();

            mHandlerSingleton.mMPDConnection.setpIdleListener(mHandlerSingleton);

            mHandlerSingleton.mLastStatus = new MPDCurrentStatus();
        }
        return mHandlerSingleton;
    }

    /**
     * This is the main entry point of messages.
     * Here all possible messages types need to be handled with the MPDConnection.
     *
     * @param msg Message to process.
     */
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        if (!(msg.obj instanceof MPDHandlerAction)) {
            /* Check if the message object is of correct type. Otherwise just abort here. */
            return;
        }

        MPDHandlerAction mpdAction = (MPDHandlerAction) msg.obj;
        /* Catch MPD exceptions here for now. */
        MPDResponseHandler responseHandler;
        MPDHandlerAction.NET_HANDLER_ACTION action = mpdAction.getAction();


    }

    /**
     * Set the server parameters for the connection. MUST be called before trying to
     * initiate a connection because it will fail otherwise.
     *
     * @param hostname Hostname or ip address to connect to.
     * @param password Password that is used to authenticate with the server. Can be left empty.
     * @param port     Port to use for the connection. (Default: 6600)
     */
    public static void setServerParameters(String hostname, String password, int port) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_SERVER_PARAMETERS);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SERVER_HOSTNAME, hostname);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SERVER_PASSWORD, password);
        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SERVER_PORT, port);
        msg.obj = action;
        MPDStateMonitoringHandler.getHandler().sendMessage(msg);
    }

    /**
     * Connect to the previously configured MPD server.
     */
    public static void connectToMPDServer() {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_CONNECT_MPD_SERVER);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }
        msg.obj = action;
        MPDStateMonitoringHandler.getHandler().sendMessage(msg);
    }

    /**
     * Disconnect to the previously connected MPD server.
     */
    public static void disconnectFromMPDServer() {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_DISCONNECT_MPD_SERVER);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }
        msg.obj = action;
        MPDStateMonitoringHandler.getHandler().sendMessage(msg);
    }

    private void resyncState() {
        synchronized (this) {
            Log.v(TAG, "Resyncing MPD state");

            // Stop the interpolation
            if (null != mInterpolateTimer) {
                mInterpolateTimer.cancel();
                mInterpolateTimer.purge();
                mInterpolateTimer = null;
            }

            // If a resync timer is running kill it also. It will be restarted when idling again

            if (null != mResyncTimer) {
                mResyncTimer.cancel();
                mResyncTimer.purge();
                mResyncTimer = null;
            }
        }
        mLastTimeBase = System.nanoTime();
        try {
            MPDCurrentStatus status = mMPDConnection.getCurrentServerStatus();

            if (status.getCurrentSongIndex() != mLastStatus.getCurrentSongIndex()) {
                // New track started playing. Get it and inform the listener.
                mLastFile = mMPDConnection.getCurrentSong();
                distributeNewTrack(mLastFile);
            }

            mLastStatus = status;
            distributeNewStatus(status);

            startInterpolation();
        } catch (IOException e) {
            // FIXME
            e.printStackTrace();
        }

    }

    private void interpolateState() {
        // Generate a new dummy state
        if (null != mLastStatus) {
            MPDCurrentStatus status = new MPDCurrentStatus(mLastStatus);
            long timeDiff = (System.nanoTime() - mLastTimeBase) / (1000 * 1000 * 1000);

            // FIXME move timestamp to MPDConnection and MPDCurrentStatus (more precise, less time until saved)

            status.setElapsedTime(mLastStatus.getElapsedTime() + (int) timeDiff);
            distributeNewStatus(status);
        }
    }

    private synchronized void startInterpolation() {
        if (mLastStatus.getPlaybackState() == MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PLAYING) {
            if (null != mInterpolateTimer) {
                mInterpolateTimer.cancel();
                mInterpolateTimer.purge();
                mInterpolateTimer = null;
            }
            mInterpolateTimer = new Timer();

            mInterpolateTimer.schedule(new InterpolateTask(), 0, INTERPOLATE_INTERVAL);
        }

        if (null != mResyncTimer) {
            mResyncTimer.cancel();
            mResyncTimer.purge();
            mResyncTimer = null;
        }
        mResyncTimer = new Timer();
        mResyncTimer.schedule(new ResyncTask(), IDLE_TIME);
        Log.v(TAG, "Resyncing state in: " + IDLE_TIME + " ms");

    }

    public static MPDCurrentStatus getLastStatus() {
        return getHandler().mLastStatus;
    }

    public static void registerConnectionStateListener(MPDConnectionStateChangeHandler stateHandler) {
        mHandlerSingleton.internalRegisterConnectionStateListener(stateHandler);
    }

    public static void unregisterConnectionStateListener(MPDConnectionStateChangeHandler stateHandler) {
        mHandlerSingleton.internalUnregisterConnectionStateListener(stateHandler);
    }


    public static void registerStatusListener(MPDStatusChangeHandler handler) {
        if (null != handler) {
            getHandler().mStatusListeners.add(handler);
        }
    }

    public static void unregisterStatusListener(MPDStatusChangeHandler handler) {
        if (null != handler) {
            getHandler().mStatusListeners.remove(handler);
        }
    }


    private void distributeNewStatus(MPDCurrentStatus status) {
        //Log.v(TAG, "Distribute status: " + status.printStatus());
        for (MPDStatusChangeHandler handler : mStatusListeners) {
            handler.newMPDStatusReady(status);
        }
    }

    private void distributeNewTrack(MPDFile track) {
        for (MPDStatusChangeHandler handler : mStatusListeners) {
            handler.newMPDTrackReady(track);
        }
    }

    @Override
    public void onConnected() {
        super.onConnected();
        Log.v(TAG, "Connected to a MPD host");
        resyncState();
    }

    @Override
    public void onDisconnected() {
        super.onDisconnected();
        synchronized (this) {
            // Stop the interpolation
            if (null != mInterpolateTimer) {
                mInterpolateTimer.cancel();
                mInterpolateTimer = null;
            }

            // Stop the resync timeout timer
            if (null != mResyncTimer) {
                mResyncTimer.cancel();
                mResyncTimer = null;
            }
        }
    }

    @Override
    public void onIdle() {

    }

    @Override
    public void onNonIdle() {
        Log.v(TAG, "Server idle over");
        // Server idle is over (reason unclear), resync the state
        resyncState();
    }


    private class ResyncTask extends TimerTask {

        @Override
        public void run() {
            resyncState();
        }
    }

    private class InterpolateTask extends TimerTask {

        @Override
        public void run() {
            interpolateState();
        }
    }
}
