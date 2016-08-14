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

package andrompd.org.andrompd.application;

import android.content.Context;
import android.util.Log;

import java.util.Timer;

import andrompd.org.andrompd.mpdservice.handlers.MPDConnectionStateChangeHandler;
import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDCommandHandler;
import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDQueryHandler;
import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDStateMonitoringHandler;
import andrompd.org.andrompd.mpdservice.profilemanagement.MPDProfileManager;
import andrompd.org.andrompd.mpdservice.profilemanagement.MPDServerProfile;

/**
 * Simple class that manages the three MPD Connections (Queries, State monitoring, Commands)
 */
public class ConnectionManager extends MPDConnectionStateChangeHandler {
    private static final String TAG = "ConnectionManager";

    /**
     * Short time to wait for reconnect
     */
    private static final int SHORT_RECONNECT_TIME = 10 * 1000;

    /**
     * Long time to wait for reconnect
     */
    private static final int LONG_RECONNECT_TIME = 5 * 60 * 1000;

    private String mHostname;
    private String mPassword;
    private int mPort;

    private boolean mConnected;
    private boolean mDisconnectRequested;

    private Timer mReconnectTimer;
    private int mReconnectCounter;

    private static ConnectionManager mConnectionManager = null;

    private ConnectionManager() {
    }

    private static ConnectionManager getInstance() {
        if (null == mConnectionManager) {
            mConnectionManager = new ConnectionManager();
        }
        return mConnectionManager;
    }


    public static void setParameters(MPDServerProfile profile, Context context) {

        Log.v(TAG, "Connection to: " + profile.getHostname() + ':' + String.valueOf(profile.getPort()));


        getInstance().mHostname = profile.getHostname();
        getInstance().mPassword = profile.getPassword();
        getInstance().mPort = profile.getPort();

        MPDProfileManager profileManager = new MPDProfileManager(context);
        profileManager.deleteProfile(profile);
        profile.setAutoconnect(true);
        profileManager.addProfile(profile);
    }

    public static void reconnectLastServer() {
        ConnectionManager instance = getInstance();

        instance.mDisconnectRequested = false;

        String hostname = instance.mHostname;
        String password = instance.mPassword;
        int port = instance.mPort;


        MPDStateMonitoringHandler.setServerParameters(hostname, password, port);
        MPDStateMonitoringHandler.connectToMPDServer();

        MPDQueryHandler.setServerParameters(hostname, password, port);
        MPDQueryHandler.connectToMPDServer();

        MPDCommandHandler.setServerParameters(hostname, password, port);
        MPDCommandHandler.connectToMPDServer();
    }

    public static void disconnectFromServer() {
        Log.v(TAG,"Disconnecting from server");

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
    }

    @Override
    public void onDisconnected() {
        if ( !mDisconnectRequested ) {
            if ( mReconnectCounter <= 3 ) {
                // FIXME, start timer short
            } else {
                // FIXME start timer long
            }
        }
    }

    // FIXME create timertask
}
