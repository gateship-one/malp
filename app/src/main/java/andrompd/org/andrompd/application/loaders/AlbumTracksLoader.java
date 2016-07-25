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

import andrompd.org.andrompd.mpdservice.handlers.MPDQueryHandler;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseTrackList;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDFile;

/**
 * Loader class for albumtracks and artist album tracks
 */
public class AlbumTracksLoader extends Loader<List<MPDFile>> {
    /**
     * Response handler used for the asynchronous callback of the networking thread
     */
    private MPDResponseTrackList pTrackResponseHandler;

    /**
     * Artist name of this album. Can be left empty
     */
    private String mArtistName;

    /**
     * Name of the album to retrieve
     */
    private String mAlbumName;

    /**
     * Creates the loader that retrieves the information from the MPD server
     * @param context Context used
     * @param albumName Name of the album to retrieve
     * @param artistName Name of the artist of the album to retrieve (can be left empty)
     */
    public AlbumTracksLoader(Context context, String albumName, String artistName) {
        super(context);

        // Create a new Handler for asynchronous callback
        pTrackResponseHandler = new TrackResponseHandler();

        // Set the album properties
        mArtistName = artistName;
        mAlbumName = albumName;
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
     * Start the actual laoding process. Check if an artistname was provided.
     * If fetch the artistalbumtracks otherwise fetch all tracks for a specific album.
     */
    @Override
    public void onForceLoad() {
        if ( (null == mArtistName) || mArtistName.equals("") ) {
            MPDQueryHandler.getAlbumTracks(pTrackResponseHandler,mAlbumName);
        } else {
            MPDQueryHandler.getArtistAlbumTracks(pTrackResponseHandler,mAlbumName,mArtistName);
        }
    }
}
