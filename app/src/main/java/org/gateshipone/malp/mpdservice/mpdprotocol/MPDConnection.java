/*
 * Copyright (C) 2016 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/malp/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gateshipone.malp.mpdservice.mpdprotocol;

import android.util.Log;

import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDDirectory;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFile;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDOutput;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDPlaylist;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDStatistics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

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

    private String mID;

    /**
     * Set this flag to enable debugging in this class. DISABLE before releasing
     */
    private static final boolean DEBUG_ENABLED = false;

    /**
     * Timeout to wait for socket operations (time in ms)
     */
    private static final int SOCKET_TIMEOUT = 5 * 1000;

    /**
     * Time to wait for response from server. If server is not answering this prevents a livelock
     * after 5 seconds. (time in ns)
     */
    private static final long RESPONSE_TIMEOUT = 5L * 1000L * 1000L * 1000L;

    /**
     * Time to sleep the process waiting for a server response. This reduces the busy-waiting to
     * a bit more efficent sleep/check pattern.
     */
    private static int RESPONSE_WAIT_SLEEP_TIME = 250;

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
    private MPDCapabilities mServerCapabilities;

    /**
     * Only get the server capabilities if server parameters changed
     */
    private boolean mCapabilitiesChanged;

    /**
     * One listener for the state of the connection (connected, disconnected)
     */
    private ArrayList<MPDConnectionStateChangeListener> pStateListeners = null;

    /**
     * One listener for the idle state of the connection. Can be used to react
     * to changes to the server from other clients. When the server is deidled (from outside)
     * it will notify this listener.
     */
    private ArrayList<MPDConnectionIdleChangeListener> pIdleListeners = null;

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

    private static MPDConnection mInstance;

    public static synchronized MPDConnection getInstance() {
        if ( null == mInstance) {
            mInstance = new MPDConnection("global");
        }
        return mInstance;
    }

    /**
     * Creates disconnected MPDConnection with following parameters
     */
    private MPDConnection(String id) {
        pSocket = null;
        pReader = null;
        mIdleWaitLock = new Semaphore(1);
        mID = id;
        mServerCapabilities = new MPDCapabilities("", null, null);
        pIdleListeners = new ArrayList<>();
        pStateListeners = new ArrayList<>();
    }

    /**
     * Private function to handle read error. Try to disconnect and remove old sockets.
     * Clear up connection state variables.
     */
    private synchronized void handleSocketError() {
        printDebug("Read error exception. Disconnecting and cleaning up");

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
                pSocket.setSoTimeout(500);
                pSocket.close();
            }
            pSocket = null;
        } catch (IOException e) {
            printDebug("Error during read error handling");
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
     * @param password Password for the server to authenticate with. Can be left empty.
     * @param port     TCP port to connect to.
     */
    public synchronized void setServerParameters(String hostname, String password, int port) {
        pHostname = hostname;
        if (!password.equals("")) {
            pPassword = password;
        }
        pPort = port;
        mCapabilitiesChanged = true;
    }

    /**
     * This is the actual start of the connection. It tries to resolve the hostname
     * and initiates the connection to the address and the configured tcp-port.
     */
    public synchronized void connectToServer() {
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
        try {
            pSocket.connect(new InetSocketAddress(pHostname, pPort), SOCKET_TIMEOUT);
        } catch (IOException e) {
            handleSocketError();
            return;
        }

        /* Check if the socket is connected */
        if (pSocket.isConnected()) {
            /* Try reading from the stream */

            /* Create the reader used for reading from the socket. */
            if (pReader == null) {
                try {
                    pReader = new BufferedReader(new InputStreamReader(pSocket.getInputStream()));
                } catch (IOException e) {
                    handleSocketError();
                    return;
                }
            }

            /* Create the writer used for writing to the socket */
            if (pWriter == null) {
                try {
                    pWriter = new PrintWriter(new OutputStreamWriter(pSocket.getOutputStream()));
                } catch (IOException e) {
                    handleSocketError();
                    return;
                }
            }

            try {
                waitForResponse();
            } catch (IOException e) {
                handleSocketError();
                return;
            }

            /* If connected try to get MPDs version */
            String readString = null;

            String versionString = "";
            try {
                while (readyRead()) {
                    readString = readLine();
                    /* Look out for the greeting message */
                    if (readString.startsWith("OK MPD ")) {
                        versionString = readString.substring(7);

                        String[] versions = versionString.split("\\.");
                        if (versions.length == 3) {
                            // Check if server version changed and if, reread server capabilities later.
                            if ( Integer.valueOf(versions[0]) != mServerCapabilities.getMajorVersion() ||
                                    (Integer.valueOf(versions[0]) == mServerCapabilities.getMajorVersion() && Integer.valueOf(versions[1]) != mServerCapabilities.getMinorVersion())) {
                                mCapabilitiesChanged = true;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                handleSocketError();
                return;
            }
            pMPDConnectionReady = true;

            if (pPassword != null && !pPassword.equals("")) {
                /* Authenticate with server because password is set. */
                boolean authenticated = authenticateMPDServer();
            }


            if (mCapabilitiesChanged) {
                // Get available commands
                sendMPDCommand(MPDCommands.MPD_COMMAND_GET_COMMANDS);

                List<String> commands = null;
                try {
                    commands = parseMPDCommands();
                } catch (IOException e) {
                    handleSocketError();
                    return;
                }

                // Get list of supported tags
                sendMPDCommand(MPDCommands.MPD_COMMAND_GET_TAGS);
                List<String> tags = null;
                try {
                    tags = parseMPDTagTypes();
                } catch (IOException e) {
                    handleSocketError();
                    return;
                }

                mServerCapabilities = new MPDCapabilities(versionString, commands, tags);
                mCapabilitiesChanged = false;
            }


            // Start the initial idling procedure.
            startIdleWait();

            // Set the timeout to infinite again
            try {
                pSocket.setSoTimeout(SOCKET_TIMEOUT);
            } catch (SocketException e) {
                handleSocketError();
                return;
            }

            // Notify listener
            notifyConnected();
        }
    }


    /**
     * If the password for the MPDConnection is set then the client should
     * try to authenticate with the server
     */
    private boolean authenticateMPDServer() {
        /* Check if connection really is good to go. */
        if (!pMPDConnectionReady || pMPDConnectionIdle) {
            return false;
        }

        sendMPDCommand(MPDCommands.MPD_COMMAND_PASSWORD + pPassword);

        /* Check if the result was positive or negative */

        String readString = null;

        boolean success = false;
        try {
            while (readyRead()) {
                readString = readLine();
                if (readString.startsWith("OK")) {
                    success = true;
                } else if (readString.startsWith("ACK")) {
                    success = false;
                    printDebug("Could not successfully authenticate with mpd server");
                }
            }
        } catch (IOException e) {
            handleSocketError();
        }


        return success;
    }

    /**
     * Requests to disconnect from server. This will close the conection and cleanup the socket.
     * After this call it should be safe to reconnect to another server. If this connection is
     * currently in idle state, then it will be deidled before.
     */
    public synchronized void disconnectFromServer() {
        // Stop possible timers waiting for the timeout to go idle
        stopIdleWait();

        // Check if the connection is currently idling, if then deidle.
        if (pMPDConnectionIdle) {
            stopIdleing();
        }

        // Close connection gracefully
        sendMPDRAWCommand(MPDCommands.MPD_COMMAND_CLOSE);

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
                pSocket.setSoTimeout(500);
                pSocket.close();
                pSocket = null;
            }
        } catch (IOException e) {
            printDebug("Error during disconnecting:" + e.toString());
        }

        /* Clear up connection state variables */
        pMPDConnectionIdle = false;
        pMPDConnectionReady = false;

        // Notify listener
        notifyDisconnect();
    }

    /**
     * Access to the currently server capabilities
     *
     * @return
     */
    public MPDCapabilities getServerCapabilities() {
        if (isConnected()) {
            return mServerCapabilities;
        }
        return null;
    }

    /**
     * This functions sends the command to the MPD server.
     * If the server is currently idling then it will deidle it first.
     *
     * @param command
     */
    private void sendMPDCommand(String command) {
        printDebug("Send command: " + command);
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

            // During deidle a disconnect could happen, check again if connection is ready
            if (!pMPDConnectionReady) {
                return;
            }

            /*
             * Send the command to the server
             *
             */
            writeLine(command);

            printDebug("Sent command: " + command);

            // This waits until the server sends a response (OK,ACK(failure) or the requested data)
            try {
                waitForResponse();
            } catch (IOException e) {
                handleSocketError();
            }
            printDebug("Sent command, got response");
        } else {
            printDebug("Connection not ready, command not sent");
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
            writeLine(command);

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
            writeLine(MPDCommands.MPD_START_COMMAND_LIST);


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
            writeLine(MPDCommands.MPD_END_COMMAND_LIST);
            try {
                waitForResponse();
            } catch (IOException e) {
                handleSocketError();
            }
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

        try {
            pSocket.setSoTimeout(SOCKET_TIMEOUT);
        } catch (SocketException e) {
            handleSocketError();
        }

        /* Send the "noidle" command to the server to initiate noidle */
        writeLine(MPDCommands.MPD_COMMAND_STOP_IDLE);

        printDebug("Sent deidle request");

        /* Wait for idle thread to release the lock, which means we are finished waiting */
        try {
            mIdleWaitLock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        printDebug("Deidle lock acquired, server usage allowed again");

        mIdleWaitLock.release();
    }

    /**
     * Initiates the idling procedure. A separate thread is started to wait (blocked)
     * for a deidle from the MPD host. Otherwise it is impossible to get notified on changes
     * from other mpd clients (eg. volume change)
     */
    private synchronized void startIdleing() {
        /* Check if server really is in idling mode */
        if (!pMPDConnectionReady || pMPDConnectionIdle) {
            return;
        }
        printDebug("Start idle mode");

        // Set the timeout to zero to block when no data is available
        try {
            pSocket.setSoTimeout(0);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        mRequestedDeidle = false;

        // This will send the idle command to the server. From there on we need to deidle before
        // sending new requests.
        writeLine(MPDCommands.MPD_COMMAND_START_IDLE);


        // Technically we are in idle mode now, set boolean
        pMPDConnectionIdle = true;

        // Get the lock to prevent the handler thread from (stopIdling) to interfere with deidling sequence.
        try {
            mIdleWaitLock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        pIdleThread = new IdleThread();
        pIdleThread.start();


        for ( MPDConnectionIdleChangeListener listener: pIdleListeners) {
            listener.onIdle();
        }
    }

    /**
     * Function only actively waits for reader to get ready for
     * the response.
     */
    private void waitForResponse() throws IOException {
        printDebug("Waiting for response");
        if (null != pReader) {
            long currentTime = System.nanoTime();

            while (!readyRead()) {
                long compareTime = System.nanoTime() - currentTime;
                // Terminate waiting after waiting to long. This indicates that the server is not responding
                if (compareTime > RESPONSE_TIMEOUT) {
                    printDebug("Stuck waiting for server response");
                    printStackTrace();
                    throw new IOException();
                }
//                if ( compareTime > 500L * 1000L * 1000L ) {
//                    SystemClock.sleep(RESPONSE_WAIT_SLEEP_TIME);
//                }
            }
        } else {
            throw new IOException();
        }
    }

    /**
     * Checks if a simple command was successful or not (OK vs. ACK)
     * <p>
     * This should only be used for simple commands like play,pause, setVolume, ...
     *
     * @return True if command was successfully executed, false otherwise.
     */
    public boolean checkResponse() throws IOException {
        boolean success = false;
        String response;

        printDebug("Check response");

        // Wait for data to be available to read. MPD communication could take some time.
        while (readyRead()) {
            response = readLine();
            if (response.startsWith("OK")) {
                success = true;
            } else if (response.startsWith("ACK")) {
                success = false;
                printDebug("Server response error: " + response);
            }
        }

        printDebug("Response: " + success);
        // The command was handled now it is time to set the connection to idle again (after the timeout,
        // to prevent disconnecting).
        startIdleWait();
        printDebug("Started idle wait");
        // Return if successful or not.
        return success;
    }

    public boolean isConnected() {
        if (null != pSocket && pSocket.isConnected() && pMPDConnectionReady) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * *******************************
     * * Response handling functions *
     * *******************************
     */

    /**
     * Parses the return of MPD when a list of albums was requested.
     *
     * @return List of MPDAlbum objects
     * @throws IOException
     */
    private ArrayList<MPDAlbum> parseMPDAlbums() throws IOException {
        ArrayList<MPDAlbum> albumList = new ArrayList<MPDAlbum>();
        if (!isConnected()) {
            return albumList;
        }
        /* Parse the MPD response and create a list of MPD albums */
        String response = readLine();

        boolean emptyAlbum = false;
        String albumName = "";

        MPDAlbum tempAlbum = null;
        while (isConnected() && response != null && !response.startsWith("OK") && !response.startsWith("ACK")) {
            /* Check if the response is an album */
            if (response.startsWith(MPDResponses.MPD_RESPONSE_ALBUM_NAME)) {
                /* We found an album, add it to the list. */
                if (null != tempAlbum) {
                    albumList.add(tempAlbum);
                }
                albumName = response.substring(MPDResponses.MPD_RESPONSE_ALBUM_NAME.length());
                tempAlbum = new MPDAlbum(albumName);
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_ALBUM_MBID)) {
                // FIXME this crashed with a null-ptr. This should not happen. Investigate if repeated. (Protocol should always send "Album:" first
                tempAlbum.setMBID(response.substring(MPDResponses.MPD_RESPONSE_ALBUM_MBID.length()));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_ALBUM_ARTIST_NAME)) {
                /* Check if the response is a albumartist. */
                tempAlbum.setArtistName(response.substring(MPDResponses.MPD_RESPONSE_ALBUM_ARTIST_NAME.length()));
            }
            response = readLine();
        }

        /* Because of the loop structure the last album has to be added because no
        "ALBUM:" is sent anymore.
         */
        if (null != tempAlbum) {
            albumList.add(tempAlbum);
        }

        printDebug("Parsed: " + albumList.size() + " albums");

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
        if (!isConnected()) {
            return artistList;
        }

        /* Parse MPD artist return values and create a list of MPDArtist objects */
        String response = readLine();

        /* Artist properties */
        String artistName = null;
        String artistMBID = "";

        MPDArtist tempArtist = null;

        while (isConnected() && response != null && !response.startsWith("OK") && !response.startsWith("ACK")) {

            if (response == null) {
                /* skip this invalid (empty) response */
                continue;
            }

            if (response.startsWith(MPDResponses.MPD_RESPONSE_ARTIST_NAME)) {
                if (null != tempArtist) {
                    artistList.add(tempArtist);
                }
                artistName = response.substring(MPDResponses.MPD_RESPONSE_ARTIST_NAME.length());
                tempArtist = new MPDArtist(artistName);
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_ALBUMARTIST_NAME)) {
                if (null != tempArtist) {
                    artistList.add(tempArtist);
                }
                artistName = response.substring(MPDResponses.MPD_RESPONSE_ALBUMARTIST_NAME.length());
                tempArtist = new MPDArtist(artistName);
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_ARTIST_MBID)) {
                artistMBID = response.substring(MPDResponses.MPD_RESPONSE_ARTIST_MBID.length());
                tempArtist.addMBID(artistMBID);
            } else if (response.startsWith("OK")) {
                break;
            }
            response = readLine();
        }

        // Add last artist
        if (null != tempArtist) {
            artistList.add(tempArtist);
        }

        printDebug("Parsed: " + artistList.size() + " artists");

        // Start the idling timeout again.
        startIdleWait();

        // Sort the artists for later sectioning.
        Collections.sort(artistList);

        // If we used MBID filtering, it could happen that a user as an artist in the list multiple times,
        // once with and once without MBID. Try to filter this by sorting the list first by name and mbid count
        // and then remove duplicates.
        if (mServerCapabilities.hasMusicBrainzTags() && mServerCapabilities.hasListGroup()) {
            ArrayList<MPDArtist> clearedList = new ArrayList<>();

            // Remove multiple entries when one artist is in list with and without MBID
            for (int i = 0; i < artistList.size(); i++) {
                MPDArtist artist = artistList.get(i);
                if (i + 1 != artistList.size()) {
                    MPDArtist nextArtist = artistList.get(i + 1);
                    if (!artist.getArtistName().equals(nextArtist.getArtistName())) {
                        clearedList.add(artist);
                    }
                } else {
                    clearedList.add(artist);
                }
            }
            return clearedList;
        } else {
            return artistList;
        }
    }

    /**
     * Parses the response of mpd on requests that return track items. This is also used
     * for MPD file, directory and playlist responses. This allows the GUI to develop
     * one adapter for all three types. Also MPD mixes them when requesting directory listings.
     * <p/>
     * It will return a list of MPDFileEntry objects which is a parent class for (MPDFile, MPDPlaylist,
     * MPDDirectory) you can use instanceof to check which type you got.
     *
     * @param filterArtist    Artist used for filtering against the Artist AND AlbumArtist tag. Non matching tracks
     *                        will be discarded.
     * @param filterAlbumMBID MusicBrainzID of the album that is also used as a filter criteria.
     *                        This can be used to differentiate albums with same name, same artist but different MBID.
     *                        This is often the case for soundtrack releases. (E.g. LOTR DVD-Audio vs. CD release)
     * @return List of MPDFileEntry objects
     * @throws IOException
     */
    private ArrayList<MPDFileEntry> parseMPDTracks(String filterArtist, String filterAlbumMBID) throws IOException {
        ArrayList<MPDFileEntry> trackList = new ArrayList<MPDFileEntry>();
        if (!isConnected()) {
            return trackList;
        }

        /* Temporary track item (added to list later */
        MPDFileEntry tempFileEntry = null;


        /* Response line from MPD */
        String response = readLine();
        while (isConnected() && response != null && !response.startsWith("OK") && !response.startsWith("ACK")) {
            /* This if block will just check all the different response possible by MPDs file/dir/playlist response */
            if (response.startsWith(MPDResponses.MPD_RESPONSE_FILE)) {
                if (null != tempFileEntry) {
                    /* Check the artist filter criteria here */
                    if (tempFileEntry instanceof MPDFile) {
                        MPDFile file = (MPDFile) tempFileEntry;
                        if ((filterArtist.isEmpty() || filterArtist.equals(file.getTrackAlbumArtist()) || filterArtist.equals(file.getTrackArtist()))
                                && (filterAlbumMBID.isEmpty() || filterAlbumMBID.equals(file.getTrackAlbumMBID()))) {
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
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_ALBUM_ARTIST_MBID)) {
                ((MPDFile) tempFileEntry).setTrackAlbumArtistMBID(response.substring(MPDResponses.MPD_RESPONSE_ALBUM_ARTIST_MBID.length()));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_TRACK_MBID)) {
                ((MPDFile) tempFileEntry).setTrackMBID(response.substring(MPDResponses.MPD_RESPONSE_TRACK_MBID.length()));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_TRACK_TIME)) {
                ((MPDFile) tempFileEntry).setLength(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_TRACK_TIME.length())));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_SONG_ID)) {
                ((MPDFile) tempFileEntry).setSongID(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_SONG_ID.length())));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_SONG_POS)) {
                ((MPDFile) tempFileEntry).setSongPosition(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_SONG_POS.length())));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_DISC_NUMBER)) {
                /*
                * Check if MPD returned a discnumber like: "1" or "1/3" and set disc count accordingly.
                */
                String discNumber = response.substring(MPDResponses.MPD_RESPONSE_DISC_NUMBER.length());
                discNumber = discNumber.replaceAll(" ", "");
                String[] discNumberSep = discNumber.split("/");
                if (discNumberSep.length > 0) {
                    try {
                        ((MPDFile) tempFileEntry).setDiscNumber(Integer.valueOf(discNumberSep[0]));
                    } catch (NumberFormatException e) {
                    }

                    if (discNumberSep.length > 1) {
                        try {
                            ((MPDFile) tempFileEntry).psetAlbumDiscCount(Integer.valueOf(discNumberSep[1]));
                        } catch (NumberFormatException e) {
                        }
                    }
                } else {
                    try {
                        ((MPDFile) tempFileEntry).setDiscNumber(Integer.valueOf(discNumber));
                    } catch (NumberFormatException e) {
                    }
                }
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_TRACK_NUMBER)) {
                /*
                 * Check if MPD returned a tracknumber like: "12" or "12/42" and set albumtrack count accordingly.
                 */
                String trackNumber = response.substring(MPDResponses.MPD_RESPONSE_TRACK_NUMBER.length());
                trackNumber = trackNumber.replaceAll(" ", "");
                String[] trackNumbersSep = trackNumber.split("/");
                if (trackNumbersSep.length > 0) {
                    try {
                        ((MPDFile) tempFileEntry).setTrackNumber(Integer.valueOf(trackNumbersSep[0]));
                    } catch (NumberFormatException e) {
                    }
                    if (trackNumbersSep.length > 1) {
                        try {
                            ((MPDFile) tempFileEntry).setAlbumTrackCount(Integer.valueOf(trackNumbersSep[1]));
                        } catch (NumberFormatException e) {
                        }
                    }
                } else {
                    try {
                        ((MPDFile) tempFileEntry).setTrackNumber(Integer.valueOf(trackNumber));
                    } catch (NumberFormatException e) {
                    }
                }
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_LAST_MODIFIED)) {
                tempFileEntry.setLastModified(response.substring(MPDResponses.MPD_RESPONSE_LAST_MODIFIED.length()));
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_PLAYLIST)) {
                if (null != tempFileEntry) {
                    /* Check the artist filter criteria here */
                    if (tempFileEntry instanceof MPDFile) {
                        MPDFile file = (MPDFile) tempFileEntry;
                        if ((filterArtist.isEmpty() || filterArtist.equals(file.getTrackAlbumArtist()) || filterArtist.equals(file.getTrackArtist()))
                                && (filterAlbumMBID.isEmpty() || filterAlbumMBID.equals(file.getTrackAlbumMBID()))) {
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
                        if ((filterArtist.isEmpty() || filterArtist.equals(file.getTrackAlbumArtist()) || filterArtist.equals(file.getTrackArtist()))
                                && (filterAlbumMBID.isEmpty() || filterAlbumMBID.equals(file.getTrackAlbumMBID()))) {
                            trackList.add(tempFileEntry);
                        }
                    } else {
                        trackList.add(tempFileEntry);
                    }
                }
                tempFileEntry = new MPDDirectory(response.substring(MPDResponses.MPD_RESPONSE_DIRECTORY.length()));
            }

            // Move to the next line.
            response = readLine();

        }

        /* Add last remaining track to list. */
        if (null != tempFileEntry) {
                    /* Check the artist filter criteria here */
            if (tempFileEntry instanceof MPDFile) {
                MPDFile file = (MPDFile) tempFileEntry;
                if ((filterArtist.isEmpty() || filterArtist.equals(file.getTrackAlbumArtist()) || filterArtist.equals(file.getTrackArtist()))
                        && (filterAlbumMBID.isEmpty() || filterAlbumMBID.equals(file.getTrackAlbumMBID()))) {
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
    public synchronized List<MPDAlbum> getAlbums() {
        // Get a list of albums. Check if server is new enough for MB and AlbumArtist filtering
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMS(mServerCapabilities.hasListGroup() && mServerCapabilities.hasMusicBrainzTags()));
        try {
            return parseMPDAlbums();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a list of all albums available in the database.
     *
     * @return List of MPDAlbum
     */
    public synchronized List<MPDAlbum> getAlbumsInPath(String path) {
        // Get a list of albums. Check if server is new enough for MB and AlbumArtist filtering
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMS_FOR_PATH(path, mServerCapabilities.hasListGroup() && mServerCapabilities.hasMusicBrainzTags()));
        try {
            return parseMPDAlbums();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a list of all albums by an artist where artist is part of or artist is the AlbumArtist (tag)
     *
     * @param artistName Artist to filter album list with.
     * @return List of MPDAlbum objects
     */
    public synchronized List<MPDAlbum> getArtistAlbums(String artistName) {
        // Get all albums that artistName is part of (Also the legacy album list pre v. 0.19)
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ARTIST_ALBUMS(artistName, mServerCapabilities.hasMusicBrainzTags() && mServerCapabilities.hasListGroup()));

        try {
            if (mServerCapabilities.hasListGroup() && mServerCapabilities.hasMusicBrainzTags()) {
                // Use a hashset for the results, to filter duplicates that will exist.
                Set<MPDAlbum> result = new HashSet<>(parseMPDAlbums());

                // Also get the list where artistName matches on AlbumArtist
                sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMARTIST_ALBUMS(artistName));

                result.addAll(parseMPDAlbums());

                List<MPDAlbum> resultList = new ArrayList<MPDAlbum>(result);

                // Sort the created list
                Collections.sort(resultList);
                return resultList;
            } else {
                List<MPDAlbum> result = parseMPDAlbums();
                return result;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a list of all artists available in MPDs database
     *
     * @return List of MPDArtist objects
     */
    public synchronized List<MPDArtist> getArtists() {
        // Get a list of artists. If server is new enough this will contain MBIDs for artists, that are tagged correctly.
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ARTISTS(mServerCapabilities.hasListGroup() && mServerCapabilities.hasMusicBrainzTags()));
        try {
            return parseMPDArtists();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a list of all album artists available in MPDs database
     *
     * @return List of MPDArtist objects
     */
    public synchronized List<MPDArtist> getAlbumArtists() {
        // Get a list of artists. If server is new enough this will contain MBIDs for artists, that are tagged correctly.
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMARTISTS(mServerCapabilities.hasListGroup() && mServerCapabilities.hasMusicBrainzTags()));
        try {
            return parseMPDArtists();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a list of all playlists available in MPDs database
     *
     * @return List of MPDArtist objects
     */
    public synchronized List<MPDFileEntry> getPlaylists() {
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_SAVED_PLAYLISTS);
        try {
            List<MPDFileEntry> playlists = parseMPDTracks("", "");
            Collections.sort(playlists);
            return playlists;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets all tracks from MPD server. This could take a long time to process. Be warned.
     *
     * @return A list of all tracks in MPDFile objects
     */
    public synchronized List<MPDFileEntry> getAllTracks() {
        Log.w(TAG, "This command should not be used");
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALL_FILES);
        try {
            return parseMPDTracks("", "");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Returns the list of tracks that are part of albumName
     *
     * @param albumName Album to get tracks from
     * @return List of MPDFile track objects
     */
    public synchronized List<MPDFileEntry> getAlbumTracks(String albumName, String mbid) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUM_TRACKS(albumName));
        try {
            List<MPDFileEntry> result = parseMPDTracks("", mbid);
            MPDSortHelper.sortFileListNumeric(result);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns the list of tracks that are part of albumName and from artistName
     *
     * @param albumName  Album name used as primary filter.
     * @param artistName Artist to filter with. This is checked with Artist AND AlbumArtist tag.
     * @param mbid       MusicBrainzID of the album to get tracks from. Necessary if one item with the
     *                   same name exists multiple times.
     * @return List of MPDFile track objects
     */
    public synchronized List<MPDFileEntry> getArtistAlbumTracks(String albumName, String artistName, String mbid) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUM_TRACKS(albumName));
        try {
        /* Filter tracks with artistName */
            List<MPDFileEntry> result = parseMPDTracks(artistName, mbid);
            // Sort with disc & track number
            MPDSortHelper.sortFileListNumeric(result);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Requests the current playlist of the server
     *
     * @return List of MPDFile items with all tracks of the current playlist
     */
    public synchronized List<MPDFileEntry> getCurrentPlaylist() {
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_PLAYLIST);
        try {
        /* Parse the return */
            return parseMPDTracks("", "");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Requests the current playlist of the server with a window
     *
     * @return List of MPDFile items with all tracks of the current playlist
     */
    public synchronized List<MPDFileEntry> getCurrentPlaylistWindow(int start, int end) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_PLAYLIST_WINDOW(start, end));
        try {
        /* Parse the return */
            return parseMPDTracks("", "");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Requests the current playlist of the server
     *
     * @return List of MPDFile items with all tracks of the current playlist
     */
    public synchronized List<MPDFileEntry> getSavedPlaylist(String playlistName) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_SAVED_PLAYLIST(playlistName));
        try {
        /* Parse the return */
            return parseMPDTracks("", "");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Requests the files for a specific path with info
     *
     * @return List of MPDFile items with all tracks of the current playlist
     */
    public synchronized List<MPDFileEntry> getFiles(String path) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_FILES_INFO(path));
        try {
        /* Parse the return */
            List<MPDFileEntry> retList = parseMPDTracks("", "");
            Collections.sort(retList);
            return retList;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Requests the files for a specific search term and type
     *
     * @param term The search term to use
     * @param type The type of items to search
     * @return List of MPDFile items with all tracks matching the search
     */
    public synchronized List<MPDFileEntry> getSearchedFiles(String term, MPDCommands.MPD_SEARCH_TYPE type) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SEARCH_FILES(term, type));
        try {
        /* Parse the return */
            return parseMPDTracks("", "");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Searches a URL in the current playlist. If available the track is part of the returned list.
     * @param url URL to search in the current playlist.
     * @return List with one entry or none.
     */
    public synchronized List<MPDFileEntry> getPlaylistFindTrack(String url) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_PLAYLIST_FIND_URI(url));
        try {
            /* Parse the return */
            return parseMPDTracks("", "");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Requests the currentstatus package from the mpd server.
     *
     * @return The CurrentStatus object with all gathered information.
     */
    public synchronized MPDCurrentStatus getCurrentServerStatus() {
        MPDCurrentStatus status = new MPDCurrentStatus();

        /* Request status */
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_STATUS);

        try {
            if (!readyRead()) {
                return status;
            }
        } catch (IOException e) {
            handleSocketError();
            return status;
        }
        /* Response line from MPD */
        String response = null;
        response = readLine();

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
                status.setTrackLength(Math.round(Float.valueOf(response.substring(MPDResponses.MPD_RESPONSE_DURATION.length()))));
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
                        status.setBitDepth(audioInfoSep[1]);
                /* Third is channel count */
                        status.setChannelCount(Integer.valueOf(audioInfoSep[2]));
                    } catch (NumberFormatException e) {
                    }
                }
            } else if (response.startsWith(MPDResponses.MPD_RESPONSE_UPDATING_DB)) {
                status.setUpdateDBJob(Integer.valueOf(response.substring(MPDResponses.MPD_RESPONSE_UPDATING_DB.length())));
            }

            response = readLine();

        }

        startIdleWait();
        return status;
    }

    /**
     * Requests the server statistics package from the mpd server.
     *
     * @return The CurrentStatus object with all gathered information.
     */
    public synchronized MPDStatistics getServerStatistics() {
        MPDStatistics stats = new MPDStatistics();

        /* Request status */
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_STATISTICS);

        try {
            if (!readyRead()) {
                return stats;
            }
        } catch (IOException e) {
            handleSocketError();
            return stats;
        }
        /* Response line from MPD */
        String response = null;

        response = readLine();

        while (isConnected() && response != null && !response.startsWith("OK") && !response.startsWith("ACK")) {
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

            response = readLine();

        }

        startIdleWait();
        return stats;
    }

    /**
     * This will query the current song playing on the mpd server.
     *
     * @return MPDFile entry for the song playing.
     */
    public synchronized MPDFile getCurrentSong() {
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_SONG);

        // Reuse the parsing function for tracks here.
        List<MPDFileEntry> retList = null;
        try {
            retList = parseMPDTracks("", "");
        } catch (IOException e) {
            handleSocketError();
            return null;
        }
        if (retList.size() == 1) {
            // If one element is in the list it is safe to assume that this element is
            // the current song. So casting is no problem.
            return (MPDFile) retList.get(0);
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
    public synchronized boolean pause(boolean pause) {
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
     *
     * @return true if successful, false otherwise
     */
    public synchronized boolean nextSong() {
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
     *
     * @return true if successful, false otherwise
     */
    public synchronized boolean previousSong() {
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
     *
     * @return true if successful, false otherwise
     */
    public synchronized boolean stopPlayback() {
        sendMPDCommand(MPDCommands.MPD_COMMAND_STOP);

    /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Sets random to true or false
     *
     * @param random If random should be set (true) or not (false)
     * @return True if server responed with ok
     */
    public synchronized boolean setRandom(boolean random) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SET_RANDOM(random));

    /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Sets repeat to true or false
     *
     * @param repeat If repeat should be set (true) or not (false)
     * @return True if server responed with ok
     */
    public synchronized boolean setRepeat(boolean repeat) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SET_REPEAT(repeat));

    /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Sets single playback to enable (true) or disabled (false)
     *
     * @param single if single playback should be enabled or not.
     * @return True if server responed with ok
     */
    public synchronized boolean setSingle(boolean single) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SET_SINGLE(single));

    /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Sets if files should be removed after playback (consumed)
     *
     * @param consume True if yes and false if not.
     * @return True if server responed with ok
     */
    public synchronized boolean setConsume(boolean consume) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SET_CONSUME(consume));

    /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Plays the song with the index in the current playlist.
     *
     * @param index Index of the song that should be played.
     * @return True if server responed with ok
     */
    public synchronized boolean playSongIndex(int index) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_PLAY_SONG_INDEX(index));

    /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Seeks the currently playing song to a certain position
     *
     * @param seconds Position in seconds to which a seek is requested to.
     * @return True if server responed with ok
     */
    public synchronized boolean seekSeconds(int seconds) {
        MPDCurrentStatus status = null;
        status = getCurrentServerStatus();


        sendMPDCommand(MPDCommands.MPD_COMMAND_SEEK_SECONDS(status.getCurrentSongIndex(), seconds));

    /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Sets the volume of the mpd servers output. It is an absolute value between (0-100).
     *
     * @param volume Volume to set to the server.
     * @return True if server responed with ok
     */
    public synchronized boolean setVolume(int volume) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SET_VOLUME(volume));

    /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
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
    public synchronized boolean addTrackList(List<MPDFileEntry> tracks) {
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

    /**
     * Adds all tracks from a certain album from artistname to the current playlist.
     *
     * @param albumname  Name of the album to add to the current playlist.
     * @param artistname Name of the artist of the album to add to the list. This
     *                   allows filtering of album tracks to a specified artist. Can also
     *                   be left empty then all tracks from the album will be added.
     * @return True if server responed with ok
     */
    public synchronized boolean addAlbumTracks(String albumname, String artistname, String mbid) {
        List<MPDFileEntry> tracks = getArtistAlbumTracks(albumname, artistname, mbid);
        return addTrackList(tracks);
    }

    /**
     * Adds all albums of an artist to the current playlist. Will first get a list of albums for the
     * artist and then call addAlbumTracks for every album on this result.
     *
     * @param artistname Name of the artist to enqueue the albums from.
     * @return True if server responed with ok
     */
    public synchronized boolean addArtist(String artistname) {
        List<MPDAlbum> albums = getArtistAlbums(artistname);
        if (null == albums) {
            return false;
        }

        boolean success = true;
        for (MPDAlbum album : albums) {
            // This will add all tracks from album where artistname is either the artist or
            // the album artist.
            if (!(addAlbumTracks(album.getName(), artistname, ""))) {
                success = false;
            }
        }
        return success;
    }

    /**
     * Adds a single File/Directory to the current playlist.
     *
     * @param url URL of the file or directory! to add to the current playlist.
     * @return True if server responed with ok
     */
    public synchronized boolean addSong(String url) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_ADD_FILE(url));

    /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * This method adds a song to a specified positiion in the current playlist.
     * This allows GUI developers to implement a method like "add after current".
     *
     * @param url   URL to add to the playlist.
     * @param index Index at which the item should be added.
     * @return True if server responed with ok
     */
    public synchronized boolean addSongatIndex(String url, int index) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_ADD_FILE_AT_INDEX(url, index));

    /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Adds files to the playlist with a search term for a specific type
     *
     * @param term The search term to use
     * @param type The type of items to search
     * @return True if server responed with ok
     */
    public synchronized boolean addSearchedFiles(String term, MPDCommands.MPD_SEARCH_TYPE type) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_ADD_SEARCH_FILES(term, type));
        try {
        /* Parse the return */
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Instructs the mpd server to clear its current playlist.
     *
     * @return True if server responed with ok
     */
    public synchronized boolean clearPlaylist() {
        sendMPDCommand(MPDCommands.MPD_COMMAND_CLEAR_PLAYLIST);
    /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Instructs the mpd server to remove one item from the current playlist at index.
     *
     * @param index Position of the item to remove from current playlist.
     * @return True if server responed with ok
     */
    public synchronized boolean removeIndex(int index) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_SONG_FROM_CURRENT_PLAYLIST(index));
    /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Moves one item from an index in the current playlist to an new index. This allows to move
     * tracks for example after the current to priotize songs.
     *
     * @param from Item to move from.
     * @param to   Position to enter item
     * @return
     */
    public synchronized boolean moveSongFromTo(int from, int to) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_MOVE_SONG_FROM_INDEX_TO_INDEX(from, to));
    /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Saves the current playlist as a new playlist with a name.
     *
     * @param name Name of the playlist to save to.
     * @return True if server responed with ok
     */
    public synchronized boolean savePlaylist(String name) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SAVE_PLAYLIST(name));

    /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Adds a song to the saved playlist
     *
     * @param playlistName Name of the playlist to add the url to.
     * @param url          URL to add to the saved playlist
     * @return True if server responed with ok
     */
    public synchronized boolean addSongToPlaylist(String playlistName, String url) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_ADD_TRACK_TO_PLAYLIST(playlistName, url));

    /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Removes a song from a saved playlist
     *
     * @param playlistName Name of the playlist of which the song should be removed from
     * @param position     Index of the song to remove from the lits
     * @return
     */
    public synchronized boolean removeSongFromPlaylist(String playlistName, int position) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_TRACK_FROM_PLAYLIST(playlistName, position));

        /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Removes a saved playlist from the servers database.
     *
     * @param name Name of the playlist to remove.
     * @return True if server responed with ok
     */
    public synchronized boolean removePlaylist(String name) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_PLAYLIST(name));

    /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Loads a saved playlist (added after the last song) to the current playlist.
     *
     * @param name Of the playlist to add to.
     * @return True if server responed with ok
     */
    public synchronized boolean loadPlaylist(String name) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_LOAD_PLAYLIST(name));

    /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
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

        if (!isConnected()) {
            return null;
        }

        /* Response line from MPD */
        String response = readLine();
        while (isConnected() && response != null && !response.startsWith("OK") && !response.startsWith("ACK")) {
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
            response = readLine();
        }

        // Add remaining output to list
        if (null != outputName) {
            MPDOutput tempOutput = new MPDOutput(outputName, outputActive, outputId);
            outputList.add(tempOutput);
        }

        return outputList;

    }

    /**
     * Private parsing method for MPDs command list
     *
     * @return A list of Strings of commands that are allowed on the server
     * @throws IOException
     */
    private List<String> parseMPDCommands() throws IOException {
        ArrayList<String> commandList = new ArrayList<>();
        // Parse outputs
        String commandName = null;
        if (!isConnected()) {
            return commandList;
        }

        /* Response line from MPD */
        String response = readLine();
        while (isConnected() && response != null && !response.startsWith("OK") && !response.startsWith("ACK")) {
            if (response.startsWith(MPDResponses.MPD_COMMAND)) {
                commandName = response.substring(MPDResponses.MPD_COMMAND.length());
                commandList.add(commandName);
            }
            response = readLine();
        }
        printDebug("Command list length: " + commandList.size());
        return commandList;

    }

    /**
     * Parses the response of MPDs supported tag types
     *
     * @return List of tags supported by the connected MPD host
     * @throws IOException
     */
    private List<String> parseMPDTagTypes() throws IOException {
        ArrayList<String> tagList = new ArrayList<>();
        // Parse outputs
        String tagName = null;
        if (!isConnected()) {
            return tagList;
        }

        /* Response line from MPD */
        String response = readLine();
        while (isConnected() && response != null && !response.startsWith("OK") && !response.startsWith("ACK")) {
            if (response.startsWith(MPDResponses.MPD_TAGTYPE)) {
                tagName = response.substring(MPDResponses.MPD_TAGTYPE.length());
                tagList.add(tagName);
            }
            response = readLine();
        }

        return tagList;

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
                handleSocketError();
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
    public synchronized boolean toggleOutput(int id) {
        sendMPDCommand(MPDCommands.MPD_COMMAND_TOGGLE_OUTPUT(id));

        /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            handleSocketError();
        }
        return false;
    }

    /**
     * Instructs to update the database of the mpd server (path: / )
     *
     * @return True if server responed with ok
     */
    public synchronized boolean updateDatabase() {
        sendMPDCommand(MPDCommands.MPD_COMMAND_UPDATE_DATABASE);

        /* Return the response value of MPD */
        try {
            return checkResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;

    }


    /**
     * Checks if the socket is ready for read operations
     *
     * @return True if ready
     * @throws IOException
     */
    private boolean readyRead() throws IOException {
        return (null != pSocket) && (null != pReader) && pSocket.isConnected() && pReader.ready();
    }

    /**
     * Will notify a connected listener that the connection is now ready to be used.
     */
    private void notifyConnected() {
        for ( MPDConnectionStateChangeListener listener: pStateListeners ) {
            listener.onConnected();
        }
    }

    /**
     * Will notify a connected listener that the connection is disconnect and not ready for use.
     */
    private void notifyDisconnect() {
        for ( MPDConnectionStateChangeListener listener: pStateListeners ) {
            listener.onDisconnected();
        }
    }

    /**
     * Registers a listener to be notified about connection state changes
     *
     * @param listener Listener to be connected
     */
    public void setStateListener(MPDConnectionStateChangeListener listener) {
        pStateListeners.add(listener);
    }

    /**
     * Registers a listener to be notified about changes in idle state of this connection.
     *
     * @param listener
     */
    public void setpIdleListener(MPDConnectionIdleChangeListener listener) {
        pIdleListeners.add(listener);
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

            printDebug("Waiting for input from server");
            // Set thread to sleep, because there should be no line available to read.
            String response = readLine();
            return response;
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
            boolean externalDeIdle = false;

            // This will block this thread until the server has some data available to read again.
            String response = waitForIdleResponse();

            printDebug("Idle over with response: " + response);

            // This happens when disconnected
            if (null == response || response.isEmpty()) {
                printDebug("Probably disconnected during idling");
                // First handle the disconnect, then allow further action
                handleSocketError();

                // Release the idle mode
                mIdleWaitLock.release();
                return;
            }
            /**
             * At this position idling is over. Check if we were the reason to deidle or if the
             * server initiated the deidling. If it was done by the server we will trigger
             * the idle again.
             */
            if (response.startsWith("changed")) {
                printDebug("Externally deidled!");
                externalDeIdle = true;
                try {
                    while (readyRead()) {
                        response = readLine();
                        if (response.startsWith("OK")) {
                            printDebug("Deidled with status ok");
                        } else if (response.startsWith("ACK")) {
                            printDebug("Server response error: " + response);
                        }
                    }
                } catch (IOException e) {
                    handleSocketError();
                }
            } else {
                printDebug("Deidled on purpose");
            }
            // Set the connection as non-idle again.
            pMPDConnectionIdle = false;

            // Reset the timeout again
            try {
                if (pSocket != null) {
                    pSocket.setSoTimeout(SOCKET_TIMEOUT);
                }
            } catch (SocketException e) {
                handleSocketError();
            }

            // Release the lock for possible threads waiting from outside this idling thread (handler thread).
            mIdleWaitLock.release();

            // Notify a possible listener for deidling.
            for ( MPDConnectionIdleChangeListener listener: pIdleListeners) {
                listener.onNonIdle();
            }
            printDebug("Idling over");

            // Start the idle clock again, but only if we were deidled from the server. Otherwise we let the
            // active command deidle when finished.
            if (externalDeIdle) {
                startIdleWait();
            }
        }
    }


    /**
     * This will start the timeout to set the connection tto the idle state after use.
     */
    private synchronized void startIdleWait() {
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
        mIdleWait.schedule(new IdleWaitTimeoutTask(), IDLE_WAIT_TIME);
        printDebug("IdleWait scheduled");
        printStackTrace();
    }

    /**
     * This will stop a potential running timeout task.
     */
    private synchronized void stopIdleWait() {
        if (null != mIdleWait) {
            mIdleWait.cancel();
            mIdleWait.purge();
            mIdleWait = null;
        }
        printDebug("IdleWait terminated");
    }

    /**
     * Task that will trigger the idle state of this MPDConnection.
     */
    private class IdleWaitTimeoutTask extends TimerTask {

        @Override
        public void run() {
            startIdleing();
        }
    }

    public void setID(String id) {
        mID = id;
    }

    /**
     * Central method to read a line from the sockets reader
     * @return The read string. null if no data is available.
     */
    private String readLine() {
        if (pReader != null) {
            try {
                String line = pReader.readLine();
                //printDebug("Read line: " + line);
                return line;
            } catch (IOException e) {
                handleSocketError();
            }
        }
        return null;
    }

    /**
     * Central method to write a line to the sockets writer. Socket will be flushed afterwards
     * to ensure that the string is sent.
     * @param line String to write to the socket.
     */
    private void writeLine(String line) {
        if (pWriter != null) {
            pWriter.println(line);
            pWriter.flush();
            printDebug("Write line: " + line);
        }
    }

    private void printDebug(String debug) {
        if (!DEBUG_ENABLED) {
            return;
        }

        Log.v(TAG, mID + ':' + Thread.currentThread().getId() + ':' + "Idle:" + pMPDConnectionIdle + ':' + debug);
    }

    private void printStackTrace() {
        StackTraceElement[] st = new Exception().getStackTrace();
        for (StackTraceElement el : st) {
            printDebug(el.toString());
        }
    }
}
