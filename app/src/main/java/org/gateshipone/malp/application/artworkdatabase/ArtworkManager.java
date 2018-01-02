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

package org.gateshipone.malp.application.artworkdatabase;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.artworkdatabase.network.MALPRequestQueue;
import org.gateshipone.malp.application.artworkdatabase.network.artprovider.FanartTVManager;
import org.gateshipone.malp.application.artworkdatabase.network.artprovider.HTTPAlbumImageProvider;
import org.gateshipone.malp.application.artworkdatabase.network.artprovider.LastFMManager;
import org.gateshipone.malp.application.artworkdatabase.network.artprovider.MusicBrainzManager;
import org.gateshipone.malp.application.artworkdatabase.network.responses.AlbumFetchError;
import org.gateshipone.malp.application.artworkdatabase.network.responses.AlbumImageResponse;
import org.gateshipone.malp.application.artworkdatabase.network.responses.ArtistFetchError;
import org.gateshipone.malp.application.artworkdatabase.network.responses.ArtistImageResponse;
import org.gateshipone.malp.application.artworkdatabase.network.responses.TrackAlbumFetchError;
import org.gateshipone.malp.application.artworkdatabase.network.responses.TrackAlbumImageResponse;
import org.gateshipone.malp.application.utils.BitmapUtils;
import org.gateshipone.malp.application.utils.FormatHelper;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseAlbumList;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseArtistList;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseFileList;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ArtworkManager implements ArtistFetchError, AlbumFetchError {
    private static final String TAG = ArtworkManager.class.getSimpleName();

    /**
     * Maximmum size for either x or y of an image
     */
    private static final int MAXIMUM_IMAGE_RESOLUTION = 500;

    /**
     * Compression level if images are rescaled
     */
    private static final int IMAGE_COMPRESSION_SETTING = 80;

    /**
     * Maximum size of an image blob to insert in SQLite database. (1MB)
     */
    private static final int MAXIMUM_IMAGE_SIZE = 1024*1024;

    /**
     * Manager for the SQLite database handling
     */
    private ArtworkDatabaseManager mDBManager;

    /**
     * List of observers that needs updating if a new ArtistImage is downloaded.
     */
    private final ArrayList<onNewArtistImageListener> mArtistListeners;

    /**
     * List of observers that needs updating if a new AlbumImage is downloaded.
     */
    private final ArrayList<onNewAlbumImageListener> mAlbumListeners;

    /**
     * Private static singleton instance that can be used by other classes via the
     * getInstance method.
     */
    private static ArtworkManager mInstance;

    /**
     * Private {@link Context} used for all kinds of things like Broadcasts.
     * It is using the ApplicationContext so it should be safe against
     * memory leaks.
     */
    private Context mContext;

    /**
     * Lists of {@link MPDAlbum} objects used for bulk downloading.
     */
    private final List<MPDAlbum> mAlbumList = new ArrayList<>();

    /**
     * Lists of {@link MPDArtist} objects used for bulk downloading.
     */
    private final List<MPDArtist> mArtistList = new ArrayList<>();

    /**
     * Lists of {@link MPDTrack} objects used for bulk downloading.
     */
    private final List<MPDTrack> mTrackList = new ArrayList<>();

    /**
     * Current {@link MPDAlbum} handled by the bulk downloading
     */
    private MPDAlbum mCurrentBulkAlbum = null;

    /**
     * Current {@link MPDArtist} handled by the bulk downloading
     */
    private MPDArtist mCurrentBulkArtist = null;

    /**
     * Current {@link MPDTrack} handled by the bulk downloading
     */
    private MPDTrack mCurrentBulkTrack = null;

    /**
     * Callback for the bulkdownload observer (s. {@link BulkDownloadService})
     */
    private BulkLoadingProgressCallback mBulkProgressCallback;

    /**
     * Settings string which artist download provider to use
     */
    private String mArtistProvider;

    /**
     * Settings string which album download provider to use
     */
    private String mAlbumProvider;

    /**
     * Settings value if artwork download is only allowed via wifi/wired connection.
     */
    private boolean mWifiOnly;

    /**
     * Set when the list of albums for the bulk loading is ready to be processed
     */
    private boolean mBulkLoadAlbumsReady;

    /**
     * Set when the list of artists for the bulk loading is ready to be processed
     */
    private boolean mBulkLoadArtistsReady;


    /*
     * Broadcast constants
     */
    public static final String ACTION_NEW_ARTWORK_READY = "org.gateshipone.malp.action_new_artwork_ready";

    public static final String INTENT_EXTRA_KEY_ARTIST_MBID = "org.gateshipone.malp.extra.artist_mbid";
    public static final String INTENT_EXTRA_KEY_ARTIST_NAME = "org.gateshipone.malp.extra.artist_name";

    public static final String INTENT_EXTRA_KEY_ALBUM_MBID = "org.gateshipone.malp.extra.album_mbid";
    public static final String INTENT_EXTRA_KEY_ALBUM_NAME = "org.gateshipone.malp.extra.album_name";

    private ArtworkManager(Context context) {

        mDBManager = ArtworkDatabaseManager.getInstance(context.getApplicationContext());

        mArtistListeners = new ArrayList<>();
        mAlbumListeners = new ArrayList<>();


        mContext = context.getApplicationContext();

        ConnectionStateReceiver receiver = new ConnectionStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(receiver, filter);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        mArtistProvider = sharedPref.getString(context.getString(R.string.pref_artist_provider_key), context.getString(R.string.pref_artwork_provider_artist_default));
        mAlbumProvider = sharedPref.getString(context.getString(R.string.pref_album_provider_key), context.getString(R.string.pref_artwork_provider_album_default));
        mWifiOnly = sharedPref.getBoolean(context.getString(R.string.pref_download_wifi_only_key), context.getResources().getBoolean(R.bool.pref_download_wifi_default));
    }

    public static synchronized ArtworkManager getInstance(Context context) {
        if (null == mInstance) {
            mInstance = new ArtworkManager(context);
        }
        return mInstance;
    }

    public void setWifiOnly(boolean wifiOnly) {
        mWifiOnly = wifiOnly;
    }

    public void setAlbumProvider(String albumProvider) {
        mAlbumProvider = albumProvider;
    }

    public void setArtistProvider(String artistProvider) {
        mArtistProvider = artistProvider;
    }

    public void initialize(String artistProvider, String albumProvider, boolean wifiOnly) {
        mArtistProvider = artistProvider;
        mAlbumProvider = albumProvider;
        mWifiOnly = wifiOnly;
    }


    /**
     * Removes the image for the album and tries to reload it from the internet
     *
     * @param album {@link MPDAlbum} to reload the image for
     */
    public void resetAlbumImage(final MPDAlbum album) {
        if (null == album) {
            return;
        }

        // Clear the old image
        mDBManager.removeAlbumImage(mContext, album);

        // Reload the image from the internet
        fetchAlbumImage(album);
    }


    /**
     * Removes the image for the artist and tries to reload it from the internet
     *
     * @param artist {@link MPDArtist} to reload the image for
     */
    public void resetArtistImage(final MPDArtist artist) {
        if (null == artist) {
            return;
        }

        // Clear the old image
        mDBManager.removeArtistImage(mContext, artist);

        // Reload the image from the internet
        fetchArtistImage(artist);
    }

    /**
     * Returns an artist image for the given artist.
     *
     * @param artist {@link MPDArtist} to get the image for-
     * @return The image if found or null if it is not available and has been tried to download before.
     * @throws ImageNotFoundException If the image is not found and was not searched before.
     */
    public Bitmap getArtistImage(final MPDArtist artist, int width, int height, boolean skipCache) throws ImageNotFoundException {
        if (null == artist) {
            return null;
        }

        if(!skipCache) {
            // Try cache first
            Bitmap cacheBitmap = BitmapCache.getInstance().requestArtistImage(artist);
            if (cacheBitmap != null && width <= cacheBitmap.getWidth() && height <= cacheBitmap.getWidth()) {
                return cacheBitmap;
            }
        }


        String image = null;

        /**
         * If no artist id is set for the album (possible with data set of Odyssey) check
         * the artist with name instead of id.
         */
        if (artist.getMBIDCount() != 0) {
            image = mDBManager.getArtistImage(mContext, artist);
        } else if (!artist.getArtistName().isEmpty()) {
            image = mDBManager.getArtistImage(mContext, artist.getArtistName());
        }


        // Checks if the database has an image for the requested artist
        if (null != image) {
            // Create a bitmap from the data blob in the database
            Bitmap bm = BitmapUtils.decodeSampledBitmapFromFile(image, width, height);
            BitmapCache.getInstance().putArtistImage(artist, bm);
            return bm;
        }
        return null;
    }

    /**
     * Returns an album image for the given album.
     *
     * @param mbid MusicBrainzID for the given album.
     * @return The image if found or null if it is not available and has been tried to download before.
     * @throws ImageNotFoundException If the image is not found and was not searched before.
     */
    public Bitmap getAlbumImageFromMBID(final String mbid, int width, int height, boolean skipCache) throws ImageNotFoundException {
        if (null == mbid) {
            return null;
        }

        if(!skipCache) {
            // Try cache first
            Bitmap cacheBitmap = BitmapCache.getInstance().requestAlbumBitmapMBID(mbid);
            if (cacheBitmap != null && width <= cacheBitmap.getWidth() && height <= cacheBitmap.getWidth()) {
                return cacheBitmap;
            }
        }

        String image;

        image = mDBManager.getAlbumImageFromMBID(mContext, mbid);

        // Checks if the database has an image for the requested album
        if (null != image) {
            // Create a bitmap from the data blob in the database
            Bitmap bm = BitmapUtils.decodeSampledBitmapFromFile(image, width, height);
            BitmapCache.getInstance().putAlbumBitmapMBID(mbid,bm);
            return bm;
        }
        return null;
    }

    /**
     * Returns an album image for the given album name and artist name.
     *
     * @param albumName  Name of the album to look for
     * @param artistName Name of the albums artists
     * @return The image if found or null if it is not available and has been tried to download before.
     * @throws ImageNotFoundException If the image is not found and was not searched before.
     */
    public Bitmap getAlbumImageFromAlbumNameArtistName(final String albumName, final String artistName, int width, int height, boolean skipCache) throws ImageNotFoundException {
        if (null == albumName || null == artistName) {
            return null;
        }

        if(!skipCache) {
            // Try cache first
            Bitmap cacheBitmap = BitmapCache.getInstance().requestAlbumBitmap(albumName, artistName);
            if (cacheBitmap != null && width <= cacheBitmap.getWidth() && height <= cacheBitmap.getWidth()) {
                return cacheBitmap;
            }
        }

        String image;


        image = mDBManager.getAlbumImage(mContext, albumName, artistName);

        // Checks if the database has an image for the requested album
        if (null != image) {
            // Create a bitmap from the data blob in the database
            Bitmap bm = BitmapUtils.decodeSampledBitmapFromFile(image, width, height);
            BitmapCache.getInstance().putAlbumBitmap(albumName, artistName, bm);
            return bm;
        }
        return null;
    }

    /**
     * Returns an album image for the given album name.
     *
     * @param albumName Name of the album to look for
     * @return The image if found or null if it is not available and has been tried to download before.
     * @throws ImageNotFoundException If the image is not found and was not searched before.
     */
    public Bitmap getAlbumImageFromName(final String albumName, int width, int height) throws ImageNotFoundException {
        if (null == albumName) {
            return null;
        }

        String image;

        image = mDBManager.getAlbumImage(mContext, albumName);

        // Checks if the database has an image for the requested album
        if (null != image) {
            // Create a bitmap from the data blob in the database
            return BitmapUtils.decodeSampledBitmapFromFile(image, width, height);
        }
        return null;
    }

    /**
     * Returns an album image for the given track.
     *
     * @param track {@link MPDTrack} to get the album image for.
     * @return The image if found or null if it is not available and has been tried to download before.
     * @throws ImageNotFoundException If the image is not found and was not searched before.
     */
    public Bitmap getAlbumImageForTrack(final MPDTrack track, int width, int height, boolean skipCache) throws ImageNotFoundException {
        if (null == track) {
            return null;
        }
        Bitmap image = null;
        if (!track.getTrackAlbumMBID().isEmpty()) {
            try {
                image = getAlbumImageFromMBID(track.getTrackAlbumMBID(), width, height, skipCache);
            } catch (ImageNotFoundException e) {
            }
            if (null != image) {
                return image;
            }
        }

        // Try to get image from Albumname/Album artistname
        try {
            image = getAlbumImageFromAlbumNameArtistName(track.getTrackAlbum(), track.getTrackAlbumArtist(), width, height, skipCache);
        } catch (ImageNotFoundException e) {
        }
        if (null != image) {
            return image;
        }

        try {
            image = getAlbumImageFromAlbumNameArtistName(track.getTrackAlbum(), track.getTrackArtist(), width, height, skipCache);
        } catch (ImageNotFoundException e) {
        }
        if (null != image) {
            return image;
        }

        // Last resort, try just the name
        image = getAlbumImageFromName(track.getTrackAlbum(), width, height);

        if (null != image) {
            return image;
        } else {
            return null;
        }

    }

    /**
     * Returns an album image for the given {@link MPDAlbum}
     *
     * @param album {@link MPDAlbum} to get the image for.
     * @return The image if found or null if it is not available and has been tried to download before.
     * @throws ImageNotFoundException If the image is not found and was not searched before.
     */
    public Bitmap getAlbumImage(final MPDAlbum album, int width, int height, boolean skipCache) throws ImageNotFoundException {
        if (null == album) {
            return null;
        }

        if(!skipCache) {
            // Try cache first
            Bitmap cacheBitmap = BitmapCache.getInstance().requestAlbumBitmap(album);
            if (null != cacheBitmap && width <= cacheBitmap.getWidth() && height <= cacheBitmap.getWidth()) {
                return cacheBitmap;
            }
        }

        String image;

        if (album.getMBID().isEmpty()) {
            // Check if ID is available (should be the case). If not use the album name for
            // lookup.
            // FIXME use artistname also
            image = mDBManager.getAlbumImage(mContext, album.getName());
        } else {
            // If id is available use it.
            image = mDBManager.getAlbumImage(mContext, album);
        }

        // Checks if the database has an image for the requested album
        if (null != image) {
            // Create a bitmap from the data blob in the database
            Bitmap bm = BitmapUtils.decodeSampledBitmapFromFile(image, width, height);
            BitmapCache.getInstance().putAlbumBitmap(album, bm);
            return bm;
        }
        return null;
    }

    /**
     * Starts an asynchronous fetch for the image of the given artist.
     *
     * @param artist Artist to fetch an image for.
     */
    public void fetchArtistImage(final MPDArtist artist) {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null) {
            return;
        }
        boolean isWifi = networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET;

        if (mWifiOnly && !isWifi) {
            return;
        }


        if (mArtistProvider.equals(mContext.getString(R.string.pref_artwork_provider_lastfm_key))) {
            LastFMManager.getInstance(mContext).fetchArtistImage(artist, new Response.Listener<ArtistImageResponse>() {
                @Override
                public void onResponse(ArtistImageResponse response) {
                    new InsertArtistImageTask().execute(response);
                }
            }, this);
        } else if (mArtistProvider.equals(mContext.getString(R.string.pref_artwork_provider_fanarttv_key))) {
            FanartTVManager.getInstance(mContext).fetchArtistImage(artist, new Response.Listener<ArtistImageResponse>() {
                @Override
                public void onResponse(ArtistImageResponse response) {
                    new InsertArtistImageTask().execute(response);
                }
            }, this);
        }
    }

    /**
     * Starts an asynchronous fetch for the image of the given album
     *
     * @param album Album to fetch an image for.
     */
    public void fetchAlbumImage(final MPDAlbum album) {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null) {
            return;
        }
        boolean isWifi = networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET;

        if (mWifiOnly && !isWifi) {
            return;
        }

        if (mAlbumProvider.equals(mContext.getString(R.string.pref_artwork_provider_musicbrainz_key))) {
            MusicBrainzManager.getInstance(mContext).fetchAlbumImage(album, new Response.Listener<AlbumImageResponse>() {
                @Override
                public void onResponse(AlbumImageResponse response) {
                    new InsertAlbumImageTask().execute(response);
                }
            }, this);
        } else if (mAlbumProvider.equals(mContext.getString(R.string.pref_artwork_provider_lastfm_key))) {
            LastFMManager.getInstance(mContext).fetchAlbumImage(album, new Response.Listener<AlbumImageResponse>() {
                @Override
                public void onResponse(AlbumImageResponse response) {
                    new InsertAlbumImageTask().execute(response);
                }
            }, this);
        }
    }

    /**
     * Starts an asynchronous fetch for the image of the given album
     *
     * @param track Track to be used for image fetching
     */
    public void fetchAlbumImage(final MPDTrack track) {
        // Create a dummy album
        MPDAlbum album = new MPDAlbum(track.getTrackAlbum());
        album.setMBID(track.getTrackAlbumMBID());
        album.setArtistName(track.getTrackAlbumArtist());


        // Check if user-specified HTTP cover download is activated
        if (HTTPAlbumImageProvider.getInstance(mContext).getActive()) {
            HTTPAlbumImageProvider.getInstance(mContext).fetchAlbumImage(track, new Response.Listener<TrackAlbumImageResponse>() {
                @Override
                public void onResponse(TrackAlbumImageResponse response) {
                    new InsertTrackAlbumImageTask().execute(response);
                }
            }, new TrackAlbumFetchError() {
                @Override
                public void fetchJSONException(MPDTrack track, JSONException exception) {

                }

                @Override
                public void fetchVolleyError(MPDTrack track, VolleyError error) {
                    Log.v(TAG,"Local HTTP download failed, try user-selected download provider");
                    MPDAlbum album = new MPDAlbum(track.getTrackAlbum());
                    album.setMBID(track.getTrackAlbumMBID());
                    album.setArtistName(track.getTrackAlbumArtist());
                    fetchAlbumImage(album);
                    synchronized (mTrackList) {
                        if (!mTrackList.isEmpty()) {
                            fetchNextBulkTrackAlbum();
                        }
                    }
                }
            });
        } else {
            // Use the dummy album to fetch the image
            fetchAlbumImage(album);
        }
    }

    /**
     * Registers a listener that gets notified when a new artist image was added to the dataset.
     *
     * @param listener Listener to register
     */
    public void registerOnNewArtistImageListener(onNewArtistImageListener listener) {
        if (null != listener) {
            synchronized (mArtistListeners) {
                mArtistListeners.add(listener);
            }
        }
    }

    /**
     * Unregisters a listener that got notified when a new artist image was added to the dataset.
     *
     * @param listener Listener to unregister
     */
    public void unregisterOnNewArtistImageListener(onNewArtistImageListener listener) {
        if (null != listener) {
            synchronized (mArtistListeners) {
                mArtistListeners.remove(listener);
            }
        }
    }

    /**
     * Registers a listener that gets notified when a new album image was added to the dataset.
     *
     * @param listener Listener to register
     */
    public void registerOnNewAlbumImageListener(onNewAlbumImageListener listener) {
        if (null != listener) {
            synchronized (mArtistListeners) {
                mAlbumListeners.add(listener);
            }
        }
    }

    /**
     * Unregisters a listener that got notified when a new album image was added to the dataset.
     *
     * @param listener Listener to unregister
     */
    public void unregisterOnNewAlbumImageListener(onNewAlbumImageListener listener) {
        if (null != listener) {
            synchronized (mArtistListeners) {
                mAlbumListeners.remove(listener);
            }
        }
    }


    /**
     * Interface implementation to handle errors during fetching of album images
     *
     * @param album Album that resulted in a fetch error
     */
    public void fetchJSONException(MPDAlbum album, JSONException exception) {
        Log.e(TAG, "Error fetching album: " + album.getName() + "-" + album.getArtistName());
        AlbumImageResponse imageResponse = new AlbumImageResponse();
        imageResponse.album = album;
        imageResponse.image = null;
        imageResponse.url = null;
        new InsertAlbumImageTask().execute(imageResponse);
    }

    /**
     * Called if a volley error occurs during internet communication.
     *
     * @param album {@link MPDAlbum} the error occured for.
     * @param error {@link VolleyError} that was emitted
     */
    public void fetchVolleyError(MPDAlbum album, VolleyError error) {
        Log.e(TAG, "VolleyError for album: " + album.getName() + "-" + album.getArtistName());

        if (error != null) {
            NetworkResponse networkResponse = error.networkResponse;
            /**
             * Rate limit probably reached. Discontinue downloading to prevent
             * ban on the servers.
             */
            if (networkResponse != null && networkResponse.statusCode == 503) {
                mAlbumList.clear();
                cancelAllRequests();
                boolean isEmpty;
                synchronized (mArtistList) {
                    isEmpty = mArtistList.isEmpty();
                }
                if (isEmpty && mBulkProgressCallback != null) {
                    mBulkProgressCallback.finishedLoading();
                }
                return;
            }
        }

        AlbumImageResponse imageResponse = new AlbumImageResponse();
        imageResponse.album = album;
        imageResponse.image = null;
        imageResponse.url = null;
        new InsertAlbumImageTask().execute(imageResponse);
    }

    /**
     * Interface implementation to handle errors during fetching of artist images
     *
     * @param artist Artist that resulted in a fetch error
     */
    public void fetchJSONException(MPDArtist artist, JSONException exception) {
        Log.e(TAG, "Error fetching artist: " + artist.getArtistName());
        ArtistImageResponse imageResponse = new ArtistImageResponse();
        imageResponse.artist = artist;
        imageResponse.image = null;
        imageResponse.url = null;
        new InsertArtistImageTask().execute(imageResponse);
    }

    /**
     * Called if a volley error occurs during internet communication.
     *
     * @param artist {@link MPDArtist} the error occured for.
     * @param error  {@link VolleyError} that was emitted
     */
    public void fetchVolleyError(MPDArtist artist, VolleyError error) {
        Log.e(TAG, "VolleyError fetching: " + artist.getArtistName());

        if (error != null) {
            NetworkResponse networkResponse = error.networkResponse;
            /**
             * Rate limit probably reached. Discontinue downloading to prevent
             * ban on the servers.
             */
            if (networkResponse != null && networkResponse.statusCode == 503) {
                mArtistList.clear();
                cancelAllRequests();
                boolean isEmpty;
                synchronized (mAlbumList) {
                    isEmpty = mAlbumList.isEmpty();
                }
                if (isEmpty && mBulkProgressCallback != null) {
                    mBulkProgressCallback.finishedLoading();
                }
                return;
            }
        }
        ArtistImageResponse imageResponse = new ArtistImageResponse();
        imageResponse.artist = artist;
        imageResponse.image = null;
        imageResponse.url = null;
        new InsertArtistImageTask().execute(imageResponse);
    }

    /**
     * AsyncTask to insert the images to the SQLdatabase. This is necessary as the Volley response
     * is handled in the UI thread.
     */
    private class InsertArtistImageTask extends AsyncTask<ArtistImageResponse, Object, MPDArtist> {

        /**
         * Inserts the image to the database.
         *
         * @param params Pair of byte[] (containing the image itself) and MPDArtist for which the image is for
         * @return the artist model that was inserted to the database.
         */
        @Override
        protected MPDArtist doInBackground(ArtistImageResponse... params) {
            ArtistImageResponse response = params[0];
            if (mCurrentBulkArtist == response.artist) {
                fetchNextBulkArtist();
            }


            if (response.image == null) {
                mDBManager.insertArtistImage(mContext, response.artist, response.image);
                return response.artist;
            }

            // Rescale them if to big
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(response.image, 0, response.image.length, options);
            if ((options.outHeight > MAXIMUM_IMAGE_RESOLUTION || options.outWidth > MAXIMUM_IMAGE_RESOLUTION)) {
                Log.v(TAG, "Image to big, rescaling");
                options.inJustDecodeBounds = false;
                Bitmap bm = BitmapFactory.decodeByteArray(response.image, 0, response.image.length, options);
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                Bitmap.createScaledBitmap(bm, MAXIMUM_IMAGE_RESOLUTION, MAXIMUM_IMAGE_RESOLUTION, true).compress(Bitmap.CompressFormat.JPEG, IMAGE_COMPRESSION_SETTING, byteStream);
                if(byteStream.size() <= MAXIMUM_IMAGE_SIZE) {
                    mDBManager.insertArtistImage(mContext, response.artist, byteStream.toByteArray());
                }
            } else {
                if(response.image.length <= MAXIMUM_IMAGE_SIZE) {
                    mDBManager.insertArtistImage(mContext, response.artist, response.image);
                }
            }

            broadcastNewArtistImageInfo(response, mContext);

            return response.artist;
        }

        /**
         * Notifies the listeners about a change in the image dataset. Called in the UI thread.
         *
         * @param result Artist that was inserted in the database
         */
        protected void onPostExecute(MPDArtist result) {
            synchronized (mArtistListeners) {
                for (onNewArtistImageListener artistListener : mArtistListeners) {
                    artistListener.newArtistImage(result);
                }
            }
        }

    }

    /**
     * AsyncTask to insert the images to the SQLdatabase. This is necessary as the Volley response
     * is handled in the UI thread.
     */
    private class InsertAlbumImageTask extends AsyncTask<AlbumImageResponse, Object, MPDAlbum> {

        /**
         * Inserts the image to the database.
         *
         * @param params Pair of byte[] (containing the image itself) and MPDAlbum for which the image is for
         * @return the album model that was inserted to the database.
         */
        @Override
        protected MPDAlbum doInBackground(AlbumImageResponse... params) {
            AlbumImageResponse response = params[0];
            if (mCurrentBulkAlbum == response.album) {
                fetchNextBulkAlbum();
            }
            if (response.image == null) {
                mDBManager.insertAlbumImage(mContext, response.album, response.image);
                return response.album;
            }

            Log.v(TAG, "Inserting image for album: " + response.album.getName());
            // Rescale them if to big
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(response.image, 0, response.image.length, options);
            if ((options.outHeight > MAXIMUM_IMAGE_RESOLUTION || options.outWidth > MAXIMUM_IMAGE_RESOLUTION)) {
                Log.v(TAG, "Image to big, rescaling");
                options.inJustDecodeBounds = false;
                Bitmap bm = BitmapFactory.decodeByteArray(response.image, 0, response.image.length, options);
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                Bitmap.createScaledBitmap(bm, MAXIMUM_IMAGE_RESOLUTION, MAXIMUM_IMAGE_RESOLUTION, true).compress(Bitmap.CompressFormat.JPEG, IMAGE_COMPRESSION_SETTING, byteStream);
                if(byteStream.size() <= MAXIMUM_IMAGE_SIZE) {
                    mDBManager.insertAlbumImage(mContext, response.album, byteStream.toByteArray());
                }
            } else {
                if(response.image.length <= MAXIMUM_IMAGE_SIZE) {
                    mDBManager.insertAlbumImage(mContext, response.album, response.image);
                }
            }

            broadcastNewAlbumImageInfo(response, mContext);

            return response.album;
        }

        /**
         * Notifies the listeners about a change in the image dataset. Called in the UI thread.
         *
         * @param result Album that was inserted in the database
         */
        protected void onPostExecute(MPDAlbum result) {
            synchronized (mAlbumListeners) {
                for (onNewAlbumImageListener albumListener : mAlbumListeners) {
                    albumListener.newAlbumImage(result);
                }
            }
        }
    }

    /**
     * AsyncTask to insert the images to the SQLdatabase. This is necessary as the Volley response
     * is handled in the UI thread.
     */
    private class InsertTrackAlbumImageTask extends AsyncTask<TrackAlbumImageResponse, Object, MPDAlbum> {

        /**
         * Inserts the image to the database.
         *
         * @param params Pair of byte[] (containing the image itself) and MPDAlbum for which the image is for
         * @return the album model that was inserted to the database.
         */
        @Override
        protected MPDAlbum doInBackground(TrackAlbumImageResponse... params) {
            TrackAlbumImageResponse response = params[0];
            if (mCurrentBulkTrack == response.track) {
                fetchNextBulkTrackAlbum();
            }

            MPDAlbum fakeAlbum = new MPDAlbum(response.track.getTrackAlbum());
            fakeAlbum.setArtistName(response.track.getTrackAlbumArtist());
            fakeAlbum.setMBID(response.track.getTrackAlbumMBID());


            Log.v(TAG, "Inserting image for track: " + response.track.getTrackAlbum());
            // Rescale them if to big
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(response.image, 0, response.image.length, options);
            if ((options.outHeight > MAXIMUM_IMAGE_SIZE || options.outWidth > MAXIMUM_IMAGE_SIZE)) {
                Log.v(TAG, "Image to big, rescaling");
                options.inJustDecodeBounds = false;
                Bitmap bm = BitmapFactory.decodeByteArray(response.image, 0, response.image.length, options);
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bm, MAXIMUM_IMAGE_SIZE, MAXIMUM_IMAGE_SIZE, true);
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_COMPRESSION_SETTING, byteStream);
                mDBManager.insertAlbumImage(mContext, fakeAlbum, byteStream.toByteArray());
            } else {
                mDBManager.insertAlbumImage(mContext, fakeAlbum, response.image);
            }

            AlbumImageResponse albumResponse = new AlbumImageResponse();
            albumResponse.album = fakeAlbum;

            broadcastNewAlbumImageInfo(albumResponse, mContext);

            return fakeAlbum;
        }

        /**
         * Notifies the listeners about a change in the image dataset. Called in the UI thread.
         *
         * @param result Album that was inserted in the database
         */
        protected void onPostExecute(MPDAlbum result) {
            synchronized (mAlbumListeners) {
                for (onNewAlbumImageListener albumListener : mAlbumListeners) {
                    albumListener.newAlbumImage(result);
                }
            }
        }
    }

    /**
     * Used to broadcast information about new available artwork to {@link BroadcastReceiver} like
     * the {@link org.gateshipone.malp.application.background.WidgetProvider} to reload its artwork.
     *
     * @param artistImage Image response containing the artist that an image was inserted for.
     * @param context     Context used for broadcasting
     */
    private void broadcastNewArtistImageInfo(ArtistImageResponse artistImage, Context context) {
        Intent newImageIntent = new Intent(ACTION_NEW_ARTWORK_READY);

        newImageIntent.putExtra(INTENT_EXTRA_KEY_ARTIST_MBID, artistImage.artist.getMBIDCount() > 0 ? artistImage.artist.getMBID(0) : "");
        newImageIntent.putExtra(INTENT_EXTRA_KEY_ARTIST_NAME, artistImage.artist.getArtistName());

        context.sendBroadcast(newImageIntent);
    }

    /**
     * Used to broadcast information about new available artwork to {@link BroadcastReceiver} like
     * the {@link org.gateshipone.malp.application.background.WidgetProvider} to reload its artwork.
     *
     * @param albumImage Image response containing the album that an image was inserted for.
     * @param context    Context used for broadcasting
     */
    private void broadcastNewAlbumImageInfo(AlbumImageResponse albumImage, Context context) {
        Intent newImageIntent = new Intent(ACTION_NEW_ARTWORK_READY);

        newImageIntent.putExtra(INTENT_EXTRA_KEY_ALBUM_MBID, albumImage.album.getMBID());
        newImageIntent.putExtra(INTENT_EXTRA_KEY_ALBUM_NAME, albumImage.album.getName());

        context.sendBroadcast(newImageIntent);
    }

    /**
     * Asynchronous task that is called as a callback for the list of albums.
     * Clears the old list and starts to download album images.
     */
    private class ParseMPDAlbumListTask extends AsyncTask<List<MPDAlbum>, Object, Object> {

        @Override
        protected Object doInBackground(List<MPDAlbum>... lists) {
            List<MPDAlbum> albumList = lists[0];

            mBulkProgressCallback.startAlbumLoading(albumList.size());

            Log.v(TAG, "Received " + albumList.size() + " albums for bulk loading");
            synchronized (mAlbumList) {
                mAlbumList.clear();
                mAlbumList.addAll(albumList);
            }
            mBulkLoadAlbumsReady = true;
            fetchNextBulkAlbum();
            return null;
        }
    }

    /**
     * Asynchronous task that is called as a callback for the list of artists.
     * Clears the old list and starts to download artist images.
     */
    private class ParseMPDArtistListTask extends AsyncTask<List<MPDArtist>, Object, Object> {

        @Override
        protected Object doInBackground(List<MPDArtist>... lists) {
            List<MPDArtist> artistList = lists[0];

            Log.v(TAG, "Received " + artistList.size() + " artists for bulk loading");
            mBulkProgressCallback.startArtistLoading(artistList.size());
            synchronized (mArtistList) {
                mArtistList.clear();
                mArtistList.addAll(artistList);
            }

            mBulkLoadArtistsReady = true;
            fetchNextBulkArtist();
            return null;
        }
    }

    /**
     * Asynchronous task that is called as a callback for the list of albums.
     * Clears the old list and starts to download album images.
     */
    private class ParseMPDTrackListTask extends AsyncTask<List<MPDFileEntry>, Object, Object> {
        private HashMap<String,MPDTrack> mAlbumPaths;
        @Override
        protected Object doInBackground(List<MPDFileEntry>... lists) {
            mAlbumPaths = new HashMap<>();
            List<MPDFileEntry> tracks = lists[0];

            // Get a list of unique album folders
            for(MPDFileEntry track: tracks) {
                String dirPath = FormatHelper.getDirectoryFromPath(track.getPath());
                if (track instanceof MPDTrack && !mAlbumPaths.containsKey(dirPath)) {
                    mAlbumPaths.put(FormatHelper.getDirectoryFromPath(track.getPath()),(MPDTrack)track);
                }
            }
            Log.v(TAG,"Unique path count: " + mAlbumPaths.size());

            int count = mAlbumPaths.size();
            mBulkProgressCallback.startAlbumLoading(count);
            synchronized (mTrackList) {
                mTrackList.clear();
                mTrackList.addAll(mAlbumPaths.values());
            }
            fetchNextBulkTrackAlbum();
            return null;
        }
    }

    /**
     * Entrance point to start downloading all images for the complete database of the current
     * default MPD server.
     *
     * @param progressCallback Used callback interface to be notified about the download progress.
     */
    public void bulkLoadImages(BulkLoadingProgressCallback progressCallback) {
        if (progressCallback == null) {
            return;
        }
        mBulkProgressCallback = progressCallback;
        mArtistList.clear();
        mAlbumList.clear();
        mBulkLoadAlbumsReady = false;
        mBulkLoadArtistsReady = false;
        Log.v(TAG, "Start bulk loading");

        if(HTTPAlbumImageProvider.getInstance(mContext).getActive()) {
            Log.v(TAG,"Try to get all tracks from MPD");
            MPDQueryHandler.getAllTracks(new MPDResponseFileList() {
                @Override
                public void handleTracks(List<MPDFileEntry> fileList, int windowstart, int windowend) {
                    Log.v(TAG,"Received track count: " + fileList.size());
                    new ParseMPDTrackListTask().execute(fileList);
                }
            });
        } else {

            if (!mAlbumProvider.equals(mContext.getString((R.string.pref_artwork_provider_none_key)))) {
                MPDQueryHandler.getAlbums(new MPDResponseAlbumList() {
                    @Override
                    public void handleAlbums(List<MPDAlbum> albumList) {
                        new ParseMPDAlbumListTask().execute(albumList);
                    }
                });
            }
        }

        if (!mArtistProvider.equals(mContext.getString((R.string.pref_artwork_provider_none_key)))) {
            MPDQueryHandler.getArtists(new MPDResponseArtistList() {
                @Override
                public void handleArtists(List<MPDArtist> artistList) {
                    new ParseMPDArtistListTask().execute(artistList);
                }
            });
        }
    }

    /**
     * Iterates over the list of albums and downloads images for them.
     */
    private void fetchNextBulkAlbum() {
        boolean isEmpty;
        synchronized (mAlbumList) {
            isEmpty = mAlbumList.isEmpty();
        }

        while (!isEmpty) {
            MPDAlbum album;
            synchronized (mAlbumList) {
                album = mAlbumList.remove(0);
                Log.v(TAG, "Bulk load next album: " + album.getName() + ":" + album.getArtistName() + " remaining: " + mAlbumList.size());
                mBulkProgressCallback.albumsRemaining(mAlbumList.size());
            }
            mCurrentBulkAlbum = album;

            // Check if image already there
            try {
                mDBManager.getAlbumImage(mContext, album);
                // If this does not throw the exception it already has an image.
            } catch (ImageNotFoundException e) {
                fetchAlbumImage(album);
                return;
            }

            synchronized (mAlbumList) {
                isEmpty = mAlbumList.isEmpty();
            }
        }
        if (mArtistList.isEmpty()) {
            mBulkProgressCallback.finishedLoading();
        }

    }

    /**
     * Iterates over the list of artists and downloads images for them.
     */
    private void fetchNextBulkArtist() {
        Log.v(TAG,"fetchNextBulkArtist");

        boolean isEmpty;
        synchronized (mArtistList) {
            isEmpty = mArtistList.isEmpty();
        }

        while (!isEmpty) {
            Log.v(TAG,"Next artist");

            MPDArtist artist;
            synchronized (mArtistList) {
                artist = mArtistList.remove(0);
                Log.v(TAG, "Bulk load next artist: " + artist.getArtistName() + " remaining: " + mArtistList.size());
                mBulkProgressCallback.artistsRemaining(mArtistList.size());
            }
            mCurrentBulkArtist = artist;

            // Check if image already there
            try {
                mDBManager.getArtistImage(mContext, artist);
                // If this does not throw the exception it already has an image.
            } catch (ImageNotFoundException e) {
                fetchArtistImage(artist);
                return;
            }

            synchronized (mArtistList) {
                isEmpty = mArtistList.isEmpty();
            }
        }

        if (mAlbumList.isEmpty()) {
            mBulkProgressCallback.finishedLoading();
        }
    }

    /**
     * Iterates over the list of artists and downloads images for them.
     */
    private void fetchNextBulkTrackAlbum() {
        Log.v(TAG,"fetchNextBulkTrackAlbum");

        boolean isEmpty;
        synchronized (mTrackList) {
            isEmpty = mTrackList.isEmpty();
        }

        while (!isEmpty) {
            Log.v(TAG,"Next track album");

            MPDTrack track;
            synchronized (mTrackList) {
                track = mTrackList.remove(0);
                mBulkProgressCallback.albumsRemaining(mTrackList.size());
            }
            mCurrentBulkTrack = track;

            // Check if image already there
            try {
                getAlbumImageForTrack(track,-1,-1, true);
                // If this does not throw the exception it already has an image.
            } catch (ImageNotFoundException e) {
                fetchAlbumImage(track);
                return;
            }

            synchronized (mTrackList) {
                isEmpty = mTrackList.isEmpty();
            }
        }

        if (mAlbumList.isEmpty()) {
            mBulkProgressCallback.finishedLoading();
        }
    }


    /**
     * Interface used for adapters to be notified about data set changes
     */
    public interface onNewArtistImageListener {
        void newArtistImage(MPDArtist artist);
    }

    /**
     * Interface used for adapters to be notified about data set changes
     */
    public interface onNewAlbumImageListener {
        void newAlbumImage(MPDAlbum album);
    }

    /**
     * Called if the connection state of the device is changing. This ensures no data is downloaded
     * if it is not intended (mobile data connection).
     */
    private class ConnectionStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (null == netInfo) {
                return;
            }
            boolean isWifi = netInfo.getType() == ConnectivityManager.TYPE_WIFI || netInfo.getType() == ConnectivityManager.TYPE_ETHERNET;

            if (mWifiOnly && !isWifi) {
                // Cancel all downloads
                Log.v(TAG, "Cancel all downloads because of connection change");
                cancelAllRequests();
            }

        }
    }

    /**
     * This will cancel the last used album/artist image providers. To make this useful on connection change
     * it is important to cancel all requests when changing the provider in settings.
     */
    public void cancelAllRequests() {
        Log.v(TAG, "Cancel all download requests");
        MALPRequestQueue.getInstance(mContext).cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });

        // Stop bulk loading as well
        synchronized (mAlbumList) {
            mAlbumList.clear();
        }
        synchronized (mArtistList) {
            mArtistList.clear();
        }

        if (null != mBulkProgressCallback) {
            mBulkProgressCallback.finishedLoading();
        }
    }

    /**
     * Interface used for BulkLoading processes. (S. {@link BulkDownloadService} )
     */
    public interface BulkLoadingProgressCallback {
        void startAlbumLoading(int albumCount);

        void startArtistLoading(int artistCount);

        void albumsRemaining(int remainingAlbums);

        void artistsRemaining(int remainingArtists);

        void finishedLoading();
    }
}
