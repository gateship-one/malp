package andrompd.org.andrompd.mpdservice.handlers.responsehandler;


import android.os.Message;

import java.util.List;

import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDAlbum;

/**
 * Response class for album lists.
 */
public abstract class MPDResponseAlbumList extends MPDResponseHandler {

    public MPDResponseAlbumList() {

    }

    /**
     * Handle function for the album list. This only calls the abstract method
     * which needs to get implemented by the user of this class.
     * @param msg Message object containing a list of MPDAlbum items.
     */
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        /* Call album response handler */
        List<MPDAlbum> albumList = (List<MPDAlbum>)msg.obj;
        handleAlbums(albumList);
    }

    /**
     * Abstract method to be implemented by the user of the MPD implementation.
     * This should be a callback for the UI thread and run in the UI thread.
     * This can be used for updating lists of adapters and views.
     * @param albumList List of MPDAlbum objects containing a list of mpds album response.
     */
    abstract public void handleAlbums(List<MPDAlbum> albumList);
}
