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

package org.gateshipone.malp.mpdservice.handlers.serverhandler;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;

import org.gateshipone.malp.mpdservice.handlers.MPDConnectionErrorHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.MPDException;
import org.gateshipone.malp.mpdservice.mpdprotocol.MPDInterface;

/**
 * This class is a base class for all derived handlers that talk to the MPD server.
 * There are three handlers for different tasks.
 * <p/>
 * - MPDCommandHandler - Issues all playback commands to the mpd server which are handled instantly.
 * - For example: play,pause,stop ...
 * <p/>
 * - MPDQueryHandler   - Handles all requests to the database and other state objects of the MPD server.
 * - For example: Album, artist, playlists, output list ...
 * - MPDStateMonitoringHandler - Monitors the state and notifies connected callback listeners in a certain
 * interval about changes in state. It uses the idle state of the MPDConnection
 * as a notification about changes at the server side.
 * <p/>
 * All handlers are static singletons and run in a new spawned thread. You can get the singleton via
 * an static method. Each handler has one MPDConnection object, that is used for talking to the mpd server.
 */
public abstract class MPDGenericHandler extends Handler {
    private static final String TAG = MPDGenericHandler.class.getSimpleName();


    /**
     * List of handlers that get notified when there is a change of state for the network connection
     * (connected, disconnected)
     */

    protected final ArrayList<MPDConnectionErrorHandler> mErrorListeners;

    /**
     * Protected constructor that has to be called from subclasses. If it is not called
     * the MPDConnection objcet is not ready for use and the class will cause a crash.
     *
     * @param looper Looper that is used by this handler. Needs to be created by the
     *               subclass otherwise the network communication is not handled in a separate thread.
     */
    protected MPDGenericHandler(Looper looper) {
        super(looper);
        mErrorListeners = new ArrayList<>();

        // Register all handlers as StateObservers with the MPDConnection. This ensures that all subclasses
        // will get a notification about connection state changes.

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

        // Check if the attached object is of the correct type.
        if (!(msg.obj instanceof MPDHandlerAction)) {
            /* Check if the message object is of correct type. Otherwise just abort here. */
            return;
        }

        // Cast the message object to the MPDHandlerAction class to use its properties.
        MPDHandlerAction mpdAction = (MPDHandlerAction) msg.obj;

        /* Catch MPD exceptions here for now. */

        // Get the set action for this message object and handle it accordingly.
        // Allowed actions are defined in the MPDHandlerAction class.

        // This class only handles base actions as more defined actions are only handled
        // in the subclasses.
        // Usually the pattern of handling is the same. Get the necessary extras out of the Action objcet
        // and then call the mMPDConnection with the correct method
        MPDHandlerAction.NET_HANDLER_ACTION action = mpdAction.getAction();
        // Set the server parameters (hostname, password, port)
        if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_SERVER_PARAMETERS) {
            /* Parse message objects extras */
            // Get the message extras
            String hostname = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SERVER_HOSTNAME);
            String password = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SERVER_PASSWORD);
            Integer port = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SERVER_PORT);
            if ((null == hostname) || (null == port)) {
                return;
            }
            MPDInterface.mInstance.setServerParameters(hostname, password, port);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_CONNECT_MPD_SERVER) {
            // Connect to the mpd server. Server parameters have to be set before.
            try {
                MPDInterface.mInstance.connect();
            } catch (MPDException e) {
                handleMPDError(e);
            }

        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_DISCONNECT_MPD_SERVER) {
            // Disconnect from the mpd server.
            MPDInterface.mInstance.disconnect();
        }
    }


    public void addErrorListener(MPDConnectionErrorHandler errorListener) {
        synchronized (mErrorListeners) {
            mErrorListeners.add(errorListener);
        }
    }

    public void removeErrorListener(MPDConnectionErrorHandler errorListener) {
        synchronized (mErrorListeners) {
            mErrorListeners.remove(errorListener);
        }
    }


    /**
     * Set the server parameters for the connection. MUST be called before trying to
     * initiate a connection because it will fail otherwise.
     *
     * @param hostname Hostname or ip address to connect to.
     * @param password Password that is used to authenticate with the server. Can be left empty.
     * @param port     Port to use for the connection. (Default: 6600)
     */
    public void setServerParameters(String hostname, String password, int port) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_SERVER_PARAMETERS);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SERVER_HOSTNAME, hostname);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SERVER_PASSWORD, password);
        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SERVER_PORT, port);
        msg.obj = action;
        sendMessage(msg);
    }

    protected void handleMPDError(MPDException e) {
        Log.e(TAG, "MPD error: " + e.getError());

        // Notify error listeners
        synchronized (mErrorListeners) {
            for (MPDConnectionErrorHandler handler : mErrorListeners) {
                handler.newMPDError(e);
            }
        }
    }
//
//    private static class ConnectionStateListener extends MPDConnectionStateChangeHandler {
//
//        private final WeakReference<MPDGenericHandler> mParentHandler;
//
//        ConnectionStateListener(MPDGenericHandler handler) {
//            mParentHandler = new WeakReference<MPDGenericHandler>(handler);
//        }
//
//        @Override
//        public void onConnected() {
//            mParentHandler.get().onConnected();
//        }
//
//        @Override
//        public void onDisconnected() {
//            mParentHandler.get().onDisconnected();
//        }
//    }
}
