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


import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import andrompd.org.andrompd.mpdservice.handlers.MPDConnectionStateChangeHandler;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseAlbumList;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseArtistList;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseHandler;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseFileList;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseOutputList;
import andrompd.org.andrompd.mpdservice.mpdprotocol.MPDConnection;
import andrompd.org.andrompd.mpdservice.mpdprotocol.MPDCurrentStatus;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDOutput;

public class MPDQueryHandler extends MPDGenericHandler implements MPDConnection.MPDConnectionIdleChangeListener {
    private static final String TAG = "MPDQueryHandler";
    private static final String THREAD_NAME = "AndroMPD-NetHandler";

    /**
     * Wait 5 seconds before going to idle again
     */
    private static final int IDLE_WAIT_TIME = 5 * 1000;

    private static HandlerThread mHandlerThread = null;
    private static MPDQueryHandler mHandlerSingleton = null;

    /**
     * Private constructor for use in singleton.
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
        if (null == mHandlerSingleton) {
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

        if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ALBUMS) {
            responseHandler = mpdAction.getResponseHandler();
            if (!(responseHandler instanceof MPDResponseAlbumList)) {
                return;
            }

            List<MPDAlbum> albumList = mMPDConnection.getAlbums();

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
            responseHandler = mpdAction.getResponseHandler();
            if (!(responseHandler instanceof MPDResponseFileList) || (null == albumName)) {
                return;
            }

            List<MPDFileEntry> trackList = mMPDConnection.getAlbumTracks(albumName);

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = trackList;
            responseHandler.sendMessage(responseMessage);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTIST_ALBUM_TRACKS) {
            String artistName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME);
            String albumName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME);
            responseHandler = mpdAction.getResponseHandler();
            if (!(responseHandler instanceof MPDResponseFileList) || (null == albumName) || (null == artistName)) {
                return;
            }

            List<MPDFileEntry> trackList = mMPDConnection.getArtistAlbumTracks(albumName, artistName);

            Message responseMessage = this.obtainMessage();
            responseMessage.obj = trackList;
            responseHandler.sendMessage(responseMessage);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_CURRENT_PLAYLIST) {
            responseHandler = mpdAction.getResponseHandler();
            if (!(responseHandler instanceof MPDResponseFileList)) {
                return;
            }

            List<MPDFileEntry> trackList = mMPDConnection.getCurrentPlaylist();
            Log.v(TAG, "Received current playlist with " + trackList.size() + " tracks");

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
            Log.v(TAG, "Received current playlist with " + trackList.size() + " tracks for window: " + start + ':' + end);

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

            mMPDConnection.addAlbumTracks(albumname, artistname);
        } else if (action == MPDHandlerAction.NET_HANDLER_ACTION.ACTION_PLAY_ARTIST_ALBUM) {
            String albumname = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME);
            String artistname = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME);

            mMPDConnection.clearPlaylist();
            mMPDConnection.addAlbumTracks(albumname, artistname);
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
                } else {
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
            Log.v(TAG, "Add directory: " + path);
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
        }
    }


    /* Convenient methods for message generation */

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

    public static void getAlbumTracks(MPDResponseFileList responseHandler, String albumName) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ALBUM_TRACKS);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }
        action.setResponseHandler(responseHandler);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME, albumName);
        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static void getArtistAlbumTracks(MPDResponseFileList responseHandler, String albumName, String artistName) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTIST_ALBUM_TRACKS);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }
        action.setResponseHandler(responseHandler);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME, albumName);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME, artistName);
        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

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

    public static void getCurrentPlaylist(MPDResponseFileList responseHandler, int start, int end) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_CURRENT_PLAYLIST_WINDOW);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }
        Log.v(TAG,"Current playlist window requested: " + start + ':' + end);
        action.setResponseHandler(responseHandler);
        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_WINDOW_START, start);
        action.setIntExtras(MPDHandlerAction.NET_HANDLER_EXTRA_INT.EXTRA_WINDOW_END, end);
        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

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

    public static void addArtistAlbum(String albumname, String artistname) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_ADD_ARTIST_ALBUM);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME, albumname);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME, artistname);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

    public static void playArtistAlbum(String albumname, String artistname) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_PLAY_ARTIST_ALBUM);
        Message msg = Message.obtain();
        if (null == msg) {
            return;
        }

        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ALBUM_NAME, albumname);
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME, artistname);

        msg.obj = action;

        MPDQueryHandler.getHandler().sendMessage(msg);
    }

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
        Log.v(TAG, "Move index: " + index + "after current");
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

    public static void registerConnectionStateListener(MPDConnectionStateChangeHandler stateHandler) {
        mHandlerSingleton.internalRegisterConnectionStateListener(stateHandler);
    }

    public static void unregisterConnectionStateListener(MPDConnectionStateChangeHandler stateHandler) {
        mHandlerSingleton.internalUnregisterConnectionStateListener(stateHandler);
    }


    @Override
    public void onIdle() {

    }

    @Override
    public void onNonIdle() {

    }

    @Override
    public void onConnected() {
        super.onConnected();
        Log.v(TAG, "Go idle after connection");
    }

    @Override
    public void onDisconnected() {
        Log.v(TAG, "Disconnected stop idling");
        super.onDisconnected();
    }


}
