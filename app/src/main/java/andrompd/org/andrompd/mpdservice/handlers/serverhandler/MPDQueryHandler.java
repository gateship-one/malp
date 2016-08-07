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

import java.util.List;

import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseAlbumList;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseArtistList;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseHandler;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseTrackList;
import andrompd.org.andrompd.mpdservice.mpdprotocol.MPDConnection;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDAlbum;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDArtist;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDFile;

public class MPDQueryHandler extends MPDGenericHandler implements MPDConnection.MPDConnectionIdleChangeListener {
    private static final String TAG = "MPDQueryHandler";
    private static final String THREAD_NAME = "AndroMPD-NetHandler";



    private static HandlerThread mHandlerThread = null;
    private static MPDQueryHandler mHandlerSingleton = null;

    /**
     * Private constructor for use in singleton.
     * @param looper Looper of a HandlerThread (that is NOT the UI thread)
     */
    protected MPDQueryHandler(Looper looper) {
        super(looper);


    }

    /**
     * Private method to ensure that the singleton runs in a separate thread.
     * Otherwise android will deny network access because of UI blocks.
     * @return
     */
    private synchronized static MPDQueryHandler getHandler() {
        if ( null == mHandlerSingleton) {
            mHandlerThread = new HandlerThread(THREAD_NAME);
            mHandlerThread.start();
            mHandlerSingleton = new MPDQueryHandler(mHandlerThread.getLooper());

            mHandlerSingleton.mMPDConnection.setpIdleListener(mHandlerSingleton);
        }
        return mHandlerSingleton;
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

        if ( action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ALBUMS) {
            responseHandler = mpdAction.getResponseHandler();
            if ( !(responseHandler instanceof MPDResponseAlbumList) ) {
                return;
            }

            List<MPDAlbum> albumList = mMPDConnection.getAlbums();

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = albumList;
            responseHandler.sendMessage(responseMessage);
        } else if ( action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTIST_ALBUMS ) {
            String artistName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME);
            responseHandler = mpdAction.getResponseHandler();
            if ( !(responseHandler instanceof MPDResponseAlbumList) || (null == artistName) ) {
                return;
            }

            List<MPDAlbum> albumList = mMPDConnection.getArtistAlbums(artistName);

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = albumList;
            responseHandler.sendMessage(responseMessage);
        } else if ( action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTISTS ) {
            responseHandler = mpdAction.getResponseHandler();

            if ( !(responseHandler instanceof MPDResponseArtistList) ) {
                return;
            }

            List<MPDArtist> artistList = mMPDConnection.getArtists();

            Message artistResponseMsg = this.obtainMessage();
            artistResponseMsg.obj = artistList;

            responseHandler.sendMessage(artistResponseMsg);
        } else if ( action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ALBUM_TRACKS ) {
            String albumName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME);
            responseHandler = mpdAction.getResponseHandler();
            if ( !(responseHandler instanceof MPDResponseTrackList) || (null == albumName) ) {
                return;
            }

            List<MPDFile> trackList = mMPDConnection.getAlbumTracks(albumName);

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

            List<MPDFile> trackList = mMPDConnection.getArtistAlbumTracks(albumName, artistName);

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = trackList;
            responseHandler.sendMessage(responseMessage);
        } else if ( action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_CURRENT_PLAYLIST ) {
            responseHandler = mpdAction.getResponseHandler();
            if ( !(responseHandler instanceof MPDResponseTrackList) ) {
                return;
            }

            List<MPDFile> trackList = mMPDConnection.getCurrentPlaylist();
            Log.v(TAG,"Received current playlist with "  + trackList.size() + " tracks");

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = trackList;
            responseHandler.sendMessage(responseMessage);
        } else if ( action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_SAVED_PLAYLIST ) {
            String playlistName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PLAYLIST_NAME);
            responseHandler = mpdAction.getResponseHandler();
            if ( !(responseHandler instanceof MPDResponseTrackList) ) {
                return;
            }

            List<MPDFile> trackList = mMPDConnection.getSavedPlaylist(playlistName);

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = trackList;
            responseHandler.sendMessage(responseMessage);
        }
        mMPDConnection.startIdleing();
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
        MPDQueryHandler.getHandler().sendMessage(msg);
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
        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Disconnect to the previously connected MPD server.
     */
    public static void disconnectFromMPDServer() {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_DISCONNECT_MPD_SERVER);
        Message msg = Message.obtain();
        if ( msg == null ) {
            return;
        }
        msg.obj = action;
        MPDQueryHandler.getHandler().sendMessage(msg);
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
        MPDQueryHandler.getHandler().sendMessage(msg);
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
        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static void getArtists(MPDResponseHandler responseHandler) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTISTS);
        Message msg = Message.obtain();
        if ( null == msg ) {
            return;
        }
        action.setResponseHandler(responseHandler);
        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
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

        MPDQueryHandler.getHandler().sendMessage(msg);
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

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static void getCurrentPlaylist(MPDResponseTrackList responseHandler) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_CURRENT_PLAYLIST);
        Message msg = Message.obtain();
        if ( null == msg ) {
            return;
        }
        action.setResponseHandler(responseHandler);
        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static void getSavedPlaylist(MPDResponseTrackList responseHandler, String playlistName) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_SAVED_PLAYLIST);
        Message msg = Message.obtain();
        if ( null == msg ) {
            return;
        }
        action.setResponseHandler(responseHandler);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PLAYLIST_NAME,playlistName);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static void startIdle() {
        Log.v(TAG,"sending idling to MPDConnection");
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_START_IDLE);
        Message msg = Message.obtain();
        if ( null == msg ) {
            return;
        }

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }


    @Override
    public void onIdle() {

    }

    @Override
    public void onNonIdle() {
        Log.v(TAG,"Go idle again");
        mMPDConnection.startIdleing();
    }
}
