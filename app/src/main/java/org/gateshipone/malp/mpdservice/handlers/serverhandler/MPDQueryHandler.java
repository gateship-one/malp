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

package org.gateshipone.malp.mpdservice.handlers.serverhandler;


import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.gateshipone.malp.mpdservice.handlers.MPDConnectionStateChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseAlbumList;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseArtistList;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseFileList;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseHandler;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseOutputList;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseServerStatistics;
import org.gateshipone.malp.mpdservice.mpdprotocol.MPDCapabilities;
import org.gateshipone.malp.mpdservice.mpdprotocol.MPDCommands;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDOutput;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDStatistics;

import java.io.IOException;
import java.util.List;

/**
 * This handler is used for all long running queries to the mpd server. This includes:
 * database requests, playlists, outputs, current playlist, searches, file listings.
 * <p/>
 * To request certain items the caller needs to provide an instance of another Handler, called ResponseHandlers,
 * that ensure that the return of the requested values is also done asynchronously.
 * <p/>
 * Requests should look like this:
 * <p/>
 * UI-Thread --> QueryHandler |(send message to another thread)-->    MPDConnection
 * <--(send message to another thread)<--ResponseHandler<-- MPDConnection
 */
public class MPDQueryHandler extends MPDGenericHandler {
    private static final String TAG = "MPDQueryHandler";
    /**
     * Name of the thread created for the Looper.
     */
    private static final String THREAD_NAME = "AndroMPD-QueryHandler";


    /**
     * HandlerThread that is used by the looper. This ensures that all requests to this handler
     * are done multi-threaded and do not block the UI.
     */
    private static HandlerThread mHandlerThread = null;
    private static MPDQueryHandler mHandlerSingleton = null;

    /**
     * Private constructor for use in singleton. Called by the static singleton retrieval method.
     *
     * @param looper Looper of a HandlerThread (that is NOT the UI thread)
     */
    protected MPDQueryHandler(Looper looper) {
        super(looper);
    }

    /**
     * Private method to ensure that the singleton runs in a separate thread.
     * Otherwise android will deny network access because of UI blocks.
     *
     * @return
     */
    private synchronized static MPDQueryHandler getHandler() {
        // Check if handler was accessed before. If not create the singleton object for the first
        // time.
        if (null == mHandlerSingleton) {
            // Create a new thread used as a looper for this handler.
            // This is the thread in which all messages sent to this handler are handled.
            mHandlerThread = new HandlerThread(THREAD_NAME);
            // It is important to start the thread before using it as a thread for the Handler.
            // Otherwise the handler will cause a crash.
            mHandlerThread.start();
            // Create the actual singleton instance.
            mHandlerSingleton = new MPDQueryHandler(mHandlerThread.getLooper());
        }
        return mHandlerSingleton;
    }


    /**
     * This is the main entry point of messages.
     * Here all possible messages types need to be handled with the MPDConnection.
     * Have a look into the baseclass MPDGenericHandler for more information about the handling.
     *
     * @param msg Message to process.
     */
    @Override
    public void handleMessage(Message msg) {
        // Call the baseclass handleMessage method here to ensure that the messages handled
        // by the baseclass are handled in subclasses as well.
        super.handleMessage(msg);

        // Type checking
        if (!(msg.obj instanceof MPDHandlerAction)) {
            /* Check if the message object is of correct type. Otherwise just abort here. */
            return;
        }

        MPDHandlerAction mpdAction = (MPDHandlerAction) msg.obj;
        /* Catch MPD exceptions here for now. */

        // ResponseHandler used to return the requested items to the caller
        MPDResponseHandler responseHandler;

        /**
         * All messages are handled the same way:
         *  * Check which action was requested
         *  * Check if a ResponseHandler is necessary and also provided. (If not just abort here)
         *  * Request the list of data objects from the MPDConnection (and therefor from the server)
         *  * Pack the response in a Message requested from the given ResponseHandler.
         *  * Send the message to the ResponseHandler
         */
        MPDHandlerAction.NET_HANDLER_ACTION action = mpdAction.getAction();
        if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ALBUMS) {
            responseHandler = mpdAction.getResponseHandler();
            if (!(responseHandler instanceof MPDResponseAlbumList)) {
                return;
            }

            List<MPDAlbum> albumList = mMPDConnection.getAlbums();

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = albumList;
            responseHandler.sendMessage(responseMessage);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ALBUMS_IN_PATH) {
            responseHandler = mpdAction.getResponseHandler();
            if (!(responseHandler instanceof MPDResponseAlbumList)) {
                return;
            }
            String path = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PATH);
            List<MPDAlbum> albumList = mMPDConnection.getAlbumsInPath(path);

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = albumList;
            responseHandler.sendMessage(responseMessage);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTIST_ALBUMS) {
            String artistName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME);
            responseHandler = mpdAction.getResponseHandler();
            if (!(responseHandler instanceof MPDResponseAlbumList) || (null == artistName)) {
                return;
            }

            List<MPDAlbum> albumList = mMPDConnection.getArtistAlbums(artistName);

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = albumList;
            responseHandler.sendMessage(responseMessage);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTISTS) {
            responseHandler = mpdAction.getResponseHandler();

            if (!(responseHandler instanceof MPDResponseArtistList)) {
                return;
            }

            List<MPDArtist> artistList = mMPDConnection.getArtists();

            Message artistResponseMsg = this.obtainMessage();
            artistResponseMsg.obj = artistList;

            responseHandler.sendMessage(artistResponseMsg);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ALBUM_TRACKS) {
            String albumName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME);
            String albumMBID = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_MBID);
            responseHandler = mpdAction.getResponseHandler();
            if (!(responseHandler instanceof MPDResponseFileList) || (null == albumName)) {
                return;
            }

            List<MPDFileEntry> trackList = mMPDConnection.getAlbumTracks(albumName, albumMBID);

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = trackList;
            responseHandler.sendMessage(responseMessage);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTIST_ALBUM_TRACKS) {
            String artistName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME);
            String albumName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME);
            String albumMBID = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_MBID);
            responseHandler = mpdAction.getResponseHandler();
            if (!(responseHandler instanceof MPDResponseFileList) || (null == albumName) || (null == artistName)) {
                return;
            }

            List<MPDFileEntry> trackList = mMPDConnection.getArtistAlbumTracks(albumName, artistName, albumMBID);

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = trackList;
            responseHandler.sendMessage(responseMessage);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_CURRENT_PLAYLIST) {
            responseHandler = mpdAction.getResponseHandler();
            if (!(responseHandler instanceof MPDResponseFileList)) {
                return;
            }

            List<MPDFileEntry> trackList = mMPDConnection.getCurrentPlaylist();

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = trackList;
            responseHandler.sendMessage(responseMessage);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_CURRENT_PLAYLIST_WINDOW) {
            int start = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_WINDOW_START);
            int end = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_WINDOW_END);
            responseHandler = mpdAction.getResponseHandler();
            if (!(responseHandler instanceof MPDResponseFileList)) {
                return;
            }

            List<MPDFileEntry> trackList = mMPDConnection.getCurrentPlaylistWindow(start, end);

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = trackList;
            Bundle data = new Bundle();
            data.putInt(MPDResponseFileList.EXTRA_WINDOW_START, start);
            data.putInt(MPDResponseFileList.EXTRA_WINDOW_END, end);
            responseMessage.setData(data);
            responseHandler.sendMessage(responseMessage);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_SAVED_PLAYLIST) {
            String playlistName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PLAYLIST_NAME);
            responseHandler = mpdAction.getResponseHandler();
            if (!(responseHandler instanceof MPDResponseFileList)) {
                return;
            }

            List<MPDFileEntry> trackList = mMPDConnection.getSavedPlaylist(playlistName);

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = trackList;
            responseHandler.sendMessage(responseMessage);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_SAVED_PLAYLISTS) {
            responseHandler = mpdAction.getResponseHandler();
            if (!(responseHandler instanceof MPDResponseFileList)) {
                return;
            }

            List<MPDFileEntry> playlistList = mMPDConnection.getPlaylists();

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = playlistList;
            responseHandler.sendMessage(responseMessage);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SAVE_PLAYLIST) {
            String playlistName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PLAYLIST_NAME);

            mMPDConnection.savePlaylist(playlistName);

        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_ADD_SONG_TO_PLAYLIST) {
            String playlistName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PLAYLIST_NAME);
            String path = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PATH);

            mMPDConnection.addSongToPlaylist(playlistName, path);

        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_REMOVE_SONG_FROM_PLAYLIST) {
            String playlistName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PLAYLIST_NAME);
            int position = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SONG_INDEX);

            mMPDConnection.removeSongFromPlaylist(playlistName, position);

        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_REMOVE_PLAYLIST) {
            String playlistName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PLAYLIST_NAME);

            mMPDConnection.removePlaylist(playlistName);

        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_LOAD_PLAYLIST) {
            String playlistName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PLAYLIST_NAME);


            mMPDConnection.loadPlaylist(playlistName);

        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_PLAY_PLAYLIST) {
            String playlistName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PLAYLIST_NAME);

            mMPDConnection.clearPlaylist();
            mMPDConnection.loadPlaylist(playlistName);
            mMPDConnection.playSongIndex(0);

        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_ADD_ARTIST_ALBUM) {
            String albumname = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME);
            String artistname = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME);
            String albumMBID = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_MBID);

            mMPDConnection.addAlbumTracks(albumname, artistname,albumMBID);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_PLAY_ARTIST_ALBUM) {
            String albumname = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME);
            String artistname = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME);
            String albumMBID = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_MBID);

            mMPDConnection.clearPlaylist();
            mMPDConnection.addAlbumTracks(albumname, artistname,albumMBID);
            mMPDConnection.playSongIndex(0);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_ADD_ARTIST) {
            String artistname = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME);

            mMPDConnection.addArtist(artistname);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_PLAY_ARTIST) {
            String artistname = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME);

            mMPDConnection.clearPlaylist();
            mMPDConnection.addArtist(artistname);
            mMPDConnection.playSongIndex(0);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_ADD_SONG) {
            String url = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SONG_URL);

            mMPDConnection.addSong(url);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_PLAY_SONG_NEXT) {
            String url = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SONG_URL);

            try {
                MPDCurrentStatus status = mMPDConnection.getCurrentServerStatus();
                mMPDConnection.addSongatIndex(url, status.getCurrentSongIndex() + 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_PLAY_SONG) {
            String url = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SONG_URL);

            try {
                mMPDConnection.addSong(url);
                MPDCurrentStatus status = mMPDConnection.getCurrentServerStatus();
                mMPDConnection.playSongIndex(status.getPlaylistLength() - 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_CLEAR_CURRENT_PLAYLIST) {
            mMPDConnection.clearPlaylist();
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_MOVE_SONG_AFTER_CURRENT) {

            try {
                MPDCurrentStatus status = mMPDConnection.getCurrentServerStatus();
                int index = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SONG_INDEX);
                if (index < status.getCurrentSongIndex()) {
                    mMPDConnection.moveSongFromTo(index, status.getCurrentSongIndex());
                } else if (index > status.getCurrentSongIndex()) {
                    mMPDConnection.moveSongFromTo(index, status.getCurrentSongIndex() + 1);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_REMOVE_SONG_FROM_CURRENT_PLAYLIST) {
            int index = mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SONG_INDEX);
            mMPDConnection.removeIndex(index);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_FILES) {
            String path = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PATH);

            responseHandler = mpdAction.getResponseHandler();
            if (!(responseHandler instanceof MPDResponseFileList)) {
                return;
            }

            List<MPDFileEntry> fileList = mMPDConnection.getFiles(path);

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = fileList;
            responseHandler.sendMessage(responseMessage);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_ADD_DIRECTORY) {
            String path = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PATH);
            mMPDConnection.addSong(path);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_PLAY_DIRECTORY) {
            String path = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PATH);

            mMPDConnection.clearPlaylist();
            mMPDConnection.addSong(path);
            mMPDConnection.playSongIndex(0);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_OUTPUTS) {
            responseHandler = mpdAction.getResponseHandler();

            List<MPDOutput> outputList = mMPDConnection.getOutputs();

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = outputList;
            responseHandler.sendMessage(responseMessage);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_SERVER_STATISTICS) {
            responseHandler = mpdAction.getResponseHandler();

            MPDStatistics stats = null;
            try {
                stats = mMPDConnection.getServerStatistics();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = stats;
            responseHandler.sendMessage(responseMessage);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_UPDATE_DATABASE) {

            mMPDConnection.updateDatabase();
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SEARCH_FILES) {
            String term = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SEARCH_TERM);
            MPDCommands.MPD_SEARCH_TYPE type = MPDCommands.MPD_SEARCH_TYPE.values()[mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SEARCH_TYPE)];

            responseHandler = mpdAction.getResponseHandler();
            if (!(responseHandler instanceof MPDResponseFileList)) {
                return;
            }

            List<MPDFileEntry> fileList = mMPDConnection.getSearchedFiles(term, type);

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = fileList;
            responseHandler.sendMessage(responseMessage);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_ADD_SEARCH_FILES) {
            String term = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SEARCH_TERM);
            MPDCommands.MPD_SEARCH_TYPE type = MPDCommands.MPD_SEARCH_TYPE.values()[mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SEARCH_TYPE)];

            // Check if server has the add search result capability
            if (mMPDConnection.getServerCapabilities().hasSearchAdd()) {
                mMPDConnection.addSearchedFiles(term, type);
            } else {
                // Fetch search results and add them
                List<MPDFileEntry> searchResults = mMPDConnection.getSearchedFiles(term, type);
                mMPDConnection.addTrackList(searchResults);
            }
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_PLAY_SEARCH_FILES) {
            String term = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SEARCH_TERM);
            MPDCommands.MPD_SEARCH_TYPE type = MPDCommands.MPD_SEARCH_TYPE.values()[mpdAction.getIntExtra(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SEARCH_TYPE)];

            mMPDConnection.clearPlaylist();

            // Check if server has the add search result capability
            if (mMPDConnection.getServerCapabilities().hasSearchAdd()) {
                mMPDConnection.addSearchedFiles(term, type);
            } else {
                // Fetch search results and add them
                List<MPDFileEntry> searchResults = mMPDConnection.getSearchedFiles(term, type);
                mMPDConnection.addTrackList(searchResults);
            }

            mMPDConnection.playSongIndex(0);
        }
    }


    /**
     * These static methods provide the only interface to outside classes.
     * They should not be allowed to interact with the instance itself.
     *
     * All of these methods work with the same principle. They all create an handler message
     * that will contain a MPDHandlerAction as a payload that contains all the information
     * for the requested action with extras.
     */


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
        MPDQueryHandler.getHandler().sendMessage(msg);
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
        MPDQueryHandler.getHandler().sendMessage(msg);
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
        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Method to retrieve a list of all albums available on the currently connected MPD server.
     *
     * @param responseHandler The Handler that is used for asynchronous callback calls when the result
     *                        of the MPD server is ready and parsed.
     */
    public static void getAlbums(MPDResponseAlbumList responseHandler) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ALBUMS);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }
        action.setResponseHandler(responseHandler);
        msg.obj = action;
        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Method to retrieve a list of all albums available on the currently connected MPD server.
     * This only shows album that lay in the given path. This feature is only available for servers
     * >= 0.19.
     *
     * @param path Path to list albums for
     * @param responseHandler The Handler that is used for asynchronous callback calls when the result
     *                        of the MPD server is ready and parsed.
     */
    public static void getAlbumsInPath(String path, MPDResponseAlbumList responseHandler) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ALBUMS_IN_PATH);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PATH, path);
        action.setResponseHandler(responseHandler);
        msg.obj = action;
        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Requests a list of albums of an artist.
     *
     * @param responseHandler The handler used to send the requested data
     * @param artist          Artist to get a list of albums from.
     */
    public static void getArtistAlbums(MPDResponseAlbumList responseHandler, String artist) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTIST_ALBUMS);
        Message msg = Message.obtain();
        if (msg == null) {
            return;
        }
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME, artist);
        action.setResponseHandler(responseHandler);
        msg.obj = action;
        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Requests a list of all the artists available on this server
     *
     * @param responseHandler The handler used to send the requested data
     */
    public static void getArtists(MPDResponseHandler responseHandler) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTISTS);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }
        action.setResponseHandler(responseHandler);
        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Requests a list of tracks (MPDFileEntry objects) on a album.
     *
     * @param responseHandler The handler used to send the requested data
     * @param albumName       Album to get tracks from
     */
    public static void getAlbumTracks(MPDResponseFileList responseHandler, String albumName, String mbid) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ALBUM_TRACKS);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }
        action.setResponseHandler(responseHandler);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME, albumName);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_MBID, mbid);
        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Requests a list of tracks (MPDFileEntry) on an album. This method will also filter the results
     * with a given artistname
     *
     * @param responseHandler The handler used to send the requested data
     * @param albumName       Album go get tracks from
     * @param artistName      Artist name to filter results with
     */
    public static void getArtistAlbumTracks(MPDResponseFileList responseHandler, String albumName, String artistName, String mbid) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTIST_ALBUM_TRACKS);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }
        action.setResponseHandler(responseHandler);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME, albumName);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME, artistName);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_MBID, mbid);
        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Requests a list of all tracks enlisted in the current playlist.
     *
     * @param responseHandler The handler used to send the requested data
     */
    public static void getCurrentPlaylist(MPDResponseFileList responseHandler) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_CURRENT_PLAYLIST);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }
        action.setResponseHandler(responseHandler);
        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Requests a list of tracks enlisted in the current playlist.
     * This method is able to request a partial list to speed up the query and lower the network
     * usage.
     *
     * @param responseHandler The handler used to send the requested data
     */
    public static void getCurrentPlaylist(MPDResponseFileList responseHandler, int start, int end) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_CURRENT_PLAYLIST_WINDOW);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }
        action.setResponseHandler(responseHandler);
        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_WINDOW_START, start);
        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_WINDOW_END, end);
        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Requests a list of playlists saved on the server.
     *
     * @param responseHandler The handler used to send the requested data
     */
    public static void getSavedPlaylists(MPDResponseFileList responseHandler) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_SAVED_PLAYLISTS);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }
        action.setResponseHandler(responseHandler);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Returns a list of tracks listed in a saved playlist.
     *
     * @param responseHandler The handler used to send the requested data
     * @param playlistName    Name of the playlist to get the tracks from.
     */
    public static void getSavedPlaylist(MPDResponseFileList responseHandler, String playlistName) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_SAVED_PLAYLIST);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }
        action.setResponseHandler(responseHandler);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PLAYLIST_NAME, playlistName);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Requests a list of files for a specified path. If no path is given the database root is used.
     *
     * @param responseHandler The handler used to send the requested data
     * @param path            Path to get the files/directory/playlist from
     */
    public static void getFiles(MPDResponseFileList responseHandler, String path) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_FILES);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }
        action.setResponseHandler(responseHandler);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PATH, path);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Requests a list of available outputs configured on the MPD server.
     *
     * @param responseHandler The handler used to send the requested data.
     */
    public static void getOutputs(MPDResponseOutputList responseHandler) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_OUTPUTS);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }
        action.setResponseHandler(responseHandler);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Requests a statistics object for the connected mpd server.
     *
     * @param responseHandler The handler used to send the requested data.
     */
    public static void getStatistics(MPDResponseServerStatistics responseHandler) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_SERVER_STATISTICS);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }
        action.setResponseHandler(responseHandler);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Adds all tracks from an album (filtered with an artist name) to the current playlist.
     *
     * @param albumname  Album name of the album to add
     * @param artistname Artist name to filter tracks before enqueueing
     */
    public static void addArtistAlbum(String albumname, String artistname, String mbid) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_ADD_ARTIST_ALBUM);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME, albumname);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME, artistname);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_MBID, mbid);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Adds an album to the current playlist and start playing it
     *
     * @param albumname  Album name of the album to add
     * @param artistname Artist name to filter tracks before enqueueing
     */
    public static void playArtistAlbum(String albumname, String artistname, String mbid) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_PLAY_ARTIST_ALBUM);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME, albumname);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME, artistname);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_MBID, mbid);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Adds all albums from an artist to the current playlist.
     *
     * @param artistname Name of the artist to add to the current playlist.
     */
    public static void addArtist(String artistname) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_ADD_ARTIST);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME, artistname);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Adds all albums from an artist to the current playlist and starts playing them.
     *
     * @param artistname Name of the artist to play its albums
     */
    public static void playArtist(String artistname) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_PLAY_ARTIST);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME, artistname);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    // FIXME check if song and directory actually need two different methods. It will result in the same MPD command

    /**
     * Adds a path to the current playlist. Can be a file or directory
     *
     * @param url URL of the path to add.
     */
    public static void addSong(String url) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_ADD_SONG);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SONG_URL, url);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static void addDirectory(String url) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_ADD_DIRECTORY);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PATH, url);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static void playDirectory(String url) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_PLAY_DIRECTORY);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PATH, url);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }


    public static void playSong(String url) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_PLAY_SONG);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SONG_URL, url);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static void playSongNext(String url) {
        Log.v(TAG, "Play song: " + url + " as next");
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_PLAY_SONG_NEXT);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SONG_URL, url);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static void clearPlaylist() {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_CLEAR_CURRENT_PLAYLIST);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static void removeSongFromCurrentPlaylist(int index) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_REMOVE_SONG_FROM_CURRENT_PLAYLIST);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        msg.obj = action;
        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SONG_INDEX, index);
        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static void playIndexAsNext(int index) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_MOVE_SONG_AFTER_CURRENT);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        msg.obj = action;
        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SONG_INDEX, index);
        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static void savePlaylist(String name) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SAVE_PLAYLIST);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PLAYLIST_NAME, name);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static void addURLToSavedPlaylist(String playlistName, String url) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_ADD_SONG_TO_PLAYLIST);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PLAYLIST_NAME, playlistName);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PATH, url);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static void removeSongFromSavedPlaylist(String playlistName, int position) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_REMOVE_SONG_FROM_PLAYLIST);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PLAYLIST_NAME, playlistName);
        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SONG_INDEX, position);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static void removePlaylist(String name) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_REMOVE_PLAYLIST);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PLAYLIST_NAME, name);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static void loadPlaylist(String name) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_LOAD_PLAYLIST);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PLAYLIST_NAME, name);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static void playPlaylist(String name) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_PLAY_PLAYLIST);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_PLAYLIST_NAME, name);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }


    public static void updateDatabase() {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_UPDATE_DATABASE);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Requests a list of files matching the search term and type
     *
     * @param term            The string to search for
     * @param type            The type of items to search for
     * @param responseHandler The handler used to send the requested data.
     */
    public static void searchFiles(String term, MPDCommands.MPD_SEARCH_TYPE type, MPDResponseFileList responseHandler) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_SEARCH_FILES);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }
        action.setResponseHandler(responseHandler);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SEARCH_TERM, term);
        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SEARCH_TYPE, type.ordinal());

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Requests to add a search request
     *
     * @param term The string to search for
     * @param type The type of items to search for
     */
    public static void searchAddFiles(String term, MPDCommands.MPD_SEARCH_TYPE type) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_ADD_SEARCH_FILES);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SEARCH_TERM, term);
        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SEARCH_TYPE, type.ordinal());

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    /**
     * Requests to play a search result
     *
     * @param term The string to search for
     * @param type The type of items to search for
     */
    public static void searchPlayFiles(String term, MPDCommands.MPD_SEARCH_TYPE type) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_PLAY_SEARCH_FILES);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_SEARCH_TERM, term);
        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_SEARCH_TYPE, type.ordinal());

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static MPDCapabilities getServerCapabilities() {
        return MPDQueryHandler.getHandler().mMPDConnection.getServerCapabilities();
    }

    public static void registerConnectionStateListener(MPDConnectionStateChangeHandler stateHandler) {
        getHandler().internalRegisterConnectionStateListener(stateHandler);
    }

    public static void unregisterConnectionStateListener(MPDConnectionStateChangeHandler stateHandler) {
        getHandler().internalUnregisterConnectionStateListener(stateHandler);
    }
}
