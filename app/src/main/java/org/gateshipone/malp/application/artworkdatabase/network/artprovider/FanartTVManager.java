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

import android.content.Context;
import android.net.Uri;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;

import org.gateshipone.malp.application.artworkdatabase.network.responses.ArtistFetchError;
import org.gateshipone.malp.application.artworkdatabase.network.responses.ArtistImageResponse;
import org.gateshipone.malp.application.artworkdatabase.network.responses.FanartFetchError;
import org.gateshipone.malp.application.artworkdatabase.network.requests.ArtistImageByteRequest;
import org.gateshipone.malp.application.artworkdatabase.network.requests.FanartImageRequest;
import org.gateshipone.malp.application.artworkdatabase.network.requests.MALPJsonObjectRequest;
import org.gateshipone.malp.application.artworkdatabase.network.MALPRequestQueue;
import org.gateshipone.malp.application.artworkdatabase.network.responses.FanartResponse;
import org.gateshipone.malp.application.utils.FormatHelper;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Artwork downloading class for http://fanart.tv. This class provides an interface
 * to download artist images and artist fanart images.
 */
public class FanartTVManager implements ArtistImageProvider, FanartProvider {
    private static final String TAG = FanartTVManager.class.getSimpleName();

    /**
     * API-URL for MusicBrainz database. Used to resolve artist names to MBIDs
     */
    private static final String MUSICBRAINZ_API_URL = "http://musicbrainz.org/ws/2";

    /**
     * API-URL for fanart.tv itself.
     */
    private static final String FANART_TV_API_URL = "http://webservice.fanart.tv/v3/music";

    /**
     * {@link RequestQueue} used to handle the requests of this class.
     */
    private RequestQueue mRequestQueue;

    /**
     * Singleton instance
     */
    private static FanartTVManager mInstance;

    /**
     * constant API url part to instruct MB to return json format
     */
    private static final String MUSICBRAINZ_FORMAT_JSON = "&fmt=json";

    /**
     * Limit the number of results to one. Used for resolving artist names to MBIDs
     */
    private static final int MUSICBRAINZ_LIMIT_RESULT_COUNT = 1;

    /**
     * Constant URL format to limit results
     */
    private static final String MUSICBRAINZ_LIMIT_RESULT = "&limit=" + String.valueOf(MUSICBRAINZ_LIMIT_RESULT_COUNT);

    /**
     * Maximum number of fanart images to return an URL for.
     */
    private static final int FANART_COUNT_LIMIT = 50;

    /**
     * API-Key for used for fanart.tv.
     * THIS KEY IS ONLY INTENDED FOR THE USE BY GATESHIP-ONE APPLICATIONS. PLEASE RESPECT THIS.
     */
    private static final String API_KEY = "c0cc5d1b6e807ce93e49d75e0e5d371b";

    private FanartTVManager(Context context) {
        mRequestQueue = MALPRequestQueue.getInstance(context);
    }

    public static synchronized FanartTVManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new FanartTVManager(context);
        }
        return mInstance;
    }

    /**
     * Fetch an image for an given {@link MPDArtist}. Make sure to provide response and error listener.
     * @param artist Artist to try to get an image for.
     * @param listener ResponseListener that reacts on successful retrieval of an image.
     * @param errorListener Error listener that is called when an error occurs.
     */
    public void fetchArtistImage(final MPDArtist artist, final Response.Listener<ArtistImageResponse> listener, final ArtistFetchError errorListener) {
        // Check if the artist already has MBIDs set.
        if (artist.getMBIDCount() > 0) {
            // Try to use the first MBID set for this artist
            tryArtistMBID(0, artist, listener, errorListener);
        } else {
            // If no MBID is set at this point try to resolve one with musicbrainz database.
            String artistURLName = Uri.encode(artist.getArtistName().replaceAll("/", " "));

            // Get the list of artists "matching" the name.
            getArtists(artistURLName, response -> {
                JSONArray artists = null;
                try {
                    artists = response.getJSONArray("artists");

                    // Only check the first matching artist
                    if (!artists.isNull(0)) {
                        JSONObject artistObj = artists.getJSONObject(0);
                        final String artistMBID = artistObj.getString("id");

                        // Try to get information for this artist from fanart.tv
                        queryArtistMBIDonFanartTV(artistMBID, response1 -> {
                            JSONArray thumbImages = null;
                            try {
                                thumbImages = response1.getJSONArray("artistthumb");

                                JSONObject firstThumbImage = thumbImages.getJSONObject(0);

                                // Get the image for the artist.
                                getArtistImage(firstThumbImage.getString("url"), artist, listener, error -> errorListener.fetchVolleyError(artist, error));

                            } catch (JSONException e) {
                                errorListener.fetchJSONException(artist, e);
                            }
                        }, error -> errorListener.fetchVolleyError(artist, error));
                    }
                } catch (JSONException e) {
                    errorListener.fetchJSONException(artist, e);
                }
            }, error -> errorListener.fetchVolleyError(artist, error));
        }
    }

    /**
     * Recursive method to try all available MBIDs from an {@link MPDArtist}
     * @param mbidIndex Index of the available MBIDs for the given artists
     * @param artist {@link MPDArtist} to check for images
     * @param listener Response listener called when an image is found
     * @param errorListener Error listener called when an error occurs during communication
     */
    private void tryArtistMBID(final int mbidIndex, final MPDArtist artist, final Response.Listener<ArtistImageResponse> listener, final ArtistFetchError errorListener) {
        // Check if recursive call ends here.
        if (mbidIndex < artist.getMBIDCount()) {
            // Query fanart.tv for this MBID
            queryArtistMBIDonFanartTV(artist.getMBID(0), response -> {
                JSONArray thumbImages = null;
                try {
                    thumbImages = response.getJSONArray("artistthumb");

                    JSONObject firstThumbImage = thumbImages.getJSONObject(0);
                    getArtistImage(firstThumbImage.getString("url"), artist, listener, error -> {
                        // If we have multiple artist mbids try the next one
                        if (mbidIndex + 1 < artist.getMBIDCount()) {
                            tryArtistMBID(mbidIndex + 1, artist, listener, errorListener);
                        } else {
                            // All tried
                            errorListener.fetchVolleyError(artist, null);
                        }
                    });

                } catch (JSONException e) {
                    // If we have multiple artist mbids try the next one
                    if (mbidIndex + 1 < artist.getMBIDCount()) {
                        tryArtistMBID(mbidIndex + 1, artist, listener, errorListener);
                    } else {
                        // All tried
                        errorListener.fetchJSONException(artist, e);
                    }
                }
            }, error -> errorListener.fetchVolleyError(artist, error));
        }
    }

    /**
     * Gets a list of possible artists from Musicbrainz database.
     * @param artistName Name of the artist to search for
     * @param listener Response listener to handle the artist list
     * @param errorListener Error listener
     */
    private void getArtists(String artistName, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        if ( artistName == null || artistName.isEmpty() ) {
            // Cancel the nonsense here
            return;
        }

        String queryArtist = FormatHelper.escapeSpecialCharsLucene(artistName);
        String url = MUSICBRAINZ_API_URL + "/" + "artist/?query=artist:" + queryArtist + MUSICBRAINZ_LIMIT_RESULT + MUSICBRAINZ_FORMAT_JSON;
        MALPJsonObjectRequest jsonObjectRequest = new MALPJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        mRequestQueue.add(jsonObjectRequest);
    }

    /**
     * Retrieves all available information for an artist with an MBID of fanart.tv
     * @param artistMBID Artists MBID to query
     * @param listener Response listener to handle the artists information from fanart.tv
     * @param errorListener Error listener
     */
    private void queryArtistMBIDonFanartTV(String artistMBID, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        String url = FANART_TV_API_URL + "/" + artistMBID + "?api_key=" + API_KEY;

        MALPJsonObjectRequest jsonObjectRequest = new MALPJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        mRequestQueue.add(jsonObjectRequest);
    }

    /**
     * Raw download for an image-
     * @param url Final image URL to download
     * @param artist Artist associated with the image to download
     * @param listener Response listener to receive the image as a byte array
     * @param errorListener Error listener
     */
    private void getArtistImage(String url, MPDArtist artist, Response.Listener<ArtistImageResponse> listener, Response.ErrorListener errorListener) {
        Request<ArtistImageResponse> byteResponse = new ArtistImageByteRequest(url, artist, listener, errorListener);

        mRequestQueue.add(byteResponse);
    }

    /**
     * Wrapper to get an artist out of an {@link MPDTrack}.
     * @param track Track to get artist information for
     * @param listener Response listener
     * @param errorListener Error listener
     */
    @Override
    public void getTrackArtistMBID(final MPDTrack track, final Response.Listener<String> listener, final FanartFetchError errorListener) {
        // Create a dummy artist
        final MPDArtist artist;
        if (!track.getTrackAlbumArtist().isEmpty()) {
            artist = new MPDArtist(track.getTrackAlbumArtist());
        } else {
            artist = new MPDArtist(track.getTrackArtist());
        }

        if (!track.getTrackAlbumArtistMBID().isEmpty()) {
            artist.addMBID(track.getTrackAlbumArtistMBID());
        }

        getArtists(Uri.encode(artist.getArtistName()), response -> {
            JSONArray artists = null;
            try {
                artists = response.getJSONArray("artists");

                if (!artists.isNull(0)) {
                    JSONObject artistObj = artists.getJSONObject(0);
                    final String artistMBID = artistObj.getString("id");
                    artist.addMBID(artistMBID);
                    listener.onResponse(artistMBID);
                }
            } catch (JSONException e) {
                errorListener.fanartFetchError(track);
            }
        }, error -> errorListener.fanartFetchError(track));

    }

    /**
     * Retrieves a list of fanart image urls for the given MBID.
     * @param mbid MBID to get fanart images for.
     * @param listener Response listener to handle the URL list retrieved by this method
     * @param errorListener Error listener
     */
    @Override
    public void getArtistFanartURLs(String mbid, final Response.Listener<List<String>> listener, final FanartFetchError errorListener) {
        queryArtistMBIDonFanartTV(mbid, response -> {
            JSONArray backgroundImages = null;
            try {
                backgroundImages = response.getJSONArray("artistbackground");
                if (backgroundImages.length() == 0) {
                    errorListener.imageListFetchError();
                } else {
                    ArrayList<String> urls = new ArrayList<>();
                    for (int i = 0; i < backgroundImages.length() && i < FANART_COUNT_LIMIT; i++) {
                        JSONObject image = backgroundImages.getJSONObject(i);
                        urls.add(image.getString("url"));
                    }
                    listener.onResponse(urls);
                }
            } catch (JSONException exception) {

            }
        }, Throwable::printStackTrace);
    }

    /**
     * Raw image download to download fanart images
     * @param track Track for the associated image
     * @param url URL to download
     * @param listener Listener to handle the downloaded image as a byte response.
     * @param errorListener Error listener
     */
    @Override
    public void getFanartImage(MPDTrack track, String url, Response.Listener<FanartResponse> listener, Response.ErrorListener errorListener) {
        Request<FanartResponse> byteResponse = new FanartImageRequest(url, track, listener, errorListener);

        mRequestQueue.add(byteResponse);
    }


}
