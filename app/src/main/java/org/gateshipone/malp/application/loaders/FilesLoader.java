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
     * Requests the file list from the MPDQueryHandler, it will respond asynchronously
     */
    @Override
    public void onForceLoad() {
        MPDQueryHandler.getFiles(mFilesResponseHandler,mPath);
    }
}
