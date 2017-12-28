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

import android.util.Log;

import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDOutput;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDStatistics;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class MPDInterface {

    /*
     * **********************
     * * Request functions  *
     * **********************
     */

    /**
     * Get a list of all albums available in the database.
     *
     * @return List of MPDAlbum
     */
    public static List<MPDAlbum> getAlbums(final MPDConnection connection) throws MPDException {
        // Get a list of albums. Check if server is new enough for MB and AlbumArtist filtering
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMS(connection.getServerCapabilities()));

        // Remove empty albums at beginning of the list
        List<MPDAlbum> albums = MPDResponseParser.parseMPDAlbums(connection);
        ListIterator<MPDAlbum> albumIterator = albums.listIterator();
        while (albumIterator.hasNext()) {
            MPDAlbum album = albumIterator.next();
            if (album.getName().isEmpty()) {
                albumIterator.remove();
            } else {
                break;
            }
        }
        return albums;
    }

    /**
     * Get a list of all albums available in the database.
     *
     * @return List of MPDAlbum
     */
    public static List<MPDAlbum> getAlbumsInPath(final MPDConnection connection, String path) throws MPDException {
        // Get a list of albums. Check if server is new enough for MB and AlbumArtist filtering
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMS_FOR_PATH(path, connection.getServerCapabilities()));

        // Remove empty albums at beginning of the list
        List<MPDAlbum> albums = MPDResponseParser.parseMPDAlbums(connection);
        ListIterator<MPDAlbum> albumIterator = albums.listIterator();
        while (albumIterator.hasNext()) {
            MPDAlbum album = albumIterator.next();
            if (album.getName().isEmpty()) {
                albumIterator.remove();
            } else {
                break;
            }
        }
        return albums;
    }

    /**
     * Get a list of all albums by an artist where artist is part of or artist is the AlbumArtist (tag)
     *
     * @param artistName Artist to filter album list with.
     * @return List of MPDAlbum objects
     */
    public static List<MPDAlbum> getArtistAlbums(final MPDConnection connection, String artistName) throws MPDException {
        // Get all albums that artistName is part of (Also the legacy album list pre v. 0.19)
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ARTIST_ALBUMS(artistName, connection.getServerCapabilities()));

        if (connection.getServerCapabilities().hasTagAlbumArtist() && connection.getServerCapabilities().hasListGroup()) {
            // Use a hashset for the results, to filter duplicates that will exist.
            Set<MPDAlbum> result = new HashSet<>(MPDResponseParser.parseMPDAlbums(connection));

            // Also get the list where artistName matches on AlbumArtist
            connection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMARTIST_ALBUMS(artistName, connection.getServerCapabilities()));

            result.addAll(MPDResponseParser.parseMPDAlbums(connection));

            List<MPDAlbum> resultList = new ArrayList<MPDAlbum>(result);

            // Sort the created list
            Collections.sort(resultList);
            return resultList;
        } else {
            List<MPDAlbum> result = MPDResponseParser.parseMPDAlbums(connection);
            return result;
        }
    }

    /**
     * Get a list of all artists available in MPDs database
     *
     * @return List of MPDArtist objects
     */
    public static List<MPDArtist> getArtists(final MPDConnection connection) throws MPDException {
        // Get a list of artists. If server is new enough this will contain MBIDs for artists, that are tagged correctly.
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ARTISTS(connection.getServerCapabilities().hasListGroup() && connection.getServerCapabilities().hasMusicBrainzTags()));

        // Remove first empty artist
        List<MPDArtist> artists = MPDResponseParser.parseMPDArtists(connection, connection.getServerCapabilities().hasMusicBrainzTags(), connection.getServerCapabilities().hasListGroup());
        if (artists.size() > 0 && artists.get(0).getArtistName().isEmpty()) {
            artists.remove(0);
        }

        return artists;
    }

    /**
     * Get a list of all album artists available in MPDs database
     *
     * @return List of MPDArtist objects
     */
    public static List<MPDArtist> getAlbumArtists(final MPDConnection connection) throws MPDException {
        // Get a list of artists. If server is new enough this will contain MBIDs for artists, that are tagged correctly.
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMARTISTS(connection.getServerCapabilities().hasListGroup() && connection.getServerCapabilities().hasMusicBrainzTags()));

        List<MPDArtist> artists = MPDResponseParser.parseMPDArtists(connection, connection.getServerCapabilities().hasMusicBrainzTags(), connection.getServerCapabilities().hasListGroup());
        if (artists.size() > 0 && artists.get(0).getArtistName().isEmpty()) {
            artists.remove(0);
        }
        return artists;
    }

    /**
     * Get a list of all playlists available in MPDs database
     *
     * @return List of MPDArtist objects
     */
    public static List<MPDFileEntry> getPlaylists(final MPDConnection connection) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_SAVED_PLAYLISTS);
        List<MPDFileEntry> playlists = MPDResponseParser.parseMPDTracks(connection, "", "");
        Collections.sort(playlists);
        return playlists;
    }

    /**
     * Gets all tracks from MPD server. This could take a long time to process. Be warned.
     *
     * @return A list of all tracks in MPDTrack objects
     */
    public static List<MPDFileEntry> getAllTracks(final MPDConnection connection) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALL_FILES);

        List<MPDFileEntry> result = MPDResponseParser.parseMPDTracks(connection, "", "");
        return result;
    }


    /**
     * Returns the list of tracks that are part of albumName
     *
     * @param albumName Album to get tracks from
     * @return List of MPDTrack track objects
     */
    public static List<MPDFileEntry> getAlbumTracks(final MPDConnection connection, String albumName, String mbid) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUM_TRACKS(albumName));

        List<MPDFileEntry> result = MPDResponseParser.parseMPDTracks(connection, "", mbid);
        MPDSortHelper.sortFileListNumeric(result);
        return result;
    }

    /**
     * Returns the list of tracks that are part of albumName and from artistName
     *
     * @param albumName  Album name used as primary filter.
     * @param artistName Artist to filter with. This is checked with Artist AND AlbumArtist tag.
     * @param mbid       MusicBrainzID of the album to get tracks from. Necessary if one item with the
     *                   same name exists multiple times.
     * @return List of MPDTrack track objects
     */
    public static List<MPDFileEntry> getArtistAlbumTracks(final MPDConnection connection, String albumName, String artistName, String mbid) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUM_TRACKS(albumName));

            /* Filter tracks with artistName */
        List<MPDFileEntry> result = MPDResponseParser.parseMPDTracks(connection, artistName, mbid);
        // Sort with disc & track number
        MPDSortHelper.sortFileListNumeric(result);
        return result;
    }

    /**
     * Requests the current playlist of the server
     *
     * @return List of MPDTrack items with all tracks of the current playlist
     */
    public static List<MPDFileEntry> getCurrentPlaylist(final MPDConnection connection) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_PLAYLIST);

            /* Parse the return */
        List<MPDFileEntry> result = MPDResponseParser.parseMPDTracks(connection, "", "");
        return result;
    }

    /**
     * Requests the current playlist of the server with a window
     *
     * @return List of MPDTrack items with all tracks of the current playlist
     */
    public static List<MPDFileEntry> getCurrentPlaylistWindow(final MPDConnection connection, int start, int end) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_PLAYLIST_WINDOW(start, end));

            /* Parse the return */
        List<MPDFileEntry> result = MPDResponseParser.parseMPDTracks(connection, "", "");
        return result;
    }

    /**
     * Requests the current playlist of the server
     *
     * @return List of MPDTrack items with all tracks of the current playlist
     */
    public static List<MPDFileEntry> getSavedPlaylist(final MPDConnection connection, String playlistName) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_SAVED_PLAYLIST(playlistName));

            /* Parse the return */
        List<MPDFileEntry> result = MPDResponseParser.parseMPDTracks(connection, "", "");
        return result;
    }

    /**
     * Requests the files for a specific path with info
     *
     * @return List of MPDTrack items with all tracks of the current playlist
     */
    public static List<MPDFileEntry> getFiles(final MPDConnection connection, String path) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_FILES_INFO(path));

        /* Parse the return */
        List<MPDFileEntry> retList = MPDResponseParser.parseMPDTracks(connection, "", "");
        Collections.sort(retList);
        return retList;
    }

    /**
     * Requests the files for a specific search term and type
     *
     * @param term The search term to use
     * @param type The type of items to search
     * @return List of MPDTrack items with all tracks matching the search
     */
    public static List<MPDFileEntry> getSearchedFiles(final MPDConnection connection, String term, MPDCommands.MPD_SEARCH_TYPE type) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_SEARCH_FILES(term, type));

            /* Parse the return */
        List<MPDFileEntry> result = MPDResponseParser.parseMPDTracks(connection, "", "");
        return result;
    }

    /**
     * Searches a URL in the current playlist. If available the track is part of the returned list.
     *
     * @param url URL to search in the current playlist.
     * @return List with one entry or none.
     */
    public static List<MPDFileEntry> getPlaylistFindTrack(final MPDConnection connection, String url) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_PLAYLIST_FIND_URI(url));

            /* Parse the return */
        List<MPDFileEntry> result = MPDResponseParser.parseMPDTracks(connection, "", "");
        return result;
    }

    /**
     * Requests the currentstatus package from the mpd server.
     *
     * @return The CurrentStatus object with all gathered information.
     */
    public static MPDCurrentStatus getCurrentServerStatus(final MPDConnection connection) throws MPDException {
        /* Request status */
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_STATUS);
        MPDCurrentStatus retStatus = MPDResponseParser.parseMPDCurrentStatus(connection);

        return retStatus;
    }

    /**
     * Requests the server statistics package from the mpd server.
     *
     * @return The CurrentStatus object with all gathered information.
     */
    public static MPDStatistics getServerStatistics(final MPDConnection connection) throws MPDException {
        /* Request status */
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_STATISTICS);

        MPDStatistics stats = null;

        stats = MPDResponseParser.parseMPDStatistic(connection);

        return stats;
    }

    /**
     * This will query the current song playing on the mpd server.
     *
     * @return MPDTrack entry for the song playing.
     */
    public static MPDTrack getCurrentSong(final MPDConnection connection) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_SONG);

        // Reuse the parsing function for tracks here.
        List<MPDFileEntry> retList = null;

        retList = MPDResponseParser.parseMPDTracks(connection, "", "");

        if (retList.size() == 1) {
            MPDFileEntry tmpFileEntry = retList.get(0);
            if (null != tmpFileEntry && tmpFileEntry instanceof MPDTrack) {
                return (MPDTrack) tmpFileEntry;
            }
            return null;
        } else {
            return null;
        }
    }


    /*
     ***********************
     *    Control commands *
     ***********************
     */

    /**
     * Sends the pause commando to MPD.
     *
     * @param pause 1 if playback should be paused, 0 if resumed
     * @return
     */
    public static boolean pause(final MPDConnection connection, boolean pause) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_PAUSE(pause));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Jumps to the next song
     *
     * @return true if successful, false otherwise
     */
    public static boolean nextSong(final MPDConnection connection) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_NEXT);

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Jumps to the previous song
     *
     * @return true if successful, false otherwise
     */
    public static boolean previousSong(final MPDConnection connection) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_PREVIOUS);

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Stops playback
     *
     * @return true if successful, false otherwise
     */
    public static boolean stopPlayback(final MPDConnection connection) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_STOP);

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Sets random to true or false
     *
     * @param random If random should be set (true) or not (false)
     * @return True if server responded with ok
     */
    public static boolean setRandom(final MPDConnection connection, boolean random) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_SET_RANDOM(random));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Sets repeat to true or false
     *
     * @param repeat If repeat should be set (true) or not (false)
     * @return True if server responded with ok
     */
    public static boolean setRepeat(final MPDConnection connection, boolean repeat) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_SET_REPEAT(repeat));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Sets single playback to enable (true) or disabled (false)
     *
     * @param single if single playback should be enabled or not.
     * @return True if server responded with ok
     */
    public static boolean setSingle(final MPDConnection connection, boolean single) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_SET_SINGLE(single));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Sets if files should be removed after playback (consumed)
     *
     * @param consume True if yes and false if not.
     * @return True if server responded with ok
     */
    public static boolean setConsume(final MPDConnection connection, boolean consume) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_SET_CONSUME(consume));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Plays the song with the index in the current playlist.
     *
     * @param index Index of the song that should be played.
     * @return True if server responded with ok
     */
    public static boolean playSongIndex(final MPDConnection connection, int index) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_PLAY_SONG_INDEX(index));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Seeks the currently playing song to a certain position
     *
     * @param seconds Position in seconds to which a seek is requested to.
     * @return True if server responded with ok
     */
    public static boolean seekSeconds(final MPDConnection connection, int seconds) throws MPDException {
        MPDCurrentStatus status = null;

        status = getCurrentServerStatus(connection);


        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_SEEK_SECONDS(status.getCurrentSongIndex(), seconds));

            /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Sets the volume of the mpd servers output. It is an absolute value between (0-100).
     *
     * @param volume Volume to set to the server.
     * @return True if server responded with ok
     */
    public static boolean setVolume(final MPDConnection connection, int volume) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_SET_VOLUME(volume));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /*
     ***********************
     *    Queue commands   *
     ***********************
     */

    /**
     * This method adds songs in a bulk command list. Should be reasonably in performance this way.
     *
     * @param tracks List of MPDFileEntry objects to add to the current playlist.
     * @return True if server responded with ok
     */
    public static boolean addTrackList(final MPDConnection connection, List<MPDFileEntry> tracks) throws MPDException {
        if (null == tracks) {
            return false;
        }
        connection.startCommandList();

        for (MPDFileEntry track : tracks) {
            if (track instanceof MPDTrack) {
                connection.sendMPDRAWCommand(MPDCommands.MPD_COMMAND_ADD_FILE(track.getPath()));
            }
        }
        connection.endCommandList();

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Adds all tracks from a certain album from artistname to the current playlist.
     *
     * @param albumname  Name of the album to add to the current playlist.
     * @param artistname Name of the artist of the album to add to the list. This
     *                   allows filtering of album tracks to a specified artist. Can also
     *                   be left empty then all tracks from the album will be added.
     * @return True if server responded with ok
     */
    public static boolean addAlbumTracks(final MPDConnection connection, String albumname, String artistname, String mbid) throws MPDException {
        List<MPDFileEntry> tracks = getArtistAlbumTracks(connection, albumname, artistname, mbid);
        return addTrackList(connection, tracks);
    }

    /**
     * Adds all albums of an artist to the current playlist. Will first get a list of albums for the
     * artist and then call addAlbumTracks for every album on this result.
     *
     * @param artistname Name of the artist to enqueue the albums from.
     * @return True if server responded with ok
     */
    public static boolean addArtist(final MPDConnection connection, String artistname, MPDAlbum.MPD_ALBUM_SORT_ORDER sortOrder) throws MPDException {
        List<MPDAlbum> albums = getArtistAlbums(connection, artistname);
        if (null == albums) {
            return false;
        }

        // Check if sort by date is active and resort collection first
        if (sortOrder == MPDAlbum.MPD_ALBUM_SORT_ORDER.DATE) {
            Collections.sort(albums, new MPDAlbum.MPDAlbumDateComparator());
        }

        boolean success = true;
        for (MPDAlbum album : albums) {
            // This will add all tracks from album where artistname is either the artist or
            // the album artist.
            if (!(addAlbumTracks(connection, album.getName(), artistname, ""))) {
                success = false;
            }
        }
        return success;
    }

    /**
     * Adds a single File/Directory to the current playlist.
     *
     * @param url URL of the file or directory! to add to the current playlist.
     * @return True if server responded with ok
     */
    public static boolean addSong(final MPDConnection connection, String url) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_ADD_FILE(url));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * This method adds a song to a specified positiion in the current playlist.
     * This allows GUI developers to implement a method like "add after current".
     *
     * @param url   URL to add to the playlist.
     * @param index Index at which the item should be added.
     * @return True if server responded with ok
     */
    public static boolean addSongatIndex(final MPDConnection connection, String url, int index) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_ADD_FILE_AT_INDEX(url, index));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Adds files to the playlist with a search term for a specific type
     *
     * @param term The search term to use
     * @param type The type of items to search
     * @return True if server responded with ok
     */
    public static boolean addSearchedFiles(final MPDConnection connection, String term, MPDCommands.MPD_SEARCH_TYPE type) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_ADD_SEARCH_FILES(term, type));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Instructs the mpd server to clear its current playlist.
     *
     * @return True if server responded with ok
     */
    public static boolean clearPlaylist(final MPDConnection connection) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_CLEAR_PLAYLIST);

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Instructs the mpd server to shuffle its current playlist.
     *
     * @return True if server responded with ok
     */
    public static boolean shufflePlaylist(final MPDConnection connection) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_SHUFFLE_PLAYLIST);

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Instructs the mpd server to remove one item from the current playlist at index.
     *
     * @param index Position of the item to remove from current playlist.
     * @return True if server responded with ok
     */
    public static boolean removeIndex(final MPDConnection connection, int index) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_SONG_FROM_CURRENT_PLAYLIST(index));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Instructs the mpd server to remove an range of songs from current playlist.
     *
     * @param start Start of songs to remoge
     * @param end   End of the range
     * @return True if server responded with ok
     */
    public static boolean removeRange(final MPDConnection connection, int start, int end) throws MPDException {
        // Check capabilities if removal with one command is possible
        if (connection.getServerCapabilities().hasCurrentPlaylistRemoveRange()) {
            connection.sendMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_RANGE_FROM_CURRENT_PLAYLIST(start, end + 1));
        } else {
            // Create commandlist instead
            connection.startCommandList();
            for (int i = start; i <= end; i++) {
                connection.sendMPDRAWCommand(MPDCommands.MPD_COMMAND_REMOVE_SONG_FROM_CURRENT_PLAYLIST(start));
            }
            connection.endCommandList();
        }


        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Moves one item from an index in the current playlist to an new index. This allows to move
     * tracks for example after the current to priotize songs.
     *
     * @param from Item to move from.
     * @param to   Position to enter item
     * @return
     */
    public static boolean moveSongFromTo(final MPDConnection connection, int from, int to) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_MOVE_SONG_FROM_INDEX_TO_INDEX(from, to));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Saves the current playlist as a new playlist with a name.
     *
     * @param name Name of the playlist to save to.
     * @return True if server responded with ok
     */
    public static boolean savePlaylist(final MPDConnection connection, String name) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_SAVE_PLAYLIST(name));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Adds a song to the saved playlist
     *
     * @param playlistName Name of the playlist to add the url to.
     * @param url          URL to add to the saved playlist
     * @return True if server responded with ok
     */
    public static boolean addSongToPlaylist(final MPDConnection connection, String playlistName, String url) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_ADD_TRACK_TO_PLAYLIST(playlistName, url));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Removes a song from a saved playlist
     *
     * @param playlistName Name of the playlist of which the song should be removed from
     * @param position     Index of the song to remove from the lits
     * @return
     */
    public static boolean removeSongFromPlaylist(final MPDConnection connection, String playlistName, int position) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_TRACK_FROM_PLAYLIST(playlistName, position));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Removes a saved playlist from the servers database.
     *
     * @param name Name of the playlist to remove.
     * @return True if server responded with ok
     */
    public static boolean removePlaylist(final MPDConnection connection, String name) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_PLAYLIST(name));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Loads a saved playlist (added after the last song) to the current playlist.
     *
     * @param name Of the playlist to add to.
     * @return True if server responded with ok
     */
    public static boolean loadPlaylist(final MPDConnection connection, String name) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_LOAD_PLAYLIST(name));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }


    /**
     * Returns the list of MPDOutputs to the outside callers.
     *
     * @return List of MPDOutput objects or null in case of error.
     */
    public static List<MPDOutput> getOutputs(final MPDConnection connection) throws MPDException {

            connection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_OUTPUTS);


            List<MPDOutput> list = MPDResponseParser.parseMPDOutputs(connection);
            return list;
    }

    /**
     * Toggles the state of the output with the id.
     *
     * @param id Id of the output to toggle (active/deactive)
     * @return True if server responded with ok
     */
    public static boolean toggleOutput(final MPDConnection connection, int id) throws MPDException {
        if (connection.getServerCapabilities().hasToggleOutput()) {
            connection.sendMPDCommand(MPDCommands.MPD_COMMAND_TOGGLE_OUTPUT(id));
        } else {
            // Implement functionality with enable/disable
            List<MPDOutput> outputs = getOutputs(connection);
            if (id < outputs.size()) {
                if (outputs.get(id).getOutputState()) {
                    disableOutput(connection, id);
                } else {
                    enableOutput(connection, id);
                }
            }
        }

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Enable the output with the id.
     *
     * @param id Id of the output to enable (active/deactive)
     * @return True if server responded with ok
     */
    public static boolean enableOutput(final MPDConnection connection, int id) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_ENABLE_OUTPUT(id));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Disable the output with the id.
     *
     * @param id Id of the output to disable (active/deactive)
     * @return True if server responded with ok
     */
    public static boolean disableOutput(final MPDConnection connection, int id) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_DISABLE_OUTPUT(id));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }

    /**
     * Instructs to update the database of the mpd server.
     *
     * @param path Path to update
     * @return True if server responded with ok
     */
    public static boolean updateDatabase(final MPDConnection connection, String path) throws MPDException {
        // Update root directory
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_UPDATE_DATABASE(path));

        /* Return the response value of MPD */
        return connection.checkResponse();
    }
}
