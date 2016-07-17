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

    /**
     * Creates disconnected MPDConnection with following parameters
     * @param hostname to connect to later on
     * @param password password used for authentication. Can be empry.
     * @param port Port to connect to. Default should be 6600 !
     */
    public MPDConnection(String hostname, String password, int port) {
        pHostname = hostname;
        if ( !password.equals("") ) {
            pPassword = password;
        }
        pPort = port;

        pSocket = null;
        pReader = null;
    }

    /**
     * Private function to handle read error. Try to disconnect and remove old sockets.
     * Clear up connection state variables.
     */
    private void handleReadError() {
        Log.e(TAG, "Read error exception. Disconnecting and cleaning up");
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
    }

    /**
     * This is the actual start of the connection. It tries to resolve the hostname
     * and initiates the connection to the address and the configured tcp-port.
     */
    public void connectToServer() throws IOException {
        if ( pSocket == null ) {
            /* Create a new socket used for the TCP-connection. */
            pSocket = new Socket(pHostname, pPort);
        }

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
            while ( pReader.ready() ) {
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

            if ( pPassword != null && !pPassword.equals("") ) {
                Log.v(TAG,"Try to authenticate with mpd server");
                    /* Authenticate with server because password is set. */
                boolean authenticated = authenticateMPDServer();
                Log.v(TAG, "Authentication successful: " + authenticated);
            }
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
        while ( pReader.ready() ) {
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

    /**
     * This functions sends the command to the MPD server.
     * If the server is currently idling then it will deidle it first.
     * @param command
     */
    public void sendMPDCommand(String command) {
        /* Check if the server is connected. */
        if ( pMPDConnectionReady ) {
            /* Check if server is in idling mode, this needs unidling first,
            otherwise the server will disconnect the client.
             */
            if ( pMPDConnectionIdle ) {
                cancelIDLEMode();
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
    private void cancelIDLEMode() {
        /* Check if server really is in idling mode */
        if ( !pMPDConnectionIdle || !pMPDConnectionReady ) {
            return;
        }

        /* Set flag */
        pMPDConnectionIdle = false;

        /* Send the "noidle" command to the server to initiate noidle */
        pWriter.println(MPDCommands.MPD_COMMAND_NODIDLE);
        pWriter.flush();
        waitForResponse();

        /* Check if successfully deidled when server returns "OK" */
        try {
            String readString = pReader.readLine();
            while ( null != readString ) {
                // FIXME check if OK was send. For now flush the read buffer.
            }
        } catch (IOException e) {
            handleReadError();
        }
    }

    /**
     * Function only actively waits for reader to get ready for
     * the response.
     */
    private void waitForResponse() {
        if ( null != pReader ) {
            try {
                while ( !pReader.ready() ) {}
            } catch (IOException e) {
                handleReadError();
            }
        }
    }

    /**
     * Checks if a simple command was successful or not (OK vs. ACK)
     * @return True if command was successfully executed, false otherwise.
     */
    public boolean checkResponse() {
        boolean success = false;

        String response;
        while ( pReader.ready() ) {
            response = pReader.readLine();
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

        while ( pReader.ready() ) {
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
                    Log.v(TAG,"Add album to list: " + albumName + ":" + albumMBID + ":"  + albumArtist);
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

        while ( pReader.ready() ) {
            response = pReader.readLine();
            if ( response == null ) {
                /* skip this invalid (empty) response */
                continue;
            }

            if ( response.startsWith(MPDResponses.MPD_RESPONSE_ARTIST_NAME) ) {
                artistName = response.substring(MPDResponses.MPD_RESPONSE_ARTIST_NAME.length());
                tempArtist = new MPDArtist(artistName,artistMBID);
                artistList.add(tempArtist);
                Log.v(TAG,"Added artist: " + artistName + ":" + artistMBID);
            } else if ( response.startsWith("OK") ) {
                break;
            }
        }

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
        while ( pReader.ready() ) {
            response = pReader.readLine();

            if ( response.startsWith(MPDResponses.MPD_RESPONSE_FILE)) {
                if ( !tempTrack.getFileURL().equals("") ) {
                    /* Check the artist filter criteria here */
                    if ( filterArtist == tempTrack.getTrackArtist() || filterArtist.equals("") ) {
                        trackList.add(tempTrack);
                        Log.v(TAG,"Added track: " + tempTrack.getTrackTitle() + ":" + tempTrack.getFileURL());
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
            if ( filterArtist == tempTrack.getTrackArtist() || filterArtist.equals("") ) {
                trackList.add(tempTrack);
            }
        }

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
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALL_FILES);
        try {
            return parseMPDTracks("");
        } catch (IOException e) {
            e.printStackTrace();
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
        return checkResponse();
    }

    /**
     * Jumps to the next song
     * @return true if successful, false otherwise
     */
    public boolean nextSong() {
        sendMPDCommand(MPDCommands.MPD_COMMAND_NEXT);

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Jumps to the previous song
     * @return true if successful, false otherwise
     */
    public boolean previousSong() {
        sendMPDCommand(MPDCommands.MPD_COMMAND_PREVIOUS);

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Stops playback
     * @return true if successful, false otherwise
     */
    public boolean stopPlayback() {
        sendMPDCommand(MPDCommands.MPD_COMMAND_STOP);

        /* Return the response value of MPD */
        return checkResponse();
    }


}
