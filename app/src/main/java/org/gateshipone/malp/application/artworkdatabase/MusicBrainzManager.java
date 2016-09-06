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

public class MusicBrainzManager implements ArtistImageProvider, AlbumImageProvider {
    private static final String TAG = MusicBrainzManager.class.getSimpleName();

    private static final String MUSICBRAINZ_API_URL = "http://musicbrainz.org/ws/2";
    private static final String COVERART_ARCHIVE_API_URL = "http://coverartarchive.org";

    private RequestQueue mRequestQueue;

    private static MusicBrainzManager mInstance;

    private static final String MUSICBRAINZ_FORMAT_JSON = "&fmt=json";

    private static final int MUSICBRAINZ_LIMIT_RESULT_COUNT = 10;
    private static final String MUSICBRAINZ_LIMIT_RESULT = "&limit=" + String.valueOf(MUSICBRAINZ_LIMIT_RESULT_COUNT);

    private MusicBrainzManager() {
        mRequestQueue = getRequestQueue();
    }

    public static synchronized MusicBrainzManager getInstance() {
        if (mInstance == null) {
            mInstance = new MusicBrainzManager();
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            Cache cache = new NoCache();
            Network nw = new BasicNetwork(new HurlStack());
            mRequestQueue = new RequestQueue(cache, nw, 1);
            mRequestQueue.start();
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public void fetchArtistImage(final MPDArtist artist, final Response.Listener<Pair<byte[], MPDArtist>> listener, final ArtistFetchError errorListener) {

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

    private void getArtists(String artistName, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        Log.v(MusicBrainzManager.class.getSimpleName(), artistName);

        String url = MUSICBRAINZ_API_URL + "/" + "artist/?query=artist:" + artistName + MUSICBRAINZ_FORMAT_JSON;

        MALPJsonObjectRequest jsonObjectRequest = new MALPJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
    }

    private void getArtistImageURL(String artistMBID, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {

        Log.v(MusicBrainzManager.class.getSimpleName(), artistMBID);

        String url = MUSICBRAINZ_API_URL + "/" + "artist/" + artistMBID + "?inc=url-rels" + MUSICBRAINZ_FORMAT_JSON;

        MALPJsonObjectRequest jsonObjectRequest = new MALPJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
    }

    private void getArtistImage(String url, Response.Listener<Pair<byte[], MPDArtist>> listener, Response.ErrorListener errorListener) {
        Log.v(MusicBrainzManager.class.getSimpleName(), url);

//        Request<byte[]> byteResponse = new ArtistImageByteRequest(url, listener, errorListener);

//        addToRequestQueue(byteResponse);
    }

    @Override
    public void fetchAlbumImage(final MPDAlbum album, final Response.Listener<Pair<byte[], MPDAlbum>> listener, final AlbumFetchError errorListener) {

        if ( album.getMBID().isEmpty()) {
            resolveAlbumMBID(album, listener, errorListener);
        } else {
            Log.v(TAG,"Directly using MPDs MBID");
            String url = COVERART_ARCHIVE_API_URL + "/" + "release/" + album.getMBID() + "/front-500";
            getAlbumImage(url, album, listener, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.v(TAG,"No image found for: " + album.getName() + " with direct MBID use");
                    // Try without MBID from MPD
                    resolveAlbumMBID(album, listener, errorListener);
                }
            });
        }
    }

    private void resolveAlbumMBID( final MPDAlbum album, final Response.Listener<Pair<byte[], MPDAlbum>> listener, final AlbumFetchError errorListener ) {
        Log.v(TAG,"Manually resolving MBID");
        getAlbumMBID(album, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                parseMusicBrainzReleaseJSON(album, 0, response, listener, errorListener);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                errorListener.fetchError(album);
            }
        });
    }

    private void parseMusicBrainzReleaseJSON(final MPDAlbum album, final int releaseIndex, final JSONObject response, final Response.Listener<Pair<byte[], MPDAlbum>> listener, final AlbumFetchError errorListener) {
        if (releaseIndex >= MUSICBRAINZ_LIMIT_RESULT_COUNT) {
            return;
        }

        try {
            JSONArray releases = response.getJSONArray("releases");
            if ( releases.length() > releaseIndex) {
                String mbid = releases.getJSONObject(releaseIndex).getString("id");
                album.setMBID(mbid);

                String url = COVERART_ARCHIVE_API_URL + "/" + "release/" + mbid + "/front-500";

                getAlbumImage(url, album, listener, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.v(TAG,"No image found for: " + album.getName() + " with release index: " + releaseIndex);
                        parseMusicBrainzReleaseJSON(album, releaseIndex+1, response, listener, errorListener);
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    private void getAlbumMBID(MPDAlbum album, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String albumName = Uri.encode(album.getName());
        String artistName = Uri.encode(album.getArtistName());
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

    private void getAlbumImageURL(String releaseMBID, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
        String url = COVERART_ARCHIVE_API_URL + "/" + "release/" + releaseMBID;

        Log.v(TAG, "Requesting release image urls for: " + url);

        MALPJsonObjectRequest jsonObjectRequest = new MALPJsonObjectRequest(Request.Method.GET, url, null, listener, errorListener);

        addToRequestQueue(jsonObjectRequest);
    }

    private void getAlbumImage(String url, MPDAlbum album, Response.Listener<Pair<byte[], MPDAlbum>> listener, Response.ErrorListener errorListener) {
        Request<Pair<byte[], MPDAlbum>> byteResponse = new AlbumImageByteRequest(url, album, listener, errorListener);
        Log.v(TAG,"Get image: " + url);
        addToRequestQueue(byteResponse);
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

}
