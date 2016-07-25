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

package andrompd.org.andrompd.mpdservice.mpdprotocol;

public class MPDCommands {

    public static final String MPD_COMMAND_CLOSE = "close";
    public static final String MPD_COMMAND_NODIDLE = "noidle";

    public static final String MPD_COMMAND_PASSWORD = "password ";

    /* Database request commands */
    public static final String MPD_COMMAND_REQUEST_ALBUMS = "list album";
    public static final String MPD_COMMAND_REQUEST_ALBUMS_WITH_MBID = "list album group MUSICBRAINZ_ALBUMID";


    public static String MPD_COMMAND_REQUEST_ARTIST_ALBUMS(String artistName) {
        return "list album \"" + artistName + "\"";
    }

    public static String MPD_COMMAND_REQUEST_ARTIST_ALBUMS_WITH_MBID(String artistName) {
        return "list album artist \"" + artistName + "\" group MUSICBRAINZ_ALBUMID";
    }

    public static String MPD_COMMAND_REQUEST_ALBUM_TRACKS(String albumName) {
        return "find album \"" + albumName + "\"";
    }

    public static final String MPD_COMMAND_REQUEST_ARTISTS = "list artist";

    public static final String MPD_COMMAND_REQUEST_ALL_FILES = "listallinfo";

    /* Control commands */
    public static String MPD_COMMAND_PAUSE(boolean pause) {
        return "pause " + (pause ? "1" : "0");
    }

    public static final String MPD_COMMAND_NEXT = "next";
    public static final String MPD_COMMAND_PREVIOUS = "previous";
    public static final String MPD_COMMAND_STOP = "stop";

    public static final String MPD_COMMAND_GET_CURRENT_STATUS = "status";

    public static final String MPD_COMMAND_GET_CURRENT_PLAYLIST = "playlistinfo";
}
