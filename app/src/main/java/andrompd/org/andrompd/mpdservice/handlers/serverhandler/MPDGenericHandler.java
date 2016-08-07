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


import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.util.ArrayList;

import andrompd.org.andrompd.mpdservice.handlers.MPDConnectionStateChangeHandler;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseHandler;
import andrompd.org.andrompd.mpdservice.mpdprotocol.MPDConnection;

public abstract class MPDGenericHandler extends Handler implements MPDConnection.MPDConnectionStateChangeListener {

    protected MPDConnection mMPDConnection;

    protected ArrayList<MPDConnectionStateChangeHandler> mConnectionStateListener;


    protected MPDGenericHandler(Looper looper) {
        super(looper);
        mMPDConnection = new MPDConnection();
        mConnectionStateListener = new ArrayList<>();

        mMPDConnection.setStateListener(this);
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
        if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_SERVER_PARAMETERS) {
            /* Parse message objects extras */
            String hostname = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SERVER_HOSTNAME);
            String password = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SERVER_PASSWORD);
            Integer port = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SERVER_PORT);
            if ((null == hostname) || (null == port)) {
                return;
            }

            mMPDConnection.setServerParameters(hostname, password, port);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_CONNECT_MPD_SERVER) {
            try {
                mMPDConnection.connectToServer();
            } catch (IOException e) {
                onDisconnected();
            }
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_START_IDLE) {
            mMPDConnection.startIdleing();
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_DISCONNECT_MPD_SERVER) {
            mMPDConnection.disconnectFromServer();
        }
    }

    public void internalRegisterConnectionStateListener(MPDConnectionStateChangeHandler stateHandler) {
        if (null == stateHandler) {
            return;
        }
        mConnectionStateListener.add(stateHandler);
    }

    public void internalUnregisterConnectionStateListener(MPDConnectionStateChangeHandler stateHandler) {
        if (null == stateHandler) {
            return;
        }
        mConnectionStateListener.remove(stateHandler);
    }

    @Override
    public void onConnected() {
        // Send a message to all registered listen handlers.
        for (MPDConnectionStateChangeHandler handler : mConnectionStateListener) {
            Message msg = handler.obtainMessage();
            msg.obj = MPDConnectionStateChangeHandler.CONNECTION_STATE_CHANGE.CONNECTED;
            handler.sendMessage(msg);
        }
    }

    @Override
    public void onDisconnected() {
        // Send a message to all registered listen handlers.
        for (MPDConnectionStateChangeHandler handler : mConnectionStateListener) {
            Message msg = handler.obtainMessage();
            msg.obj = MPDConnectionStateChangeHandler.CONNECTION_STATE_CHANGE.DISCONNECTED;
            handler.sendMessage(msg);
        }
    }
}
