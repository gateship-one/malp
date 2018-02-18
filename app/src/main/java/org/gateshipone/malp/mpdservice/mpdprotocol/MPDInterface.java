/*
 *  Copyright (C) 2018 Team Gateship-One
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


import org.gateshipone.malp.mpdservice.handlers.MPDConnectionStateChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.MPDIdleChangeHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDOutput;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDStatistics;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class MPDInterface {
    private final String TAG = MPDInterface.class.getSimpleName();

    private final MPDConnection mConnection;

    // Singleton instance
    public static MPDInterface mInstance = new MPDInterface();

    private MPDInterface() {
        mConnection = new MPDConnection();
    }

    // Connection methods

    public void setServerParameters(String hostname, String password, int port) {
        mConnection.setServerParameters(hostname, password, port);
    }

    public synchronized void connect() throws MPDException {
        mConnection.connectToServer();
    }

    public synchronized void disconnect() {
        mConnection.disconnectFromServer();
    }

    public boolean isConnected() {
        return mConnection.isConnected();
    }

    // Observer methods
    public void addMPDConnectionStateChangeListener(MPDConnectionStateChangeHandler listener) {
        mConnection.addConnectionStateChangeHandler(listener);
    }

    public void removeMPDConnectionStateChangeListener(MPDConnectionStateChangeHandler listener) {
        mConnection.removeConnectionStateChangeHandler(listener);
    }

    public void addMPDIdleChangeHandler(MPDIdleChangeHandler listener) {
        mConnection.setIdleListener(listener);
    }



    /*
     * **********************
     * * Request functions  *
     * **********************
     */

    public MPDCapabilities getServerCapabilities() {
        return mConnection.getServerCapabilities();
    }

    /**
     * Get a list of all albums available in the database.
     *
     * @return List of MPDAlbum
     */
    public List<MPDAlbum> getAlbums() throws MPDException {
        List<MPDAlbum> albums;
        synchronized (this) {
            // Get a list of albums. Check if server is new enough for MB and AlbumArtist filtering
            mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMS(mConnection.getServerCapabilities()));

            // Remove empty albums at beginning of the list
            albums = MPDResponseParser.parseMPDAlbums(mConnection);
        }
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
    public List<MPDAlbum> getAlbumsInPath(String path) throws MPDException {
        List<MPDAlbum> albums;
        synchronized (this) {
            // Get a list of albums. Check if server is new enough for MB and AlbumArtist filtering
            mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMS_FOR_PATH(path, mConnection.getServerCapabilities()));

            // Remove empty albums at beginning of the list
            albums = MPDResponseParser.parseMPDAlbums(mConnection);
        }
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
    public List<MPDAlbum> getArtistAlbums(String artistName) throws MPDException {
        MPDCapabilities capabilities = mConnection.getServerCapabilities();


        if (capabilities.hasTagAlbumArtist() && capabilities.hasListGroup()) {
            Set<MPDAlbum> result;
            synchronized (this) {
                // Get all albums that artistName is part of (Also the legacy album list pre v. 0.19)
                mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ARTIST_ALBUMS(artistName, capabilities));
                // Use a hashset for the results, to filter duplicates that will exist.
                result = new HashSet<>(MPDResponseParser.parseMPDAlbums(mConnection));

                // Also get the list where artistName matches on AlbumArtist
                mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMARTIST_ALBUMS(artistName, capabilities));

                result.addAll(MPDResponseParser.parseMPDAlbums(mConnection));
            }

            List<MPDAlbum> resultList = new ArrayList<>(result);

            // Sort the created list
            Collections.sort(resultList);
            return resultList;
        } else {
            synchronized (this) {
                // Get all albums that artistName is part of (Also the legacy album list pre v. 0.19)
                mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ARTIST_ALBUMS(artistName, capabilities));
                return MPDResponseParser.parseMPDAlbums(mConnection);
            }
        }
    }

    /**
     * Get a list of all albums by an artist where artist is part of or artist is the AlbumArtist (tag)
     *
     * @param artistName Artist to filter album list with.
     * @return List of MPDAlbum objects
     */
    public List<MPDAlbum> getArtistSortAlbums(String artistName) throws MPDException {
        MPDCapabilities capabilities = mConnection.getServerCapabilities();

        // Check if tag is supported
        if (!capabilities.hasTagArtistSort() || !capabilities.hasListFiltering() ) {
            return getArtistAlbums(artistName);
        }


        if (capabilities.hasTagAlbumArtist() && capabilities.hasListGroup()) {
            Set<MPDAlbum> result;
            synchronized (this) {
                // Get all albums that artistName is part of (Also the legacy album list pre v. 0.19)
                mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ARTISTSORT_ALBUMS(artistName, capabilities));
                // Use a hashset for the results, to filter duplicates that will exist.
                result = new HashSet<>(MPDResponseParser.parseMPDAlbums(mConnection));

                // Also get the list where artistName matches on AlbumArtistSort
                mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMARTISTSORT_ALBUMS(artistName, capabilities));

                result.addAll(MPDResponseParser.parseMPDAlbums(mConnection));
            }

            List<MPDAlbum> resultList = new ArrayList<>(result);

            // Sort the created list
            Collections.sort(resultList);
            return resultList;
        } else {
            synchronized (this) {
                // Get all albums that artistName is part of (Also the legacy album list pre v. 0.19)
                mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ARTIST_ALBUMS(artistName, capabilities));
                return MPDResponseParser.parseMPDAlbums(mConnection);
            }
        }
    }

    /**
     * Get a list of all artists available in MPDs database
     *
     * @return List of MPDArtist objects
     */
    public synchronized List<MPDArtist> getArtists() throws MPDException {
        // Get a list of artists. If server is new enough this will contain MBIDs for artists, that are tagged correctly.
        mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ARTISTS(mConnection.getServerCapabilities().hasListGroup() && mConnection.getServerCapabilities().hasMusicBrainzTags()));

        // Remove first empty artist
        List<MPDArtist> artists = MPDResponseParser.parseMPDArtists(mConnection, mConnection.getServerCapabilities().hasMusicBrainzTags(), mConnection.getServerCapabilities().hasListGroup());
        if (artists.size() > 0 && artists.get(0).getArtistName().isEmpty()) {
            artists.remove(0);
        }

        return artists;
    }

    /**
     * Get a list of all artists (tag: artistsort) available in MPDs database
     *
     * @return List of MPDArtist objects
     */
    public synchronized List<MPDArtist> getArtistsSort() throws MPDException {
        MPDCapabilities capabilities = mConnection.getServerCapabilities();
        // Check if tag is supported
        if (!capabilities.hasTagArtistSort() ) {
            return getArtists();
        }

        // Get a list of artists. If server is new enough this will contain MBIDs for artists, that are tagged correctly.
        mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ARTISTS_SORT(mConnection.getServerCapabilities().hasListGroup() && mConnection.getServerCapabilities().hasMusicBrainzTags()));

        // Remove first empty artist
        List<MPDArtist> artists = MPDResponseParser.parseMPDArtists(mConnection, mConnection.getServerCapabilities().hasMusicBrainzTags(), mConnection.getServerCapabilities().hasListGroup());
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
    public List<MPDArtist> getAlbumArtists() throws MPDException {
        // Get list of artists for MBID correction
        List<MPDArtist> normalArtists = getArtists();

        MPDCapabilities capabilities = mConnection.getServerCapabilities();

        List<MPDArtist> artists;
        synchronized (this) {

            // Get a list of artists. If server is new enough this will contain MBIDs for artists, that are tagged correctly.
            mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMARTISTS(capabilities.hasListGroup() && capabilities.hasMusicBrainzTags()));

            artists = MPDResponseParser.parseMPDArtists(mConnection, capabilities.hasMusicBrainzTags(), capabilities.hasListGroup());
        }

        // If MusicBrainz support is present, try to correct the MBIDs
        if (capabilities.hasMusicBrainzTags()) {
            // Merge normalArtists MBIDs with album artists MBIDs
            HashMap<String, MPDArtist> normalArtistsHashed = new HashMap<>();
            for (MPDArtist artist : normalArtists) {
                normalArtistsHashed.put(artist.getArtistName(), artist);
            }

            // For every albumartist try to get normal artistMBID
            for (MPDArtist artist : artists) {
                MPDArtist hashedArtist = normalArtistsHashed.get(artist.getArtistName());
                if (hashedArtist != null && hashedArtist.getMBIDCount() > 0) {
                    artist.setMBID(hashedArtist.getMBID(0));
                }
            }
        }

        // Remove first empty artist if present.
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
    public List<MPDArtist> getAlbumArtistsSort() throws MPDException {
        MPDCapabilities capabilities = mConnection.getServerCapabilities();

        // Check if tag is supported
        if (!capabilities.hasTagAlbumArtistSort()) {
            return getAlbumArtists();
        }

        // Get list of artists for MBID correction
        List<MPDArtist> normalArtists = getArtistsSort();


        List<MPDArtist> artists;
        synchronized (this) {

            // Get a list of artists. If server is new enough this will contain MBIDs for artists, that are tagged correctly.
            mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMARTISTS_SORT(capabilities.hasListGroup() && capabilities.hasMusicBrainzTags()));

            artists = MPDResponseParser.parseMPDArtists(mConnection, capabilities.hasMusicBrainzTags(), capabilities.hasListGroup());
        }

        // If MusicBrainz support is present, try to correct the MBIDs
        if (capabilities.hasMusicBrainzTags()) {
            // Merge normalArtists MBIDs with album artists MBIDs
            HashMap<String, MPDArtist> normalArtistsHashed = new HashMap<>();
            for (MPDArtist artist : normalArtists) {
                normalArtistsHashed.put(artist.getArtistName(), artist);
            }

            // For every albumartist try to get normal artistMBID
            for (MPDArtist artist : artists) {
                MPDArtist hashedArtist = normalArtistsHashed.get(artist.getArtistName());
                if (hashedArtist != null && hashedArtist.getMBIDCount() > 0) {
                    artist.setMBID(hashedArtist.getMBID(0));
                }
            }
        }

        // Remove first empty artist if present.
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
    public List<MPDFileEntry> getPlaylists() throws MPDException {
        List<MPDFileEntry> playlists;
        synchronized (this) {
            mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_SAVED_PLAYLISTS);
            playlists = MPDResponseParser.parseMPDTracks(mConnection);
        }
        Collections.sort(playlists);
        return playlists;
    }

    /**
     * Gets all tracks from MPD server. This could take a long time to process. Be warned.
     *
     * @return A list of all tracks in MPDTrack objects
     */
    public synchronized List<MPDFileEntry> getAllTracks() throws MPDException {
        mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALL_FILES);

        return MPDResponseParser.parseMPDTracks(mConnection);
    }


    /**
     * Returns the list of tracks that are part of albumName
     *
     * @param albumName Album to get tracks from
     * @return List of MPDTrack track objects
     */
    public List<MPDFileEntry> getAlbumTracks(String albumName, String mbid) throws MPDException {
        List<MPDFileEntry> result;
        synchronized (this) {
            mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUM_TRACKS(albumName));

            result = MPDResponseParser.parseMPDTracks(mConnection);
            MPDFileListFilter.filterAlbumMBID(result, mbid);
        }
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
    public List<MPDFileEntry> getArtistAlbumTracks(String albumName, String artistName, String mbid) throws MPDException {
        List<MPDFileEntry> result;
        synchronized (this) {
            mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUM_TRACKS(albumName));

            // Filter tracks with artistName
            result = MPDResponseParser.parseMPDTracks(mConnection);
            MPDFileListFilter.filterAlbumMBIDandAlbumArtist(result, mbid, artistName);
        }
        // Sort with disc & track number
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
    public List<MPDFileEntry> getArtistSortAlbumTracks(String albumName, String artistName, String mbid) throws MPDException {
        List<MPDFileEntry> result;
        synchronized (this) {
            mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUM_TRACKS(albumName));

            // Filter tracks with artistName
            result = MPDResponseParser.parseMPDTracks(mConnection);
            MPDFileListFilter.filterAlbumMBIDandAlbumArtistSort(result, mbid, artistName);
        }
        // Sort with disc & track number
        MPDSortHelper.sortFileListNumeric(result);
        return result;
    }

    /**
     * Requests the current playlist of the server
     *
     * @return List of MPDTrack items with all tracks of the current playlist
     */
    public synchronized List<MPDFileEntry> getCurrentPlaylist() throws MPDException {
        mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_PLAYLIST);

        /* Parse the return */
        return MPDResponseParser.parseMPDTracks(mConnection);
    }

    /**
     * Requests the current playlist of the server with a window
     *
     * @return List of MPDTrack items with all tracks of the current playlist
     */
    public synchronized List<MPDFileEntry> getCurrentPlaylistWindow(int start, int end) throws MPDException {
        mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_PLAYLIST_WINDOW(start, end));

        /* Parse the return */
        return MPDResponseParser.parseMPDTracks(mConnection);
    }

    /**
     * Requests the current playlist of the server
     *
     * @return List of MPDTrack items with all tracks of the current playlist
     */
    public synchronized List<MPDFileEntry> getSavedPlaylist(String playlistName) throws MPDException {
        mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_SAVED_PLAYLIST(playlistName));

        /* Parse the return */
        return MPDResponseParser.parseMPDTracks(mConnection);
    }

    /**
     * Requests the files for a specific path with info
     *
     * @return List of MPDTrack items with all tracks of the current playlist
     */
    public List<MPDFileEntry> getFiles(String path) throws MPDException {
        List<MPDFileEntry> retList;
        synchronized (this) {
            mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_FILES_INFO(path));

            // Parse the return
            retList = MPDResponseParser.parseMPDTracks(mConnection);
        }
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
    public synchronized List<MPDFileEntry> getSearchedFiles(String term, MPDCommands.MPD_SEARCH_TYPE type) throws MPDException {
        mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_SEARCH_FILES(term, type));

        /* Parse the return */
        return MPDResponseParser.parseMPDTracks(mConnection);
    }

    /**
     * Searches a URL in the current playlist. If available the track is part of the returned list.
     *
     * @param url URL to search in the current playlist.
     * @return List with one entry or none.
     */
    public synchronized List<MPDFileEntry> getPlaylistFindTrack(String url) throws MPDException {
        mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_PLAYLIST_FIND_URI(url));

        /* Parse the return */
        return MPDResponseParser.parseMPDTracks(mConnection);
    }

    /**
     * Requests the currentstatus package from the mpd server.
     *
     * @return The CurrentStatus object with all gathered information.
     */
    public synchronized MPDCurrentStatus getCurrentServerStatus() throws MPDException {
        /* Request status */
        mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_STATUS);
        return MPDResponseParser.parseMPDCurrentStatus(mConnection);
    }

    /**
     * Requests the server statistics package from the mpd server.
     *
     * @return The CurrentStatus object with all gathered information.
     */
    public synchronized MPDStatistics getServerStatistics() throws MPDException {
        /* Request status */
        mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_STATISTICS);

        return MPDResponseParser.parseMPDStatistic(mConnection);
    }

    /**
     * This will query the current song playing on the mpd server.
     *
     * @return MPDTrack entry for the song playing.
     */
    public synchronized MPDTrack getCurrentSong() throws MPDException {
        mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_SONG);

        // Reuse the parsing function for tracks here.
        List<MPDFileEntry> retList;

        retList = MPDResponseParser.parseMPDTracks(mConnection);

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
    public synchronized void pause(boolean pause) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_PAUSE(pause));
    }

    /**
     * Jumps to the next song
     */
    public synchronized void nextSong() throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_NEXT);
    }

    /**
     * Jumps to the previous song
     */
    public synchronized void previousSong() throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_PREVIOUS);
    }

    /**
     * Stops playback
     */
    public synchronized void stopPlayback() throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_STOP);
    }

    /**
     * Sets random to true or false
     *
     * @param random If random should be set (true) or not (false)
     */
    public synchronized void setRandom(boolean random) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_SET_RANDOM(random));
    }

    /**
     * Sets repeat to true or false
     *
     * @param repeat If repeat should be set (true) or not (false)
     */
    public synchronized void setRepeat(boolean repeat) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_SET_REPEAT(repeat));
    }

    /**
     * Sets single playback to enable (true) or disabled (false)
     *
     * @param single if single playback should be enabled or not.
     */
    public void setSingle(boolean single) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_SET_SINGLE(single));
    }

    /**
     * Sets if files should be removed after playback (consumed)
     *
     * @param consume True if yes and false if not.
     */
    public synchronized void setConsume(boolean consume) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_SET_CONSUME(consume));
    }

    /**
     * Plays the song with the index in the current playlist.
     *
     * @param index Index of the song that should be played.
     */
    public synchronized void playSongIndex(int index) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_PLAY_SONG_INDEX(index));
    }

    /**
     * Seeks the currently playing song to a certain position
     *
     * @param seconds Position in seconds to which a seek is requested to.
     */
    public synchronized void seekSeconds(int seconds) throws MPDException {
        if(mConnection.getServerCapabilities().hasSeekCurrent()) {
            mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_SEEK_CURRENT_SECONDS(seconds));
        } else {
            MPDCurrentStatus status;

            status = getCurrentServerStatus();

            mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_SEEK_SECONDS(status.getCurrentSongIndex(), seconds));
        }
    }

    /**
     * Sets the volume of the mpd servers output. It is an absolute value between (0-100).
     *
     * @param volume Volume to set to the server.
     */
    public synchronized void setVolume(int volume) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_SET_VOLUME(volume));
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
    public synchronized void addTrackList(List<MPDFileEntry> tracks) throws MPDException {
        if (null == tracks) {
            return;
        }
        mConnection.startCommandList();

        for (MPDFileEntry track : tracks) {
            if (track instanceof MPDTrack) {
                mConnection.sendMPDRAWCommand(MPDCommands.MPD_COMMAND_ADD_FILE(track.getPath()));
            }
        }
        mConnection.endCommandList();
    }

    /**
     * Adds all tracks from a certain album from artistname to the current playlist.
     *
     * @param albumname  Name of the album to add to the current playlist.
     * @param artistname Name of the artist of the album to add to the list. This
     *                   allows filtering of album tracks to a specified artist. Can also
     *                   be left empty then all tracks from the album will be added.
     */
    public synchronized void addAlbumTracks(String albumname, String artistname, String mbid) throws MPDException {
        List<MPDFileEntry> tracks = getArtistAlbumTracks(albumname, artistname, mbid);
        addTrackList(tracks);
    }

    /**
     * Adds all tracks from a certain album from artistname to the current playlist.
     *
     * @param albumname  Name of the album to add to the current playlist.
     * @param artistname Name of the artist of the album to add to the list. This
     *                   allows filtering of album tracks to a specified artist. Can also
     *                   be left empty then all tracks from the album will be added.
     */
    public synchronized void addArtistSortAlbumTracks(String albumname, String artistname, String mbid) throws MPDException {
        List<MPDFileEntry> tracks = getArtistSortAlbumTracks(albumname, artistname, mbid);
        addTrackList(tracks);
    }


    /**
     * Adds all albums of an artist to the current playlist. Will first get a list of albums for the
     * artist and then call addAlbumTracks for every album on this result.
     *
     * @param artistname Name of the artist to enqueue the albums from.
     */
    public synchronized void addArtist(String artistname, MPDAlbum.MPD_ALBUM_SORT_ORDER sortOrder) throws MPDException {
        List<MPDAlbum> albums = getArtistAlbums(artistname);
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
            addAlbumTracks(album.getName(), artistname, "");
        }
    }

    /**
     * Adds a single File/Directory to the current playlist.
     *
     * @param url URL of the file or directory! to add to the current playlist.
     */
    public synchronized void addSong(String url) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_ADD_FILE(url));
    }

    /**
     * This method adds a song to a specified positiion in the current playlist.
     * This allows GUI developers to implement a method like "add after current".
     *
     * @param url   URL to add to the playlist.
     * @param index Index at which the item should be added.
     */
    public synchronized void addSongatIndex(String url, int index) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_ADD_FILE_AT_INDEX(url, index));
    }

    /**
     * Adds files to the playlist with a search term for a specific type
     *
     * @param term The search term to use
     * @param type The type of items to search
     */
    public synchronized void addSearchedFiles(String term, MPDCommands.MPD_SEARCH_TYPE type) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_ADD_SEARCH_FILES(term, type));
    }

    /**
     * Instructs the mpd server to clear its current playlist.
     */
    public synchronized void clearPlaylist() throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_CLEAR_PLAYLIST);
    }

    /**
     * Instructs the mpd server to shuffle its current playlist.
     */
    public synchronized void shufflePlaylist() throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_SHUFFLE_PLAYLIST);
    }

    /**
     * Instructs the mpd server to remove one item from the current playlist at index.
     *
     * @param index Position of the item to remove from current playlist.
     */
    public synchronized void removeIndex(int index) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_SONG_FROM_CURRENT_PLAYLIST(index));
    }

    /**
     * Instructs the mpd server to remove an range of songs from current playlist.
     *
     * @param start Start of songs to remoge
     * @param end   End of the range
     */
    public synchronized void removeRange(int start, int end) throws MPDException {
        // Check capabilities if removal with one command is possible
        if (mConnection.getServerCapabilities().hasCurrentPlaylistRemoveRange()) {
            mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_RANGE_FROM_CURRENT_PLAYLIST(start, end + 1));
        } else {
            // Create commandlist instead
            mConnection.startCommandList();
            for (int i = start; i <= end; i++) {
                mConnection.sendMPDRAWCommand(MPDCommands.MPD_COMMAND_REMOVE_SONG_FROM_CURRENT_PLAYLIST(start));
            }
            mConnection.endCommandList();
        }
    }

    /**
     * Moves one item from an index in the current playlist to an new index. This allows to move
     * tracks for example after the current to priotize songs.
     *
     * @param from Item to move from.
     * @param to   Position to enter item
     */
    public synchronized void moveSongFromTo(int from, int to) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_MOVE_SONG_FROM_INDEX_TO_INDEX(from, to));
    }

    /**
     * Saves the current playlist as a new playlist with a name.
     *
     * @param name Name of the playlist to save to.
     */
    public synchronized void savePlaylist(String name) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_SAVE_PLAYLIST(name));
    }

    /**
     * Adds a song to the saved playlist
     *
     * @param playlistName Name of the playlist to add the url to.
     * @param url          URL to add to the saved playlist
     */
    public synchronized void addSongToPlaylist(String playlistName, String url) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_ADD_TRACK_TO_PLAYLIST(playlistName, url));
    }

    /**
     * Removes a song from a saved playlist
     *
     * @param playlistName Name of the playlist of which the song should be removed from
     * @param position     Index of the song to remove from the lits
     */
    public synchronized void removeSongFromPlaylist(String playlistName, int position) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_TRACK_FROM_PLAYLIST(playlistName, position));
    }

    /**
     * Removes a saved playlist from the servers database.
     *
     * @param name Name of the playlist to remove.
     */
    public synchronized void removePlaylist(String name) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_PLAYLIST(name));
    }

    /**
     * Loads a saved playlist (added after the last song) to the current playlist.
     *
     * @param name Of the playlist to add to.
     */
    public synchronized void loadPlaylist(String name) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_LOAD_PLAYLIST(name));
    }


    /**
     * Returns the list of MPDOutputs to the outside callers.
     *
     * @return List of MPDOutput objects or null in case of error.
     */
    public synchronized List<MPDOutput> getOutputs() throws MPDException {
        mConnection.sendMPDCommand(MPDCommands.MPD_COMMAND_GET_OUTPUTS);

        return MPDResponseParser.parseMPDOutputs(mConnection);
    }

    /**
     * Toggles the state of the output with the id.
     *
     * @param id Id of the output to toggle (active/deactive)
     */
    public synchronized void toggleOutput(int id) throws MPDException {
        if (mConnection.getServerCapabilities().hasToggleOutput()) {
            mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_TOGGLE_OUTPUT(id));
        } else {
            // Implement functionality with enable/disable
            List<MPDOutput> outputs = getOutputs();
            if (id < outputs.size()) {
                if (outputs.get(id).getOutputState()) {
                    disableOutput(id);
                } else {
                    enableOutput(id);
                }
            }
        }
    }

    /**
     * Enable the output with the id.
     *
     * @param id Id of the output to enable (active/deactive)
     */
    public synchronized void enableOutput(int id) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_ENABLE_OUTPUT(id));
    }

    /**
     * Disable the output with the id.
     *
     * @param id Id of the output to disable (active/deactive)
     */
    public synchronized void disableOutput(int id) throws MPDException {
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_DISABLE_OUTPUT(id));
    }

    /**
     * Instructs to update the database of the mpd server.
     *
     * @param path Path to update
     */
    public synchronized void updateDatabase(String path) throws MPDException {
        // Update root directory
        mConnection.sendSimpleMPDCommand(MPDCommands.MPD_COMMAND_UPDATE_DATABASE(path));
    }
}
