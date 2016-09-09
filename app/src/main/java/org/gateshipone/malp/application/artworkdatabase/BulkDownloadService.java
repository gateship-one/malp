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

package org.gateshipone.malp.application.artworkdatabase;


import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.activities.MainActivity;
import org.gateshipone.malp.mpdservice.ConnectionManager;
import org.gateshipone.malp.mpdservice.handlers.MPDConnectionStateChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.profilemanagement.MPDProfileManager;

public class BulkDownloadService extends Service implements ArtworkManager.BulkLoadingProgressCallback {
    private static final String TAG = BulkDownloadService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 42;

    private static final String ACTION_CANCEL = "cancel_download";
    public static final String ACTION_START_BULKDOWNLOAD = "start_download";

    private MPDProfileManager mProfileManager;

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;

    private ConnectionStateHandler mConnectionHandler;

    private int mRemainingArtists;
    private int mRemainingAlbums;

    private int mSumImageDownloads;

    private ActionReceiver mBroadcastReceiver;


    /**
     * Called when the service is created because it is requested by an activity
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if ( null == mConnectionHandler ) {
            mConnectionHandler = new ConnectionStateHandler(this);
            Log.v(TAG, "Registering connection state listener");
            MPDQueryHandler.registerConnectionStateListener(mConnectionHandler);
        }
        if ( null == mProfileManager ) {
            mProfileManager = new MPDProfileManager(this);
        }

        mSumImageDownloads = 0;


    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
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

        if (intent.getAction().equals(ACTION_START_BULKDOWNLOAD)) {
            Log.v(TAG, "Starting bulk download in service with thread id: " + Thread.currentThread().getId());
            ConnectionManager.reconnectLastServer(this);
        }
        return START_STICKY;

    }

    private void runAsForeground() {
        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = new ActionReceiver();

            // Create a filter to only handle certain actions
            IntentFilter intentFilter = new IntentFilter();

            intentFilter.addAction(ACTION_CANCEL);

            registerReceiver(mBroadcastReceiver, intentFilter);
        }

        mBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getResources().getString(R.string.downloader_notification_title))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(getResources().getString(R.string.downloader_notification_remaining_images) + ' ' + String.valueOf(mSumImageDownloads - (mRemainingArtists + mRemainingAlbums)) + '/' + String.valueOf(mSumImageDownloads)))
                .setProgress(mSumImageDownloads, mSumImageDownloads - (mRemainingArtists + mRemainingAlbums), false)
                .setSmallIcon(R.drawable.icon_outline_24dp);

        mBuilder.setOngoing(true);

        // Cancel action
        Intent nextIntent = new Intent(BulkDownloadService.ACTION_CANCEL);
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 1, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        android.support.v7.app.NotificationCompat.Action cancelAction = new android.support.v7.app.NotificationCompat.Action.Builder(R.drawable.ic_cancel_24dp, getResources().getString(R.string.dialog_action_cancel), nextPendingIntent).build();

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

    private void updateNotification() {
        if ((mSumImageDownloads - (mRemainingArtists + mRemainingAlbums)) % 10 == 0) {
            mBuilder.setProgress(mSumImageDownloads, mSumImageDownloads - (mRemainingArtists + mRemainingAlbums), false);
            mBuilder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(getResources().getString(R.string.downloader_notification_remaining_images) + ' ' + String.valueOf(mSumImageDownloads - (mRemainingArtists + mRemainingAlbums)) + '/' + String.valueOf(mSumImageDownloads)));
            mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        }
    }

    private class ConnectionStateHandler extends MPDConnectionStateChangeHandler {

        BulkDownloadService mService;

        public ConnectionStateHandler(BulkDownloadService service) {
            mService = service;
        }

        @Override
        public void onConnected() {
            Log.v(TAG, "Connected to mpd host");
            mSumImageDownloads = 0;
            mRemainingArtists = 0;
            mRemainingAlbums = 0;
            ArtworkManager.getInstance(getApplicationContext()).bulkLoadImages(mService);
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
                MPDQueryHandler.unregisterConnectionStateListener(mConnectionHandler);
                stopSelf();
            }
        }
    }
}
