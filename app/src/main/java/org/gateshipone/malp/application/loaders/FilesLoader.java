package org.gateshipone.malp.application.loaders;


import android.content.Context;
import android.support.v4.content.Loader;

import java.util.List;

import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseFileList;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;

/**
 * Loads a list of files, directories and playlists from the MPDQueryHandler
 */
public class FilesLoader extends Loader<List<MPDFileEntry>> {
    private FilesResponseHandler mFilesResponseHandler;

    /**
     * Used context
     */
    Context mContext;

    /**
     * Path to request file entries from
     */
    String mPath;

    public FilesLoader(Context context, String path) {
        super(context);
        mContext = context;
        mPath = path;

        // Response handler for receiving the file list asynchronously
        mFilesResponseHandler = new FilesResponseHandler();
    }


    /**
     * Delivers the results to the GUI thread
     */
    private class FilesResponseHandler extends MPDResponseFileList {
        @Override
        public void handleTracks(List<MPDFileEntry> fileList, int start, int end) {
            deliverResult(fileList);
        }
    }


    /**
     * Start the loader
     */
    @Override
    public void onStartLoading() {
        forceLoad();
    }


    /**
     * Stop the loader
     */
    @Override
    public void onStopLoading() {

    }

    /**
     * Reset the data with an empty data set
     */
    @Override
    public void onReset() {
        deliverResult(null);
    }

    /**
     * Requests the file list from the MPDQueryHandler, it will respond asynchronously
     */
    @Override
    public void onForceLoad() {
        MPDQueryHandler.getFiles(mFilesResponseHandler,mPath);
    }
}
