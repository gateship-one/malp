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

package org.gateshipone.malp.mpdservice.mpdprotocol;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDDirectory;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFile;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDOutput;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDPlaylist;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDStatistics;

/**
 * This is the main MPDConnection class. It will connect to an MPD server via an java TCP socket.
 * If no action, query, or other command to the server is send, this connection will immediately
 * start to idle. This means that the connection is waiting for a response from the mpd server.
 * <p/>
 * For this this class spawns a new thread which is then blocked by the waiting read operation
 * on the reader of the socket.
 * <p/>
 * If a new command is requested by the handler thread the stopIdling function is called, which
 * will send the "noidle" command to the server and requests to deidle the connection. Only then the
 * server is ready again to receive commands. If this is not done properly the server will just
 * terminate the connection.
 * <p/>
 * This mpd connection needs to be run in a different thread than the UI otherwise the UI will block
 * (or android will just throw an exception).
 * <p/>
 * For more information check the protocol definition of the mpd server or contact me via mail.
 */

public class MPDConnection {
    private static final String TAG = "MPDConnection";

    private static final int SOCKET_TIMEOUT = 30 * 1000;

    private static final int IDLE_WAIT_TIME = 500;

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
    // FIXME do some capability checking in respect to the server version
    private String pVersionString;
    private int pMajorVersion;
    private int pMinorVersion;

    /**
     * One listener for the state of the connection (connected, disconnected)
     */
    private MPDConnectionStateChangeListener pStateListener = null;

    /**
     * One listener for the idle state of the connection. Can be used to react
     * to changes to the server from other clients. When the server is deidled (from outside)
     * it will notify this listener.
     */
    private MPDConnectionIdleChangeListener pIdleListener = null;

    /**
     * Thread that will spawn when the server is not requested at the moment. Will start an
     * blocking read operation on the socket reader.
     */
    private Thread pIdleThread = null;

    /**
     * Timeout to start the actual idling thread. It will start after IDLE_WAIT_TIME milliseconds
     * passed. To prevent interfering with possible handler calls at the same time
     * all the methods that could be called from outside are synchronized to this MPDConnection class.
     * This means that you have to be careful when calling these functions to prevent deadlocks.
     */
    private Timer mIdleWait = null;

    /**
     * Semaphore lock used by the deidling process. Necessary to guarantee the correct order of
     * deidling write / read operations.
     */
    Semaphore mIdleWaitLock;

    /**
     * Saves if a deidle was requested by this connection or is triggered by another client/connection.
     */
    boolean mRequestedDeidle;

    /**
     * Creates disconnected MPDConnection with following parameters
     */
    public MPDConnection() {
        pSocket = null;
        pReader = null;
        mIdleWaitLock = new Semaphore(1);

    }

    /**
     * Private function to handle read error. Try to disconnect and remove old sockets.
     * Clear up connection state variables.
     */
    private void handleReadError() {
        Log.e(TAG, "Read error exception. Disconnecting and cleaning up");

        try {
            /* Clear reader/writer up */
            if (null != pReader) {
                pReader = null;
            }
            if (null != pWriter) {
                pWriter = null;
            }

            /* Clear TCP-Socket up */
            if (null != pSocket && pSocket.isConnected()) {
                pSocket.setSoTimeout(1000);
                pSocket.close();
                pSocket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error during read error handling");
        }

        /* Clear up connection state variables */
        pMPDConnectionIdle = false;
        pMPDConnectionReady = false;


        // Notify listener
        notifyDisconnect();
    }

    /**
     * Set the parameters to connect to. Should be called before the connection attempt
     * otherwise the connection object does not know where to put it.
     *
     * @param hostname Hostname to connect to. Can also be an ip.
     * @param password Password for the server to authenticate with. Can bel eft empty.
     * @param port     TCP port to connect to.
     */
    public void setServerParameters(String hostname, String password, int port) {
        pHostname = hostname;
        if (!password.equals("")) {
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
        if ((null != pSocket) && (pSocket.isConnected())) {
            disconnectFromServer();
        }

        if ((null == pHostname) || pHostname.equals("")) {
            return;
        }
        pMPDConnectionIdle = false;
        pMPDConnectionReady = false;
        /* Create a new socket used for the TCP-connection. */
        pSocket = new Socket();
        pSocket.connect(new InetSocketAddress(pHostname, pPort), SOCKET_TIMEOUT);

        /* Check if the socket is connected */
        if (pSocket.isConnected()) {
            Log.v(TAG, "MPD server is connected");
            /* Try reading from the stream */

            /* Create the reader used for reading from the socket. */
            if (pReader == null) {
                pReader = new BufferedReader(new InputStreamReader(pSocket.getInputStream()));
            }

            /* Create the writer used for writing to the socket */
            if (pWriter == null) {
                pWriter = new PrintWriter(new OutputStreamWriter(pSocket.getOutputStream()));
            }

            waitForResponse();

            /* If connected try to get MPDs version */
            String readString = null;
            while (readyRead()) {
                readString = pReader.readLine();
                Log.v(TAG, "Read string: " + readString);
                /* Look out for the greeting message */
                if (readString.startsWith("OK MPD ")) {
                    pVersionString = readString.substring(7);
                    String[] versions = pVersionString.split("\\.");
                    if (versions.length == 3) {
                        pMajorVersion = Integer.valueOf(versions[0]);
                        pMinorVersion = Integer.valueOf(versions[1]);
                    }

                }
            }
            Log.v(TAG, "MPD Version: " + pMajorVersion + ":" + pMinorVersion);
            pMPDConnectionReady = true;

            if (pPassword != null && !pPassword.equals("")) {
                /* Authenticate with server because password is set. */
                boolean authenticated = authenticateMPDServer();
            }

            // Start the initial idling procedure.
            startIdleWait();

            // Set the timeout to infinite again
            pSocket.setSoTimeout(0);

            // Notify listener
            notifyConnected();
        }
    }


    /**
     * If the password for the MPDConnection is set then the client should
     * try to authenticate with the server
     */
    private boolean authenticateMPDServer() throws IOException {
        Log.v(TAG, "authenticateMPDServer: " + this + " ready: " + pMPDConnectionReady + " connection idle: " + pMPDConnectionIdle);

        /* Check if connection really is good to go. */
        if (!pMPDConnectionReady || pMPDConnectionIdle) {
            return false;
        }

        sendMPDCommand(MPDCommands.MPD_COMMAND_PASSWORD + pPassword);

        /* Check if the result was positive or negative */

        String readString = null;

        boolean success = false;
        while (readyRead()) {
            readString = pReader.readLine();
            if (readString.startsWith("OK")) {
                success = true;
            } else if (readString.startsWith("ACK")) {
                success = false;
                Log.e(TAG, "Could not successfully authenticate with mpd server");
            }
        }


        return success;
    }

    /**
     * Requests to disconnect from server. This will close the conection and cleanup the socket.
     * After this call it should be safe to reconnect to another server. If this connection is
     * currently in idle state, then it will be deidled before.
     */
    public void disconnectFromServer() {
        synchronized (this) {
            // Stop possible timers waiting for the timeout to go idle
            stopIdleWait();

            // Check if the connection is currently idling, if then deidle.
            if (pMPDConnectionIdle) {
                stopIdleing();
            }

            /* Cleanup reader/writer */
            try {
                /* Clear reader/writer up */
                if (null != pReader) {
                    pReader = null;
                }
                if (null != pWriter) {
                    pWriter = null;
                }

                /* Clear TCP-Socket up */
                if (null != pSocket && pSocket.isConnected()) {
                    pSocket.setSoTimeout(1000);
                    pSocket.close();
                    pSocket = null;
                }
            } catch (IOException e) {
                Log.e(TAG, "Error during disconnecting:" + e.toString());
            }

            /* Clear up connection state variables */
            pMPDConnectionIdle = false;
            pMPDConnectionReady = false;

            // Notify listener
            notifyDisconnect();
        }
    }

    /**
     * This functions sends the command to the MPD server.
     * If the server is currently idling then it will deidle it first.
     *
     * @param command
     */
    private void sendMPDCommand(String command) {
        // Stop possible idling timeout tasks.
        stopIdleWait();


        /* Check if the server is connected. */
        if (pMPDConnectionReady) {

            /*
             * Check if server is in idling mode, this needs unidling first,
             * otherwise the server will disconnect the client.
             */
            if (pMPDConnectionIdle) {
                stopIdleing();
            }

            /*
             * Send the command to the server
             * FIXME Should be validated in the future.
             */
            pWriter.println(command);
            pWriter.flush();

            // This waits until the server sends a response (OK,ACK(failure) or the requested data)
            waitForResponse();
        }
    }

    /**
     * This functions sends the command to the MPD server.
     * This function is used between start command list and the end. It has no check if the
     * connection is currently idle.
     * Also it will not wait for a response because this would only deadlock, because the mpd server
     * waits until the end_command is received.
     *
     * @param command
     */
    private void sendMPDRAWCommand(String command) {
        /* Check if the server is connected. */
        if (pMPDConnectionReady) {
            /*
             * Send the command to the server
             * FIXME Should be validated in the future.
             */
            pWriter.println(command);
            pWriter.flush();
        }
    }

    /**
     * This will start a command list to the server. It can be used to speed up multiple requests
     * like adding songs to the current playlist. Make sure that the idle timeout is stopped
     * before starting a command list.
     */
    private void startCommandList() {
        /* Check if the server is connected. */
        if (pMPDConnectionReady) {
            /* Check if server is in idling mode, this needs unidling first,
            otherwise the server will disconnect the client.
             */
            if (pMPDConnectionIdle) {
                stopIdleing();
            }

            /*
             * Send the command to the server
             * FIXME Should be validated in the future.
             */
            pWriter.println(MPDCommands.MPD_START_COMMAND_LIST);
            pWriter.flush();

        }
    }

    /**
     * This command will end the command list. After this call it is important to call
     * checkResponse to clear the possible response in the read buffer. There should be at
     * least one "OK" or "ACK" from the mpd server.
     */
    private void endCommandList() {
        /* Check if the server is connected. */
        if (pMPDConnectionReady) {
            /*
             * Send the command to the server
             * FIXME Should be validated in the future.
             */
            pWriter.println(MPDCommands.MPD_END_COMMAND_LIST);
            pWriter.flush();

        }
    }


    /**
     * This method needs to be called before a new MPD command is sent to
     * the server to correctly unidle. Otherwise the mpd server will disconnect
     * the disobeying client.
     */
    private void stopIdleing() {
        /* Check if server really is in idling mode */
        if (!pMPDConnectionIdle || !pMPDConnectionReady) {
            return;
        }
        Log.v(TAG, "Deidling MPD before request");


        /* Send the "noidle" command to the server to initiate noidle */
        pWriter.println(MPDCommands.MPD_COMMAND_STOP_IDLE);
        pWriter.flush();

        /* Wait for idle thread to release the lock, which means we are finished waiting */
        try {
            mIdleWaitLock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        mIdleWaitLock.release();

    }

    /**
     * Initiates the idling procedure. A separate thread is started to wait (blocked)
     * for a deidle from the MPD host. Otherwise it is impossible to get notified on changes
     * from other mpd clients (eg. volume change)
     */
    private void startIdleing() {
        synchronized (this) {
        /* Check if server really is in idling mode */
            if (!pMPDConnectionReady || pMPDConnectionIdle) {
                return;
            }
            Log.v(TAG, "MPDConnection: " + this + "Sending idle command");

            mRequestedDeidle = false;

            pWriter.println(MPDCommands.MPD_COMMAND_START_IDLE);
            pWriter.flush();
            pIdleThread = new IdleThread();
            pIdleThread.start();

            Log.v(TAG, "Runnable is waiting for idle finish");
            pMPDConnectionIdle = true;

            if (null != pIdleListener) {
                pIdleListener.onIdle();
            }
        }
    }

    /**
     * Function only actively waits for reader to get ready for
     * the response.
     */
    private void waitForResponse() {
        if (null != pReader) {
            try {
                while (!readyRead()) {
                }
            } catch (IOException e) {
                handleReadError();
            }
        }
    }

    /**
     * Checks if a simple command was successful or not (OK vs. ACK)
     *
     * @return True if command was successfully executed, false otherwise.
     */
    public boolean checkResponse() throws IOException {
        boolean success = false;
        waitForResponse();
        String response;

        // Wait for data to be available to read. MPD communication could take some time.
        while (readyRead()) {
            response = pReader.readLine();
            if (response.startsWith("OK")) {
                success = true;
            } else if (response.startsWith("ACK")) {
                success = false;
                Log.e(TAG, "Server response error: " + response);
            }
        }

        // The command was handled now it is time to set the connection to idle again (after the timeout,
        // to prevent disconnecting).
        startIdleWait();

        // Return if successful or not.
        return success;
    }



    /*
     * *******************************
     * * Response handling functions *
     * *******************************
     */

    /**
     * Parses the return of MPD when a list of albums was requested.
     *
     * @param albumArtist Artist to be added to all MPDAlbum objects. (Useful for later GUI)
     * @return List of MPDAlbum objects
     * @throws IOException
     */
    private ArrayList<MPDAlbum> parseMPDAlbums(String albumArtist) throws IOException {
        ArrayList<MPDAlbum> albumList = new ArrayList<MPDAlbum>();
        if (!pMPDConnectionReady) {
            return albumList;
        }
        if (!readyRead()) {
            return null;
        }
        /* Parse the MPD response and create a list of MPD albums */
        String response = pReader.readLine();

        boolean emptyAlbum = false;
        String albumName = "";
        String albumMBID = "";

        MPDAlbum tempAlbum;

        while (!response.startsWith("OK") && !response.startsWith("ACK") && !pSocket.isClosed()) {

            if (response == null) {
                /* skip this invalid (empty) response */
                continue;
            }

            /* Check if the response is an album */
            if (response.startsWith(MPDResponses.MPD_RESPONSE_ALBUM_NAME)) {
                /* We found an album, add it to the list. */
                if (!albumName.equals("") || emptyAlbum) {
                    tempAlbum = new MPDAlbum(albumName, albumMBID, albumArtist);
                    //Log.v(TAG,"Add album to list: " + albumName + ":" + albumMBID + ":"  + albumArtist);
                    albumList.add(tempAlbum);
                }
                albumName = response.substring(MPDResponses.MPD_RESPONSE_ALBUM_NAME.length());
                if (albumName.equals("")) {
                    emptyAlbum = true;
                }
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_ALBUM_MBID)) {
                /* Check if the response is a musicbrainz_albumid. */
                albumMBID = response.substring(MPDResponses.MPD_RESPONSE_ALBUM_MBID.length());
            }
            response = pReader.readLine();
        }

        /* Because of the loop structure the last album has to be added because no
        "ALBUM:" is sent anymore.
         */
        if (!albumName.equals("") || emptyAlbum) {
            tempAlbum = new MPDAlbum(albumName, albumMBID, albumArtist);
            albumList.add(tempAlbum);
        }

        // Start the idling timeout again.
        startIdleWait();
        // Sort the albums for later sectioning.
        Collections.sort(albumList);
        return albumList;
    }

    /**
     * Parses the return stream of MPD when a list of artists was requested.
     *
     * @return List of MPDArtists objects
     * @throws IOException
     */
    private ArrayList<MPDArtist> parseMPDArtists() throws IOException {
        ArrayList<MPDArtist> artistList = new ArrayList<MPDArtist>();
        if (!pMPDConnectionReady) {
            return artistList;
        }
        if (!readyRead()) {
            return null;
        }

        /* Parse MPD artist return values and create a list of MPDArtist objects */
        String response = pReader.readLine();

        /* Artist properties */
        String artistName;
        String artistMBID = "";

        MPDArtist tempArtist;

        while (!response.startsWith("OK") && !response.startsWith("ACK") && !pSocket.isClosed()) {

            if (response == null) {
                /* skip this invalid (empty) response */
                continue;
            }

            if (response.startsWith(MPDResponses.MPD_RESPONSE_ARTIST_NAME)) {
                artistName = response.substring(MPDResponses.MPD_RESPONSE_ARTIST_NAME.length());
                tempArtist = new MPDArtist(artistName, artistMBID);
                artistList.add(tempArtist);
                //Log.v(TAG,"Added artist: " + artistName + ":" + artistMBID);
            } else if (response.startsWith("OK")) {
                break;
            }
            response = pReader.readLine();
        }

        // Start the idling timeout again.
        startIdleWait();
        // Sort the artists for later sectioning.
        Collections.sort(artistList);
        return artistList;
    }

    /**
     * Parses the response of mpd on requests that return track items. This is also used
     * for MPD file, directory and playlist responses. This allows the GUI to develop
     * one adapter for all three types. Also MPD mixes them when requesting directory listings.
     * <p/>
     * It will return a list of MPDFileEntry objects which is a parent class for (MPDFile, MPDPlaylist,
     * MPDDirectory) you can use instanceof to check which type you got.
     *
     * @param filterArtist Artist used for filtering. Non-matching tracks get discarded.
     * @return List of MPDFileEntry objects
     * @throws IOException
     */
    private ArrayList<MPDFileEntry> parseMPDTracks(String filterArtist) throws IOException {
        ArrayList<MPDFileEntry> trackList = new ArrayList<MPDFileEntry>();
        if (!pMPDConnectionReady) {
            return trackList;
        }

        /* Temporary track item (added to list later */
        MPDFileEntry tempFileEntry = null;

        if (!readyRead()) {
            return null;
        }

        /* Response line from MPD */
        String response = pReader.readLine();
        while (!response.startsWith("OK") && !response.startsWith("ACK") && !pSocket.isClosed()) {

            /* This if block will just check all the different response possible by MPDs file/dir/playlist response */
            if (response.startsWith(MPDResponses.MPD_RESPONSE_FILE)) {
                if (null != tempFileEntry) {
                    /* Check the artist filter criteria here */
                    if (tempFileEntry instanceof MPDFile) {
                        MPDFile file = (MPDFile) tempFileEntry;
                        if (filterArtist.equals(file.getTrackArtist()) || filterArtist.equals("")) {
                            trackList.add(tempFileEntry);
                        }
                    } else {
                        trackList.add(tempFileEntry);
                    }
                }
                tempFileEntry = new MPDFile(response.substring(MPDResponses.MPD_RESPONSE_FILE.length()));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_TRACK_TITLE)) {
                ((MPDFile) tempFileEntry).setTrackTitle(response.substring(MPDResponses.MPD_RESPONSE_TRACK_TITLE.length()));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_ARTIST_NAME)) {
                ((MPDFile) tempFileEntry).setTrackArtist(response.substring(MPDResponses.MPD_RESPONSE_ARTIST_NAME.length()));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_ALBUM_ARTIST_NAME)) {
                ((MPDFile) tempFileEntry).setTrackAlbumArtist(response.substring(MPDResponses.MPD_RESPONSE_ALBUM_ARTIST_NAME.length()));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_ALBUM_NAME)) {
                ((MPDFile) tempFileEntry).setTrackAlbum(response.substring(MPDResponses.MPD_RESPONSE_ALBUM_NAME.length()));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_DATE)) {
                ((MPDFile) tempFileEntry).setDate(response.substring(MPDResponses.MPD_RESPONSE_DATE.length()));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_ALBUM_MBID)) {
                ((MPDFile) tempFileEntry).setTrackAlbumMBID(response.substring(MPDResponses.MPD_RESPONSE_ALBUM_MBID.length()));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_ARTIST_MBID)) {
                ((MPDFile) tempFileEntry).setTrackArtistMBID(response.substring(MPDResponses.MPD_RESPONSE_ARTIST_MBID.length()));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_TRACK_MBID)) {
                ((MPDFile) tempFileEntry).setTrackMBID(response.substring(MPDResponses.MPD_RESPONSE_TRACK_MBID.length()));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_TRACK_TIME)) {
                ((MPDFile) tempFileEntry).setLength(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_TRACK_TIME.length())));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_DISC_NUMBER)) {
                /*
                * Check if MPD returned a discnumber like: "1" or "1/3" and set disc count accordingly.
                */
                String discNumber = response.substring(MPDResponses.MPD_RESPONSE_DISC_NUMBER.length());
                discNumber = discNumber.replaceAll(" ","");
                String[] discNumberSep = discNumber.split("/");
                if (discNumberSep.length > 0) {
                    try {
                        ((MPDFile) tempFileEntry).setDiscNumber(Integer.valueOf(discNumberSep[0]));
                    } catch (NumberFormatException e ) {
                        Log.w(TAG,"Could not parse disc number: " + discNumber);
                    }

                    if (discNumberSep.length > 1) {
                        try {
                            ((MPDFile) tempFileEntry).psetAlbumDiscCount(Integer.valueOf(discNumberSep[1]));
                        } catch (NumberFormatException e ) {
                            Log.w(TAG,"Could not parse disc number: " + discNumber);
                        }
                    }
                } else {
                    try {
                        ((MPDFile) tempFileEntry).setDiscNumber(Integer.valueOf(discNumber));
                    } catch (NumberFormatException e ) {
                        Log.w(TAG,"Could not parse disc number: " + discNumber);
                    }
                }
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_TRACK_NUMBER)) {
                /*
                 * Check if MPD returned a tracknumber like: "12" or "12/42" and set albumtrack count accordingly.
                 */
                String trackNumber = response.substring(MPDResponses.MPD_RESPONSE_TRACK_NUMBER.length());
                trackNumber = trackNumber.replaceAll(" ","");
                String[] trackNumbersSep = trackNumber.split("/");
                if (trackNumbersSep.length > 0) {
                    try {
                        ((MPDFile) tempFileEntry).setTrackNumber(Integer.valueOf(trackNumbersSep[0]));
                    } catch (NumberFormatException e ) {
                        Log.w(TAG,"Could not parse track number: " + trackNumber);
                    }
                    if (trackNumbersSep.length > 1) {
                        try {
                            ((MPDFile) tempFileEntry).setAlbumTrackCount(Integer.valueOf(trackNumbersSep[1]));
                        } catch (NumberFormatException e ) {
                            Log.w(TAG,"Could not parse track number: " + trackNumber);
                        }
                    }
                } else {
                    try {
                        ((MPDFile) tempFileEntry).setTrackNumber(Integer.valueOf(trackNumber));
                    } catch (NumberFormatException e ) {
                        Log.w(TAG,"Could not parse track number: " + trackNumber);
                    }
                }
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_LAST_MODIFIED)) {
                tempFileEntry.setLastModified(response.substring(MPDResponses.MPD_RESPONSE_LAST_MODIFIED.length()));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_PLAYLIST)) {
                if (null != tempFileEntry) {
                    /* Check the artist filter criteria here */
                    if (tempFileEntry instanceof MPDFile) {
                        MPDFile file = (MPDFile) tempFileEntry;
                        if (filterArtist.equals(file.getTrackArtist()) || filterArtist.equals("")) {
                            trackList.add(tempFileEntry);
                        }
                    } else {
                        trackList.add(tempFileEntry);
                    }
                }
                tempFileEntry = new MPDPlaylist(response.substring(MPDResponses.MPD_RESPONSE_PLAYLIST.length()));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_DIRECTORY)) {
                if (null != tempFileEntry) {
                    /* Check the artist filter criteria here */
                    if (tempFileEntry instanceof MPDFile) {
                        MPDFile file = (MPDFile) tempFileEntry;
                        if (filterArtist.equals(file.getTrackArtist()) || filterArtist.equals("")) {
                            trackList.add(tempFileEntry);
                        }
                    } else {
                        trackList.add(tempFileEntry);
                    }
                }
                tempFileEntry = new MPDDirectory(response.substring(MPDResponses.MPD_RESPONSE_DIRECTORY.length()));
            }

            // Move to the next line.
            response = pReader.readLine();

        }

        /* Add last remaining track to list. */
        if (null != tempFileEntry) {
                    /* Check the artist filter criteria here */
            if (tempFileEntry instanceof MPDFile) {
                MPDFile file = (MPDFile) tempFileEntry;
                if (filterArtist.equals(file.getTrackArtist()) || filterArtist.equals("")) {
                    trackList.add(tempFileEntry);
                }
            } else {
                trackList.add(tempFileEntry);
            }
        }
        startIdleWait();
        return trackList;
    }

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
    public List<MPDAlbum> getAlbums() {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMS);
            try {
        /* No artistName here because it is a full list */
                return parseMPDAlbums("");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Get a list of all albums by an artist.
     *
     * @param artistName Artist to filter album lsit with.
     * @return List of MPDAlbum objects
     */
    public List<MPDAlbum> getArtistAlbums(String artistName) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ARTIST_ALBUMS(artistName));
            try {
                return parseMPDAlbums(artistName);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Get a list of all artists available in MPDs database
     *
     * @return List of MPDArtist objects
     */
    public List<MPDArtist> getArtists() {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ARTISTS);
            try {
                return parseMPDArtists();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Get a list of all playlists available in MPDs database
     *
     * @return List of MPDArtist objects
     */
    public List<MPDFileEntry> getPlaylists() {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_GET_SAVED_PLAYLISTS);
            try {
                return parseMPDTracks("");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Gets all tracks from MPD server. This could take a long time to process. Be warned.
     *
     * @return A list of all tracks in MPDFile objects
     */
    public List<MPDFileEntry> getAllTracks() {
        synchronized (this) {
            Log.w(TAG, "This command should not be used");
            sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALL_FILES);
            try {
                return parseMPDTracks("");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }


    /**
     * Returns the list of tracks that are part of albumName
     *
     * @param albumName Album to get tracks from
     * @return List of MPDFile track objects
     */
    public List<MPDFileEntry> getAlbumTracks(String albumName) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUM_TRACKS(albumName));
            try {
                return parseMPDTracks("");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Returns the list of tracks that are part of albumName and from artistName
     *
     * @param albumName  Album to get tracks from
     * @param artistName Artist to filter with
     * @return List of MPDFile track objects
     */
    public List<MPDFileEntry> getArtistAlbumTracks(String albumName, String artistName) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUM_TRACKS(albumName));
            try {
            /* Filter tracks with artistName */
                return parseMPDTracks(artistName);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Requests the current playlist of the server
     *
     * @return List of MPDFile items with all tracks of the current playlist
     */
    public List<MPDFileEntry> getCurrentPlaylist() {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_PLAYLIST);
            try {
            /* Parse the return */
                return parseMPDTracks("");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Requests the current playlist of the server with a window
     *
     * @return List of MPDFile items with all tracks of the current playlist
     */
    public List<MPDFileEntry> getCurrentPlaylistWindow(int start, int end) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_PLAYLIST_WINDOW(start, end));
            try {
            /* Parse the return */
                return parseMPDTracks("");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Requests the current playlist of the server
     *
     * @return List of MPDFile items with all tracks of the current playlist
     */
    public List<MPDFileEntry> getSavedPlaylist(String playlistName) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_GET_SAVED_PLAYLIST(playlistName));
            try {
            /* Parse the return */
                return parseMPDTracks("");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Requests the files for a specific path with info
     *
     * @return List of MPDFile items with all tracks of the current playlist
     */
    public List<MPDFileEntry> getFiles(String path) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_GET_FILES_INFO(path));
            try {
            /* Parse the return */
                return parseMPDTracks("");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Requests the files for a specific search term and type
     * @param term The search term to use
     * @param type The type of items to search
     * @return List of MPDFile items with all tracks matching the search
     */
    public List<MPDFileEntry> getSearchedFiles(String term, MPDCommands.MPD_SEARCH_TYPE type) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_SEARCH_FILES(term,type));
            try {
            /* Parse the return */
                return parseMPDTracks("");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Requests the currentstatus package from the mpd server.
     *
     * @return The CurrentStatus object with all gathered information.
     */
    public MPDCurrentStatus getCurrentServerStatus() throws IOException {
        synchronized (this) {
            MPDCurrentStatus status = new MPDCurrentStatus();

        /* Request status */
            sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_STATUS);

            if (!readyRead()) {
                return status;
            }
        /* Response line from MPD */
            String response = pReader.readLine();
            while (!response.startsWith("OK") && !response.startsWith("ACK") && !pSocket.isClosed()) {


                if (response.startsWith(MPDResponses.MPD_RESPONSE_VOLUME)) {
                    status.setVolume(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_VOLUME.length())));
                } else if (response.startsWith(MPDResponses.MPD_RESPONSE_REPEAT)) {
                    status.setRepeat(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_REPEAT.length())));
                } else if (response.startsWith(MPDResponses.MPD_RESPONSE_RANDOM)) {
                    status.setRandom(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_RANDOM.length())));
                } else if (response.startsWith(MPDResponses.MPD_RESPONSE_SINGLE)) {
                    status.setSinglePlayback(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_SINGLE.length())));
                } else if (response.startsWith(MPDResponses.MPD_RESPONSE_CONSUME)) {
                    status.setConsume(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_CONSUME.length())));
                } else if (response.startsWith(MPDResponses.MPD_RESPONSE_PLAYLIST_VERSION)) {
                    status.setPlaylistVersion(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_PLAYLIST_VERSION.length())));
                } else if (response.startsWith(MPDResponses.MPD_RESPONSE_PLAYLIST_LENGTH)) {
                    status.setPlaylistLength(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_PLAYLIST_LENGTH.length())));
                } else if (response.startsWith(MPDResponses.MPD_RESPONSE_PLAYBACK_STATE)) {
                    String state = response.substring(MPDResponses.MPD_RESPONSE_PLAYBACK_STATE.length());

                    if (state.equals(MPDResponses.MPD_PLAYBACK_STATE_RESPONSE_PLAY)) {
                        status.setPlaybackState(MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PLAYING);
                    } else if (state.equals(MPDResponses.MPD_PLAYBACK_STATE_RESPONSE_PAUSE)) {
                        status.setPlaybackState(MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PAUSING);
                    } else if (state.equals(MPDResponses.MPD_PLAYBACK_STATE_RESPONSE_STOP)) {
                        status.setPlaybackState(MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_STOPPED);
                    }
                } else if (response.startsWith(MPDResponses.MPD_RESPONSE_CURRENT_SONG_INDEX)) {
                    status.setCurrentSongIndex(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_CURRENT_SONG_INDEX.length())));
                } else if (response.startsWith(MPDResponses.MPD_RESPONSE_NEXT_SONG_INDEX)) {
                    status.setNextSongIndex(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_NEXT_SONG_INDEX.length())));
                } else if (response.startsWith(MPDResponses.MPD_RESPONSE_TIME_INFORMATION_OLD)) {
                    String timeInfo = response.substring(MPDResponses.MPD_RESPONSE_TIME_INFORMATION_OLD.length());

                    String timeInfoSep[] = timeInfo.split(":");
                    if (timeInfoSep.length == 2) {
                        status.setElapsedTime(Integer.valueOf(timeInfoSep[0]));
                        status.setTrackLength(Integer.valueOf(timeInfoSep[1]));
                    }
                } else if (response.startsWith(MPDResponses.MPD_RESPONSE_ELAPSED_TIME)) {
                    status.setElapsedTime(Math.round(Float.valueOf(response.substring(MPDResponses.MPD_RESPONSE_ELAPSED_TIME.length()))));
                } else if (response.startsWith(MPDResponses.MPD_RESPONSE_DURATION)) {
                    status.setTrackLength(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_DURATION.length())));
                } else if (response.startsWith(MPDResponses.MPD_RESPONSE_BITRATE)) {
                    status.setBitrate(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_BITRATE.length())));
                } else if (response.startsWith(MPDResponses.MPD_RESPONSE_AUDIO_INFORMATION)) {
                    String audioInfo = response.substring(MPDResponses.MPD_RESPONSE_AUDIO_INFORMATION.length());

                    String audioInfoSep[] = audioInfo.split(":");
                    if (audioInfoSep.length == 3) {
                    /* Extract the separate pieces */
                        try {
                    /* First is sampleRate */
                            status.setSamplerate(Integer.valueOf(audioInfoSep[0]));
                    /* Second is bitresolution */
                            status.setBitDepth(Integer.valueOf(audioInfoSep[1]));
                    /* Third is channel count */
                            status.setChannelCount(Integer.valueOf(audioInfoSep[2]));
                        } catch (NumberFormatException e) {
                            Log.w(TAG,"Error parsing audio properties");
                        }
                    }
                } else if (response.startsWith(MPDResponses.MPD_RESPONSE_UPDATING_DB)) {
                    status.setUpdateDBJob(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_UPDATING_DB.length())));
                }

                response = pReader.readLine();
            }

            startIdleWait();
            return status;
        }
    }

    /**
     * Requests the server statistics package from the mpd server.
     *
     * @return The CurrentStatus object with all gathered information.
     */
    public MPDStatistics getServerStatistics() throws IOException {
        synchronized (this) {
            MPDStatistics stats = new MPDStatistics();

        /* Request status */
            sendMPDCommand(MPDCommands.MPD_COMMAND_GET_STATISTICS);

        /* Response line from MPD */
            String response;
            while (readyRead()) {
                response = pReader.readLine();

                if (response.startsWith(MPDResponses.MPD_STATS_UPTIME)) {
                    stats.setServerUptime(Integer.valueOf(response.substring(MPDResponses.MPD_STATS_UPTIME.length())));
                } else if (response.startsWith(MPDResponses.MPD_STATS_PLAYTIME)) {
                    stats.setPlayDuration(Integer.valueOf(response.substring(MPDResponses.MPD_STATS_PLAYTIME.length())));
                } else if (response.startsWith(MPDResponses.MPD_STATS_ARTISTS)) {
                    stats.setArtistsCount(Integer.valueOf(response.substring(MPDResponses.MPD_STATS_ARTISTS.length())));
                } else if (response.startsWith(MPDResponses.MPD_STATS_ALBUMS)) {
                    stats.setAlbumCount(Integer.valueOf(response.substring(MPDResponses.MPD_STATS_ALBUMS.length())));
                } else if (response.startsWith(MPDResponses.MPD_STATS_SONGS)) {
                    stats.setSongCount(Integer.valueOf(response.substring(MPDResponses.MPD_STATS_SONGS.length())));
                } else if (response.startsWith(MPDResponses.MPD_STATS_DB_PLAYTIME)) {
                    stats.setAllSongDuration(Integer.valueOf(response.substring(MPDResponses.MPD_STATS_DB_PLAYTIME.length())));
                } else if (response.startsWith(MPDResponses.MPD_STATS_DB_LAST_UPDATE)) {
                    stats.setLastDBUpdate(Long.valueOf(response.substring(MPDResponses.MPD_STATS_DB_LAST_UPDATE.length())));
                }
            }

            startIdleWait();
            return stats;
        }
    }

    /**
     * This will query the current song playing on the mpd server.
     *
     * @return MPDFile entry for the song playing.
     * @throws IOException
     */
    public MPDFile getCurrentSong() throws IOException {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_SONG);

            // Reuse the parsing function for tracks here.
            List<MPDFileEntry> retList = parseMPDTracks("");
            if (retList.size() == 1) {
                // If one element is in the list it is safe to assume that this element is
                // the current song. So casting is no problem.
                return (MPDFile) retList.get(0);
            } else {
                return null;
            }
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
    public boolean pause(boolean pause) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_PAUSE(pause));

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Jumps to the next song
     *
     * @return true if successful, false otherwise
     */
    public boolean nextSong() {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_NEXT);

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Jumps to the previous song
     *
     * @return true if successful, false otherwise
     */
    public boolean previousSong() {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_PREVIOUS);

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Stops playback
     *
     * @return true if successful, false otherwise
     */
    public boolean stopPlayback() {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_STOP);

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Sets random to true or false
     *
     * @param random If random should be set (true) or not (false)
     * @return True if server responed with ok
     */
    public boolean setRandom(boolean random) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_SET_RANDOM(random));

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Sets repeat to true or false
     *
     * @param repeat If repeat should be set (true) or not (false)
     * @return True if server responed with ok
     */
    public boolean setRepeat(boolean repeat) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_SET_REPEAT(repeat));

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Sets single playback to enable (true) or disabled (false)
     *
     * @param single if single playback should be enabled or not.
     * @return True if server responed with ok
     */
    public boolean setSingle(boolean single) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_SET_SINGLE(single));

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Sets if files should be removed after playback (consumed)
     *
     * @param consume True if yes and false if not.
     * @return True if server responed with ok
     */
    public boolean setConsume(boolean consume) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_SET_CONSUME(consume));

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Plays the song with the index in the current playlist.
     *
     * @param index Index of the song that should be played.
     * @return True if server responed with ok
     */
    public boolean playSongIndex(int index) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_PLAY_SONG_INDEX(index));

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Seeks the currently playing song to a certain position
     *
     * @param seconds Position in seconds to which a seek is requested to.
     * @return True if server responed with ok
     */
    public boolean seekSeconds(int seconds) {
        synchronized (this) {
            MPDCurrentStatus status = null;
            try {
                status = getCurrentServerStatus();
            } catch (IOException e) {
                e.printStackTrace();
            }

            sendMPDCommand(MPDCommands.MPD_COMMAND_SEEK_SECONDS(status.getCurrentSongIndex(), seconds));

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Sets the volume of the mpd servers output. It is an absolute value between (0-100).
     *
     * @param volume Volume to set to the server.
     * @return True if server responed with ok
     */
    public boolean setVolume(int volume) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_SET_VOLUME(volume));

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
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
     * @return True if server responed with ok
     */
    private boolean addTrackList(List<MPDFileEntry> tracks) {
        synchronized (this) {
            if (null == tracks) {
                return false;
            }
            startCommandList();

            for (MPDFileEntry track : tracks) {
                if (track instanceof MPDFile) {
                    sendMPDRAWCommand(MPDCommands.MPD_COMMAND_ADD_FILE(track.getPath()));
                }
            }
            endCommandList();

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Adds all tracks from a certain album from artistname to the current playlist.
     *
     * @param albumname  Name of the album to add to the current playlist.
     * @param artistname Name of the artist of the album to add to the list. This
     *                   allows filtering of album tracks to a specified artist. Can also
     *                   be left empty then all tracks from the album will be added.
     * @return True if server responed with ok
     */
    public boolean addAlbumTracks(String albumname, String artistname) {
        synchronized (this) {
            List<MPDFileEntry> tracks = getArtistAlbumTracks(albumname, artistname);
            return addTrackList(tracks);
        }
    }

    /**
     * Adds all albums of an artist to the current playlist. Will first get a list of albums for the
     * artist and then call addAlbumTracks for every album on this result.
     *
     * @param artistname Name of the artist to enqueue the albums from.
     * @return True if server responed with ok
     */
    public boolean addArtist(String artistname) {
        synchronized (this) {
            List<MPDAlbum> albums = getArtistAlbums(artistname);
            if (null == albums) {
                return false;
            }

            boolean success = true;
            for (MPDAlbum album : albums) {
                if (!(addAlbumTracks(album.getName(), artistname))) {
                    success = false;
                }
            }
            return success;
        }
    }

    /**
     * Adds a single File/Directory to the current playlist.
     *
     * @param url URL of the file or directory! to add to the current playlist.
     * @return True if server responed with ok
     */
    public boolean addSong(String url) {
        synchronized (this) {
            Log.v(TAG, "Add: " + url);
            sendMPDCommand(MPDCommands.MPD_COMMAND_ADD_FILE(url));

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * This method adds a song to a specified positiion in the current playlist.
     * This allows GUI developers to implement a method like "add after current".
     *
     * @param url   URL to add to the playlist.
     * @param index Index at which the item should be added.
     * @return True if server responed with ok
     */
    public boolean addSongatIndex(String url, int index) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_ADD_FILE_AT_INDEX(url, index));

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Adds files to the playlist with a search term for a specific type
     * @param term The search term to use
     * @param type The type of items to search
     * @return True if server responed with ok
     */
    public boolean addSearchedFiles(String term, MPDCommands.MPD_SEARCH_TYPE type) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_ADD_SEARCH_FILES(term,type));
            try {
            /* Parse the return */
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Instructs the mpd server to clear its current playlist.
     *
     * @return True if server responed with ok
     */
    public boolean clearPlaylist() {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_CLEAR_PLAYLIST);
        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Instructs the mpd server to remove one item from the current playlist at index.
     *
     * @param index Position of the item to remove from current playlist.
     * @return True if server responed with ok
     */
    public boolean removeIndex(int index) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_SONG_FROM_CURRENT_PLAYLIST(index));
        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Moves one item from an index in the current playlist to an new index. This allows to move
     * tracks for example after the current to priotize songs.
     *
     * @param from Item to move from.
     * @param to   Position to enter item
     * @return
     */
    public boolean moveSongFromTo(int from, int to) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_MOVE_SONG_FROM_INDEX_TO_INDEX(from, to));
        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Saves the current playlist as a new playlist with a name.
     *
     * @param name Name of the playlist to save to.
     * @return True if server responed with ok
     */
    public boolean savePlaylist(String name) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_SAVE_PLAYLIST(name));

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Removes a saved playlist from the servers database.
     *
     * @param name Name of the playlist to remove.
     * @return True if server responed with ok
     */
    public boolean removePlaylist(String name) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_PLAYLIST(name));

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Loads a saved playlist (added after the last song) to the current playlist.
     *
     * @param name Of the playlist to add to.
     * @return True if server responed with ok
     */
    public boolean loadPlaylist(String name) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_LOAD_PLAYLIST(name));

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Private parsing method for MPDs output lists.
     *
     * @return A list of MPDOutput objects with name,active,id values if successful. Otherwise empty list.
     * @throws IOException
     */
    private List<MPDOutput> parseMPDOutputs() throws IOException {
        ArrayList<MPDOutput> outputList = new ArrayList<>();
        // Parse outputs
        String outputName = null;
        boolean outputActive = false;
        int outputId = -1;

        if (!readyRead()) {
            return null;
        }

        /* Response line from MPD */
        String response = pReader.readLine();
        while (!response.startsWith("OK") && !response.startsWith("ACK")) {
            if (response.startsWith(MPDResponses.MPD_OUTPUT_ID)) {
                if (null != outputName) {
                    MPDOutput tempOutput = new MPDOutput(outputName, outputActive, outputId);
                    outputList.add(tempOutput);
                }
                outputId = Integer.valueOf(response.substring(MPDResponses.MPD_OUTPUT_ID.length()));
            } else if (response.startsWith(MPDResponses.MPD_OUTPUT_NAME)) {
                outputName = response.substring(MPDResponses.MPD_OUTPUT_NAME.length());
            } else if (response.startsWith(MPDResponses.MPD_OUTPUT_ACTIVE)) {
                String activeRespsonse = response.substring(MPDResponses.MPD_OUTPUT_ACTIVE.length());
                if (activeRespsonse.equals("1")) {
                    outputActive = true;
                } else {
                    outputActive = false;
                }
            }
            response = pReader.readLine();
        }

        // Add remaining output to list
        if (null != outputName) {
            MPDOutput tempOutput = new MPDOutput(outputName, outputActive, outputId);
            outputList.add(tempOutput);
        }

        return outputList;

    }

    /**
     * Returns the list of MPDOutputs to the outside callers.
     *
     * @return List of MPDOutput objects or null in case of error.
     */
    public List<MPDOutput> getOutputs() {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_GET_OUTPUTS);

            try {
                return parseMPDOutputs();
            } catch (IOException e) {
                handleReadError();
            }
            return null;
        }
    }

    /**
     * Toggles the state of the output with the id.
     *
     * @param id Id of the output to toggle (active/deactive)
     * @return True if server responed with ok
     */
    public boolean toggleOutput(int id) {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_TOGGLE_OUTPUT(id));

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                handleReadError();
            }
            return false;
        }
    }

    /**
     * Instructs to update the database of the mpd server (path: / )
     *
     * @return True if server responed with ok
     */
    public boolean updateDatabase() {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_UPDATE_DATABASE);

        /* Return the response value of MPD */
            try {
                return checkResponse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }
    }


    /**
     * Checks if the socket is ready for read operations
     *
     * @return True if ready
     * @throws IOException
     */
    private boolean readyRead() throws IOException {
        return (null != pReader) && pReader.ready();
    }

    /**
     * Will notify a connected listener that the connection is now ready to be used.
     */
    private void notifyConnected() {
        if (null != pStateListener) {
            pStateListener.onConnected();
        }
    }

    /**
     * Will notify a connected listener that the connection is disconnect and not ready for use.
     */
    private void notifyDisconnect() {
        if (null != pStateListener) {
            pStateListener.onDisconnected();
        }
    }

    /**
     * Registers a listener to be notified about connection state changes
     *
     * @param listener Listener to be connected
     */
    public void setStateListener(MPDConnectionStateChangeListener listener) {
        pStateListener = listener;
    }

    /**
     * Registers a listener to be notified about changes in idle state of this connection.
     *
     * @param listener
     */
    public void setpIdleListener(MPDConnectionIdleChangeListener listener) {
        pIdleListener = listener;
    }

    /**
     * Interface to used to be informed about connection state changes.
     */
    public interface MPDConnectionStateChangeListener {
        void onConnected();

        void onDisconnected();
    }

    /**
     * Interface to be used to be informed about connection idle state changes.
     */
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
     *
     * @return
     */
    private String waitForIdleResponse() {
        if (null != pReader) {
            try {
                // Set thread to sleep, because there should be no line available to read.
                String response = pReader.readLine();

                return response;
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
     * 1. A deidling request notified the server to quit idling.
     * 2. A change in the MPDs internal state changed and the status of this client needs updating.
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

            // Get the lock to prevent the handler thread from (stopIdling) to interfere with deidling sequence.
            try {
                mIdleWaitLock.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            // This will block this thread until the server has some data available to read again.
            String response = waitForIdleResponse();
            while (response == null) {
                response = waitForIdleResponse();
            }

            // At this position idling is over.
            if (response.startsWith("changed")) {
                try {
                    if (checkResponse()) {
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // No further handling necessary then
            /*else if (response.startsWith("OK")) {

            }*/

            // Set the connection as non-idle again.
            pMPDConnectionIdle = false;

            // Release the lock for possible threads waiting from outside this idling thread (handler thread).
            mIdleWaitLock.release();

            // Notify a possible listener for deidling.
            if (null != pIdleListener) {
                pIdleListener.onNonIdle();
            }

        }
    }


    /**
     * This will start the timeout to set the connection to the idle state after use.
     */
    private void startIdleWait() {
        /**
         * Check if a timer was running and then remove it.
         * This will reset the timeout.
         */
        if (null != mIdleWait) {
            mIdleWait.cancel();
            mIdleWait.purge();
        }
        // Start the new timer with a new Idle Task.
        mIdleWait = new Timer();
        mIdleWait.schedule(new IdleWaitTask(), IDLE_WAIT_TIME);
    }

    /**
     * This will stop a potential running timeout task.
     */
    private void stopIdleWait() {
        if (null != mIdleWait) {
            mIdleWait.cancel();
            mIdleWait.purge();
            mIdleWait = null;
        }
    }

    /**
     * Task that will trigger the idle state of this MPDConnection.
     */
    private class IdleWaitTask extends TimerTask {

        @Override
        public void run() {
            startIdleing();
        }
    }
}
