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

package org.gateshipone.malp.mpdservice;

import android.content.Context;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import org.gateshipone.malp.mpdservice.handlers.MPDConnectionStateChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDCommandHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDStateMonitoringHandler;
import org.gateshipone.malp.mpdservice.profilemanagement.MPDProfileManager;
import org.gateshipone.malp.mpdservice.profilemanagement.MPDServerProfile;

/**
 * Simple class that manages the three MPD Connections (Queries, State monitoring, Commands)
 */
public class ConnectionManager extends MPDConnectionStateChangeHandler {
    private static final String TAG = ConnectionManager.class.getSimpleName();

    /**
     * Short time to wait for reconnect
     */
    private static final int SHORT_RECONNECT_TIME = 10 * 1000;

    /**
     * Long time to wait for reconnect
     */
    private static final int LONG_RECONNECT_TIME = 5 * 60 * 1000;

    private static final int SHORT_RECONNECT_TRIES = 5;

    private String mHostname;
    private String mPassword;
    private int mPort;

    private boolean mConnected;
    private boolean mDisconnectRequested;

    private Timer mReconnectTimer;
    private int mReconnectCounter;

    private static ConnectionManager mConnectionManager = null;

    private ConnectionManager() {
        MPDStateMonitoringHandler.registerConnectionStateListener(this);
        mHostname = null;
        mPassword = null;
    }

    private static ConnectionManager getInstance() {
        if (null == mConnectionManager) {
            mConnectionManager = new ConnectionManager();
        }
        return mConnectionManager;
    }


    public static void setParameters(MPDServerProfile profile, Context context) {
        if ( null == profile ) {
            return;
        }
        getInstance().mHostname = profile.getHostname();
        getInstance().mPassword = profile.getPassword();
        getInstance().mPort = profile.getPort();

        MPDProfileManager profileManager = new MPDProfileManager(context);
        profileManager.deleteProfile(profile);
        profile.setAutoconnect(true);
        profileManager.addProfile(profile);

        String hostname = getInstance().mHostname;
        String password = getInstance().mPassword;
        int port = getInstance().mPort;

        MPDStateMonitoringHandler.setServerParameters(hostname, password, port);
        MPDQueryHandler.setServerParameters(hostname, password, port);
        MPDCommandHandler.setServerParameters(hostname, password, port);
    }

    public static void reconnectLastServer(Context context) {
        ConnectionManager instance = getInstance();

        if (instance.mHostname == null  && null != context) {
            // Not connected so far
            autoConnect(context);
        }

        instance.mDisconnectRequested = false;

        MPDStateMonitoringHandler.connectToMPDServer();

        MPDQueryHandler.connectToMPDServer();

        MPDCommandHandler.connectToMPDServer();
    }

    public static void disconnectFromServer() {
        getInstance().mDisconnectRequested = true;

        MPDStateMonitoringHandler.disconnectFromMPDServer();
        MPDQueryHandler.disconnectFromMPDServer();
        MPDCommandHandler.disconnectFromMPDServer();
    }

    public static void autoConnect(Context context) {
        MPDProfileManager profileManager = new MPDProfileManager(context);

        setParameters(profileManager.getAutoconnectProfile(),context);
    }


    @Override
    public void onConnected() {
        mConnected = true;

        mReconnectCounter = 0;
        mDisconnectRequested = false;

        if (null != mReconnectTimer) {
            mReconnectTimer.cancel();
            mReconnectTimer.purge();
            mReconnectTimer = null;
        }
    }

    @Override
    public void onDisconnected() {
        if ( !mDisconnectRequested ) {
            if (null != mReconnectTimer) {
                mReconnectTimer.cancel();
                mReconnectTimer.purge();
                mReconnectTimer = null;
            }
            mReconnectTimer = new Timer();
            if ( mReconnectCounter <= SHORT_RECONNECT_TRIES ) {
                mReconnectTimer.schedule(new ReconnectTask(), SHORT_RECONNECT_TIME);
            } else {
                mReconnectTimer.schedule(new ReconnectTask(), LONG_RECONNECT_TIME);
            }
        }
    }

    // FIXME create timertask

    private class ReconnectTask extends TimerTask {

        @Override
        public void run() {
            // Increase connection try counter
            mReconnectCounter++;
            reconnectLastServer(null);
        }
    }
}
