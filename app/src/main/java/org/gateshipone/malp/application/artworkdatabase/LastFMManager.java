/*
 * Copyright (C) 2016  Hendrik Borghorst & Frederik Luetkes
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
 *
 */
package org.gateshipone.malp.application.artworkdatabase;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.NoCache;

import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LastFMManager implements ArtistImageProvider, AlbumImageProvider {
    private static final String TAG = LastFMManager.class.getSimpleName();

    private static final String LAST_FM_API_URL = "http://ws.audioscrobbler.com/2.0/?method=";
    private static final String API_KEY = "8de46d96e49e78234f206fd9f21712de";

    private static final String LAST_FM_FORMAT_JSON = "&format=json";

    private static final String LAST_FM_REQUESTED_IMAGE_SIZE = "extralarge";

    private RequestQueue mRequestQueue;


    private static LastFMManager mInstance;

    private LastFMManager(Context context) {
        mRequestQueue = MALPRequestQueue.getInstance(context);
    }

    public static synchronized LastFMManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new LastFMManager(context);
        }
        return mInstance;
    }



    public <T> void addToRequestQueue(Request<T> req) {
        mRequestQueue.add(req);
    }

    public void fetchArtistImage(final MPDArtist artist, final Response.Listener<Pair<byte[], MPDArtist>> listener, final ArtistFetchError errorListener) {


        String artistURLName = Uri.encode(artist.getArtistName().replaceAll("/", " "));

        getArtistImageURL(artistURLName, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject artistObj = response.getJSONObject("artist");
                    // FIXME optionally get mbid here without aborting the image fetch
                    JSONArray images = artistObj.getJSONArray("image");
                    for (int i = 0; i < images.length(); i++) {
                        JSONObject image = images.getJSONObject(i);
                        if (image.getString("size").equals(LAST_FM_REQUESTED_IMAGE_SIZE)) {
                            String url = image.getString("#text");
                            if (!url.isEmpty()) {
                                getArtistImage(image.getString("#text"), artist, listener, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        errorListener.fetchError(artist);
                                    }
                                });
                            } else {
                                errorListener.fetchError(artist);
                            }
                        }
                    }
                } catch (JSONException e) {
                    errorListener.fetchError(artist);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                errorListener.fetchError(artist);
            }
        });

    }

    @Override
    public void cancelAll() {
        mRequestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }


    private void getArtistImageURL(String artistName, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {


        String url = LAST_FM_API_URL + "artist.getinfo&artist=" + artistName + "&api_key=" + API_KEY + LAST_FM_FORMAT_JSON;
        Log.v(TAG, url);

        MALPJsonObjectRequest jsonObjectRequest = new MALPJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
    }

    private void getArtistImage(String url, MPDArtist artist, Response.Listener<Pair<byte[], MPDArtist>> listener, Response.ErrorListener errorListener) {
        Log.v(LastFMManager.class.getSimpleName(), url);

        Request<Pair<byte[], MPDArtist>> byteResponse = new ArtistImageByteRequest(url, artist, listener, errorListener);

        addToRequestQueue(byteResponse);
    }


    @Override
    public void fetchAlbumImage(final MPDAlbum album, final Response.Listener<Pair<byte[], MPDAlbum>> listener, final AlbumFetchError errorListener) {
        getAlbumImageURL(album, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject albumObj = response.getJSONObject("album");
                    JSONArray images = albumObj.getJSONArray("image");
                    // FIXME optionally get mbid here without aborting the image fetch
                    for (int i = 0; i < images.length(); i++) {
                        JSONObject image = images.getJSONObject(i);
                        if (image.getString("size").equals(LAST_FM_REQUESTED_IMAGE_SIZE)) {
                            String url = image.getString("#text");
                            if (!url.isEmpty()) {
                                getAlbumImage(image.getString("#text"), album, listener, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        errorListener.fetchError(album);
                                    }
                                });
                            } else {
                                errorListener.fetchError(album);
                            }

                        }
                    }
                } catch (JSONException e) {
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
    }

    private void getAlbumImageURL(MPDAlbum album, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String albumName = Uri.encode(album.getName());
        String artistName = Uri.encode(album.getArtistName());

        String url = LAST_FM_API_URL + "album.getinfo&album=" + albumName + "&artist=" + artistName + "&api_key=" + API_KEY + LAST_FM_FORMAT_JSON;
        Log.v(TAG, url);

        MALPJsonObjectRequest jsonObjectRequest = new MALPJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
    }

    private void getAlbumImage(String url, MPDAlbum album, Response.Listener<Pair<byte[], MPDAlbum>> listener, Response.ErrorListener errorListener) {
        Log.v(LastFMManager.class.getSimpleName(), url);

        Request<Pair<byte[], MPDAlbum>> byteResponse = new AlbumImageByteRequest(url, album, listener, errorListener);

        addToRequestQueue(byteResponse);
    }
}
