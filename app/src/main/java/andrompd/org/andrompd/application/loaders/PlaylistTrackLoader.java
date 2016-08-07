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

package andrompd.org.andrompd.application.loaders;


import android.content.Context;
import android.support.v4.content.Loader;

import java.util.List;

import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDQueryHandler;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseTrackList;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDFile;

/**
 * Loader class for albumtracks and artist album tracks
 */
public class PlaylistTrackLoader extends Loader<List<MPDFile>> {
    /**
     * Response handler used for the asynchronous callback of the networking thread
     */
    private MPDResponseTrackList pTrackResponseHandler;

    /**
     * Name/Path of the playlist to get. If empty, current playlist is used.
     */
    private String mPlaylistPath;



    /**
     * Creates the loader that retrieves the information from the MPD server
     * @param context Context used
     * @param playlistPath Name of the playlist to fetch. If empty it will use the current one.
     */
    public PlaylistTrackLoader(Context context, String playlistPath) {
        super(context);

        // Create a new Handler for asynchronous callback
        pTrackResponseHandler = new TrackResponseHandler();

        // Set the playlist properties
        mPlaylistPath = playlistPath;
    }


    /**
     * Private class for the response handler.
     */
    private class TrackResponseHandler extends MPDResponseTrackList {
        @Override
        public void handleTracks(List<MPDFile> trackList) {
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
     * In case of reset just start another load.
     */
    @Override
    public void onReset() {
        forceLoad();
    }

    /**
     * Start the actual laoding process. Check if an playlistpath is provided.
     * If not it will just fetch the current playlist.
     */
    @Override
    public void onForceLoad() {
        if ( (null == mPlaylistPath) || mPlaylistPath.equals("")) {
            MPDQueryHandler.getCurrentPlaylist(pTrackResponseHandler);
        } else {
            MPDQueryHandler.getSavedPlaylist(pTrackResponseHandler, mPlaylistPath);
        }
    }
}
