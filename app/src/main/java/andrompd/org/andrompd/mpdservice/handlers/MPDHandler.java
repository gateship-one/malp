package andrompd.org.andrompd.mpdservice.handlers;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;

import andrompd.org.andrompd.mpdservice.mpdprotocol.MPDConnection;

public class MPDHandler extends Handler {
    private static final String THREAD_NAME = "AndroMPD-NetHandler";

    private static HandlerThread pHandlerThread = null;
    private static MPDHandler pHandlerSingleton = null;

    private MPDConnection pMPDConnection;

    /**
     * Private constructor for use in singleton.
     * @param looper Looper of a HandlerThread (that is NOT the UI thread)
     */
    private MPDHandler(Looper looper) {
        super(looper);
        pMPDConnection = new MPDConnection("daenerys","synchot",6600);
    }

    /**
     * Private method to ensure that the singleton runs in a separate thread.
     * Otherwise android will deny network access because of UI blocks.
     * @return
     */
    private static MPDHandler getHandler() {
        if ( null == pHandlerSingleton ) {
            pHandlerThread = new HandlerThread(THREAD_NAME);
            pHandlerThread.start();
            pHandlerSingleton = new MPDHandler(pHandlerThread.getLooper());
        }
        return pHandlerSingleton;
    }


    /**
     * This is the main entry point of messages.
     * Here all possible messages types need to be handled with the MPDConnection.
     * @param msg Message to process.
     */
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        if ( !(msg.obj instanceof MPDHandlerAction) ) {
            /* Check if the message object is of correct type. Otherwise just abort here. */
            return;
        }

        MPDHandlerAction mpdAction = (MPDHandlerAction)msg.obj;
        /* Catch MPD exceptions here for now. */
        try {
            switch ( mpdAction.getAction() ) {
                case ACTION_CONNECT_MPD_SERVER:
                    pMPDConnection.connectToServer();
                    break;
                case ACTION_GET_ALBUMS:
                    pMPDConnection.getAlbums();
                    break;
                case ACTION_GET_ARTIST_ALBUMS:
                    /* Parse message objects extras */
                    String artistName = mpdAction.getStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME);
                    pMPDConnection.getArtistAlbums(artistName);
                    break;
                case ACTION_GET_ARTISTS:

                    break;
                case ACTION_GET_TRACKS:
                    break;
                case ACTION_GET_ALBUM_TRACKS:
                    break;
                case ACTION_GET_ARTIST_ALBUM_TRACKS:
                    break;
                case ACTION_GET_SERVER_STATUS:
                    break;
                default: return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /* Convenient methods for message generation */

    /**
     * Connect to the previously configured MPD server.
     */
    public static void connectToMPDServer() {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_CONNECT_MPD_SERVER);
        Message msg = Message.obtain();
        if ( msg == null ) {
            return;
        }
        msg.obj = action;
        MPDHandler.getHandler().sendMessage(msg);
    }

    public static void getAlbums() {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ALBUMS);
        Message msg = Message.obtain();
        if ( msg == null ) {
            return;
        }
        msg.obj = action;
        MPDHandler.getHandler().sendMessage(msg);
    }

    public static void getArtistAlbums(String artist) {
        MPDHandlerAction action = new MPDHandlerAction(MPDHandlerAction.NET_HANDLER_ACTION.ACTION_GET_ARTIST_ALBUMS);
        Message msg = Message.obtain();
        if ( msg == null ) {
            return;
        }
        action.setStringExtra(MPDHandlerAction.NET_HANDLER_EXTRA_STRING.EXTRA_ARTIST_NAME, artist);
        msg.obj = action;
        MPDHandler.getHandler().sendMessage(msg);
    }


}
