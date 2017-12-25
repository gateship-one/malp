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
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDOutput;
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
import java.util.ListIterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

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
    private static final String TAG = MPDConnection.class.getSimpleName();

    /**
     * Set this flag to enable debugging in this class. DISABLE before releasing
     */
    private static final boolean DEBUG_ENABLED = false;

    /**
     * Timeout to wait for socket operations (time in ms)
     */
    private static final int SOCKET_TIMEOUT = 5 * 1000;

    /**
     * Timeout to wait until deidle should be finished (time in ms)
     */
    private static final int DEIDLE_TIMEOUT = 5 * 1000;

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
    private String mHostname;
    private String mPassword;
    private int mPort;

    private Socket mSocket;

    /* BufferedReader for all reading from the socket */
    private BufferedReader mReader;

    /* PrintWriter for all writing to the socket */
    private PrintWriter mWriter;

    /* True only if server is ready to receive commands */
    private boolean mMPDConnectionReady = false;

    /* True if server connection is in idleing state. Needs to be deidled before sending command */
    private boolean mMPDConnectionIdle = false;

    /* MPD server properties */
    private MPDCapabilities mServerCapabilities;

    /**
     * Only get the server capabilities if server parameters changed
     */
    private boolean mCapabilitiesChanged;

    /**
     * One listener for the state of the connection (connected, disconnected)
     */
    private ArrayList<MPDConnectionStateChangeListener> mStateListeners = null;

    /**
     * One listener for the idle state of the connection. Can be used to react
     * to changes to the server from other clients. When the server is deidled (from outside)
     * it will notify this listener.
     */
    private ArrayList<MPDConnectionIdleChangeListener> mIdleListeners = null;

    /**
     * Thread that will spawn when the server is not requested at the moment. Will start an
     * blocking read operation on the socket reader.
     */
    private Thread mIdleThread = null;

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
    private Semaphore mIdleWaitLock;

    /**
     * Singleton instance
     */
    private static MPDConnection mInstance;

    /**
     * Set if deidling was aborted. Used by IdleWaitThread
     */
    private boolean mDeIdlingTimedOut;

    /**
     * Semaphore to ensure correct deidle timeout execution order (TimeoutTimerTask -> IdleWaitThread)
     */
    private Semaphore mDeidleTimeoutLock;

    /**
     * Time to cancel deidling after a short period to prevent deadlocking
     */
    private Timer mDeidleTimeoutTimer;

    private ReentrantLock mDeIdleTimerLock;

    public static synchronized MPDConnection getInstance() {
        if (null == mInstance) {
            mInstance = new MPDConnection();
        }
        return mInstance;
    }

    /**
     * Creates disconnected MPDConnection with following parameters
     */
    private MPDConnection() {
        mSocket = null;
        mReader = null;
        mIdleWaitLock = new Semaphore(1);
        mDeidleTimeoutLock = new Semaphore(1);
        mServerCapabilities = new MPDCapabilities("", null, null);
        mIdleListeners = new ArrayList<>();
        mStateListeners = new ArrayList<>();

        mDeIdleTimerLock = new ReentrantLock();
    }

    /**
     * Private function to handle read error. Try to disconnect and remove old sockets.
     * Clear up connection state variables.
     */
    private synchronized void handleSocketError() {
        if (DEBUG_ENABLED) {
            Log.v(TAG, "Read error exception. Disconnecting and cleaning up");
        }
        new Exception().printStackTrace();
        try {
            /* Clear reader/writer up */
            if (null != mReader) {
                mReader = null;
            }
            if (null != mWriter) {
                mWriter = null;
            }

            /* Clear TCP-Socket up */
            if (null != mSocket && mSocket.isConnected()) {
                mSocket.setSoTimeout(500);
                mSocket.close();
            }
            mSocket = null;
        } catch (IOException e) {
            if (DEBUG_ENABLED) {
                Log.v(TAG, "Error during read error handling");
            }
        }

        /* Clear up connection state variables */
        mMPDConnectionIdle = false;
        mMPDConnectionReady = false;


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
        mHostname = hostname;
        if (!password.equals("")) {
            mPassword = password;
        }
        mPort = port;
        mCapabilitiesChanged = true;
    }

    /**
     * This is the actual start of the connection. It tries to resolve the hostname
     * and initiates the connection to the address and the configured tcp-port.
     */
    public synchronized void connectToServer() throws MPDException {
        /* If a socket is already open, close it and destroy it. */
        if ((null != mSocket) && (mSocket.isConnected())) {
            disconnectFromServer();
        }

        if ((null == mHostname) || mHostname.equals("")) {
            return;
        }
        mMPDConnectionIdle = false;
        mMPDConnectionReady = false;
        /* Create a new socket used for the TCP-connection. */
        mSocket = new Socket();
        try {
            mSocket.connect(new InetSocketAddress(mHostname, mPort), SOCKET_TIMEOUT);
        } catch (IOException e) {
            handleSocketError();
            return;
        }

        /* Check if the socket is connected */
        if (mSocket.isConnected()) {
            /* Try reading from the stream */

            /* Create the reader used for reading from the socket. */
            if (mReader == null) {
                try {
                    mReader = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
                } catch (IOException e) {
                    handleSocketError();
                    return;
                }
            }

            /* Create the writer used for writing to the socket */
            if (mWriter == null) {
                try {
                    mWriter = new PrintWriter(new OutputStreamWriter(mSocket.getOutputStream()));
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

            while (readyRead()) {
                readString = readLine();
                    /* Look out for the greeting message */
                if (readString.startsWith("OK MPD ")) {
                    versionString = readString.substring(7);

                    String[] versions = versionString.split("\\.");
                    if (versions.length == 3) {
                        // Check if server version changed and if, reread server capabilities later.
                        if (Integer.valueOf(versions[0]) != mServerCapabilities.getMajorVersion() ||
                                (Integer.valueOf(versions[0]) == mServerCapabilities.getMajorVersion() && Integer.valueOf(versions[1]) != mServerCapabilities.getMinorVersion())) {
                            mCapabilitiesChanged = true;
                        }
                    }
                }
            }

            mMPDConnectionReady = true;

            if (mPassword != null && !mPassword.equals("")) {
                /* Authenticate with server because password is set. */
                boolean authenticated = authenticateMPDServer();
            }


            if (mCapabilitiesChanged) {
                // Get available commands
                sendMPDCommand(MPDCommands.MPD_COMMAND_GET_COMMANDS);

                List<String> commands = null;
                try {
                    commands = MPDResponseParser.parseMPDCommands(this);
                } catch (IOException | MPDException e) {
                    handleSocketError();
                    return;
                }
                // Get list of supported tags
                sendMPDCommand(MPDCommands.MPD_COMMAND_GET_TAGS);
                List<String> tags = null;
                try {
                    tags = MPDResponseParser.parseMPDTagTypes(this);
                } catch (IOException | MPDException e) {
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
                mSocket.setSoTimeout(SOCKET_TIMEOUT);
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
        if (!mMPDConnectionReady || mMPDConnectionIdle) {
            return false;
        }

        sendMPDCommand(MPDCommands.MPD_COMMAND_PASSWORD(mPassword));

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
                    if (DEBUG_ENABLED) {
                        Log.v(TAG, "Could not successfully authenticate with mpd server");
                    }
                }
            }
        } catch (MPDException e) {
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
        if (mMPDConnectionIdle) {
            stopIdleing();
        }

        // Close connection gracefully
        sendMPDRAWCommand(MPDCommands.MPD_COMMAND_CLOSE);

        /* Cleanup reader/writer */
        try {
            /* Clear reader/writer up */
            if (null != mReader) {
                mReader = null;
            }
            if (null != mWriter) {
                mWriter = null;
            }

            /* Clear TCP-Socket up */
            if (null != mSocket && mSocket.isConnected()) {
                mSocket.setSoTimeout(500);
                mSocket.close();
                mSocket = null;
            }
        } catch (IOException e) {
            if (DEBUG_ENABLED) {
                Log.v(TAG, "Error during disconnecting:" + e.toString());
            }
        }

        /* Clear up connection state variables */
        mMPDConnectionIdle = false;
        mMPDConnectionReady = false;

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
        if (DEBUG_ENABLED) {
            Log.v(TAG, "Send command: " + command);
        }
        // Stop possible idling timeout tasks.
        stopIdleWait();


        /* Check if the server is connected. */
        if (mMPDConnectionReady) {

            /*
             * Check if server is in idling mode, this needs unidling first,
             * otherwise the server will disconnect the client.
             */
            if (mMPDConnectionIdle) {
                stopIdleing();
            }

            // During deidle a disconnect could happen, check again if connection is ready
            if (!mMPDConnectionReady) {
                return;
            }

            /*
             * Send the command to the server
             *
             */
            writeLine(command);

            if (DEBUG_ENABLED) {
                Log.v(TAG, "Sent command: " + command);
            }

            // This waits until the server sends a response (OK,ACK(failure) or the requested data)
            try {
                waitForResponse();
            } catch (IOException e) {
                handleSocketError();
            }
            if (DEBUG_ENABLED) {
                Log.v(TAG, "Sent command, got response");
            }
        } else {
            if (DEBUG_ENABLED) {
                Log.v(TAG, "Connection not ready, command not sent");
            }
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
        if (mMPDConnectionReady) {
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
        if (mMPDConnectionReady) {
            /* Check if server is in idling mode, this needs unidling first,
            otherwise the server will disconnect the client.
             */
            if (mMPDConnectionIdle) {
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
        if (mMPDConnectionReady) {
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
        if (!mMPDConnectionIdle || !mMPDConnectionReady) {
            return;
        }

        try {
            mSocket.setSoTimeout(SOCKET_TIMEOUT);
        } catch (SocketException e) {
            handleSocketError();
        }

        /* Send the "noidle" command to the server to initiate noidle */
        writeLine(MPDCommands.MPD_COMMAND_STOP_IDLE);

        if (DEBUG_ENABLED) {
            Log.v(TAG, "Sent deidle request");
        }

        mDeIdleTimerLock.lock();
        // Set timeout task for deidling.
        if (mDeidleTimeoutTimer != null) {
            mDeidleTimeoutTimer.cancel();
            mDeidleTimeoutTimer.purge();
        }
        mDeidleTimeoutTimer = new Timer();
        mDeidleTimeoutTimer.schedule(new DeIdleTimeoutTask(), DEIDLE_TIMEOUT);
        mDeIdleTimerLock.unlock();

        /* Wait for idle thread to release the lock, which means we are finished waiting */
        try {
            mIdleWaitLock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (DEBUG_ENABLED) {
            Log.v(TAG, "Deidle lock acquired, server usage allowed again");
        }

        mIdleWaitLock.release();
    }

    /**
     * Initiates the idling procedure. A separate thread is started to wait (blocked)
     * for a deidle from the MPD host. Otherwise it is impossible to get notified on changes
     * from other mpd clients (eg. volume change)
     */
    private synchronized void startIdleing() {
        /* Check if server really is in idling mode */
        if (!mMPDConnectionReady || mMPDConnectionIdle) {
            return;
        }
        if (DEBUG_ENABLED) {
            Log.v(TAG, "Start idle mode");
        }

        // Set the timeout to zero to block when no data is available
        try {
            mSocket.setSoTimeout(0);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        mDeIdlingTimedOut = false;

        // This will send the idle command to the server. From there on we need to deidle before
        // sending new requests.
        writeLine(MPDCommands.MPD_COMMAND_START_IDLE);

        // Technically we are in idle mode now, set boolean
        mMPDConnectionIdle = true;

        // Get the lock to prevent the handler thread from (stopIdling) to interfere with deidling sequence.
        try {
            mIdleWaitLock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mIdleThread = new IdleThread();
        mIdleThread.start();

        // Notify idle listeners
        for (MPDConnectionIdleChangeListener listener : mIdleListeners) {
            listener.onIdle();
        }
    }

    /**
     * Function only actively waits for reader to get ready for
     * the response.
     */
    private void waitForResponse() throws IOException {
        if (DEBUG_ENABLED) {
            Log.v(TAG, "Waiting for response");
        }
        if (null != mReader) {
            long currentTime = System.nanoTime();

            while (!readyRead()) {
                long compareTime = System.nanoTime() - currentTime;
                // Terminate waiting after waiting to long. This indicates that the server is not responding
                if (compareTime > RESPONSE_TIMEOUT) {
                    if (DEBUG_ENABLED) {
                        Log.v(TAG, "Stuck waiting for server response");
                    }
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
    public boolean checkResponse() throws MPDException {
        boolean success = false;
        String response;

        if (DEBUG_ENABLED) {
            Log.v(TAG, "Check response");
        }

        // Wait for data to be available to read. MPD communication could take some time.
        while (readyRead()) {
            response = readLine();
            if (response.startsWith("OK")) {
                success = true;
            } else if (response.startsWith("ACK")) {
                success = false;
                if (DEBUG_ENABLED) {
                    Log.v(TAG, "Server response error: " + response);
                }
            }
        }

        if (DEBUG_ENABLED) {
            Log.v(TAG, "Response: " + success);
        }
        // The command was handled now it is time to set the connection to idle again (after the timeout,
        // to prevent disconnecting).
        startIdleWait();
        if (DEBUG_ENABLED) {
            Log.v(TAG, "Started idle wait");
        }
        // Return if successful or not.
        return success;
    }

    public boolean isConnected() {
        if (null != mSocket && mSocket.isConnected() && mMPDConnectionReady) {
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
    public synchronized List<MPDAlbum> getAlbums() throws MPDException {
        // Get a list of albums. Check if server is new enough for MB and AlbumArtist filtering
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMS(mServerCapabilities));
        try {
            // Remove empty albums at beginning of the list
            List<MPDAlbum> albums = MPDResponseParser.parseMPDAlbums(this);
            startIdleWait();
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
        } catch (IOException e) {
            handleSocketError();
            return null;
        }
    }

    /**
     * Get a list of all albums available in the database.
     *
     * @return List of MPDAlbum
     */
    public synchronized List<MPDAlbum> getAlbumsInPath(String path) throws MPDException {
        // Get a list of albums. Check if server is new enough for MB and AlbumArtist filtering
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMS_FOR_PATH(path, mServerCapabilities));
        try {
            // Remove empty albums at beginning of the list
            List<MPDAlbum> albums = MPDResponseParser.parseMPDAlbums(this);
            startIdleWait();
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
        } catch (IOException e) {
            handleSocketError();
            return null;
        }
    }

    /**
     * Get a list of all albums by an artist where artist is part of or artist is the AlbumArtist (tag)
     *
     * @param artistName Artist to filter album list with.
     * @return List of MPDAlbum objects
     */
    public synchronized List<MPDAlbum> getArtistAlbums(String artistName) throws MPDException {
        // Get all albums that artistName is part of (Also the legacy album list pre v. 0.19)
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ARTIST_ALBUMS(artistName, mServerCapabilities));

        try {
            if (mServerCapabilities.hasTagAlbumArtist() && mServerCapabilities.hasListGroup()) {
                // Use a hashset for the results, to filter duplicates that will exist.
                Set<MPDAlbum> result = new HashSet<>(MPDResponseParser.parseMPDAlbums(this));

                // Also get the list where artistName matches on AlbumArtist
                sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMARTIST_ALBUMS(artistName, mServerCapabilities));

                result.addAll(MPDResponseParser.parseMPDAlbums(this));
                startIdleWait();

                List<MPDAlbum> resultList = new ArrayList<MPDAlbum>(result);

                // Sort the created list
                Collections.sort(resultList);
                return resultList;
            } else {
                List<MPDAlbum> result = MPDResponseParser.parseMPDAlbums(this);
                startIdleWait();
                return result;
            }

        } catch (IOException e) {
            handleSocketError();
            return null;
        }
    }

    /**
     * Get a list of all artists available in MPDs database
     *
     * @return List of MPDArtist objects
     */
    public synchronized List<MPDArtist> getArtists() throws MPDException {
        // Get a list of artists. If server is new enough this will contain MBIDs for artists, that are tagged correctly.
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ARTISTS(mServerCapabilities.hasListGroup() && mServerCapabilities.hasMusicBrainzTags()));
        try {
            // Remove first empty artist
            List<MPDArtist> artists = MPDResponseParser.parseMPDArtists(this, mServerCapabilities.hasMusicBrainzTags(), mServerCapabilities.hasListGroup());
            startIdleWait();
            if (artists.size() > 0 && artists.get(0).getArtistName().isEmpty()) {
                artists.remove(0);
            }

            return artists;
        } catch (IOException e) {
            handleSocketError();
            return null;
        }
    }

    /**
     * Get a list of all album artists available in MPDs database
     *
     * @return List of MPDArtist objects
     */
    public synchronized List<MPDArtist> getAlbumArtists() throws MPDException {
        // Get a list of artists. If server is new enough this will contain MBIDs for artists, that are tagged correctly.
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUMARTISTS(mServerCapabilities.hasListGroup() && mServerCapabilities.hasMusicBrainzTags()));
        try {
            List<MPDArtist> artists = MPDResponseParser.parseMPDArtists(this, mServerCapabilities.hasMusicBrainzTags(), mServerCapabilities.hasListGroup());
            if (artists.size() > 0 && artists.get(0).getArtistName().isEmpty()) {
                artists.remove(0);
            }
            startIdleWait();
            return artists;
        } catch (IOException e) {
            handleSocketError();
            return null;
        }
    }

    /**
     * Get a list of all playlists available in MPDs database
     *
     * @return List of MPDArtist objects
     */
    public synchronized List<MPDFileEntry> getPlaylists() throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_SAVED_PLAYLISTS);
        try {
            List<MPDFileEntry> playlists = MPDResponseParser.parseMPDTracks(this, "", "");
            startIdleWait();
            Collections.sort(playlists);
            return playlists;
        } catch (IOException e) {
            handleSocketError();
            return null;
        }
    }

    /**
     * Gets all tracks from MPD server. This could take a long time to process. Be warned.
     *
     * @return A list of all tracks in MPDTrack objects
     */
    public synchronized List<MPDFileEntry> getAllTracks() throws MPDException {
        Log.w(TAG, "This command should not be used");
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALL_FILES);
        try {
            List<MPDFileEntry> result = MPDResponseParser.parseMPDTracks(this, "", "");
            startIdleWait();
            return result;
        } catch (IOException e) {
            handleSocketError();
            return null;
        }
    }


    /**
     * Returns the list of tracks that are part of albumName
     *
     * @param albumName Album to get tracks from
     * @return List of MPDTrack track objects
     */
    public synchronized List<MPDFileEntry> getAlbumTracks(String albumName, String mbid) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUM_TRACKS(albumName));
        try {
            List<MPDFileEntry> result = MPDResponseParser.parseMPDTracks(this, "", mbid);
            startIdleWait();
            MPDSortHelper.sortFileListNumeric(result);
            return result;
        } catch (IOException e) {
            handleSocketError();
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
     * @return List of MPDTrack track objects
     */
    public synchronized List<MPDFileEntry> getArtistAlbumTracks(String albumName, String artistName, String mbid) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_REQUEST_ALBUM_TRACKS(albumName));
        try {
            /* Filter tracks with artistName */
            List<MPDFileEntry> result = MPDResponseParser.parseMPDTracks(this, artistName, mbid);
            startIdleWait();
            // Sort with disc & track number
            MPDSortHelper.sortFileListNumeric(result);
            return result;
        } catch (IOException e) {
            handleSocketError();
            return null;
        }
    }

    /**
     * Requests the current playlist of the server
     *
     * @return List of MPDTrack items with all tracks of the current playlist
     */
    public synchronized List<MPDFileEntry> getCurrentPlaylist() throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_PLAYLIST);
        try {
            /* Parse the return */
            List<MPDFileEntry> result = MPDResponseParser.parseMPDTracks(this, "", "");
            startIdleWait();
            return result;
        } catch (IOException e) {
            handleSocketError();
            return null;
        }
    }

    /**
     * Requests the current playlist of the server with a window
     *
     * @return List of MPDTrack items with all tracks of the current playlist
     */
    public synchronized List<MPDFileEntry> getCurrentPlaylistWindow(int start, int end) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_PLAYLIST_WINDOW(start, end));
        try {
            /* Parse the return */
            List<MPDFileEntry> result = MPDResponseParser.parseMPDTracks(this, "", "");
            startIdleWait();
            return result;
        } catch (IOException e) {
            handleSocketError();
            return null;
        }
    }

    /**
     * Requests the current playlist of the server
     *
     * @return List of MPDTrack items with all tracks of the current playlist
     */
    public synchronized List<MPDFileEntry> getSavedPlaylist(String playlistName) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_SAVED_PLAYLIST(playlistName));
        try {
            /* Parse the return */
            List<MPDFileEntry> result = MPDResponseParser.parseMPDTracks(this, "", "");
            startIdleWait();
            return result;
        } catch (IOException e) {
            handleSocketError();
            return null;
        }
    }

    /**
     * Requests the files for a specific path with info
     *
     * @return List of MPDTrack items with all tracks of the current playlist
     */
    public synchronized List<MPDFileEntry> getFiles(String path) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_FILES_INFO(path));
        try {
        /* Parse the return */
            List<MPDFileEntry> retList = MPDResponseParser.parseMPDTracks(this, "", "");
            startIdleWait();
            Collections.sort(retList);
            return retList;
        } catch (IOException e) {
            handleSocketError();
            return null;
        }
    }

    /**
     * Requests the files for a specific search term and type
     *
     * @param term The search term to use
     * @param type The type of items to search
     * @return List of MPDTrack items with all tracks matching the search
     */
    public synchronized List<MPDFileEntry> getSearchedFiles(String term, MPDCommands.MPD_SEARCH_TYPE type) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SEARCH_FILES(term, type));
        try {
            /* Parse the return */
            List<MPDFileEntry> result = MPDResponseParser.parseMPDTracks(this, "", "");
            startIdleWait();
            return result;
        } catch (IOException e) {
            handleSocketError();
            return null;
        }
    }

    /**
     * Searches a URL in the current playlist. If available the track is part of the returned list.
     *
     * @param url URL to search in the current playlist.
     * @return List with one entry or none.
     */
    public synchronized List<MPDFileEntry> getPlaylistFindTrack(String url) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_PLAYLIST_FIND_URI(url));
        try {
            /* Parse the return */
            List<MPDFileEntry> result = MPDResponseParser.parseMPDTracks(this, "", "");
            startIdleWait();
            return result;
        } catch (IOException e) {
            handleSocketError();
            return null;
        }
    }

    /**
     * Requests the currentstatus package from the mpd server.
     *
     * @return The CurrentStatus object with all gathered information.
     */
    public synchronized MPDCurrentStatus getCurrentServerStatus() throws MPDException {
        /* Request status */
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_STATUS);
        MPDCurrentStatus retStatus = null;
        try {
            retStatus = MPDResponseParser.parseMPDCurrentStatus(this);
        } catch (IOException e) {
            handleSocketError();
        }
        startIdleWait();

        return retStatus;
    }

    /**
     * Requests the server statistics package from the mpd server.
     *
     * @return The CurrentStatus object with all gathered information.
     */
    public synchronized MPDStatistics getServerStatistics() throws MPDException {
        /* Request status */
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_STATISTICS);
        startIdleWait();

        MPDStatistics stats = null;
        try {
            stats = MPDResponseParser.parseMPDStatistic(this);
        } catch (IOException e) {
            handleSocketError();
        }

        return stats;
    }

    /**
     * This will query the current song playing on the mpd server.
     *
     * @return MPDTrack entry for the song playing.
     */
    public synchronized MPDTrack getCurrentSong() throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_GET_CURRENT_SONG);

        // Reuse the parsing function for tracks here.
        List<MPDFileEntry> retList = null;
        try {
            retList = MPDResponseParser.parseMPDTracks(this, "", "");
            startIdleWait();
        } catch (IOException e) {
            handleSocketError();
            return null;
        }
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
    public synchronized boolean pause(boolean pause) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_PAUSE(pause));

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Jumps to the next song
     *
     * @return true if successful, false otherwise
     */
    public synchronized boolean nextSong() throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_NEXT);

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Jumps to the previous song
     *
     * @return true if successful, false otherwise
     */
    public synchronized boolean previousSong() throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_PREVIOUS);

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Stops playback
     *
     * @return true if successful, false otherwise
     */
    public synchronized boolean stopPlayback() throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_STOP);

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Sets random to true or false
     *
     * @param random If random should be set (true) or not (false)
     * @return True if server responed with ok
     */
    public synchronized boolean setRandom(boolean random) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SET_RANDOM(random));

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Sets repeat to true or false
     *
     * @param repeat If repeat should be set (true) or not (false)
     * @return True if server responed with ok
     */
    public synchronized boolean setRepeat(boolean repeat) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SET_REPEAT(repeat));

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Sets single playback to enable (true) or disabled (false)
     *
     * @param single if single playback should be enabled or not.
     * @return True if server responed with ok
     */
    public synchronized boolean setSingle(boolean single) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SET_SINGLE(single));

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Sets if files should be removed after playback (consumed)
     *
     * @param consume True if yes and false if not.
     * @return True if server responed with ok
     */
    public synchronized boolean setConsume(boolean consume) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SET_CONSUME(consume));

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Plays the song with the index in the current playlist.
     *
     * @param index Index of the song that should be played.
     * @return True if server responed with ok
     */
    public synchronized boolean playSongIndex(int index) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_PLAY_SONG_INDEX(index));

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Seeks the currently playing song to a certain position
     *
     * @param seconds Position in seconds to which a seek is requested to.
     * @return True if server responed with ok
     */
    public synchronized boolean seekSeconds(int seconds) throws MPDException {
        MPDCurrentStatus status = null;

        status = getCurrentServerStatus();


        sendMPDCommand(MPDCommands.MPD_COMMAND_SEEK_SECONDS(status.getCurrentSongIndex(), seconds));

            /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Sets the volume of the mpd servers output. It is an absolute value between (0-100).
     *
     * @param volume Volume to set to the server.
     * @return True if server responed with ok
     */
    public synchronized boolean setVolume(int volume) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SET_VOLUME(volume));

        /* Return the response value of MPD */
        return checkResponse();
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
    public synchronized boolean addTrackList(List<MPDFileEntry> tracks) throws MPDException {
        if (null == tracks) {
            return false;
        }
        startCommandList();

        for (MPDFileEntry track : tracks) {
            if (track instanceof MPDTrack) {
                sendMPDRAWCommand(MPDCommands.MPD_COMMAND_ADD_FILE(track.getPath()));
            }
        }
        endCommandList();

        /* Return the response value of MPD */
        return checkResponse();
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
    public synchronized boolean addAlbumTracks(String albumname, String artistname, String mbid) throws MPDException {
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
    public synchronized boolean addArtist(String artistname, MPDAlbum.MPD_ALBUM_SORT_ORDER sortOrder) throws MPDException {
        List<MPDAlbum> albums = getArtistAlbums(artistname);
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
    public synchronized boolean addSong(String url) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_ADD_FILE(url));

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * This method adds a song to a specified positiion in the current playlist.
     * This allows GUI developers to implement a method like "add after current".
     *
     * @param url   URL to add to the playlist.
     * @param index Index at which the item should be added.
     * @return True if server responed with ok
     */
    public synchronized boolean addSongatIndex(String url, int index) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_ADD_FILE_AT_INDEX(url, index));

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Adds files to the playlist with a search term for a specific type
     *
     * @param term The search term to use
     * @param type The type of items to search
     * @return True if server responed with ok
     */
    public synchronized boolean addSearchedFiles(String term, MPDCommands.MPD_SEARCH_TYPE type) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_ADD_SEARCH_FILES(term, type));

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Instructs the mpd server to clear its current playlist.
     *
     * @return True if server responed with ok
     */
    public synchronized boolean clearPlaylist() throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_CLEAR_PLAYLIST);

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Instructs the mpd server to shuffle its current playlist.
     *
     * @return True if server responed with ok
     */
    public synchronized boolean shufflePlaylist() throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SHUFFLE_PLAYLIST);

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Instructs the mpd server to remove one item from the current playlist at index.
     *
     * @param index Position of the item to remove from current playlist.
     * @return True if server responed with ok
     */
    public synchronized boolean removeIndex(int index) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_SONG_FROM_CURRENT_PLAYLIST(index));

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Instructs the mpd server to remove an range of songs from current playlist.
     *
     * @param start Start of songs to remoge
     * @param end   End of the range
     * @return True if server responed with ok
     */
    public synchronized boolean removeRange(int start, int end) throws MPDException {
        // Check capabilities if removal with one command is possible
        if (mServerCapabilities.hasCurrentPlaylistRemoveRange()) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_RANGE_FROM_CURRENT_PLAYLIST(start, end + 1));
        } else {
            // Create commandlist instead
            startCommandList();
            for (int i = start; i <= end; i++) {
                sendMPDRAWCommand(MPDCommands.MPD_COMMAND_REMOVE_SONG_FROM_CURRENT_PLAYLIST(start));
            }
            endCommandList();
        }


        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Moves one item from an index in the current playlist to an new index. This allows to move
     * tracks for example after the current to priotize songs.
     *
     * @param from Item to move from.
     * @param to   Position to enter item
     * @return
     */
    public synchronized boolean moveSongFromTo(int from, int to) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_MOVE_SONG_FROM_INDEX_TO_INDEX(from, to));

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Saves the current playlist as a new playlist with a name.
     *
     * @param name Name of the playlist to save to.
     * @return True if server responed with ok
     */
    public synchronized boolean savePlaylist(String name) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_SAVE_PLAYLIST(name));

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Adds a song to the saved playlist
     *
     * @param playlistName Name of the playlist to add the url to.
     * @param url          URL to add to the saved playlist
     * @return True if server responed with ok
     */
    public synchronized boolean addSongToPlaylist(String playlistName, String url) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_ADD_TRACK_TO_PLAYLIST(playlistName, url));

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Removes a song from a saved playlist
     *
     * @param playlistName Name of the playlist of which the song should be removed from
     * @param position     Index of the song to remove from the lits
     * @return
     */
    public synchronized boolean removeSongFromPlaylist(String playlistName, int position) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_TRACK_FROM_PLAYLIST(playlistName, position));

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Removes a saved playlist from the servers database.
     *
     * @param name Name of the playlist to remove.
     * @return True if server responed with ok
     */
    public synchronized boolean removePlaylist(String name) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_REMOVE_PLAYLIST(name));

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Loads a saved playlist (added after the last song) to the current playlist.
     *
     * @param name Of the playlist to add to.
     * @return True if server responed with ok
     */
    public synchronized boolean loadPlaylist(String name) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_LOAD_PLAYLIST(name));

        /* Return the response value of MPD */
        return checkResponse();
    }


    /**
     * Returns the list of MPDOutputs to the outside callers.
     *
     * @return List of MPDOutput objects or null in case of error.
     */
    public List<MPDOutput> getOutputs() throws MPDException {
        synchronized (this) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_GET_OUTPUTS);

            try {
                List<MPDOutput> list = MPDResponseParser.parseMPDOutputs(this);
                startIdleWait();
                return list;
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
    public synchronized boolean toggleOutput(int id) throws MPDException {
        if (mServerCapabilities.hasToggleOutput()) {
            sendMPDCommand(MPDCommands.MPD_COMMAND_TOGGLE_OUTPUT(id));
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

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Enable the output with the id.
     *
     * @param id Id of the output to enable (active/deactive)
     * @return True if server responed with ok
     */
    public synchronized boolean enableOutput(int id) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_ENABLE_OUTPUT(id));

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Disable the output with the id.
     *
     * @param id Id of the output to disable (active/deactive)
     * @return True if server responed with ok
     */
    public synchronized boolean disableOutput(int id) throws MPDException {
        sendMPDCommand(MPDCommands.MPD_COMMAND_DISABLE_OUTPUT(id));

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Instructs to update the database of the mpd server.
     *
     * @param path Path to update
     * @return True if server responed with ok
     */
    public synchronized boolean updateDatabase(String path) throws MPDException {
        // Update root directory
        sendMPDCommand(MPDCommands.MPD_COMMAND_UPDATE_DATABASE(path));

        /* Return the response value of MPD */
        return checkResponse();
    }

    /**
     * Checks if the socket is ready for read operations
     *
     * @return True if ready
     */
    private boolean readyRead() {
        try {
            return (null != mSocket) && (null != mReader) && mSocket.isConnected() && mReader.ready();
        } catch (IOException e) {
            handleSocketError();
            return false;
        }
    }

    /**
     * Will notify a connected listener that the connection is now ready to be used.
     */
    private void notifyConnected() {
        for (MPDConnectionStateChangeListener listener : mStateListeners) {
            listener.onConnected();
        }
    }

    /**
     * Will notify a connected listener that the connection is disconnect and not ready for use.
     */
    private void notifyDisconnect() {
        for (MPDConnectionStateChangeListener listener : mStateListeners) {
            listener.onDisconnected();
        }
    }

    /**
     * Registers a listener to be notified about connection state changes
     *
     * @param listener Listener to be connected
     */
    public void setStateListener(MPDConnectionStateChangeListener listener) {
        mStateListeners.add(listener);
    }

    /**
     * Registers a listener to be notified about changes in idle state of this connection.
     *
     * @param listener
     */
    public void setIdleListener(MPDConnectionIdleChangeListener listener) {
        mIdleListeners.add(listener);
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
    private String waitForIdleResponse() throws IOException {
        if (null != mReader) {

            Log.v(TAG, "Waiting for input from server");
            // Set thread to sleep, because there should be no line available to read.
            String response = null;
            try {
                response = readLine();
            } catch (MPDException e) {
                handleSocketError();
            }
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
            String response = null;
            try {
                response = waitForIdleResponse();

                // Wait for possible deidle lock
                try {
                    mDeidleTimeoutLock.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mDeIdleTimerLock.lock();
                // Clear possible timeout tasks
                if (mDeidleTimeoutTimer != null) {
                    mDeidleTimeoutTimer.cancel();
                    mDeidleTimeoutTimer.purge();
                    mDeidleTimeoutTimer = null;
                }
                mDeIdleTimerLock.unlock();

                // If deidling already timed out we don't need to take care here. Its already done by
                // the timeout thread.
                if (mDeIdlingTimedOut) {
                    // Release held locks
                    mDeidleTimeoutLock.release();
                    mIdleWaitLock.release();
                    return;
                }
                mDeidleTimeoutLock.release();
            } catch (IOException e) {
                Log.v(TAG, "Read error exception. Disconnecting and cleaning up");

                mDeIdleTimerLock.lock();
                // Clear possible timeout tasks
                if (mDeidleTimeoutTimer != null) {
                    mDeidleTimeoutTimer.cancel();
                    mDeidleTimeoutTimer.purge();
                    mDeidleTimeoutTimer = null;
                }
                mDeIdleTimerLock.unlock();

                // If deidling already timed out we don't need to take care here. Its already done by
                // the timeout thread.
                if (mDeIdlingTimedOut) {
                    Log.v(TAG, "Deidle timed out, cancel normal deidling");
                    mDeidleTimeoutLock.release();
                    mIdleWaitLock.release();
                    return;
                }
                mDeidleTimeoutLock.release();

                try {
                    /* Clear reader/writer up */
                    if (null != mReader) {
                        mReader = null;
                    }
                    if (null != mWriter) {
                        mWriter = null;
                    }

                    /* Clear TCP-Socket up */
                    if (null != mSocket && mSocket.isConnected()) {
                        mSocket.setSoTimeout(500);
                        mSocket.close();
                    }
                    mSocket = null;
                } catch (IOException e2) {
                    Log.v(TAG, "Error during read error handling");
                }

                /* Clear up connection state variables */
                mMPDConnectionIdle = false;
                mMPDConnectionReady = false;


                // Notify listener
                notifyDisconnect();

                // Release the idle mode
                mIdleWaitLock.release();
                return;
            }

            if (DEBUG_ENABLED) {
                Log.v(TAG, "Idle over with response: " + response);
            }

            // This happens when disconnected
            if (null == response || response.isEmpty()) {
                if (DEBUG_ENABLED) {
                    Log.v(TAG, "Probably disconnected during idling");
                }
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
                if (DEBUG_ENABLED) {
                    Log.v(TAG, "Externally deidled!");
                }
                externalDeIdle = true;
                try {
                    while (readyRead()) {
                        response = readLine();
                        if (response.startsWith("OK")) {
                            if (DEBUG_ENABLED) {
                                Log.v(TAG, "Deidled with status ok");
                            }
                        } else if (response.startsWith("ACK")) {
                            if (DEBUG_ENABLED) {
                                Log.v(TAG, "Server response error: " + response);
                            }
                        }
                    }
                } catch (MPDException e) {
                    handleSocketError();
                }
            } else {
                if (DEBUG_ENABLED) {
                    Log.v(TAG, "Deidled on purpose");
                }
            }
            // Set the connection as non-idle again.
            mMPDConnectionIdle = false;

            // Reset the timeout again
            try {
                if (mSocket != null) {
                    mSocket.setSoTimeout(SOCKET_TIMEOUT);
                }
            } catch (SocketException e) {
                handleSocketError();
            }

            // Release the lock for possible threads waiting from outside this idling thread (handler thread).
            mIdleWaitLock.release();

            // Notify a possible listener for deidling.
            for (MPDConnectionIdleChangeListener listener : mIdleListeners) {
                listener.onNonIdle();
            }
            if (DEBUG_ENABLED) {
                Log.v(TAG, "Idling over");
            }

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
        if (DEBUG_ENABLED) {
            Log.v(TAG, "IdleWait scheduled");
        }
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
        if (DEBUG_ENABLED) {
            Log.v(TAG, "IdleWait terminated");
        }
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

    /**
     * Central method to read a line from the sockets reader
     *
     * @return The read string. null if no data is available.
     */
    String readLine() throws MPDException {
        if (mReader != null) {
            String line = null;
            try {
                line = mReader.readLine();
            } catch (IOException e) {
                handleSocketError();
            }
            if (line.startsWith("ACK")) {
                if (line.contains(MPDResponses.MPD_PARSE_ARGS_LIST_ERROR)) {
                    enableMopidyWorkaround();
                    return null;
                }
                throw new MPDException(line);
            }
            //Log.v(TAG,"Read line: " + line);
            return line;
        }
        return null;
    }

    /**
     * Central method to write a line to the sockets writer. Socket will be flushed afterwards
     * to ensure that the string is sent.
     *
     * @param line String to write to the socket.
     */
    private void writeLine(String line) {
        if (mWriter != null) {
            mWriter.println(line);
            mWriter.flush();
            if (DEBUG_ENABLED) {
                Log.v(TAG, "Write line: " + line);
            }
        }
    }

    private void printStackTrace() {
        StackTraceElement[] st = new Exception().getStackTrace();
        for (StackTraceElement el : st) {
            if (DEBUG_ENABLED) {
                Log.v(TAG, el.toString());
            }
        }
    }

    /**
     * This is called if an parse list args error occurs during the parsing
     * of {@link MPDAlbum} or {@link MPDArtist} objects. This probably indicates
     * that this client is connected to Mopidy so we enable a workaround and reconnect
     * to force the GUI to reload the contents.
     */
    private void enableMopidyWorkaround() {
        // Enable the workaround in the capabilities object
        mServerCapabilities.enableMopidyWorkaround();

        // Reconnect to server
        disconnectFromServer();
        try {
            connectToServer();
        } catch (MPDException e) {
            // FIXME what to do?
        }
    }

    /**
     * Private class that aborts the idle even if the server does not respond.
     * This is necessary as the IdleWaitThread will not timeout on its on when it
     * is blocked in a read operation.
     */
    private class DeIdleTimeoutTask extends TimerTask {

        @Override
        public void run() {
            if (DEBUG_ENABLED) {
                Log.v(TAG, "Deidling timeout!");
            }
            // Disconnect here because deidling took to long

            // Acquire lock to prevent the IdleWaitThread from
            // doing its work first.
            try {
                mDeidleTimeoutLock.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            /* Cleanup reader/writer */
            try {
            /* Clear reader/writer up */
                if (null != mReader) {
                    mReader = null;
                }
                if (null != mWriter) {
                    mWriter = null;
                }

            /* Clear TCP-Socket up */
                if (null != mSocket && mSocket.isConnected()) {
                    mSocket.setSoTimeout(500);
                    mSocket.close();
                    mSocket = null;
                }
            } catch (IOException e) {
                if (DEBUG_ENABLED) {
                    Log.v(TAG, "Error during disconnecting:" + e.toString());
                }
            }

            /* Clear up connection state variables */
            mMPDConnectionIdle = false;
            mMPDConnectionReady = false;

            // Notify listener
            notifyDisconnect();

            mDeIdleTimerLock.lock();
            // Set timeout task for deidling.
            if (mDeidleTimeoutTimer != null) {
                mDeidleTimeoutTimer.cancel();
                mDeidleTimeoutTimer.purge();
                mDeidleTimeoutTimer = null;
            }
            mDeIdleTimerLock.unlock();

            // Now let the the IdleWaitThread continue
            mDeidleTimeoutLock.release();

            // Set to indicate an aborted deidling.
            mDeIdlingTimedOut = true;
        }
    }
}
