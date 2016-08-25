/*
 * Copyright (C) 2016  Hendrik Borghorst
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gateshipone.malp.application.loaders;


import android.content.Context;
import android.support.v4.content.Loader;

import java.util.List;

import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseFileList;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.MPDCommands;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;

/**
 * Loader class for search result tracks
 */
public class SearchResultLoader extends Loader<List<MPDFileEntry>> {
    /**
     * Response handler used for the asynchronous callback of the networking thread
     */
    private MPDResponseFileList pTrackResponseHandler;

    /**
     * String to instruct the server to search for
     */
    private String mSearchString;

    /**
     * Type of the requested results
     */
    private MPDCommands.MPD_SEARCH_TYPE mSearchType;


    public SearchResultLoader(Context context, String searchTerm, MPDCommands.MPD_SEARCH_TYPE type) {
        super(context);

        // Create a new Handler for asynchronous callback
        pTrackResponseHandler = new TrackResponseHandler();

        // Set the playlist properties
        mSearchString = searchTerm;
        mSearchType = type;
    }


    /**
     * Private class for the response handler.
     */
    private class TrackResponseHandler extends MPDResponseFileList {
        @Override
        public void handleTracks(List<MPDFileEntry> trackList, int start, int end) {
            deliverResult(trackList);
        }
    }


    /**
     * Starts the loading process
     */
    @Override
    public void onStartLoading() {
        forceLoad();
    }

    /**
     * When the loader is stopped
     */
    @Override
    public void onStopLoading() {

    }

    /**
     * Start the actual laoding process. Check if an playlistpath is provided.
     * If not it will just fetch the current playlist.
     */
    @Override
    public void onForceLoad() {
        if (null != mSearchString && !mSearchString.isEmpty()) {
            MPDQueryHandler.searchFiles(mSearchString, mSearchType, pTrackResponseHandler);
        }
    }
}
