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

package org.gateshipone.malp.application.background;


import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DrawFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.VolumeProviderCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.activities.MainActivity;
import org.gateshipone.malp.application.artworkdatabase.ArtworkManager;
import org.gateshipone.malp.application.utils.CoverBitmapLoader;
import org.gateshipone.malp.application.utils.FormatHelper;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDCommandHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;

public class NotificationManager implements CoverBitmapLoader.CoverBitmapListener, ArtworkManager.onNewAlbumImageListener {
    private static final String TAG = NotificationManager.class.getSimpleName();
    private static final int NOTIFICATION_ID = 0;

    private BackgroundService mService;

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

    /**
     * Last state of the MPD server
     */
    private MPDCurrentStatus mLastStatus = null;

    /**
     * Last played track of the MPD server. Used to check if track changed and a new cover is necessary.
     */
    private MPDTrack mLastTrack = null;

    /**
     * State of the notification and the media session.
     */
    private boolean mSessionActive;

    /**
     * Loader to asynchronously load cover images.
     */
    private CoverBitmapLoader mCoverLoader;


    /**
     * Mediasession to set the lockscreen picture as well
     */
    private MediaSessionCompat mMediaSession;

    /**
     * {@link VolumeProviderCompat} to react to volume changes over the hardware keys by the user.
     * Only active as long as the notification is active.
     */
    private MALPVolumeControlProvider mVolumeControlProvider;


    public NotificationManager(BackgroundService service) {
        mService = service;
        mNotificationManager = (android.app.NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
        mLastStatus = new MPDCurrentStatus();
        mLastTrack = new MPDTrack("");

        /**
         * Create loader to asynchronously load cover images. This class is the callback (s. receiveBitmap)
         */
        mCoverLoader = new CoverBitmapLoader(mService, this);

        ArtworkManager.getInstance(service).registerOnNewAlbumImageListener(this);
    }

    /**
     * Shows the notification
     */
    public void showNotification() {
        if (mMediaSession == null) {
            mMediaSession = new MediaSessionCompat(mService, mService.getString(R.string.app_name));
            mMediaSession.setCallback(new MALPMediaSessionCallback());
            mVolumeControlProvider = new MALPVolumeControlProvider();
            mMediaSession.setPlaybackToRemote(mVolumeControlProvider);
            mMediaSession.setActive(true);
        }

        updateNotification(mLastTrack, mLastStatus.getPlaybackState());
        mSessionActive = true;
    }

    /**
     * Hides the notification (if shown) and resets state variables.
     */
    public void hideNotification() {
        if (mMediaSession != null) {
            mMediaSession.setActive(false);
            mMediaSession = null;
        }

        mNotificationManager.cancel(NOTIFICATION_ID);
        if (mNotification != null) {

            mNotification = null;
            mNotificationBuilder = null;
        }
        mSessionActive = false;
    }

    /*
    * Creates a android system notification with two different remoteViews. One
    * for the normal layout and one for the big one. Sets the different
    * attributes of the remoteViews and starts a thread for Cover generation.
    */
    public synchronized void updateNotification(MPDTrack track, MPDCurrentStatus.MPD_PLAYBACK_STATE state) {
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
            Intent prevIntent = new Intent(BackgroundService.ACTION_PREVIOUS);
            PendingIntent prevPendingIntent = PendingIntent.getBroadcast(mService, INTENT_PREVIOUS, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Action prevAction = new NotificationCompat.Action.Builder(R.drawable.ic_skip_previous_48dp, "Previous", prevPendingIntent).build();

            // Pause/Play action
            PendingIntent playPauseIntent;
            int playPauseIcon;
            if (state == MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PLAYING) {
                Intent pauseIntent = new Intent(BackgroundService.ACTION_PAUSE);
                playPauseIntent = PendingIntent.getBroadcast(mService, INTENT_PLAYPAUSE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                playPauseIcon = R.drawable.ic_pause_48dp;
            } else {
                Intent playIntent = new Intent(BackgroundService.ACTION_PLAY);
                playPauseIntent = PendingIntent.getBroadcast(mService, INTENT_PLAYPAUSE, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                playPauseIcon = R.drawable.ic_play_arrow_48dp;
            }
            NotificationCompat.Action playPauseAction = new NotificationCompat.Action.Builder(playPauseIcon, "PlayPause", playPauseIntent).build();

            // Stop action
            Intent stopIntent = new Intent(BackgroundService.ACTION_STOP);
            PendingIntent stopPendingIntent = PendingIntent.getBroadcast(mService, INTENT_STOP, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Action stopActon = new NotificationCompat.Action.Builder(R.drawable.ic_stop_black_48dp, "Stop", stopPendingIntent).build();

            // Next song action
            Intent nextIntent = new Intent(BackgroundService.ACTION_NEXT);
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(mService, INTENT_NEXT, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Action nextAction = new NotificationCompat.Action.Builder(R.drawable.ic_skip_next_48dp, "Next", nextPendingIntent).build();

            // Quit action
            Intent quitIntent = new Intent(BackgroundService.ACTION_QUIT_BACKGROUND_SERVICE);
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

            String title;
            if (track.getTrackTitle().isEmpty()) {
                title = FormatHelper.getFilenameFromPath(track.getPath());
            } else {
                title = track.getTrackTitle();
            }

            mNotificationBuilder.setContentTitle(title);

            String secondRow;

            if (!track.getTrackArtist().isEmpty() && !track.getTrackAlbum().isEmpty()) {
                secondRow = track.getTrackArtist() + mService.getString(R.string.track_item_separator) + track.getTrackAlbum();
            } else if (track.getTrackArtist().isEmpty() && !track.getTrackAlbum().isEmpty()) {
                secondRow = track.getTrackAlbum();
            } else if (track.getTrackAlbum().isEmpty() && !track.getTrackArtist().isEmpty()) {
                secondRow = track.getTrackArtist();
            } else {
                secondRow = track.getPath();
            }

            // Set the media session metadata
            updateMetadata(track, state);

            mNotificationBuilder.setContentText(secondRow);

            // Remove unnecessary time info
            mNotificationBuilder.setWhen(0);

            // Cover but only if changed
            if (mNotification == null || !track.getTrackAlbum().equals(mLastTrack.getTrackAlbum())) {
                mLastTrack = track;
                mLastBitmap = null;
                mCoverLoader.getImage(mLastTrack, true);
            }

            // Only set image if an saved one is available
            if (mLastBitmap != null) {
                mNotificationBuilder.setLargeIcon(mLastBitmap);
            } else {
                /**
                 * Create a dummy placeholder image for versions greater android 7 because it
                 * does not automatically show the application icon anymore in mediastyle notifications.
                 */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Drawable icon = mService.getDrawable(R.drawable.notification_placeholder_256dp);

                    Bitmap iconBitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(iconBitmap);
                    DrawFilter filter = new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG, 1);

                    canvas.setDrawFilter(filter);
                    icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    icon.setFilterBitmap(true);


                    icon.draw(canvas);
                    mNotificationBuilder.setLargeIcon(iconBitmap);
                } else {
                    /**
                     * For older android versions set the null icon which will result in a dummy icon
                     * generated from the application icon.
                     */
                    mNotificationBuilder.setLargeIcon(null);
                }
            }

            // Build the notification
            mNotification = mNotificationBuilder.build();


            // Send the notification away
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
        }
    }

    /**
     * Updates the Metadata from Androids MediaSession. This sets track/album and stuff
     * for a lockscreen image for example.
     *
     * @param track         Current track.
     * @param playbackState State of the PlaybackService.
     */
    private void updateMetadata(MPDTrack track, MPDCurrentStatus.MPD_PLAYBACK_STATE playbackState) {
        if (track != null) {
            if (playbackState == MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PLAYING) {
                mMediaSession.setPlaybackState(new PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                        .setActions(PlaybackStateCompat.ACTION_SKIP_TO_NEXT + PlaybackStateCompat.ACTION_PAUSE +
                                PlaybackStateCompat.ACTION_PLAY + PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS +
                                PlaybackStateCompat.ACTION_STOP + PlaybackStateCompat.ACTION_SEEK_TO).build());
            } else {
                mMediaSession.setPlaybackState(new PlaybackStateCompat.Builder().
                        setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f).setActions(PlaybackStateCompat.ACTION_SKIP_TO_NEXT +
                        PlaybackStateCompat.ACTION_PAUSE + PlaybackStateCompat.ACTION_PLAY +
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS + PlaybackStateCompat.ACTION_STOP +
                        PlaybackStateCompat.ACTION_SEEK_TO).build());
            }
            // Try to get old metadata to save image retrieval.
            MediaMetadataCompat oldData = mMediaSession.getController().getMetadata();
            MediaMetadataCompat.Builder metaDataBuilder;
            if (oldData == null) {
                metaDataBuilder = new MediaMetadataCompat.Builder();
            } else {
                metaDataBuilder = new MediaMetadataCompat.Builder(mMediaSession.getController().getMetadata());
            }
            metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.getTrackTitle());
            metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.getTrackAlbum());
            metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.getTrackArtist());
            metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, track.getTrackArtist());
            metaDataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, track.getTrackTitle());
            metaDataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, track.getTrackNumber());
            metaDataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.getLength());

            mMediaSession.setMetadata(metaDataBuilder.build());
        }
    }

    /**
     * Notifies about a change in MPDs status. If not shown this may be used later.
     *
     * @param status New MPD status
     */
    public void setMPDStatus(MPDCurrentStatus status) {
        if (mSessionActive) {
            updateNotification(mLastTrack, status.getPlaybackState());
            // Notify the mediasession about the new volume
            mVolumeControlProvider.setCurrentVolume(status.getVolume());
        }
        // Save for later usage
        mLastStatus = status;
    }

    /**
     * Notifies about a change in MPDs track. If not shown this may be used later.
     *
     * @param track New MPD track
     */
    public void setMPDFile(MPDTrack track) {
        if (mSessionActive) {
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
        if (mNotification != null && bm != null) {
            mNotificationBuilder.setLargeIcon(bm);
            mNotification = mNotificationBuilder.build();
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);

            /* Set lockscreen picture and stuff */
            if (mMediaSession != null) {
                MediaMetadataCompat.Builder metaDataBuilder;
                metaDataBuilder = new MediaMetadataCompat.Builder(mMediaSession.getController().getMetadata());
                metaDataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bm);
                mMediaSession.setMetadata(metaDataBuilder.build());
            }
        }
    }

    @Override
    public void newAlbumImage(MPDAlbum album) {
        if (mLastTrack.getTrackAlbum().equals(album.getName())) {
            mCoverLoader.getImage(mLastTrack, true);
        }
    }

    /**
     * Callback class for MediaControls controlled by android system like BT remotes, etc and
     * Volume keys on some android versions.
     */
    private class MALPMediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlay() {
            super.onPlay();
            MPDCommandHandler.togglePause();
        }

        @Override
        public void onPause() {
            super.onPause();
            MPDCommandHandler.togglePause();
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            MPDCommandHandler.nextSong();
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            MPDCommandHandler.previousSong();
        }

        @Override
        public void onStop() {
            super.onStop();
            MPDCommandHandler.stop();
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
            MPDCommandHandler.seekSeconds((int) pos);
        }
    }

    /**
     * Handles volume changes from mediasession callbacks. Will send user requested changes
     * in volume back to the MPD server.
     */
    private class MALPVolumeControlProvider extends VolumeProviderCompat {

        public MALPVolumeControlProvider() {
            super(VOLUME_CONTROL_ABSOLUTE, 100, mLastStatus.getVolume());
        }

        @Override
        public void onSetVolumeTo(int volume) {
            MPDCommandHandler.setVolume(volume);
            setCurrentVolume(volume);
        }

        @Override
        public void onAdjustVolume(int direction) {
            if (direction == 1) {
                MPDCommandHandler.increaseVolume();
            } else if (direction == -1) {
                MPDCommandHandler.decreaseVolume();
            }
        }
    }

}
