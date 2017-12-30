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

import org.gateshipone.malp.BuildConfig;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
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
    private static final String TAG = MPDConnection.class.getSimpleName();

    /**
     * Set this flag to enable debugging in this class. DISABLE before releasing
     */
    private static final boolean DEBUG_ENABLED = BuildConfig.DEBUG;

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

    private final Timer mIDLETimer;
    private StartIDLETask mIDLETask;

    /**
     * Timer to schedule a timeout cancellation of the noidle logic (no response from server, e.g.
     * disconnected during idle).
     */
    private final Timer mReadTimeoutTimer;

    /**
     * Task to handle the read timeout for the noidle command.
     */
    private ReadTimeoutTask mReadTimeoutTask;

    /**
     * Semaphore to synchronize regular noidle and timeout cancellation.
     */
    private final Semaphore mTimeoutSemaphore;

    /**
     * Only get the server capabilities if server parameters changed
     */
    private boolean mCapabilitiesChanged;

    /**
     * One listener for the state of the connection (connected, disconnected)
     */
    private final ArrayList<MPDConnectionStateChangeListener> mStateListeners;

    /**
     * One listener for the idle state of the connection. Can be used to react
     * to changes to the server from other clients. When the server is deidled (from outside)
     * it will notify this listener.
     */
    private final ArrayList<MPDConnectionIdleChangeListener> mIdleListeners;


    /**
     * Singleton instance
     */
    public final static MPDConnection mInstance = new MPDConnection();


    private ConnectionSemaphore mConnectionLock;

    /**
     * Creates disconnected MPDConnection with following parameters
     */
    private MPDConnection() {
        mSocket = null;
        mReader = null;
        mServerCapabilities = new MPDCapabilities("", null, null);
        mIdleListeners = new ArrayList<>();
        mStateListeners = new ArrayList<>();

        mConnectionLock = new ConnectionSemaphore(1);
        mTimeoutSemaphore = new Semaphore(1);

        mIDLETimer = new Timer();

        mReadTimeoutTimer = new Timer();
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

        cancelIDLEWait();

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
        mPassword = password;
        mPort = port;
        mCapabilitiesChanged = true;
        if (DEBUG_ENABLED) {
            Log.v(TAG, "Connection parameters changed");
        }
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

        try {
            mConnectionLock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if ((null == mHostname) || mHostname.equals("")) {
            mConnectionLock.release();
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
            mConnectionLock.release();
            throw new MPDException.MPDConnectionException(e.getLocalizedMessage());
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
                    mConnectionLock.release();
                    throw new MPDException.MPDConnectionException(e.getLocalizedMessage());
                }
            }

            /* Create the writer used for writing to the socket */
            if (mWriter == null) {
                try {
                    mWriter = new PrintWriter(new OutputStreamWriter(mSocket.getOutputStream()));
                } catch (IOException e) {
                    handleSocketError();
                    mConnectionLock.release();
                    throw new MPDException.MPDConnectionException(e.getLocalizedMessage());
                }
            }

            try {
                waitForResponse();
            } catch (IOException e) {
                handleSocketError();
                mConnectionLock.release();
                throw new MPDException.MPDConnectionException(e.getLocalizedMessage());
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

            if (mPassword != null && !mPassword.equals("")) {
                /* Authenticate with server because password is set. */
                authenticateMPDServer();
            }

            if (mCapabilitiesChanged) {
                // Get available commands
                writeLine(MPDCommands.MPD_COMMAND_GET_COMMANDS);

                try {
                    waitForResponse();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                List<String> commands = null;
                try {
                    commands = MPDResponseParser.parseMPDCommands(this);
                } catch (IOException e) {
                    handleSocketError();
                    mConnectionLock.release();
                    throw new MPDException.MPDConnectionException(e.getLocalizedMessage());
                }
                // Get list of supported tags
                writeLine(MPDCommands.MPD_COMMAND_GET_TAGS);
                try {
                    waitForResponse();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                List<String> tags = null;
                try {
                    tags = MPDResponseParser.parseMPDTagTypes(this);
                } catch (IOException e) {
                    handleSocketError();
                    mConnectionLock.release();
                    throw new MPDException.MPDConnectionException(e.getLocalizedMessage());
                }

                mServerCapabilities = new MPDCapabilities(versionString, commands, tags);
                mCapabilitiesChanged = false;
            }

            // Set the timeout to infinite again
            try {
                mSocket.setSoTimeout(SOCKET_TIMEOUT);
            } catch (SocketException e) {
                handleSocketError();
                mConnectionLock.release();
                throw new MPDException.MPDConnectionException(e.getLocalizedMessage());
            }
            mMPDConnectionReady = true;
            mConnectionLock.release();
            if (DEBUG_ENABLED) {
                Log.v(TAG, "Connection successfully established");
            }

            // Notify listener
            notifyConnected();
        }
    }


    /**
     * If the password for the MPDConnection is set then the client should
     * try to authenticate with the server
     */
    private void authenticateMPDServer() throws MPDException {
        writeLine(MPDCommands.MPD_COMMAND_PASSWORD(mPassword));

        try {
            waitForResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }

        checkResponse();
    }

    /**
     * Requests to disconnect from server. This will close the conection and cleanup the socket.
     * After this call it should be safe to reconnect to another server. If this connection is
     * currently in idle state, then it will be deidled before.
     */
    public void disconnectFromServer() {

        // Check if the connection is currently idling, if then deidle.
        if (mMPDConnectionIdle) {
            stopIDLE();
        }

        try {
            mConnectionLock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
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

        if (DEBUG_ENABLED) {
            Log.v(TAG, "Disconnected");
        }

        cancelIDLEWait();

        mConnectionLock.release();
    }

    /**
     * Access to the currently server capabilities
     *
     * @return
     */
    public MPDCapabilities getServerCapabilities() {
        if (isConnected()) {
            return mServerCapabilities;
        } else {
            return new MPDCapabilities("", null, null);
        }
    }

    /**
     * This functions sends the command to the MPD server.
     * If the server is currently idling then it will deidle it first.
     *
     * CAUTION: After using this command it is important to clear the input buffer and read until
     * either "OK" or "ACK ..." is sent. Otherwise the connection will remain in an undefined state.
     *
     * If you want to submit a simple command (without a response other than OK/ACK)
     * like pause/play use sendSimpleMPDCommand.
     *
     * @param command Command string to send to the MPD server
     */
    synchronized void sendMPDCommand(String command) {
        if (DEBUG_ENABLED) {
            Log.v(TAG, "Send command: " + command);
        }

        /* Check if the server is connected. */
        if (mMPDConnectionReady) {
            /*
             * Check if server is in idling mode, this needs unidling first,
             * otherwise the server will disconnect the client.
             */
            if (mMPDConnectionIdle) {
                stopIDLE();
            }

            // During deidle a disconnect could happen, check again if connection is ready
            if (!mMPDConnectionReady) {
                return;
            }

            // Acquire lock
            try {
                mConnectionLock.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (DEBUG_ENABLED) {
                Log.v(TAG, "Connection lock acquired: " + command);
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
                mConnectionLock.release();
            }
            if (DEBUG_ENABLED) {
                Log.v(TAG, "Sent command, got response");
            }
        }
    }

    /**
     * Sends a simple command to the MPD server. This method automatically handles the response
     * from the server.
     *
     * @param command Command string sent to the server
     * @throws MPDException
     */
    synchronized void sendSimpleMPDCommand(String command) throws MPDException {
        // Send the command to the server
        sendMPDCommand(command);

        // Read until either OK or ACK is received
        checkResponse();
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
    void sendMPDRAWCommand(String command) {
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
    void startCommandList() {
        try {
            mConnectionLock.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        /* Check if the server is connected. */
        if (mMPDConnectionReady) {
            /* Check if server is in idling mode, this needs unidling first,
            otherwise the server will disconnect the client.
             */
            if (mMPDConnectionIdle) {
                stopIDLE();
            }

            /*
             * Send the command to the server
             * FIXME Should be validated in the future.
             */
            writeLine(MPDCommands.MPD_START_COMMAND_LIST);
        } else {
            mConnectionLock.release();
        }
    }

    /**
     * This command will end the command list. After this call it is important to call
     * checkResponse to clear the possible response in the read buffer. There should be at
     * least one "OK" or "ACK" from the mpd server.
     */
    void endCommandList() throws MPDException {
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
                mConnectionLock.release();
            }

            // Commandlist is finished. Check for servers response
            checkResponse();
        }
    }


    /**
     * This method needs to be called before a new MPD command is sent to
     * the server to correctly unidle. Otherwise the mpd server will disconnect
     * the disobeying client.
     */
    private void stopIDLE() {
        if (DEBUG_ENABLED) {
            Log.v(TAG, "Stop Idling");
        }

        /* Check if server really is in idling mode */
        if (!mMPDConnectionIdle || !mMPDConnectionReady) {
            return;
        }

        try {
            mSocket.setSoTimeout(SOCKET_TIMEOUT);
        } catch (SocketException e) {
            handleSocketError();
        }

        // Start timeout task
        synchronized (mReadTimeoutTimer) {
            mReadTimeoutTask = new ReadTimeoutTask();
            mReadTimeoutTimer.schedule(mReadTimeoutTask, DEIDLE_TIMEOUT);
            if (DEBUG_ENABLED) {
                Log.v(TAG, "noidle read timeout scheduled");
            }
        }

        /* Send the "noidle" command to the server to initiate noidle */
        writeLine(MPDCommands.MPD_COMMAND_STOP_IDLE);

        if (DEBUG_ENABLED) {
            Log.v(TAG, "Sent deidle request");
        }
    }

    /**
     * Initiates the idling procedure. A separate thread is started to wait (blocked)
     * for a deidle from the MPD host. Otherwise it is impossible to get notified on changes
     * from other mpd clients (eg. volume change)
     */
    private synchronized void startIDLE() {
        if (DEBUG_ENABLED) {
            Log.v(TAG, "Start IDLE mode");
        }

        /* Check if server really is in idling mode */
        if (!mMPDConnectionReady || mMPDConnectionIdle) {
            // Release connection
            mConnectionLock.release();
            return;
        }

        // Set the timeout to zero to block when no data is available
        try {
            mSocket.setSoTimeout(0);
        } catch (SocketException e) {
            e.printStackTrace();
        }


        // This will send the idle command to the server. From there on we need to deidle before
        // sending new requests.
        writeLine(MPDCommands.MPD_COMMAND_START_IDLE);

        // Technically we are in idle mode now, set boolean
        mMPDConnectionIdle = true;

        new IdleThread().start();

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
            if (DEBUG_ENABLED) {
                Log.v(TAG, "Listening for server-side changes");
            }
            // Set thread to sleep, because there should be no line available to read.
            String response = null;
            try {
                response = readLineInternal();
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
            String response;
            // Wait for noidle. This should block until the server is ready for commands again
            try {
                response = waitForIdleResponse();
            } catch (IOException e) {
                e.printStackTrace();
                handleSocketError();
                return;
            }

            synchronized (mReadTimeoutTimer) {
                if (mReadTimeoutTask != null) {
                    mReadTimeoutTask.cancel();
                    mReadTimeoutTask = null;
                    if (DEBUG_ENABLED) {
                        Log.v(TAG, "noidle read timeout cancelled");
                    }
                }
            }

            // Synchronize with timeout semaphore
            try {
                mTimeoutSemaphore.acquire();
            } catch (InterruptedException e) {
                // FIXME do nothing?
            }

            // Check if a timeout occurred
            if (mSocket == null) {
                mTimeoutSemaphore.release();
                // Timeout, abort!
                mConnectionLock.release();
                return;
            }


            // Check if noidle was sent or if server changed externally
            if (response.startsWith(MPDResponses.MPD_RESPONSE_CHANGED)) {
                // External change
                if (DEBUG_ENABLED) {
                    Log.v(TAG, "External changes");
                    mMPDConnectionIdle = false;

                    while (!response.equals("OK")) {
                        try {
                            response = readLineInternal();
                        } catch (MPDException e) {
                            e.printStackTrace();
                        }
                    }

                    mConnectionLock.release();
                    notifyIdleListener();

                    scheduleIDLE();
                }
            } else if (response.isEmpty()) {
                if (DEBUG_ENABLED) {
                    Log.e(TAG, "Error during idling");
                }
                handleSocketError();
                mConnectionLock.release();
            } else {
                // Noidle sent
                // Release connection
                if (DEBUG_ENABLED) {
                    Log.v(TAG, "No external change, response: " + response);
                }
                mMPDConnectionIdle = false;
                mConnectionLock.release();
            }
            mTimeoutSemaphore.release();
        }
    }

    /**
     * Central method to read a line from the sockets reader
     *
     * @return The read string. null if no data is available.
     */
    String readLine() throws MPDException {
        if (mReader != null) {
            String line;
            try {
                line = mReader.readLine();
            } catch (IOException e) {
                handleSocketError();
                return "";
            }
            if (DEBUG_ENABLED) {
                //Log.v(TAG,"Read line: " + line);
            }
            if (line.startsWith("ACK")) {
                // Probably detected mopidy. Enable workaround
                if (line.contains(MPDResponses.MPD_PARSE_ARGS_LIST_ERROR)) {
                    mConnectionLock.release();
                    enableMopidyWorkaround();
                    return null;
                }
                mConnectionLock.release();
                throw new MPDException.MPDServerException(line);
            } else if (line.startsWith("OK")) {
                if (mMPDConnectionReady) {
                    if (DEBUG_ENABLED) {
                        Log.v(TAG, "OK read, releasing connection");
                    }
                    mConnectionLock.release();
                    scheduleIDLE();
                }
            }
            return line;
        }
        return "";
    }

    private String readLineInternal() throws MPDException {
        if (mReader != null) {
            String line;
            try {
                line = mReader.readLine();
            } catch (IOException e) {
                handleSocketError();
                return "";
            }
            if (DEBUG_ENABLED) {
                Log.v(TAG, "Read line internal: " + line);
            }
            if (line == null) {
                return "";
            } else if (line.startsWith("ACK")) {
                throw new MPDException.MPDServerException(line);
            }
            return line;
        }
        return "";
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

    private void notifyIdleListener() {
        synchronized (mIdleListeners) {
            for (MPDConnectionIdleChangeListener listener : mIdleListeners) {
                listener.onNonIdle();
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

    private void scheduleIDLE() {
        if (DEBUG_ENABLED) {
            Log.v(TAG, "Schedule IDLE");
        }

        synchronized (mIDLETimer) {
            if (mIDLETask != null) {
                mIDLETask.cancel();
            }
            mIDLETask = new StartIDLETask();
            mIDLETimer.schedule(mIDLETask, IDLE_WAIT_TIME);
        }
    }

    private void cancelIDLEWait() {
        if(DEBUG_ENABLED) {
            Log.v(TAG, "Cancel IDLE wait");
        }
        synchronized (mIDLETimer) {
            if(mIDLETask != null) {
                mIDLETask.cancel();
                mIDLETask = null;
            }
        }
    }

    private class StartIDLETask extends TimerTask {
        @Override
        public void run() {
            boolean locked = mConnectionLock.tryAcquire();
            if (locked) {
                startIDLE();
            } else {
                scheduleIDLE();
            }
        }
    }

    private class ReadTimeoutTask extends TimerTask {

        @Override
        public void run() {
            try {
                mTimeoutSemaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (DEBUG_ENABLED) {
                Log.w(TAG, "Timeout on noidle");
            }
            mReadTimeoutTask = null;

            // Check if no successful noidle took place due to almost simultaneous scheduling of
            // this Task and a possible server response
            if (!mMPDConnectionIdle) {
                if (DEBUG_ENABLED) {
                    Log.w(TAG, "Connection not idle anymore");
                }
                mTimeoutSemaphore.release();
                return;
            }

            handleSocketError();

            mTimeoutSemaphore.release();
        }
    }

    private class ConnectionSemaphore extends Semaphore {

        public ConnectionSemaphore(int permits) {
            super(permits);
        }

        @Override
        public void release() {
            super.release();
            if (DEBUG_ENABLED) {
                Log.v(TAG, "Semaphore released: " + availablePermits());
                if (availablePermits() > 1) {
                    Log.e(TAG, "More than 1 permit");
                }
            }
        }

        @Override
        public void acquire() throws InterruptedException {
            super.acquire();
            if (DEBUG_ENABLED) {
                Log.v(TAG, "Semaphore acquired: " + availablePermits());
            }
        }

        @Override
        public boolean tryAcquire() {
            boolean retVal = super.tryAcquire();
            if (retVal) {
                if (DEBUG_ENABLED) {
                    Log.v(TAG, "Semaphore acquired: " + availablePermits());
                }
            } else {
                if (DEBUG_ENABLED) {
                    Log.v(TAG, "Semaphore NOT acquired: " + availablePermits());
                }
            }
            return retVal;
        }
    }

}
