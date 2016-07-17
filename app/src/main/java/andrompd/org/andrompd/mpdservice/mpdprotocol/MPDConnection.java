package andrompd.org.andrompd.mpdservice.mpdprotocol;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by hendrik on 16.07.16.
 */

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
}
