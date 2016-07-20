package andrompd.org.andrompd.mpdservice.handlers;


import java.util.HashMap;

import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseHandler;

public class MPDHandlerAction {


    /**
     * Actions of this message type. This should represent
     * all implemented methods of the MPD protocol.
     */
    public enum NET_HANDLER_ACTION {
        ACTION_SET_SERVER_PARAMETERS,
        ACTION_CONNECT_MPD_SERVER,
        ACTION_GET_ALBUMS,
        ACTION_GET_ARTIST_ALBUMS,
        ACTION_GET_ARTISTS,
        ACTION_GET_TRACKS,
        ACTION_GET_ALBUM_TRACKS,
        ACTION_GET_ARTIST_ALBUM_TRACKS,
        ACTION_GET_SERVER_STATUS
    }


    /**
     * Types of String extras for this message type that can be included.
     * Examples are artist,album and hostnames.
     */
    public enum NET_HANDLER_EXTRA_STRING {
        EXTRA_SERVER_HOSTNAME,
        EXTRA_SERVER_PASSWORD,
        EXTRA_ARTIST_NAME,
        EXTRA_ALBUM_NAME,
    }

    /**
     * Type of Integer extras for this message type that can be included.
     * Examples are the portnumber, volume, repeat, random state.
     */
    public enum NET_HANDLER_EXTRA_INT {
        EXTRA_SERVER_PORT
    }

    /**
     * HashMap of the String extras for this message. Will only be created
     * when it is used.
     */
    HashMap<NET_HANDLER_EXTRA_STRING, String> pStringExtras = null;
    /**
     * HashMap of the Integer extras for this message. Will only be created
     * when it is used.
     */
    HashMap<NET_HANDLER_EXTRA_INT, Integer> pIntExtras = null;

    private MPDResponseHandler pResponseHandler = null;

    /**
     * The action type for this message.
     */
    private NET_HANDLER_ACTION pAction;


    /**
     * Simple public constructor.
     * @param action The type of action for this message.
     */
    public MPDHandlerAction(NET_HANDLER_ACTION action) {
        pAction = action;
    }


    /**
     *
     * @return The action of this message.
     */
    public NET_HANDLER_ACTION getAction() {
        return pAction;
    }

    /**
     * Allows to put extras in Handler messages like the strings of artists,albums, etc.
     * @param type Type of the extra value
     * @param value Value of the extra
     */
    public void setStringExtra(NET_HANDLER_EXTRA_STRING type, String value) {
        /*
         * If the HashMap is not already created, create it.
         */
        if ( null == pStringExtras ) {
            pStringExtras = new HashMap<>();
        }
        pStringExtras.put(type, value);
    }

    /**
     * Allows to put extras in Handler messages like the Integers of the hosts port.
     * @param type Type of the extra value
     * @param value Value of the extra
     */
    public void setIntExtras(NET_HANDLER_EXTRA_INT type, Integer value ) {
        /*
         * If the HashMap is not already created, create it.
         */
        if ( null == pIntExtras ) {
            pIntExtras = new HashMap<>();
        }
        pIntExtras.put(type, value);
    }

    /**
     * Set the handler that is needed for the asynchronous response. Can be a Handler running in
     * the main UI thread.
     * @param responseHandler The Handler to be used for the response. Should be of the correct
     *                        type used for handling the type of resposne.
     */
    public void setResponseHandler(MPDResponseHandler responseHandler) {
        pResponseHandler = responseHandler;
    }

    /**
     * Returns the responsehandler that should be used for handling the asynchronous response.
     * @return The Handler for the MPD response.
     */
    public MPDResponseHandler getResponseHandler() {
        return pResponseHandler;
    }

    /**
     *
     * @param type Type of the extra value
     * @return The value of the extra value and null if not attached to this message
     */
    public String getStringExtra(NET_HANDLER_EXTRA_STRING type ) {
        /*
         * If the HashMap is not already created return null.
         */
        if ( null == pStringExtras ) {
            return null;
        }
        return pStringExtras.get(type);
    }

    /**
     *
     * @param type Type of the extra value
     * @return The value of the extra value and null if not attached to this message
     */
    public Integer getIntExtra(NET_HANDLER_EXTRA_INT type ) {
        /*
         * If the HashMap is not already created return null.
         */
        if ( null == pIntExtras ) {
            return null;
        }
        return pIntExtras.get(type);
    }

}
