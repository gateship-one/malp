package andrompd.org.andrompd.application.loaders;


import android.content.Context;
import android.support.v4.content.Loader;

import java.util.List;

import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseFileList;
import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDQueryHandler;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;

public class FilesLoader extends Loader<List<MPDFileEntry>> {
    private FilesResponseHandler mFilesResponseHandler;

    Context mContext;
    String mPath;

    public FilesLoader(Context context, String path) {
        super(context);
        mContext = context;
        mPath = path;
        mFilesResponseHandler = new FilesResponseHandler();
    }


    private class FilesResponseHandler extends MPDResponseFileList {
        @Override
        public void handleTracks(List<MPDFileEntry> fileList, int start, int end) {
            deliverResult(fileList);
        }
    }


    @Override
    public void onStartLoading() {
        forceLoad();
    }

    @Override
    public void onStopLoading() {

    }

    @Override
    public void onReset() {
        deliverResult(null);
    }

    @Override
    public void onForceLoad() {
        MPDQueryHandler.getFiles(mFilesResponseHandler,mPath);
    }
}
