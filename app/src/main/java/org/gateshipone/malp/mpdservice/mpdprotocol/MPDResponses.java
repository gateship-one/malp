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

package org.gateshipone.malp.mpdservice.mpdprotocol;


public class MPDResponses {
    public static final String MPD_RESPONSE_ALBUM_NAME = "Album: ";
    public static final String MPD_RESPONSE_ALBUM_MBID = "MUSICBRAINZ_ALBUMID: ";

    public static final String MPD_RESPONSE_ARTIST_NAME = "Artist: ";
    public static final String MPD_RESPONSE_ALBUMARTIST_NAME = "AlbumArtist: ";
    public static final String MPD_RESPONSE_FILE = "file: ";
    public static final String MPD_RESPONSE_DIRECTORY = "directory: ";
    public static final String MPD_RESPONSE_TRACK_TITLE = "Title: ";
    public static final String MPD_RESPONSE_ALBUM_ARTIST_NAME = "AlbumArtist: ";
    public static final String MPD_RESPONSE_TRACK_TIME = "Time: ";
    public static final String MPD_RESPONSE_DATE = "Date: ";
    public static final String MPD_RESPONSE_TRACK_NAME = "Name: ";

    public static final String MPD_RESPONSE_TRACK_MBID = "MUSICBRAINZ_TRACKID: ";
    public static final String MPD_RESPONSE_ALBUM_ARTIST_MBID = "MUSICBRAINZ_ALBUMARTISTID: ";
    public static final String MPD_RESPONSE_ARTIST_MBID = "MUSICBRAINZ_ARTISTID: ";
    public static final String MPD_RESPONSE_TRACK_NUMBER = "Track: ";
    public static final String MPD_RESPONSE_DISC_NUMBER = "Disc: ";
    public static final String MPD_RESPONSE_SONG_POS = "Pos: ";
    public static final String MPD_RESPONSE_SONG_ID = "Id: ";


    public static final String MPD_RESPONSE_PLAYLIST = "playlist: ";
    public static final String MPD_RESPONSE_LAST_MODIFIED = "Last-Modified: ";

    /* MPD currentstatus responses */
    public static final String MPD_RESPONSE_VOLUME = "volume: ";
    public static final String MPD_RESPONSE_REPEAT = "repeat: ";
    public static final String MPD_RESPONSE_RANDOM = "random: ";
    public static final String MPD_RESPONSE_SINGLE = "single: ";
    public static final String MPD_RESPONSE_CONSUME = "consume: ";
    public static final String MPD_RESPONSE_PLAYLIST_VERSION = "playlist: ";
    public static final String MPD_RESPONSE_PLAYLIST_LENGTH = "playlistlength: ";
    public static final String MPD_RESPONSE_CURRENT_SONG_INDEX = "song: ";
    public static final String MPD_RESPONSE_CURRENT_SONG_ID = "songid: ";
    public static final String MPD_RESPONSE_NEXT_SONG_INDEX = "nextsong: ";
    public static final String MPD_RESPONSE_NEXT_SONG_ID = "nextsongid: ";
    public static final String MPD_RESPONSE_TIME_INFORMATION_OLD = "time: ";
    public static final String MPD_RESPONSE_ELAPSED_TIME = "elapsed: ";
    public static final String MPD_RESPONSE_DURATION = "duration: ";
    public static final String MPD_RESPONSE_BITRATE = "bitrate: ";
    public static final String MPD_RESPONSE_AUDIO_INFORMATION = "audio: ";
    public static final String MPD_RESPONSE_UPDATING_DB = "updating_db: ";
    public static final String MPD_RESPONSE_ERROR = "error: ";

    public static final String MPD_RESPONSE_CHANGED = "changed: ";

    public static final String MPD_RESPONSE_PLAYBACK_STATE = "state: ";
    public static final String MPD_PLAYBACK_STATE_RESPONSE_PLAY = "play";
    public static final String MPD_PLAYBACK_STATE_RESPONSE_PAUSE = "pause";
    public static final String MPD_PLAYBACK_STATE_RESPONSE_STOP = "stop";

    public static final String MPD_OUTPUT_ID = "outputid: ";
    public static final String MPD_OUTPUT_NAME = "outputname: ";
    public static final String MPD_OUTPUT_ACTIVE = "outputenabled: ";

    public static final String MPD_STATS_UPTIME = "uptime: ";
    public static final String MPD_STATS_PLAYTIME = "playtime: ";
    public static final String MPD_STATS_ARTISTS = "artists: ";
    public static final String MPD_STATS_ALBUMS = "albums: ";
    public static final String MPD_STATS_SONGS = "songs: ";
    public static final String MPD_STATS_DB_PLAYTIME = "db_playtime: ";
    public static final String MPD_STATS_DB_LAST_UPDATE = "db_update: ";

    public static final String MPD_COMMAND = "command: ";
    public static final String MPD_TAGTYPE = "tagtype: ";

    public static final String MPD_PARSE_ARGS_LIST_ERROR = "not able to parse args";
}