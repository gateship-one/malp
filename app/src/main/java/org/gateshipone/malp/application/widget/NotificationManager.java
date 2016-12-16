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

package org.gateshipone.malp.application.widget;


import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.activities.MainActivity;
import org.gateshipone.malp.application.utils.CoverBitmapLoader;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFile;

public class NotificationManager implements CoverBitmapLoader.CoverBitmapListener {
    private static final int NOTIFICATION_ID = 0;

    private WidgetService mService;

    /**
     * Intent IDs used for controlling action.
     */
    private final static int INTENT_OPENGUI = 0;
    private final static int INTENT_PREVIOUS = 1;
    private final static int INTENT_PLAYPAUSE = 2;
    private final static int INTENT_STOP = 3;
    private final static int INTENT_NEXT = 4;
    private final static int INTENT_QUIT = 5;


    // Notification objects
    private final android.app.NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder = null;

    // Notification itself
    private Notification mNotification;

    // Save last track and last image
    private Bitmap mLastBitmap = null;

    private MPDCurrentStatus mLastStatus = null;
    private MPDFile mLastTrack = null;

    private boolean mShown;

    private CoverBitmapLoader mCoverLoader;


    public NotificationManager(WidgetService service) {
        mService = service;
        mNotificationManager = (android.app.NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
        mLastStatus = new MPDCurrentStatus();
        mLastTrack = new MPDFile("");

        /**
         * Create loader to asynchronously load cover images. This class is the callback (s. receiveBitmap)
         */
        mCoverLoader = new CoverBitmapLoader(mService, this);
    }

    /**
     * Shows the notification
     */
    public void showNotification() {
        updateNotification(mLastTrack, mLastStatus.getPlaybackState());
        mShown = true;
    }

    /**
     * Hides the notification (if shown) and resets state variables.
     */
    public void hideNotification() {
        if (!mShown) {
            return;
        }
        if (mNotification != null) {
            mNotificationManager.cancel(NOTIFICATION_ID);

            mNotification = null;
            mNotificationBuilder = null;
        }
        mShown = false;
    }

    /*
    * Creates a android system notification with two different remoteViews. One
    * for the normal layout and one for the big one. Sets the different
    * attributes of the remoteViews and starts a thread for Cover generation.
    */
    public synchronized void updateNotification(MPDFile track, MPDCurrentStatus.MPD_PLAYBACK_STATE state) {
        if (track != null) {
            mNotificationBuilder = new NotificationCompat.Builder(mService);

            // Open application intent
            Intent contentIntent = new Intent(mService, MainActivity.class);
            contentIntent.putExtra(MainActivity.MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW, MainActivity.MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW_NOWPLAYINGVIEW);
            contentIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NO_HISTORY);
            PendingIntent contentPendingIntent = PendingIntent.getActivity(mService, INTENT_OPENGUI, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mNotificationBuilder.setContentIntent(contentPendingIntent);

            // Set pendingintents
            // Previous song action
            Intent prevIntent = new Intent(WidgetService.ACTION_PREVIOUS);
            PendingIntent prevPendingIntent = PendingIntent.getBroadcast(mService, INTENT_PREVIOUS, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Action prevAction = new NotificationCompat.Action.Builder(R.drawable.ic_skip_previous_48dp, "Previous", prevPendingIntent).build();

            // Pause/Play action
            PendingIntent playPauseIntent;
            int playPauseIcon;
            if (state == MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PLAYING) {
                Intent pauseIntent = new Intent(WidgetService.ACTION_PAUSE);
                playPauseIntent = PendingIntent.getBroadcast(mService, INTENT_PLAYPAUSE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                playPauseIcon = R.drawable.ic_pause_48dp;
            } else {
                Intent playIntent = new Intent(WidgetService.ACTION_PLAY);
                playPauseIntent = PendingIntent.getBroadcast(mService, INTENT_PLAYPAUSE, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                playPauseIcon = R.drawable.ic_play_arrow_48dp;
            }
            NotificationCompat.Action playPauseAction = new NotificationCompat.Action.Builder(playPauseIcon, "PlayPause", playPauseIntent).build();

            // Stop action
            Intent stopIntent = new Intent(WidgetService.ACTION_STOP);
            PendingIntent stopPendingIntent = PendingIntent.getBroadcast(mService, INTENT_STOP, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Action stopActon = new NotificationCompat.Action.Builder(R.drawable.ic_stop_black_48dp, "Stop", stopPendingIntent).build();

            // Next song action
            Intent nextIntent = new Intent(WidgetService.ACTION_NEXT);
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(mService, INTENT_NEXT, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Action nextAction = new NotificationCompat.Action.Builder(R.drawable.ic_skip_next_48dp, "Next", nextPendingIntent).build();

            // Quit action
            Intent quitIntent = new Intent(WidgetService.ACTION_QUIT_NOTIFICATION);
            PendingIntent quitPendingIntent = PendingIntent.getBroadcast(mService, INTENT_QUIT, quitIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mNotificationBuilder.setDeleteIntent(quitPendingIntent);

            mNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            mNotificationBuilder.setSmallIcon(R.drawable.ic_notification_24dp);
            mNotificationBuilder.addAction(prevAction);
            mNotificationBuilder.addAction(playPauseAction);
            mNotificationBuilder.addAction(stopActon);
            mNotificationBuilder.addAction(nextAction);
            NotificationCompat.MediaStyle notificationStyle = new NotificationCompat.MediaStyle();
            notificationStyle.setShowActionsInCompactView(1, 2);
            mNotificationBuilder.setStyle(notificationStyle);
            mNotificationBuilder.setContentTitle(track.getTrackTitle());
            mNotificationBuilder.setContentText(track.getTrackAlbum());

            // Remove unnecessary time info
            mNotificationBuilder.setWhen(0);

            // Cover but only if changed
            if (mNotification == null || !track.getTrackAlbum().equals(mLastTrack.getTrackAlbum())) {
                mLastTrack = track;
                mLastBitmap = null;
                mCoverLoader.getImage(mLastTrack);
            }

            // Only set image if an saved one is available
            if (mLastBitmap != null) {
                mNotificationBuilder.setLargeIcon(mLastBitmap);
            }

            // Build the notification
            mNotification = mNotificationBuilder.build();


            // Send the notification away
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        }
    }

    /**
     * Notifies about a change in MPDs status. If not shown this may be used later.
     * @param status New MPD status
     */
    public void setMPDStatus(MPDCurrentStatus status) {
        if (mShown) {
            updateNotification(mLastTrack, status.getPlaybackState());
        }
        // Save for later usage
        mLastStatus = status;
    }

    /**
     * Notifies about a change in MPDs track. If not shown this may be used later.
     * @param track New MPD track
     */
    public void setMPDFile(MPDFile track) {
        if (mShown) {
            updateNotification(track, mLastStatus.getPlaybackState());
        }
        // Save for later usage
        mLastTrack = track;
    }

    /*
     * Receives the generated album picture from the main status helper for the
     * notification controls. Sets it and notifies the system that the
     * notification has changed
     */
    @Override
    public synchronized void receiveBitmap(Bitmap bm) {
        // Check if notification exists and set picture
        mLastBitmap = bm;
        if (mNotification != null) {
            mNotificationBuilder.setLargeIcon(bm);
            mNotification = mNotificationBuilder.build();
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        }
    }
}
