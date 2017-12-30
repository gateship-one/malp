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


import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDOutput;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDStatistics;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;

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

            List<MPDAlbum> resultList = new ArrayList<>(result);

            // Sort the created list
            Collections.sort(resultList);
            return resultList;
        } else {
            return MPDResponseParser.parseMPDAlbums(connection);
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

        return MPDResponseParser.parseMPDTracks(connection, "", "");
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
        return MPDResponseParser.parseMPDTracks(connection, "", "");
    }

    /**
     * Requests the current playlist of the server with a window
     *
     * @return List of MPDTrack items with all tracks of the current playlist
     */
    public static List<MPDFileEntry> getCurrentPlaylistWindow(final MPDConnection connection, int start, int end) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_PLAYLIST_WINDOW(start, end));

        /* Parse the return */
        return MPDResponseParser.parseMPDTracks(connection, "", "");
    }

    /**
     * Requests the current playlist of the server
     *
     * @return List of MPDTrack items with all tracks of the current playlist
     */
    public static List<MPDFileEntry> getSavedPlaylist(final MPDConnection connection, String playlistName) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_SAVED_PLAYLIST(playlistName));

        /* Parse the return */
        return MPDResponseParser.parseMPDTracks(connection, "", "");
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
        return MPDResponseParser.parseMPDTracks(connection, "", "");
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
        return MPDResponseParser.parseMPDTracks(connection, "", "");
    }

    /**
     * Requests the currentstatus package from the mpd server.
     *
     * @return The CurrentStatus object with all gathered information.
     */
    public static MPDCurrentStatus getCurrentServerStatus(final MPDConnection connection) throws MPDException {
        /* Request status */
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_STATUS);
        return MPDResponseParser.parseMPDCurrentStatus(connection);
    }

    /**
     * Requests the server statistics package from the mpd server.
     *
     * @return The CurrentStatus object with all gathered information.
     */
    public static MPDStatistics getServerStatistics(final MPDConnection connection) throws MPDException {
        /* Request status */
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_STATISTICS);

        return MPDResponseParser.parseMPDStatistic(connection);
    }

    /**
     * This will query the current song playing on the mpd server.
     *
     * @return MPDTrack entry for the song playing.
     */
    public static MPDTrack getCurrentSong(final MPDConnection connection) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_SONG);

        // Reuse the parsing function for tracks here.
        List<MPDFileEntry> retList;

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
     */
    public static void pause(final MPDConnection connection, boolean pause) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_PAUSE(pause));
    }

    /**
     * Jumps to the next song
     */
    public static void nextSong(final MPDConnection connection) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_NEXT);
    }

    /**
     * Jumps to the previous song
     */
    public static void previousSong(final MPDConnection connection) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_PREVIOUS);
    }

    /**
     * Stops playback
     */
    public static void stopPlayback(final MPDConnection connection) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_STOP);
    }

    /**
     * Sets random to true or false
     *
     * @param random If random should be set (true) or not (false)
     */
    public static void setRandom(final MPDConnection connection, boolean random) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_SET_RANDOM(random));
    }

    /**
     * Sets repeat to true or false
     *
     * @param repeat If repeat should be set (true) or not (false)
     */
    public static void setRepeat(final MPDConnection connection, boolean repeat) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_SET_REPEAT(repeat));
    }

    /**
     * Sets single playback to enable (true) or disabled (false)
     *
     * @param single if single playback should be enabled or not.
     */
    public static void setSingle(final MPDConnection connection, boolean single) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_SET_SINGLE(single));
    }

    /**
     * Sets if files should be removed after playback (consumed)
     *
     * @param consume True if yes and false if not.
     */
    public static void setConsume(final MPDConnection connection, boolean consume) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_SET_CONSUME(consume));
    }

    /**
     * Plays the song with the index in the current playlist.
     *
     * @param index Index of the song that should be played.
     */
    public static void playSongIndex(final MPDConnection connection, int index) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_PLAY_SONG_INDEX(index));
    }

    /**
     * Seeks the currently playing song to a certain position
     *
     * @param seconds Position in seconds to which a seek is requested to.
     */
    public static void seekSeconds(final MPDConnection connection, int seconds) throws MPDException {
        MPDCurrentStatus status;

        status = getCurrentServerStatus(connection);

        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_SEEK_SECONDS(status.getCurrentSongIndex(), seconds));
    }

    /**
     * Sets the volume of the mpd servers output. It is an absolute value between (0-100).
     *
     * @param volume Volume to set to the server.
     */
    public static void setVolume(final MPDConnection connection, int volume) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_SET_VOLUME(volume));
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
     */
    public static void addTrackList(final MPDConnection connection, List<MPDFileEntry> tracks) throws MPDException {
        if (null == tracks) {
            return;
        }
        connection.startCommandList();

        for (MPDFileEntry track : tracks) {
            if (track instanceof MPDTrack) {
                connection.sendMPDRAWCommand(MPDCommands.MPD_COMMAND_ADD_FILE(track.getPath()));
            }
        }
        connection.endCommandList();
    }

    /**
     * Adds all tracks from a certain album from artistname to the current playlist.
     *
     * @param albumname  Name of the album to add to the current playlist.
     * @param artistname Name of the artist of the album to add to the list. This
     *                   allows filtering of album tracks to a specified artist. Can also
     *                   be left empty then all tracks from the album will be added.
     */
    public static void addAlbumTracks(final MPDConnection connection, String albumname, String artistname, String mbid) throws MPDException {
        List<MPDFileEntry> tracks = getArtistAlbumTracks(connection, albumname, artistname, mbid);
        addTrackList(connection, tracks);
    }

    /**
     * Adds all albums of an artist to the current playlist. Will first get a list of albums for the
     * artist and then call addAlbumTracks for every album on this result.
     *
     * @param artistname Name of the artist to enqueue the albums from.
     */
    public static void addArtist(final MPDConnection connection, String artistname, MPDAlbum.MPD_ALBUM_SORT_ORDER sortOrder) throws MPDException {
        List<MPDAlbum> albums = getArtistAlbums(connection, artistname);
        if (null == albums) {
            return;
        }

        // Check if sort by date is active and resort collection first
        if (sortOrder == MPDAlbum.MPD_ALBUM_SORT_ORDER.DATE) {
            Collections.sort(albums, new MPDAlbum.MPDAlbumDateComparator());
        }

        for (MPDAlbum album : albums) {
            // This will add all tracks from album where artistname is either the artist or
            // the album artist.
            addAlbumTracks(connection, album.getName(), artistname, "");
        }
    }

    /**
     * Adds a single File/Directory to the current playlist.
     *
     * @param url URL of the file or directory! to add to the current playlist.
     */
    public static void addSong(final MPDConnection connection, String url) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_ADD_FILE(url));
    }

    /**
     * This method adds a song to a specified positiion in the current playlist.
     * This allows GUI developers to implement a method like "add after current".
     *
     * @param url   URL to add to the playlist.
     * @param index Index at which the item should be added.
     */
    public static void addSongatIndex(final MPDConnection connection, String url, int index) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_ADD_FILE_AT_INDEX(url, index));
    }

    /**
     * Adds files to the playlist with a search term for a specific type
     *
     * @param term The search term to use
     * @param type The type of items to search
     */
    public static void addSearchedFiles(final MPDConnection connection, String term, MPDCommands.MPD_SEARCH_TYPE type) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_ADD_SEARCH_FILES(term, type));
    }

    /**
     * Instructs the mpd server to clear its current playlist.
     */
    public static void clearPlaylist(final MPDConnection connection) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_CLEAR_PLAYLIST);
    }

    /**
     * Instructs the mpd server to shuffle its current playlist.
     */
    public static void shufflePlaylist(final MPDConnection connection) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_SHUFFLE_PLAYLIST);
    }

    /**
     * Instructs the mpd server to remove one item from the current playlist at index.
     *
     * @param index Position of the item to remove from current playlist.
     */
    public static void removeIndex(final MPDConnection connection, int index) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_SONG_FROM_CURRENT_PLAYLIST(index));
    }

    /**
     * Instructs the mpd server to remove an range of songs from current playlist.
     *
     * @param start Start of songs to remoge
     * @param end   End of the range
     */
    public static void removeRange(final MPDConnection connection, int start, int end) throws MPDException {
        // Check capabilities if removal with one command is possible
        if (connection.getServerCapabilities().hasCurrentPlaylistRemoveRange()) {
            connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_RANGE_FROM_CURRENT_PLAYLIST(start, end + 1));
        } else {
            // Create commandlist instead
            connection.startCommandList();
            for (int i = start; i <= end; i++) {
                connection.sendMPDRAWCommand(MPDCommands.MPD_COMMAND_REMOVE_SONG_FROM_CURRENT_PLAYLIST(start));
            }
            connection.endCommandList();
        }
    }

    /**
     * Moves one item from an index in the current playlist to an new index. This allows to move
     * tracks for example after the current to priotize songs.
     *
     * @param from Item to move from.
     * @param to   Position to enter item
     */
    public static void moveSongFromTo(final MPDConnection connection, int from, int to) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_MOVE_SONG_FROM_INDEX_TO_INDEX(from, to));
    }

    /**
     * Saves the current playlist as a new playlist with a name.
     *
     * @param name Name of the playlist to save to.
     */
    public static void savePlaylist(final MPDConnection connection, String name) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_SAVE_PLAYLIST(name));
    }

    /**
     * Adds a song to the saved playlist
     *
     * @param playlistName Name of the playlist to add the url to.
     * @param url          URL to add to the saved playlist
     */
    public static void addSongToPlaylist(final MPDConnection connection, String playlistName, String url) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_ADD_TRACK_TO_PLAYLIST(playlistName, url));
    }

    /**
     * Removes a song from a saved playlist
     *
     * @param playlistName Name of the playlist of which the song should be removed from
     * @param position     Index of the song to remove from the lits
     */
    public static void removeSongFromPlaylist(final MPDConnection connection, String playlistName, int position) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_TRACK_FROM_PLAYLIST(playlistName, position));
    }

    /**
     * Removes a saved playlist from the servers database.
     *
     * @param name Name of the playlist to remove.
     */
    public static void removePlaylist(final MPDConnection connection, String name) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_PLAYLIST(name));
    }

    /**
     * Loads a saved playlist (added after the last song) to the current playlist.
     *
     * @param name Of the playlist to add to.
     */
    public static void loadPlaylist(final MPDConnection connection, String name) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_LOAD_PLAYLIST(name));
    }


    /**
     * Returns the list of MPDOutputs to the outside callers.
     *
     * @return List of MPDOutput objects or null in case of error.
     */
    public static List<MPDOutput> getOutputs(final MPDConnection connection) throws MPDException {
        connection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_OUTPUTS);

        return MPDResponseParser.parseMPDOutputs(connection);
    }

    /**
     * Toggles the state of the output with the id.
     *
     * @param id Id of the output to toggle (active/deactive)
     */
    public static void toggleOutput(final MPDConnection connection, int id) throws MPDException {
        if (connection.getServerCapabilities().hasToggleOutput()) {
            connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_TOGGLE_OUTPUT(id));
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
    }

    /**
     * Enable the output with the id.
     *
     * @param id Id of the output to enable (active/deactive)
     */
    public static void enableOutput(final MPDConnection connection, int id) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_ENABLE_OUTPUT(id));
    }

    /**
     * Disable the output with the id.
     *
     * @param id Id of the output to disable (active/deactive)
     */
    public static void disableOutput(final MPDConnection connection, int id) throws MPDException {
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_DISABLE_OUTPUT(id));
    }

    /**
     * Instructs to update the database of the mpd server.
     *
     * @param path Path to update
     */
    public static void updateDatabase(final MPDConnection connection, String path) throws MPDException {
        // Update root directory
        connection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_UPDATE_DATABASE(path));
    }
}
