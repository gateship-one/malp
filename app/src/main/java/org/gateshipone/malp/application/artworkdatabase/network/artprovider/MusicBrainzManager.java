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

package org.gateshipone.malp.application.artworkdatabase.network.artprovider;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;


import org.gateshipone.malp.application.artworkdatabase.network.responses.AlbumFetchError;
import org.gateshipone.malp.application.artworkdatabase.network.responses.AlbumImageResponse;
import org.gateshipone.malp.application.artworkdatabase.network.responses.ArtistFetchError;
import org.gateshipone.malp.application.artworkdatabase.network.requests.AlbumImageByteRequest;
import org.gateshipone.malp.application.artworkdatabase.network.requests.MALPJsonObjectRequest;
import org.gateshipone.malp.application.artworkdatabase.network.MALPRequestQueue;
import org.gateshipone.malp.application.artworkdatabase.network.responses.ArtistImageResponse;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * FIXME:
 * ArtistImageProvider currently NOT IMPLEMENTED!!!
 */
public class MusicBrainzManager implements AlbumImageProvider {
    private static final String TAG = MusicBrainzManager.class.getSimpleName();

    private static final String LUCENE_SPECIAL_CHARACTERS_REGEX = "([+\\-\\!\\(\\)\\{\\}\\[\\]\\^\\\"\\~\\*\\?\\:\\\\\\/])";

    private static final String MUSICBRAINZ_API_URL = "http://musicbrainz.org/ws/2";
    private static final String COVERART_ARCHIVE_API_URL = "http://coverartarchive.org";

    private RequestQueue mRequestQueue;

    private static MusicBrainzManager mInstance;

    private static final String MUSICBRAINZ_FORMAT_JSON = "&fmt=json";

    private static final int MUSICBRAINZ_LIMIT_RESULT_COUNT = 3;
    private static final String MUSICBRAINZ_LIMIT_RESULT = "&limit=" + String.valueOf(MUSICBRAINZ_LIMIT_RESULT_COUNT);

    private MusicBrainzManager(Context context) {
        mRequestQueue = MALPRequestQueue.getInstance(context);
    }

    public static synchronized MusicBrainzManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new MusicBrainzManager(context);
        }
        return mInstance;
    }



    public <T> void addToRequestQueue(Request<T> req) {
        mRequestQueue.add(req);
    }

    /**
     * Fetch an image for an given {@link MPDArtist}. Make sure to provide response and error listener.
     * @param artist Artist to try to get an image for.
     * @param listener ResponseListener that reacts on successful retrieval of an image.
     * @param errorListener Error listener that is called when an error occurs.
     */
    public void fetchArtistImage(final MPDArtist artist, final Response.Listener<ArtistImageResponse> listener, final ArtistFetchError errorListener) {

        String artistURLName = Uri.encode(artist.getArtistName());

        getArtists(artistURLName, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                JSONArray artists = null;
                try {
                    artists = response.getJSONArray("artists");

                    if (!artists.isNull(0)) {
                        JSONObject artist = artists.getJSONObject(0);
                        String artistMBID = artist.getString("id");

                        getArtistImageURL(artistMBID, new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                JSONArray relations = null;
                                try {
                                    relations = response.getJSONArray("relations");
                                    for (int i = 0; i < relations.length(); i++) {
                                        JSONObject obj = relations.getJSONObject(i);

                                        if (obj.getString("type").equals("image")) {
                                            JSONObject url = obj.getJSONObject("url");

                                            getArtistImage(url.getString("resource"), listener, new Response.ErrorListener() {
                                                @Override
                                                public void onErrorResponse(VolleyError error) {
                                                    // FIXME error handling
                                                }
                                            });
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // FIXME error handling
            }
        });
    }

    /**
     * Searches for the artist with the given artist name and tries to manually get an MBID
     * @param artistName Artist name to search for
     * @param listener Callback to handle the response
     * @param errorListener Callback to handle errors
     */
    private void getArtists(String artistName, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        Log.v(MusicBrainzManager.class.getSimpleName(), artistName);

        String url = MUSICBRAINZ_API_URL + "/" + "artist/?query=artist:" + artistName + MUSICBRAINZ_FORMAT_JSON;

        MALPJsonObjectRequest jsonObjectRequest = new MALPJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
    }

    /**
     * Fetches the image URL for the raw image blob.
     * @param artistMBID Artist mbid to look for an image
     * @param listener Callback listener to handle the response
     * @param errorListener Callback to handle a fetch error
     */
    private void getArtistImageURL(String artistMBID, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        Log.v(MusicBrainzManager.class.getSimpleName(), artistMBID);

        String url = MUSICBRAINZ_API_URL + "/" + "artist/" + artistMBID + "?inc=url-rels" + MUSICBRAINZ_FORMAT_JSON;

        MALPJsonObjectRequest jsonObjectRequest = new MALPJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
    }

    /**
     * Raw download for an image
     * @param url Final image URL to download
     * @param listener Response listener to receive the image as a byte array
     * @param errorListener Error listener
     */
    private void getArtistImage(String url, Response.Listener<ArtistImageResponse> listener, Response.ErrorListener errorListener) {
        Log.v(MusicBrainzManager.class.getSimpleName(), url);

//        Request<byte[]> byteResponse = new ArtistImageByteRequest(url, listener, errorListener);

//        addToRequestQueue(byteResponse);
    }

    /**
     * Public interface to get an image for an album.
     * @param album Album to check for an image
     * @param listener Callback to handle the fetched image
     * @param errorListener Callback to handle errors
     */
    @Override
    public void fetchAlbumImage(final MPDAlbum album, final Response.Listener<AlbumImageResponse> listener, final AlbumFetchError errorListener) {

        if ( album.getMBID().isEmpty()) {
            resolveAlbumMBID(album, listener, errorListener);
        } else {
            String url = COVERART_ARCHIVE_API_URL + "/" + "release/" + album.getMBID() + "/front-500";
            getAlbumImage(url, album, listener, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    // Try without MBID from MPD
                    resolveAlbumMBID(album, listener, errorListener);
                }
            });
        }
    }

    /**
     * Wrapper to manually get an MBID for an {@link MPDAlbum} without an MBID already set
     * @param album Album to search
     * @param listener Callback listener to handle the response
     * @param errorListener Callback to handle lookup errors
     */
    private void resolveAlbumMBID( final MPDAlbum album, final Response.Listener<AlbumImageResponse> listener, final AlbumFetchError errorListener ) {
        getAlbumMBID(album, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                parseMusicBrainzReleaseJSON(album, 0, response, listener, errorListener);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                errorListener.fetchVolleyError(album, error);
            }
        });
    }

    private void parseMusicBrainzReleaseJSON(final MPDAlbum album, final int releaseIndex, final JSONObject response, final Response.Listener<AlbumImageResponse> listener, final AlbumFetchError errorListener) {
        if (releaseIndex >= MUSICBRAINZ_LIMIT_RESULT_COUNT ) {
            errorListener.fetchVolleyError(album, null);
            return;
        }
        try {
            final JSONArray releases = response.getJSONArray("releases");


            if ( releases.length() > releaseIndex) {
                String mbid = releases.getJSONObject(releaseIndex).getString("id");
                String url = COVERART_ARCHIVE_API_URL + "/" + "release/" + mbid + "/front-500";

                getAlbumImage(url, album, listener, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if ( releaseIndex + 1 < releases.length()) {
                            parseMusicBrainzReleaseJSON(album, releaseIndex + 1, response, listener, errorListener);
                        } else {
                            errorListener.fetchVolleyError(album, error);
                        }
                    }
                });
            } else {
                errorListener.fetchVolleyError(album, null);
            }
        } catch (JSONException e) {
            errorListener.fetchJSONException(album,e);
        }

    }

    /**
     * Wrapper to get an MBID out of an {@link MPDAlbum}.
     * @param album Album to get the MBID for
     * @param listener Response listener
     * @param errorListener Error listener
     */
    private void getAlbumMBID(MPDAlbum album, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String albumName = Uri.encode(album.getName());
        albumName = albumName.replaceAll(LUCENE_SPECIAL_CHARACTERS_REGEX, "\\$1");
        String artistName = Uri.encode(album.getArtistName());
        artistName = artistName.replaceAll(LUCENE_SPECIAL_CHARACTERS_REGEX, "\\$1");
        String url;
        if (!artistName.isEmpty()) {
            url = MUSICBRAINZ_API_URL + "/" + "release/?query=release:" + albumName + "%20AND%20artist:" + artistName + MUSICBRAINZ_LIMIT_RESULT + MUSICBRAINZ_FORMAT_JSON;
        } else {
            url = MUSICBRAINZ_API_URL + "/" + "release/?query=release:" + albumName + MUSICBRAINZ_LIMIT_RESULT + MUSICBRAINZ_FORMAT_JSON;
        }

        Log.v(TAG, "Requesting release mbid for: " + url);

        MALPJsonObjectRequest jsonObjectRequest = new MALPJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
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
        addToRequestQueue(byteResponse);
    }


}
