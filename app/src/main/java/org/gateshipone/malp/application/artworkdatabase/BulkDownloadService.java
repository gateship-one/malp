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


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.artworkdatabase.network.MALPRequestQueue;
import org.gateshipone.malp.application.artworkdatabase.network.artprovider.HTTPAlbumImageProvider;
import org.gateshipone.malp.mpdservice.ConnectionManager;
import org.gateshipone.malp.mpdservice.handlers.MPDConnectionStateChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.MPDInterface;

import java.lang.ref.WeakReference;

public class BulkDownloadService extends Service implements ArtworkManager.BulkLoadingProgressCallback {
    private static final String TAG = BulkDownloadService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 2;
    private static final String NOTIFICATION_CHANNEL_ID = "BulkDownloader";

    public static final String ACTION_CANCEL = "org.gateshipone.malp.cancel_download";
    public static final String ACTION_START_BULKDOWNLOAD = "org.gateshipone.malp.start_download";

    public static final String BUNDLE_KEY_ARTIST_PROVIDER = "org.gateshipone.malp.artist_provider";
    public static final String BUNDLE_KEY_ALBUM_PROVIDER = "org.gateshipone.malp.album_provider";
    public static final String BUNDLE_KEY_HTTP_COVER_REGEX = "org.gateshipone.malp.http_cover_regex";
    public static final String BUNDLE_KEY_WIFI_ONLY = "org.gateshipone.malp.wifi_only";

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;

    private ConnectionStateHandler mConnectionHandler;

    private int mRemainingArtists;
    private int mRemainingAlbums;

    private int mSumImageDownloads;

    private String mHTTPRegex;

    private ActionReceiver mBroadcastReceiver;

    private PowerManager.WakeLock mWakelock;

    private ConnectionStateReceiver mConnectionStateChangeReceiver;

    private boolean mWifiOnly;

    /**
     * Called when the service is created because it is requested by an activity
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if ( null == mConnectionHandler ) {
            mConnectionHandler = new ConnectionStateHandler(this, getMainLooper());
            Log.v(TAG, "Registering connection state listener");
            MPDInterface.mInstance.addMPDConnectionStateChangeListener(mConnectionHandler);
        }

        mSumImageDownloads = 0;

        mConnectionStateChangeReceiver = new ConnectionStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectionStateChangeReceiver, filter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        unregisterReceiver(mConnectionStateChangeReceiver);
        Log.v(TAG, "Calling super.onDestroy()");
        super.onDestroy();
        Log.v(TAG, "Called super.onDestroy()");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_START_BULKDOWNLOAD)) {
            Log.v(TAG, "Starting bulk download in service with thread id: " + Thread.currentThread().getId());

            // reset counter
            mRemainingArtists = 0;
            mRemainingAlbums = 0;
            mSumImageDownloads = 0;

            String artistProvider = getString(R.string.pref_artwork_provider_artist_default);
            String albumProvider = getString(R.string.pref_artwork_provider_album_default);
            mWifiOnly = true;

            // read setting from extras
            Bundle extras = intent.getExtras();
            if (extras != null) {
                artistProvider = extras.getString(BUNDLE_KEY_ARTIST_PROVIDER, getString(R.string.pref_artwork_provider_artist_default));
                albumProvider = extras.getString(BUNDLE_KEY_ALBUM_PROVIDER, getString(R.string.pref_artwork_provider_album_default));
                mWifiOnly = intent.getBooleanExtra(BUNDLE_KEY_WIFI_ONLY, true);
                mHTTPRegex = intent.getStringExtra(BUNDLE_KEY_HTTP_COVER_REGEX);
                if (mHTTPRegex != null && !mHTTPRegex.isEmpty()) {
                    HTTPAlbumImageProvider.getInstance(getApplicationContext()).setRegex(mHTTPRegex);
                }
            }


            ConnectivityManager cm =
                    (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (null == netInfo) {
                return START_NOT_STICKY;
            }
            boolean isWifi = netInfo.getType() == ConnectivityManager.TYPE_WIFI || netInfo.getType() == ConnectivityManager.TYPE_ETHERNET;

            if (mWifiOnly && !isWifi) {
                return START_NOT_STICKY;
            }

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            mWakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "MALP_BulkDownloader");


            ArtworkManager artworkManager = ArtworkManager.getInstance(getApplicationContext());
            artworkManager.initialize(artistProvider, albumProvider, mWifiOnly);

            // FIXME do some timeout checking. e.g. 5 minutes no new image then cancel the process
            mWakelock.acquire();
            ConnectionManager.getInstance(getApplicationContext()).reconnectLastServer(this);
        }
        return START_NOT_STICKY;

    }

    private void openChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, this.getResources().getString(R.string.notification_channel_name_bulk_download), android.app.NotificationManager.IMPORTANCE_LOW);
            // Disable lights & vibration
            channel.enableVibration(false);
            channel.enableLights(false);
            channel.setVibrationPattern(null);

            // Allow lockscreen playback control
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PRIVATE);

            // Register the channel
            mNotificationManager.createNotificationChannel(channel);
        }
    }

    private void runAsForeground() {
        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = new ActionReceiver();

            // Create a filter to only handle certain actions
            IntentFilter intentFilter = new IntentFilter();

            intentFilter.addAction(ACTION_CANCEL);

            registerReceiver(mBroadcastReceiver, intentFilter);
        }

        openChannel();

        mBuilder = new NotificationCompat.Builder(this,NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.downloader_notification_title))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(getResources().getString(R.string.downloader_notification_remaining_images) + ' ' + String.valueOf(mSumImageDownloads - (mRemainingArtists + mRemainingAlbums)) + '/' + String.valueOf(mSumImageDownloads)))
                .setProgress(mSumImageDownloads, mSumImageDownloads - (mRemainingArtists + mRemainingAlbums), false)
                .setSmallIcon(R.drawable.ic_notification_24dp);

        mBuilder.setOngoing(true);

        // Cancel action
        Intent nextIntent = new Intent(BulkDownloadService.ACTION_CANCEL);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        android.support.v4.app.NotificationCompat.Action cancelAction = new android.support.v4.app.NotificationCompat.Action.Builder(R.drawable.ic_cancel_24dp, getResources().getString(R.string.dialog_action_cancel), nextPendingIntent).build();

        mBuilder.addAction(cancelAction);

        Notification notification = mBuilder.build();
        startForeground(NOTIFICATION_ID, notification);
        mNotificationManager.notify(NOTIFICATION_ID, notification);

    }

    @Override
    public void startAlbumLoading(int albumCount) {
        Log.v(TAG, "Albumloading started with: " + albumCount + " albums");
        mSumImageDownloads += albumCount;
        mRemainingAlbums = albumCount;
        runAsForeground();
    }

    @Override
    public void startArtistLoading(int artistCount) {
        Log.v(TAG, "Artistloading started with: " + artistCount + " artists");
        mSumImageDownloads += artistCount;
        mRemainingArtists = artistCount;
        runAsForeground();
    }

    @Override
    public void albumsRemaining(int remainingAlbums) {
        mRemainingAlbums = remainingAlbums;
        updateNotification();
    }

    @Override
    public void artistsRemaining(int remainingArtists) {
        mRemainingArtists = remainingArtists;
        updateNotification();
    }

    @Override
    public void finishedLoading() {
        mNotificationManager.cancel(NOTIFICATION_ID);
        stopForeground(true);
        MPDInterface.mInstance.removeMPDConnectionStateChangeListener(mConnectionHandler);
        stopSelf();
        if ( mWakelock.isHeld() ) {
            mWakelock.release();
        }
    }

    private void updateNotification() {
        if ((mSumImageDownloads - (mRemainingArtists + mRemainingAlbums)) % 10 == 0) {
            mBuilder.setProgress(mSumImageDownloads, mSumImageDownloads - (mRemainingArtists + mRemainingAlbums), false);
            mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(getResources().getString(R.string.downloader_notification_remaining_images) + ' ' + String.valueOf(mSumImageDownloads - (mRemainingArtists + mRemainingAlbums)) + '/' + String.valueOf(mSumImageDownloads)));
            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        }
    }

    private static class ConnectionStateHandler extends MPDConnectionStateChangeHandler {
        private final WeakReference<BulkDownloadService> mService;

        private ConnectionStateHandler(BulkDownloadService service, Looper looper) {
            super(looper);
            mService = new WeakReference<BulkDownloadService>(service);
        }

        @Override
        public void onConnected() {
            Log.v(TAG, "Connected to mpd host");
            mService.get().mSumImageDownloads = 0;
            mService.get().mRemainingArtists = 0;
            mService.get().mRemainingAlbums = 0;
            ArtworkManager.getInstance(mService.get().getApplicationContext()).bulkLoadImages(mService.get());
        }

        @Override
        public void onDisconnected() {

        }
    }

    private class ActionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "Broadcast requested");
            if (intent.getAction().equals(ACTION_CANCEL)) {
                Log.e(TAG, "Cancel requested");
                ArtworkManager.getInstance(getApplicationContext()).cancelAllRequests();
                mNotificationManager.cancel(NOTIFICATION_ID);
                stopForeground(true);
                MPDInterface.mInstance.removeMPDConnectionStateChangeListener(mConnectionHandler);
                stopSelf();
            }
        }
    }

    private class ConnectionStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(BulkDownloadService.this);

            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (null == netInfo) {
                return;
            }
            boolean wifiOnly = sharedPref.getBoolean(getString(R.string.pref_download_wifi_only_key), getResources().getBoolean(R.bool.pref_download_wifi_default));
            boolean isWifi = netInfo.getType() == ConnectivityManager.TYPE_WIFI || netInfo.getType() == ConnectivityManager.TYPE_ETHERNET;

            if (wifiOnly && !isWifi) {
                // Cancel all downloads
                Log.v(TAG, "Cancel all downloads because of connection change");
                MALPRequestQueue.getInstance(BulkDownloadService.this).cancelAll(new RequestQueue.RequestFilter() {
                    @Override
                    public boolean apply(Request<?> request) {
                        return true;
                    }
                });
            }

        }
    }
}
