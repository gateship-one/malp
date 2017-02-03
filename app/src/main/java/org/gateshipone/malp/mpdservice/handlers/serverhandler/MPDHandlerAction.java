/*
 *  Copyright (C) 2017 Team Gateship-One
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


import java.util.HashMap;

import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseHandler;

public class MPDHandlerAction {


    /**
     * Actions of this message type. This should represent
     * all implemented methods of the MPD protocol.
     */
    public enum NET_HANDLER_ACTION {
        ACTION_SET_SERVER_PARAMETERS,
        ACTION_CONNECT_MPD_SERVER,
        ACTION_DISCONNECT_MPD_SERVER,
        ACTION_GET_ALBUMS,
        ACTION_GET_ALBUMS_IN_PATH,
        ACTION_GET_ARTIST_ALBUMS,
        ACTION_GET_ARTISTS,
        ACTION_GET_ALBUMARTISTS,
        ACTION_GET_TRACKS,
        ACTION_GET_ALBUM_TRACKS,
        ACTION_GET_ARTIST_ALBUM_TRACKS,
        ACTION_GET_SERVER_STATUS,
        ACTION_GET_SERVER_STATISTICS,
        ACTION_GET_CURRENT_PLAYLIST,
        ACTION_GET_CURRENT_PLAYLIST_WINDOW,
        ACTION_GET_SAVED_PLAYLIST,
        ACTION_GET_SAVED_PLAYLISTS,
        ACTION_GET_FILES,
        ACTION_GET_OUTPUTS,
        ACTION_ADD_ARTIST_ALBUM,
        ACTION_ADD_ARTIST,
        ACTION_ADD_PATH,
        ACTION_PLAY_ARTIST_ALBUM,
        ACTION_PLAY_ARTIST,
        ACTION_PLAY_DIRECTORY,
        ACTION_PLAY_SONG,
        ACTION_PLAY_SONG_NEXT,
        ACTION_START_IDLE,
        ACTION_STOP_IDLE,
        ACTION_SAVE_PLAYLIST,
        ACTION_REMOVE_PLAYLIST,
        ACTION_LOAD_PLAYLIST,
        ACTION_PLAY_PLAYLIST,
        ACTION_ADD_SONG_TO_PLAYLIST,
        ACTION_REMOVE_SONG_FROM_PLAYLIST,
        ACTION_CLEAR_CURRENT_PLAYLIST,
        ACTION_REMOVE_SONG_FROM_CURRENT_PLAYLIST,
        ACTION_REMOVE_RANGE_FROM_CURRENT_PLAYLIST,
        ACTION_MOVE_SONG_FROM_TO_INDEX,
        ACTION_MOVE_SONG_AFTER_CURRENT,
        ACTION_COMMAND_STOP,
        ACTION_COMMAND_PLAY,
        ACTION_COMMAND_PAUSE,
        ACTION_COMMAND_TOGGLE_PAUSE,
        ACTION_COMMAND_NEXT_SONG,
        ACTION_COMMAND_PREVIOUS_SONG,
        ACTION_COMMAND_JUMP_INDEX,
        ACTION_COMMAND_SEEK_SECONDS,
        ACTION_SET_REPEAT,
        ACTION_SET_RANDOM,
        ACTION_SET_SINGLE,
        ACTION_SET_CONSUME,
        ACTION_SET_VOLUME,
        ACTION_UP_VOLUME,
        ACTION_DOWN_VOLUME,
        ACTION_TOGGLE_OUTPUT,
        ACTION_UPDATE_DATABASE,
        ACTION_SEARCH_FILES,
        ACTION_ADD_SEARCH_FILES,
        ACTION_PLAY_SEARCH_FILES
    }


    /**
     * Types of String extras for this message type that can be included.
     * Examples are artist,album and hostnames.
     */
    public enum NET_HANDLER_EXTRA_STRING {
        EXTRA_SERVER_HOSTNAME,
        EXTRA_SERVER_PASSWORD,
        EXTRA_ARTIST_NAME,
        EXTRA_ALBUM_NAME,
        EXTRA_PLAYLIST_NAME,
        EXTRA_SONG_URL,
        EXTRA_PATH,
        EXTRA_SEARCH_TERM,
        EXTRA_ALBUM_MBID,
    }

    /**
     * Type of Integer extras for this message type that can be included.
     * Examples are the portnumber, volume, repeat, random state.
     */
    public enum NET_HANDLER_EXTRA_INT {
        EXTRA_SERVER_PORT,
        EXTRA_REPEAT,
        EXTRA_RANDOM,
        EXTRA_SINGLE,
        EXTRA_CONSUME,
        EXTRA_SONG_INDEX,
        EXTRA_SONG_INDEX_DESTINATION,
        EXTRA_SEEK_TIME,
        EXTRA_VOLUME,
        EXTRA_WINDOW_START,
        EXTRA_WINDOW_END,
        EXTRA_OUTPUT_ID,
        EXTRA_SEARCH_TYPE,
    }

    /**
     * HashMap of the String extras for this message. Will only be created
     * when it is used.
     */
    HashMap<NET_HANDLER_EXTRA_STRING, String> pStringExtras = null;
    /**
     * HashMap of the Integer extras for this message. Will only be created
     * when it is used.
     */
    HashMap<NET_HANDLER_EXTRA_INT, Integer> pIntExtras = null;

    private MPDResponseHandler pResponseHandler = null;

    /**
     * The action type for this message.
     */
    private NET_HANDLER_ACTION pAction;


    /**
     * Simple public constructor.
     * @param action The type of action for this message.
     */
    public MPDHandlerAction(NET_HANDLER_ACTION action) {
        pAction = action;
    }


    /**
     *
     * @return The action of this message.
     */
    public NET_HANDLER_ACTION getAction() {
        return pAction;
    }

    /**
     * Allows to put extras in Handler messages like the strings of artists,albums, etc.
     * @param type Type of the extra value
     * @param value Value of the extra
     */
    public void setStringExtra(NET_HANDLER_EXTRA_STRING type, String value) {
        /*
         * If the HashMap is not already created, create it.
         */
        if ( null == pStringExtras ) {
            pStringExtras = new HashMap<>();
        }
        pStringExtras.put(type, value);
    }

    /**
     * Allows to put extras in Handler messages like the Integers of the hosts port.
     * @param type Type of the extra value
     * @param value Value of the extra
     */
    public void setIntExtras(NET_HANDLER_EXTRA_INT type, Integer value ) {
        /*
         * If the HashMap is not already created, create it.
         */
        if ( null == pIntExtras ) {
            pIntExtras = new HashMap<>();
        }
        pIntExtras.put(type, value);
    }

    /**
     * Set the handler that is needed for the asynchronous response. Can be a Handler running in
     * the main UI thread.
     * @param responseHandler The Handler to be used for the response. Should be of the correct
     *                        type used for handling the type of resposne.
     */
    public void setResponseHandler(MPDResponseHandler responseHandler) {
        pResponseHandler = responseHandler;
    }

    /**
     * Returns the responsehandler that should be used for handling the asynchronous response.
     * @return The Handler for the MPD response.
     */
    public MPDResponseHandler getResponseHandler() {
        return pResponseHandler;
    }

    /**
     *
     * @param type Type of the extra value
     * @return The value of the extra value and null if not attached to this message
     */
    public String getStringExtra(NET_HANDLER_EXTRA_STRING type ) {
        /*
         * If the HashMap is not already created return null.
         */
        if ( null == pStringExtras ) {
            return null;
        }
        return pStringExtras.get(type);
    }

    /**
     *
     * @param type Type of the extra value
     * @return The value of the extra value and null if not attached to this message
     */
    public Integer getIntExtra(NET_HANDLER_EXTRA_INT type ) {
        /*
         * If the HashMap is not already created return null.
         */
        if ( null == pIntExtras ) {
            return null;
        }
        return pIntExtras.get(type);
    }

}
