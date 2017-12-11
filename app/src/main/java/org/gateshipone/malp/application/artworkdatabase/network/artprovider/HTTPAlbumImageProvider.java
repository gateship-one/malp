/*
 *  Copyright (C) 2017 Team Gateship-One
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


import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;

import org.gateshipone.malp.application.artworkdatabase.network.requests.AlbumImageByteRequest;
import org.gateshipone.malp.application.artworkdatabase.network.responses.AlbumFetchError;
import org.gateshipone.malp.application.artworkdatabase.network.responses.AlbumImageResponse;
import org.gateshipone.malp.application.utils.FormatHelper;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;

public class HTTPAlbumImageProvider implements TrackAlbumImageProvider {
    private static final String[] COVER_FILENAMES = {"cover","folder","Cover","Folder"};
    private static final String[] COVER_FILEEXTENSIIONS = {"png","jpg","jpeg","PNG","JPG","JPEG"};

    private static final String TAG = HTTPAlbumImageProvider.class.getSimpleName();

    private static HTTPAlbumImageProvider mInstance;

    private static String mRegex;

    private RequestQueue mRequestQueue;

    private HTTPAlbumImageProvider(Context context) {
        // Don't use MALPRequestQueue because we do not need to limit the load on the local server
        Network network = new BasicNetwork(new HurlStack());
        // 10MB disk cache
        Cache cache = new DiskBasedCache(context.getCacheDir(), 1024 * 1024 * 10);

        mRequestQueue = new RequestQueue(cache, network);
        mRequestQueue.start();
    }

    public static synchronized HTTPAlbumImageProvider getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new HTTPAlbumImageProvider(context);
        }

        return mInstance;
    }

    public void setRegex(String regex) {
        mRegex = regex;
    }

    public boolean getActive() {
        if (mRegex==null || mRegex.isEmpty()) {
            return false;
        }
        return true;
    }

    public String resolveRegex(String path) {
        String result = mRegex;
        Log.v(TAG,"Path without replacement: " + result);

        result = mRegex.replaceAll("%f", FormatHelper.encodeURLUnsafeCharacters(path));
        result = result.replaceAll("%d", FormatHelper.encodeURLUnsafeCharacters(FormatHelper.getDirectoryFromPath(path)));
        Log.v(TAG,"Path to use: " + result);

        return result;
    }

    @Override
    public void fetchAlbumImage(MPDTrack track, Response.Listener<AlbumImageResponse> listener, final AlbumFetchError errorListener) {
        Log.v(TAG,"Try fetching album for track: " + track.getTrackTitle() + " URL: " + track.getPath());

        String url = resolveRegex(track.getPath());

        final MPDAlbum fakeAlbum = new MPDAlbum(track.getTrackAlbum());
        fakeAlbum.setMBID(track.getTrackAlbumMBID());
        fakeAlbum.setArtistName(track.getTrackAlbumArtist());

        // Check if URL ends with a file or directory
        if (url.endsWith("/")) {
            // Directory check all pre-defined files
            for(String filename : COVER_FILENAMES) {
                for (String fileextension: COVER_FILEEXTENSIIONS) {
                    String fileURL = url + filename + '.' + fileextension;
                    getAlbumImage(fileURL, fakeAlbum, listener, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.v(TAG,"Error: " + error.getMessage());
                        }
                    });
                }
            }
        } else {
            // File, just check the file
            getAlbumImage(url, fakeAlbum, listener, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.v(TAG,"Error: " + error.getMessage());
                }
            });
        }
    }

    /**
     * Raw download for an image
     * @param url Final image URL to download
     * @param album Album associated with the image to download
     * @param listener Response listener to receive the image as a byte array
     * @param errorListener Error listener
     */
    private void getAlbumImage(String url, MPDAlbum album, Response.Listener<AlbumImageResponse> listener, Response.ErrorListener errorListener) {
        Request<AlbumImageResponse> byteResponse = new AlbumImageByteRequest(url, album, listener, errorListener);
        Log.v(TAG,"Get image: " + url + " for album: " + album.getName());
        mRequestQueue.add(byteResponse);
    }
}
