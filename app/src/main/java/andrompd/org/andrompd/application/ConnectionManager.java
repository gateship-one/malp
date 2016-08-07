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

import android.util.Log;

import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDCommandHandler;
import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDQueryHandler;
import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDStateMonitoringHandler;
import andrompd.org.andrompd.mpdservice.profilemanagement.MPDProfileManager;
import andrompd.org.andrompd.mpdservice.profilemanagement.MPDServerProfile;

/**
 * Simple class that manages the three MPD Connections (Queries, State monitoring, Commands)
 */
public class ConnectionManager {
    private static final String TAG = "ConnectionManager";

    private String mHostname;
    private String mPassword;
    private int mPort;

    private static ConnectionManager mConnectionManager = null;

    private ConnectionManager() {
    }

    private static ConnectionManager getInstance() {
        if (null == mConnectionManager) {
            mConnectionManager = new ConnectionManager();
        }
        return mConnectionManager;
    }

    public static void setParameters(String hostname, String password, int port) {

        Log.v(TAG, "Connection to: " + hostname + ':' + String.valueOf(port));


        getInstance().mHostname = hostname;
        getInstance().mPassword = password;
        getInstance().mPort = port;
    }

    public static void reconnectLastServer() {
        ConnectionManager instance = getInstance();

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

        MPDStateMonitoringHandler.disconnectFromMPDServer();
        MPDQueryHandler.disconnectFromMPDServer();
        MPDCommandHandler.disconnectFromMPDServer();
    }
}
