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

package andrompd.org.andrompd.mpdservice.handlers;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseAlbumList;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseArtistList;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseHandler;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseTrackList;
import andrompd.org.andrompd.mpdservice.mpdprotocol.MPDConnection;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDAlbum;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDArtist;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDFile;

public class MPDHandler extends Handler implements MPDConnection.MPDConnectionStateChangeListener {
    private static final String TAG = "MPDNetHandler";
    private static final String THREAD_NAME = "AndroMPD-NetHandler";

    private static HandlerThread pHandlerThread = null;
    private static MPDHandler pHandlerSingleton = null;

    private MPDConnection pMPDConnection;


    ArrayList<MPDConnectionStateHandler> pConnectionStateListener;

    /**
     * Private constructor for use in singleton.
     * @param looper Looper of a HandlerThread (that is NOT the UI thread)
     */
    private MPDHandler(Looper looper) {
        super(looper);
        pMPDConnection = new MPDConnection();
    }

    /**
     * Private method to ensure that the singleton runs in a separate thread.
     * Otherwise android will deny network access because of UI blocks.
     * @return
     */
    private synchronized static MPDHandler getHandler() {
        if ( null == pHandlerSingleton ) {
            pHandlerThread = new HandlerThread(THREAD_NAME);
            pHandlerThread.start();
            pHandlerSingleton = new MPDHandler(pHandlerThread.getLooper());
        }
        return pHandlerSingleton;
    }


    /**
     * This is the main entry point of messages.
     * Here all possible messages types need to be handled with the MPDConnection.
     * @param msg Message to process.
     */
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        if ( !(msg.obj instanceof MPDHandlerAction) ) {
            /* Check if the message object is of correct type. Otherwise just abort here. */
            return;
        }

        MPDHandlerAction mpdAction = (MPDHandlerAction)msg.obj;
        /* Catch MPD exceptions here for now. */
        MPDResponseHandler responseHandler;
        MPDHandlerAction.NET_HANDLER_ACTION action = mpdAction.getAction();
        if ( action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_SERVER_PARAMETERS ) {
            /* Parse message objects extras */
            String hostname = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SERVER_HOSTNAME);
            String password = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SERVER_PASSWORD);
            Integer port =  mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SERVER_PORT);
            if ( (null == hostname) || (null == port) ) {
                return;
            }

            pMPDConnection.setServerParameters(hostname,password,port);
        } else if ( action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_CONNECT_MPD_SERVER ) {
            try {
                pMPDConnection.connectToServer();
            } catch (IOException e) {
                onDisconnected();
            }
        } else if ( action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ALBUMS) {
            responseHandler = mpdAction.getResponseHandler();
            if ( !(responseHandler instanceof MPDResponseAlbumList) ) {
                return;
            }

            List<MPDAlbum> albumList = pMPDConnection.getAlbums();

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = albumList;
            responseHandler.sendMessage(responseMessage);
        } else if ( action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTIST_ALBUMS ) {
            String artistName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME);
            responseHandler = mpdAction.getResponseHandler();
            if ( !(responseHandler instanceof MPDResponseAlbumList) || (null == artistName) ) {
                return;
            }

            List<MPDAlbum> albumList = pMPDConnection.getArtistAlbums(artistName);

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = albumList;
            responseHandler.sendMessage(responseMessage);
        } else if ( action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTISTS ) {
            responseHandler = mpdAction.getResponseHandler();

            if ( !(responseHandler instanceof MPDResponseArtistList) ) {
                return;
            }

            List<MPDArtist> artistList = pMPDConnection.getArtists();

            Message artistResponseMsg = this.obtainMessage();
            artistResponseMsg.obj = artistList;

            responseHandler.sendMessage(artistResponseMsg);
        } else if ( action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ALBUM_TRACKS ) {
            String albumName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME);
            responseHandler = mpdAction.getResponseHandler();
            if ( !(responseHandler instanceof MPDResponseTrackList) || (null == albumName) ) {
                return;
            }

            List<MPDFile> trackList = pMPDConnection.getAlbumTracks(albumName);

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = trackList;
            responseHandler.sendMessage(responseMessage);
        } else if ( action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTIST_ALBUM_TRACKS ) {
            String artistName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME);
            String albumName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME);
            responseHandler = mpdAction.getResponseHandler();
            if ( !(responseHandler instanceof MPDResponseTrackList) || (null == albumName) || (null == artistName) ) {
                return;
            }

            List<MPDFile> trackList = pMPDConnection.getArtistAlbumTracks(albumName, artistName);

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = trackList;
            responseHandler.sendMessage(responseMessage);
        }
    }


    /* Convenient methods for message generation */

    /**
     * Set the server parameters for the connection. MUST be called before trying to
     * initiate a connection because it will fail otherwise.
     * @param hostname Hostname or ip address to connect to.
     * @param password Password that is used to authenticate with the server. Can be left empty.
     * @param port Port to use for the connection. (Default: 6600)
     */
    public static void setServerParameters(String hostname, String password, int port) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SET_SERVER_PARAMETERS);
        Message msg = Message.obtain();
        if ( msg == null ) {
            return;
        }
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SERVER_HOSTNAME, hostname);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SERVER_PASSWORD, password);
        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SERVER_PORT, port);
        msg.obj = action;
        MPDHandler.getHandler().sendMessage(msg);
    }

    /**
     * Connect to the previously configured MPD server.
     */
    public static void connectToMPDServer() {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_CONNECT_MPD_SERVER);
        Message msg = Message.obtain();
        if ( msg == null ) {
            return;
        }
        msg.obj = action;
        MPDHandler.getHandler().sendMessage(msg);
    }

    /**
     * Method to retrieve a list of all albums available on the currently connected MPD server.
     * @param responseHandler The Handler that is used for asynchronous callback calls when the result
     *                        of the MPD server is ready and parsed.
     */
    public static void getAlbums(MPDResponseAlbumList responseHandler) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ALBUMS);
        Message msg = Message.obtain();
        if ( msg == null ) {
            return;
        }
        action.setResponseHandler(responseHandler);
        msg.obj = action;
        MPDHandler.getHandler().sendMessage(msg);
    }

    public static void getArtistAlbums(MPDResponseAlbumList responseHandler,String artist) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTIST_ALBUMS);
        Message msg = Message.obtain();
        if ( msg == null ) {
            return;
        }
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME, artist);
        action.setResponseHandler(responseHandler);
        msg.obj = action;
        MPDHandler.getHandler().sendMessage(msg);
    }

    public static void getArtists(MPDResponseHandler responseHandler) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTISTS);
        Message msg = Message.obtain();
        if ( null == msg ) {
            return;
        }
        action.setResponseHandler(responseHandler);
        msg.obj = action;

        MPDHandler.getHandler().sendMessage(msg);
    }

    public static void getAlbumTracks(MPDResponseTrackList responseHandler, String albumName) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ALBUM_TRACKS);
        Message msg = Message.obtain();
        if ( null == msg ) {
            return;
        }
        action.setResponseHandler(responseHandler);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME,albumName);
        msg.obj = action;

        MPDHandler.getHandler().sendMessage(msg);
    }

    public static void getArtistAlbumTracks(MPDResponseTrackList responseHandler, String albumName, String artistName) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTIST_ALBUM_TRACKS);
        Message msg = Message.obtain();
        if ( null == msg ) {
            return;
        }
        action.setResponseHandler(responseHandler);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME,albumName);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME,artistName);
        msg.obj = action;

        MPDHandler.getHandler().sendMessage(msg);
    }


    public void registerConnectionStateListener(MPDConnectionStateHandler stateHandler) {
        if ( null == stateHandler ) {
            return;
        }
        pConnectionStateListener.add(stateHandler);
    }

    public void unregisterConnectionStateListener(MPDConnectionStateHandler stateHandler) {
        if ( null == stateHandler ) {
            return;
        }
        pConnectionStateListener.remove(stateHandler);
    }

    @Override
    public void onConnected() {
        // Send a message to all registered listen handlers.
        for ( MPDConnectionStateHandler handler: pConnectionStateListener ) {
            Message msg = handler.obtainMessage();
            msg.obj = MPDConnectionStateHandler.CONNECTION_STATE_CHANGE.CONNECTED;
            handler.sendMessage(msg);
        }
    }

    @Override
    public void onDisconnected() {
        // Send a message to all registered listen handlers.
        for ( MPDConnectionStateHandler handler: pConnectionStateListener ) {
            Message msg = handler.obtainMessage();
            msg.obj = MPDConnectionStateHandler.CONNECTION_STATE_CHANGE.DISCONNECTED;
            handler.sendMessage(msg);
        }
    }
}
