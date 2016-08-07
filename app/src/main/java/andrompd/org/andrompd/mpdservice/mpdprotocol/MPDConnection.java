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

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDAlbum;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDArtist;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDFile;


public class MPDConnection {
    private static final String TAG = "MPDConnection";

    /* Internal server parameters used for initiating the connection */
    private String pHostname;
    private String pPassword;
    private int pPort;

    private Socket pSocket;

    /* BufferedReader for all reading from the socket */
    private BufferedReader pReader;

    /* PrintWriter for all writing to the socket */
    private PrintWriter pWriter;

    /* True only if server is ready to receive commands */
    private boolean pMPDConnectionReady = false;

    /* True if server connection is in idleing state. Needs to be deidled before sending command */
    private boolean pMPDConnectionIdle = false;

    /* MPD server properties */
    private String pVersionString;
    private int pMajorVersion;
    private int pMinorVersion;

    private MPDConnectionStateChangeListener pStateListener = null;

    private MPDConnectionIdleChangeListener pIdleListener = null;

    private Thread pIdleThread = null;

    private String mLineBuffer;
    private Semaphore mBufferLock;


    /**
     * Creates disconnected MPDConnection with following parameters
     */
    public MPDConnection() {
        pSocket = null;
        pReader = null;
        mBufferLock = new Semaphore(1);
    }

    /**
     * Private function to handle read error. Try to disconnect and remove old sockets.
     * Clear up connection state variables.
     */
    private void handleReadError() {
        Log.e(TAG, "Read error exception. Disconnecting and cleaning up");

        // Notify listener
        notifyDisconnect();
        try {
            /* Clear reader/writer up */
            if ( null != pReader ) {
                pReader.close();
                pReader = null;
            }
            if ( null != pWriter ) {
                pWriter.close();
                pWriter = null;
            }


            /* Clear TCP-Socket up */
            if ( null != pSocket ) {
                pSocket.close();
                pSocket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error during read error handling");
        }

        /* Clear up connection state variables */
        pMPDConnectionIdle = false;
        pMPDConnectionReady = false;

        try {
            connectToServer();
        } catch (IOException e) {
            Log.w(TAG,"Reconnecting to server failed");
        }
    }

    /**
     * Set the parameters to connect to. Should be called before the connection attempt
     * otherwise the connection object does not know where to put it.
     * @param hostname Hostname to connect to. Can also be an ip.
     * @param password Password for the server to authenticate with. Can bel eft empty.
     * @param port TCP port to connect to.
     */
    public void setServerParameters(String hostname, String password, int port) {
        pHostname = hostname;
        if ( !password.equals("") ) {
            pPassword = password;
        }
        pPort = port;
    }

    /**
     * This is the actual start of the connection. It tries to resolve the hostname
     * and initiates the connection to the address and the configured tcp-port.
     */
    public void connectToServer() throws IOException {
        /* If a socket is already open, close it and destroy it. */
        if ((null != pSocket ) && (pSocket.isConnected()) ) {
            disconnectFromServer();
        }

        /* Create a new socket used for the TCP-connection. */
        pSocket = new Socket(pHostname, pPort);

        /* Check if the socket is connected */
        if ( pSocket.isConnected() ) {
            Log.v(TAG,"MPD server is connected");
            /* Try reading from the stream */

            /* Create the reader used for reading from the socket. */
            if ( pReader == null ) {
                pReader = new BufferedReader(new InputStreamReader(pSocket.getInputStream()));
            }

            /* Create the writer used for writing to the socket */
            if ( pWriter == null ) {
                pWriter = new PrintWriter(new OutputStreamWriter(pSocket.getOutputStream()));
            }

            waitForResponse();

            /* If connected try to get MPDs version */
            String readString = null;
            while ( readyRead() ) {
                readString = pReader.readLine();
                Log.v(TAG,"Read string: " + readString);
                /* Look out for the greeting message */
                if ( readString.startsWith("OK MPD ") ) {
                    pVersionString = readString.substring(7);
                    String[] versions = pVersionString.split("\\.");
                    if ( versions.length == 3 ) {
                        pMajorVersion = Integer.valueOf(versions[0]);
                        pMinorVersion = Integer.valueOf(versions[1]);
                    }

                }
            }
            Log.v(TAG, "MPD Version: " + pMajorVersion + ":" + pMinorVersion);
            pMPDConnectionReady = true;
            Log.v(TAG, "MPDConnection: " + this + " ready");

            if ( pPassword != null && !pPassword.equals("") ) {
                Log.v(TAG,"Try to authenticate with mpd server");
                    /* Authenticate with server because password is set. */
                boolean authenticated = authenticateMPDServer();
                Log.v(TAG, "Authentication successful: " + authenticated);
            }

            // Notify listener
            notifyConnected();
        }
    }


    /**
     * If the password for the MPDConnection is set then the client should
     * try to authenticate with the server
     */
    private boolean authenticateMPDServer() throws IOException {
        /* Check if connection really is good to go. */
        if ( !pMPDConnectionReady || pMPDConnectionIdle ) {
            return false;
        }

        sendMPDCommand(MPDCommands.MPD_COMMAND_PASSWORD + pPassword);

        /* Check if the result was positive or negative */

        String readString = null;

        boolean success = false;
        while ( readyRead() ) {
            readString = pReader.readLine();
            if (readString.startsWith("OK")) {
                success = true;
                Log.v(TAG, "Successfully authenticated with mpd server");
            } else if (readString.startsWith("ACK")) {
                success = false;
                Log.v(TAG, "Could not successfully authenticate with mpd server");
            }
        }

        return success;
    }

    public void disconnectFromServer() {
        Log.v(TAG,"Disconnecting: " + this);

        if ( pMPDConnectionIdle ) {
            stopIdleing();
        }

        // Notify listener
        notifyDisconnect();
        try {
            /* Clear reader/writer up */
            if ( null != pReader ) {
                pReader.close();
                pReader = null;
            }
            if ( null != pWriter ) {
                pWriter.close();
                pWriter = null;
            }


            /* Clear TCP-Socket up */
            if ( null != pSocket ) {
                pSocket.close();
                pSocket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error during disconnecting");
        }

        /* Clear up connection state variables */
        pMPDConnectionIdle = false;
        pMPDConnectionReady = false;
        Log.v(TAG,"Disconnecting finished");
    }

    /**
     * This functions sends the command to the MPD server.
     * If the server is currently idling then it will deidle it first.
     * @param command
     */
    public void sendMPDCommand(String command) {
        Log.v(TAG,"Connection: " + this + " ready: " + pMPDConnectionReady + " connection idle: " + pMPDConnectionIdle);
        /* Check if the server is connected. */
        if ( pMPDConnectionReady ) {
            /* Check if server is in idling mode, this needs unidling first,
            otherwise the server will disconnect the client.
             */
            if ( pMPDConnectionIdle ) {
                stopIdleing();
            }

            /*
             * Send the command to the server
             * FIXME Should be validated in the future.
             */
            // FIXME Remove me, seriously
            Log.v(TAG, "Sending command: " + command);
            pWriter.println(command);
            pWriter.flush();
            waitForResponse();
        }
    }


    /**
     * This method needs to be called before a new MPD command is sent to
     * the server to correctly unidle. Otherwise the mpd server will disconnect
     * the disobeying client.
     */
    public void stopIdleing() {
        /* Check if server really is in idling mode */
        if ( !pMPDConnectionIdle || !pMPDConnectionReady ) {
            return;
        }
        Log.v(TAG,"Deidling MPD");

        /* Set flag */
        pMPDConnectionIdle = false;

        /* Send the "noidle" command to the server to initiate noidle */
        pWriter.println(MPDCommands.MPD_COMMAND_STOP_IDLE);
        pWriter.flush();

        try {
            mBufferLock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.v(TAG,"Got Buffer lock in stopIdle, lineBuffer: " + mLineBuffer);
        if ( (null != mLineBuffer) && mLineBuffer.startsWith("OK") ) {
            Log.v(TAG,"Deidling ok");
        } else {
            try {
                if ( pReader.ready() && !checkResponse()) {
                    Log.w(TAG,"Error deidling");
                    handleReadError();
                }
            } catch ( IOException e ) {
                handleReadError();
            }
        }
        mBufferLock.release();

    }

    /**
     * Initiates the idling procedure. A separate thread is started to wait (blocked)
     * for a deidle from the MPD host. Otherwise it is impossible to get notified on changes
     * from other mpd clients (eg. volume change)
     */
    public void startIdleing() {
        /* Check if server really is in idling mode */
        if ( !pMPDConnectionReady || pMPDConnectionIdle ) {
            return;
        }
        Log.v(TAG,"MPDConnection: " + this + "Sending idle command");

        try {
            mBufferLock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mLineBuffer = null;
        mBufferLock.release();

        pWriter.println(MPDCommands.MPD_COMMAND_START_IDLE);
        pWriter.flush();
        pIdleThread = new IdleThread();
        pIdleThread.start();

        Log.v(TAG,"Runnable is waiting for idle finish");
        pMPDConnectionIdle = true;

        if ( null != pIdleListener ) {
            pIdleListener.onIdle();
        }
    }

    /**
     * Function only actively waits for reader to get ready for
     * the response.
     */
    private void waitForResponse() {
        if ( null != pReader ) {
            try {
                while ( !readyRead() ) {}
            } catch (IOException e) {
                handleReadError();
            }
        }
    }

    /**
     * Checks if a simple command was successful or not (OK vs. ACK)
     * @return True if command was successfully executed, false otherwise.
     */
    public boolean checkResponse() throws IOException {
        boolean success = false;

        String response;
        while ( readyRead() ) {
            response = pReader.readLine();
            Log.v(TAG,"Response: " + response);
            if ( response.startsWith("OK") ) {
                success = true;
            } else if ( response.startsWith("ACK") ) {
                success = false;
            }
        }

        return success;
    }



    /*
     * *******************************
     * * Response handling functions *
     * *******************************
     */

    /**
     * Parses the return of MPD when a list of albums was requested.
     * @param albumArtist Artist to be added to all MPDAlbum objects. (Useful for later GUI)
     * @return List of MPDAlbum objects
     * @throws IOException
     */
    private ArrayList<MPDAlbum> parseMPDAlbums(String albumArtist) throws IOException {
        ArrayList<MPDAlbum> albumList = new ArrayList<MPDAlbum>();

        /* Parse the MPD response and create a list of MPD albums */
        String response;

        boolean emptyAlbum = false;
        String albumName = "";
        String albumMBID = "";

        MPDAlbum tempAlbum;

        while ( readyRead() ) {
            response = pReader.readLine();
            if ( response == null ) {
                /* skip this invalid (empty) response */
                continue;
            }

            /* Check if the response is an album */
            if ( response.startsWith(MPDResponses.MPD_RESPONSE_ALBUM_NAME) ) {
                /* We found an album, add it to the list. */
                if ( !albumName.equals("") || emptyAlbum ) {
                    tempAlbum = new MPDAlbum(albumName,albumMBID,albumArtist);
                    //Log.v(TAG,"Add album to list: " + albumName + ":" + albumMBID + ":"  + albumArtist);
                    albumList.add(tempAlbum);
                }
                albumName = response.substring(MPDResponses.MPD_RESPONSE_ALBUM_NAME.length());
                if ( albumName.equals("") ) {
                    emptyAlbum = true;
                }
            } else if ( response.startsWith(MPDResponses.MPD_RESPONSE_ALBUM_MBID) ) {
                /* Check if the response is a musicbrainz_albumid. */
                albumMBID = response.substring(MPDResponses.MPD_RESPONSE_ALBUM_MBID.length());
            }
        }

        /* Because of the loop structure the last album has to be added because no
        "ALBUM:" is sent anymore.
         */
        if ( !albumName.equals("") || emptyAlbum ) {
            tempAlbum = new MPDAlbum(albumName,albumMBID,albumArtist);
            albumList.add(tempAlbum);
        }
        Log.v(TAG,"Albums parsed");
        return albumList;
    }

    /**
     * Parses the return stream of MPD when a list of artists was requested.
     * @return List of MPDArtists objects
     * @throws IOException
     */
    private ArrayList<MPDArtist> parseMPDArtists() throws IOException {
        ArrayList<MPDArtist> artistList = new ArrayList<MPDArtist>();

        /* Parse MPD artist return values and create a list of MPDArtist objects */
        String response;

        /* Artist properties */
        String artistName;
        String artistMBID = "";

        MPDArtist tempArtist;

        while ( readyRead() ) {
            response = pReader.readLine();
            if ( response == null ) {
                /* skip this invalid (empty) response */
                continue;
            }

            if ( response.startsWith(MPDResponses.MPD_RESPONSE_ARTIST_NAME) ) {
                artistName = response.substring(MPDResponses.MPD_RESPONSE_ARTIST_NAME.length());
                tempArtist = new MPDArtist(artistName,artistMBID);
                artistList.add(tempArtist);
                //Log.v(TAG,"Added artist: " + artistName + ":" + artistMBID);
            } else if ( response.startsWith("OK") ) {
                break;
            }
        }
        Log.v(TAG,"Artists parsed");
        return artistList;
    }

    /**
     * Parses the resposne of mpd on requests that return track items
     * @param filterArtist Artist used for filtering. Non-matching tracks get discarded.
     * @return List of MPDFile objects
     * @throws IOException
     */
    private ArrayList<MPDFile> parseMPDTracks(String filterArtist) throws IOException {
        ArrayList<MPDFile> trackList = new ArrayList<MPDFile>();

        /* Temporary track item (added to list later */
        MPDFile tempTrack = new MPDFile();

        /* Response line from MPD */
        String response;
        while ( readyRead() ) {
            response = pReader.readLine();

            if ( response.startsWith(MPDResponses.MPD_RESPONSE_FILE)) {
                if ( !tempTrack.getFileURL().equals("") ) {
                    /* Check the artist filter criteria here */
                    if ( filterArtist.equals(tempTrack.getTrackArtist()) || filterArtist.equals("") ) {
                        trackList.add(tempTrack);
                        //Log.v(TAG,"Added track: " + tempTrack.getTrackTitle() + ":" + tempTrack.getFileURL());
                    }
                }
                tempTrack = new MPDFile();
                tempTrack.setFileURL(response.substring(MPDResponses.MPD_RESPONSE_FILE.length()));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_TRACK_TITLE) ) {
                tempTrack.setTrackTitle(response.substring(MPDResponses.MPD_RESPONSE_TRACK_TITLE.length()));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_ARTIST_NAME) ) {
                tempTrack.setTrackArtist(response.substring(MPDResponses.MPD_RESPONSE_ARTIST_NAME.length()));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_ALBUM_ARTIST_NAME) ) {
                tempTrack.setTrackAlbumArtist(response.substring(MPDResponses.MPD_RESPONSE_ALBUM_ARTIST_NAME.length()));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_ALBUM_NAME) ) {
                tempTrack.setTrackAlbum(response.substring(MPDResponses.MPD_RESPONSE_ALBUM_NAME.length()));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_DATE) ) {
                tempTrack.setDate(response.substring(MPDResponses.MPD_RESPONSE_DATE.length()));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_ALBUM_MBID) ) {
                tempTrack.setTrackAlbumMBID(response.substring(MPDResponses.MPD_RESPONSE_ALBUM_MBID.length()));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_ARTIST_MBID) ) {
                tempTrack.setTrackArtistMBID(response.substring(MPDResponses.MPD_RESPONSE_ARTIST_MBID.length()));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_TRACK_MBID) ) {
                tempTrack.setTrackMBID(response.substring(MPDResponses.MPD_RESPONSE_TRACK_MBID.length()));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_TRACK_TIME) ) {
                tempTrack.setLength(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_TRACK_TIME.length())));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_DISC_NUMBER) ) {
                /*
                * Check if MPD returned a discnumber like: "1" or "1/3" and set disc count accordingly.
                */
                String discNumber = response.substring(MPDResponses.MPD_RESPONSE_DISC_NUMBER.length());
                String[] discNumberSep = discNumber.split("/");
                if ( discNumberSep.length > 0 ) {
                    tempTrack.setDiscNumber(Integer.valueOf(discNumberSep[0]));
                    if ( discNumberSep.length > 1 ) {
                        tempTrack.psetAlbumDiscCount(Integer.valueOf(discNumberSep[1]));
                    }
                } else {
                    tempTrack.setDiscNumber(Integer.valueOf(discNumber));
                }
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_TRACK_NUMBER) ) {
                /*
                 * Check if MPD returned a tracknumber like: "12" or "12/42" and set albumtrack count accordingly.
                 */
                String trackNumber = response.substring(MPDResponses.MPD_RESPONSE_TRACK_NUMBER.length());
                String[] trackNumbersSep = trackNumber.split("/");
                if ( trackNumbersSep.length > 0 ) {
                    tempTrack.setTrackNumber(Integer.valueOf(trackNumbersSep[0]));
                    if ( trackNumbersSep.length > 1 ) {
                        tempTrack.setAlbumTrackCount(Integer.valueOf(trackNumbersSep[1]));
                    }
                } else {
                    tempTrack.setTrackNumber(Integer.valueOf(trackNumber));
                }
            }

        }

        /* Add last remaining track to list. */
        if ( !tempTrack.getFileURL().equals("") ) {
            /* Check the artist filter criteria here */
            if ( filterArtist.equals(tempTrack.getTrackArtist()) || filterArtist.equals("") ) {
                trackList.add(tempTrack);
                //Log.v(TAG,"Added track: " + tempTrack.getTrackTitle() + ":" + tempTrack.getFileURL());
            }
        }
        Log.v(TAG,"Tracks parsed");

        return trackList;
    }

     /*
     * **********************
     * * Request functions  *
     * **********************
     */

    /**
     * Get a list of all albums available in the database.
     * @return List of MPDAlbum
     */
    public List<MPDAlbum> getAlbums() {
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMS);
        try {
            /* No artistName here because it is a full list */
            return parseMPDAlbums("");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a list of all albums by an artist.
     * @param artistName Artist to filter album lsit with.
     * @return List of MPDAlbum objects
     */
    public List<MPDAlbum> getArtistAlbums(String artistName) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ARTIST_ALBUMS(artistName));
        try {
            return parseMPDAlbums(artistName);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a list of all artists available in MPDs database
     * @return List of MPDArtist objects
     */
    public List<MPDArtist> getArtists() {
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ARTISTS);
        try {
            return parseMPDArtists();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets all tracks from MPD server. This could take a long time to process. Be warned.
     * @return A list of all tracks in MPDFile objects
     */
    public List<MPDFile> getAllTracks() {
        Log.w(TAG,"This command should not be used");
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALL_FILES);
        try {
            return parseMPDTracks("");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Returns the list of tracks that are part of albumName
     * @param albumName Album to get tracks from
     * @return List of MPDFile track objects
     */
    public List<MPDFile> getAlbumTracks(String albumName) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUM_TRACKS(albumName));
        try {
            return parseMPDTracks("");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the list of tracks that are part of albumName and from artistName
     * @param albumName Album to get tracks from
     * @param artistName Artist to filter with
     * @return List of MPDFile track objects
     */
    public List<MPDFile> getArtistAlbumTracks(String albumName, String artistName) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUM_TRACKS(albumName));
        try {
            /* Filter tracks with artistName */
            return parseMPDTracks(artistName);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Requests the current playlist of the server
     * @return List of MPDFile items with all tracks of the current playlist
     */
    public List<MPDFile> getCurrentPlaylist() {
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_PLAYLIST);
        try {
            /* Parse the return */
            return parseMPDTracks("");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Requests the current playlist of the server
     * @return List of MPDFile items with all tracks of the current playlist
     */
    public List<MPDFile> getSavedPlaylist(String playlistName) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_SAVED_PLAYLIST(playlistName));
        try {
            /* Parse the return */
            return parseMPDTracks("");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Requests the currentstatus package from the mpd server.
     * @return The CurrentStatus object with all gathered information.
     */
    public MPDCurrentStatus getCurrentServerStatus() throws IOException {
        MPDCurrentStatus status = new MPDCurrentStatus();

        /* Request status */
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_STATUS);

        /* Response line from MPD */
        String response;
        while ( readyRead() ) {
            response = pReader.readLine();

            if ( response.startsWith(MPDResponses.MPD_RESPONSE_VOLUME ) ) {
                status.setVolume(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_VOLUME.length())));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_REPEAT )) {
                status.setRepeat(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_REPEAT.length())));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_RANDOM )) {
                status.setRandom(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_RANDOM.length())));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_SINGLE )) {
                status.setSinglePlayback(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_SINGLE.length())));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_CONSUME )) {
                status.setConsume(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_CONSUME.length())));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_PLAYLIST_VERSION )) {
                status.setPlaylistVersion(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_PLAYLIST_VERSION.length())));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_PLAYLIST_LENGTH )) {
                status.setPlaylistLength(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_PLAYLIST_LENGTH.length())));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_PLAYBACK_STATE )) {
                String state = response.substring(MPDResponses.MPD_RESPONSE_PLAYBACK_STATE.length());

                if (state.equals(MPDResponses.MPD_PLAYBACK_STATE_RESPONSE_PLAY) ) {
                    status.setPlaybackState(MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PLAYING);
                }
                else if (state.equals(MPDResponses.MPD_PLAYBACK_STATE_RESPONSE_PAUSE)) {
                    status.setPlaybackState(MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PAUSING);
                }
                else if (state.equals(MPDResponses.MPD_PLAYBACK_STATE_RESPONSE_STOP)) {
                    status.setPlaybackState(MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_STOPPED);
                }
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_CURRENT_SONG_INDEX )) {
                status.setCurrentSongIndex(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_CURRENT_SONG_INDEX.length())));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_NEXT_SONG_INDEX )) {
                status.setNextSongIndex(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_NEXT_SONG_INDEX.length())));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_TIME_INFORMATION_OLD )) {
                String timeInfo = response.substring(MPDResponses.MPD_RESPONSE_TIME_INFORMATION_OLD.length());

                String timeInfoSep[] = timeInfo.split(":");
                if ( timeInfoSep.length == 2 ) {
                    status.setElapsedTime(Integer.valueOf(timeInfoSep[0]));
                    status.setTrackLength(Integer.valueOf(timeInfoSep[1]));
                }
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_ELAPSED_TIME )) {
                status.setElapsedTime(Math.round(Float.valueOf(response.substring(MPDResponses.MPD_RESPONSE_ELAPSED_TIME.length()))));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_DURATION )) {
                status.setTrackLength(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_DURATION.length())));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_BITRATE )) {
                status.setBitrate(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_BITRATE.length())));
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_AUDIO_INFORMATION )) {
                String audioInfo = response.substring(MPDResponses.MPD_RESPONSE_AUDIO_INFORMATION.length());

                String audioInfoSep[] = audioInfo.split(":");
                if ( audioInfoSep.length == 3 ) {
                    /* Extract the separate pieces */
                    /* First is sampleRate */
                    status.setSamplerate(Integer.valueOf(audioInfoSep[0]));
                    /* Second is bitresolution */
                    status.setBitDepth(Integer.valueOf(audioInfoSep[1]));
                    /* Third is channel count */
                    status.setChannelCount(Integer.valueOf(audioInfoSep[2]));
                }
            }
            else if ( response.startsWith(MPDResponses.MPD_RESPONSE_UPDATING_DB )) {
                status.setUpdateDBJob(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_UPDATING_DB.length())));
            }
        }

        return status;
    }


    public MPDFile getCurrentSong() throws IOException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_SONG);
        List<MPDFile> retList = parseMPDTracks("");
        if ( retList.size() == 1 )  {
            return retList.get(0);
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
     * @param pause 1 if playback should be paused, 0 if resumed
     * @return
     */
    public boolean pause(boolean pause) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_PAUSE(pause));

        /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Jumps to the next song
     * @return true if successful, false otherwise
     */
    public boolean nextSong() {
        sendMPDCommand(MPDCommands.MPD_COMMAND_NEXT);

        /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Jumps to the previous song
     * @return true if successful, false otherwise
     */
    public boolean previousSong() {
        sendMPDCommand(MPDCommands.MPD_COMMAND_PREVIOUS);

        /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Stops playback
     * @return true if successful, false otherwise
     */
    public boolean stopPlayback() {
        sendMPDCommand(MPDCommands.MPD_COMMAND_STOP);

        /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean setRandom(boolean random) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SET_RANDOM(random));

        /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean setRepeat(boolean repeat) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SET_REPEAT(repeat));

        /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean setSingle(boolean single) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SET_SINGLE(single));

        /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean setConsume(boolean consume) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SET_CONSUME(consume));

        /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean playSongIndex(int index) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_PLAY_SONG_INDEX(index));

        /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean seekSeconds(int seconds) {
        MPDCurrentStatus status = null;
        try {
            status = getCurrentServerStatus();
        } catch (IOException e) {
            e.printStackTrace();
        }

        sendMPDCommand(MPDCommands.MPD_COMMAND_SEEK_SECONDS(status.getCurrentSongIndex(),seconds));

        /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean setVolume(int volume) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SET_VOLUME(volume));

        /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    private boolean readyRead() throws IOException {
        return (null != pReader ) && pReader.ready();
    }

    private void notifyConnected() {
        if ( null != pStateListener ) {
            pStateListener.onConnected();
        }
    }

    private void notifyDisconnect() {
        if ( null != pStateListener ) {
            pStateListener.onDisconnected();
        }
    }

    public void setStateListener(MPDConnectionStateChangeListener listener) {
        Log.v(TAG,"Set state listener:" + listener);
        pStateListener = listener;
    }

    public void setpIdleListener(MPDConnectionIdleChangeListener listener) {
        pIdleListener = listener;
    }

    public interface MPDConnectionStateChangeListener {
        void onConnected();
        void onDisconnected();
    }

    public interface MPDConnectionIdleChangeListener {
        void onIdle();
        void onNonIdle();
    }


    /**
     * This method should only be used by the idling mechanism.
     * It buffers the read line so that the deidle method can check if deidling was successful.
     * To guarantee predictable execution order, the buffer is secured by a semaphore. This ensures,
     * that the read of this waiting thread is always finished before the other handler thread tries
     * to read it.
     * @return
     */
    private String waitForIdleResponse() {
        if ( null != pReader ) {
            try {

                try {
                    // Lock the semaphore, that a possible later deidling request waits for the read.
                    mBufferLock.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // Set thread to sleep, because there should be no line available to read.
                mLineBuffer = pReader.readLine();
                // After sucessful writing to the buffer, release the lock. A possible deidling call
                // can resume its work there.
                mBufferLock.release();
                return mLineBuffer;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    /**
     * Simple private thread class used for handling the idling of MPD.
     * If no line is ready to read, it will suspend itself (blocking readLine() call).
     * If suddenly a line is ready to read it can mean two things:
     *  1. A deidling request notified the server to quit idling.
     *  2. A change in the MPDs internal state changed and the status of this client needs updating.
     */
    private class IdleThread extends Thread {
        @Override
        public void run() {
            /* Try to read here. This should block this separate thread because
               readLine() inside waitForIdleResponse is blocking.
               If the response was not "OK" it means idling was stopped by us.
               If the response starts with "changed" we know, that the MPD state was altered from somewhere
               else and we need to update our status.
             */
            String response =  waitForIdleResponse();
            while ( response == null ) {
                response =  waitForIdleResponse();
            }
            Log.v(TAG,"MPDConnection IdleThread: " + this + " Deidiling response:" + response);
            if ( response.startsWith("changed") ) {
                try {
                    if ( checkResponse() ) {
                        Log.v(TAG,"Deidiling correct");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                pMPDConnectionIdle = false;

                if ( null != pIdleListener ) {
                    pIdleListener.onNonIdle();
                }
            }

        }
    }

}
