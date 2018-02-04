/*
 *  Copyright (C) 2018 Team Gateship-One
 *  (Hendrik Borghorst & Frederik Luetkes)
 *
 *  The AUTHORS.md file contains a detailed contributors list:
 *  <https://github.com/gateship-one/malp/blob/master/AUTHORS.md>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.gateshipone.malp.application.artworkdatabase.network.artprovider;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.gateshipone.malp.application.artworkdatabase.network.responses.TrackAlbumFetchError;
import org.gateshipone.malp.application.artworkdatabase.network.responses.TrackAlbumImageResponse;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseAlbumArt;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;

public class MPDAlbumImageProvider implements TrackAlbumImageProvider {

    /**
     * Singleton instance
     */
    public static MPDAlbumImageProvider mInstance = new MPDAlbumImageProvider();

    private boolean mActive;

    private Looper mResponseLooper;

    @Override
    public void fetchAlbumImage(MPDTrack track, Response.Listener<TrackAlbumImageResponse> listener, TrackAlbumFetchError errorListener) {
        if(mResponseLooper == null) {
            return;
        }
        MPDQueryHandler.getAlbumArtwork(track.getPath(), new AlbumArtResponseListener(mResponseLooper, track, listener, errorListener));
    }

    public void setActive(boolean active) {
        mActive = active;
    }

    public boolean getActive() {
        return mActive;
    }

    public void setResponseLooper(Looper looper) {
        mResponseLooper = looper;
    }


    private static class AlbumArtResponseListener extends MPDResponseAlbumArt {

        private MPDTrack mTrack;
        private Response.Listener<TrackAlbumImageResponse> mListener;
        private TrackAlbumFetchError mErrorListener;

        public AlbumArtResponseListener(Looper looper, MPDTrack track, Response.Listener<TrackAlbumImageResponse> listener, TrackAlbumFetchError errorListener) {
            super(looper);
            mTrack = track;
            mListener = listener;
            mErrorListener = errorListener;
        }

        @Override
        public void handleAlbumArt(byte[] artworkData, String url) {
            if (artworkData != null) {
                TrackAlbumImageResponse response = new TrackAlbumImageResponse();
                response.track = mTrack;
                response.image = artworkData;
                response.url = url;
                mListener.onResponse(response);
            } else {
                mErrorListener.fetchVolleyError(mTrack, new VolleyError(new NetworkResponse(404,null,null,true)));
            }
        }
    }
}
