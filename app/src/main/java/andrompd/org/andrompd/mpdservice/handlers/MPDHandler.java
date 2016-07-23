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
import java.util.List;

import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseAlbumList;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseArtistList;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseHandler;
import andrompd.org.andrompd.mpdservice.mpdprotocol.MPDConnection;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDAlbum;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDArtist;

public class MPDHandler extends Handler {
    private static final String TAG = "MPDNetHandler";
    private static final String THREAD_NAME = "AndroMPD-NetHandler";

    private static HandlerThread pHandlerThread = null;
    private static MPDHandler pHandlerSingleton = null;

    private MPDConnection pMPDConnection;

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
        try {
            switch ( mpdAction.getAction() ) {
                case ACTION_SET_SERVER_PARAMETERS:
                    /* Parse message objects extras */
                    String hostname = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SERVER_HOSTNAME);
                    String password = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SERVER_PASSWORD);
                    Integer port =  mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SERVER_PORT);
                    if ( (null == hostname) || (null == port) ) {
                        return;
                    }

                    pMPDConnection.setServerParameters(hostname,password,port);
                    break;
                case ACTION_CONNECT_MPD_SERVER:
                    pMPDConnection.connectToServer();
                    break;
                case ACTION_GET_ALBUMS:
                    MPDResponseHandler responseHandler = mpdAction.getResponseHandler();
                    if ( !(responseHandler instanceof MPDResponseAlbumList) ) {
                        return;
                    }

                    List<MPDAlbum> albumList = pMPDConnection.getAlbums();

                    Log.v(TAG,"Send album list response away from thread: " + Thread.currentThread().getId());

                    Message responseMessage = this.obtainMessage();
                    responseMessage.obj = albumList;
                    responseHandler.sendMessage(responseMessage);

                    break;
                case ACTION_GET_ARTIST_ALBUMS:
                    /* Parse message objects extras */
                    String artistName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME);
                    pMPDConnection.getArtistAlbums(artistName);
                    break;
                case ACTION_GET_ARTISTS:
                    MPDResponseHandler artistResponseHandler = mpdAction.getResponseHandler();

                    if ( !(artistResponseHandler instanceof MPDResponseArtistList) ) {
                        return;
                    }

                    List<MPDArtist> artistList = pMPDConnection.getArtists();

                    Message artistResponseMsg = this.obtainMessage();
                    artistResponseMsg.obj = artistList;

                    artistResponseHandler.sendMessage(artistResponseMsg);
                    break;
                case ACTION_GET_TRACKS:
                    break;
                case ACTION_GET_ALBUM_TRACKS:
                    break;
                case ACTION_GET_ARTIST_ALBUM_TRACKS:
                    break;
                case ACTION_GET_SERVER_STATUS:
                    break;
                default: return;
            }
        } catch (IOException e) {
            e.printStackTrace();
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
    public static void getAlbums(MPDResponseHandler responseHandler) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ALBUMS);
        Message msg = Message.obtain();
        if ( msg == null ) {
            return;
        }
        action.setResponseHandler(responseHandler);
        msg.obj = action;
        MPDHandler.getHandler().sendMessage(msg);
    }

    public static void getArtistAlbums(String artist) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTIST_ALBUMS);
        Message msg = Message.obtain();
        if ( msg == null ) {
            return;
        }
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME, artist);
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


}
