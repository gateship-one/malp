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

package org.gateshipone.malp.application.background;


import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.RemoteViews;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.activities.MainActivity;
import org.gateshipone.malp.application.activities.SplashActivity;
import org.gateshipone.malp.application.artworkdatabase.ArtworkManager;
import org.gateshipone.malp.application.utils.CoverBitmapLoader;
import org.gateshipone.malp.application.utils.FormatHelper;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;

import java.lang.ref.WeakReference;

public class WidgetProvider extends AppWidgetProvider {
    private final static String TAG = WidgetProvider.class.getSimpleName();
    /**
     * Statically save the last track and status and image. This allows loading the cover image
     * only if it really changed.
     */
    private static MPDTrack mLastTrack;
    private static MPDCurrentStatus mLastStatus;
    private static Bitmap mLastCover = null;


    /**
     * Intent IDs used for controlling action.
     */
    private final static int INTENT_OPENGUI = 0;
    private final static int INTENT_PREVIOUS = 1;
    private final static int INTENT_PLAYPAUSE = 2;
    private final static int INTENT_STOP = 3;
    private final static int INTENT_NEXT = 4;

    /**
     * Update the widgets
     *
     * @param context          Context for updateing
     * @param appWidgetManager appWidgetManager to update the widgets
     * @param appWidgetIds     Widget IDs that need updating.
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);


        // Call the updateWidget method which will update all instances.
        updateWidget(context);
    }

    /**
     * Called when widgets are removed
     *
     * @param context context used for deletion
     */
    public void onDisabled(Context context) {
        super.onDisabled(context);
        mLastTrack = null;
        mLastStatus = null;
    }


    /**
     * Updates the widget by creating a new RemoteViews object and setting all the intents for the
     * buttons and the TextViews correctly.
     *
     * @param context Context to use for updating the widgets contents
     */
    private void updateWidget(Context context) {
        boolean nowPlaying = false;

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_malp_big);
        // Check if valid object
        if (mLastStatus != null && mLastTrack != null) {

            if (!mLastTrack.getTrackTitle().isEmpty()) {
                views.setTextViewText(R.id.widget_big_trackName, mLastTrack.getTrackTitle());
            } else if (mLastTrack.getTrackTitle().isEmpty() && !mLastTrack.getPath().isEmpty()) {
                views.setTextViewText(R.id.widget_big_trackName, FormatHelper.getFilenameFromPath(mLastTrack.getTrackTitle()));
            } else {
                views.setTextViewText(R.id.widget_big_trackName, "");
            }

            if (!mLastTrack.getTrackAlbum().isEmpty() && !mLastTrack.getTrackArtist().isEmpty()) {
                views.setTextViewText(R.id.widget_big_ArtistAlbum, mLastTrack.getTrackArtist() + " - " + mLastTrack.getTrackAlbum());
            } else if (mLastTrack.getTrackAlbum().isEmpty() && !mLastTrack.getTrackArtist().isEmpty()) {
                views.setTextViewText(R.id.widget_big_ArtistAlbum, mLastTrack.getTrackArtist());
            } else if (mLastTrack.getTrackArtist().isEmpty() && !mLastTrack.getTrackAlbum().isEmpty()) {
                views.setTextViewText(R.id.widget_big_ArtistAlbum, mLastTrack.getTrackAlbum());
            } else {
                views.setTextViewText(R.id.widget_big_ArtistAlbum, mLastTrack.getPath());
            }

            if (mLastCover != null) {
                // Use the saved image
                views.setImageViewBitmap(R.id.widget_big_cover, mLastCover);
            } else {
                // Reuse the image from last calls if the album is the same
                views.setImageViewResource(R.id.widget_big_cover, R.drawable.icon_outline_256dp);
            }


            // Set the images of the play button dependent on the playback state.
            MPDCurrentStatus.MPD_PLAYBACK_STATE playState = mLastStatus.getPlaybackState();

            if (playState == MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PLAYING) {
                // Show pause icon
                nowPlaying = true;
                views.setImageViewResource(R.id.widget_big_play, R.drawable.ic_pause_48dp);
            } else {
                // Show play icon
                views.setImageViewResource(R.id.widget_big_play, R.drawable.ic_play_arrow_48dp);
            }


            // set button actions
            // Main action
            Intent mainIntent = new Intent(context, SplashActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            if (nowPlaying) {
                // add intent only if playing is active
                mainIntent.putExtra(MainActivity.MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW, MainActivity.MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW_NOWPLAYINGVIEW);
            }
            PendingIntent mainPendingIntent = PendingIntent.getActivity(context, INTENT_OPENGUI, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_big_cover, mainPendingIntent);

            // Play/Pause action
            Intent playPauseIntent = new Intent(context, BackgroundService.class);
            playPauseIntent.setAction(BackgroundService.ACTION_PLAY);
            PendingIntent playPausePendingIntent = PendingIntent.getService(context, INTENT_PLAYPAUSE, playPauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_big_play, playPausePendingIntent);

            // Stop action
            Intent stopIntent = new Intent(context, BackgroundService.class);
            stopIntent.setAction(BackgroundService.ACTION_STOP);
            PendingIntent stopPendingIntent = PendingIntent.getService(context, INTENT_STOP, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_big_stop, stopPendingIntent);

            // Previous song action
            Intent prevIntent = new Intent(context, BackgroundService.class);
            prevIntent.setAction(BackgroundService.ACTION_PREVIOUS);
            PendingIntent prevPendingIntent = PendingIntent.getService(context, INTENT_PREVIOUS, prevIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_big_previous, prevPendingIntent);

            // Next song action
            Intent nextIntent = new Intent(context, BackgroundService.class);
            nextIntent.setAction(BackgroundService.ACTION_NEXT);
            PendingIntent nextPendingIntent = PendingIntent.getService(context, INTENT_NEXT, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_big_next, nextPendingIntent);
            views.setViewVisibility(R.id.widget_control_layout, View.VISIBLE);
            views.setViewVisibility(R.id.widget_disconnected_layout, View.GONE);
        } else {
            // connect action
            Intent connectIntent = new Intent(context, BackgroundService.class);
            connectIntent.setAction(BackgroundService.ACTION_CONNECT);
            PendingIntent connectPendingIntent = PendingIntent.getService(context, INTENT_NEXT, connectIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_connect_button, connectPendingIntent);

            // Main action
            Intent mainIntent = new Intent(context, SplashActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            PendingIntent mainPendingIntent = PendingIntent.getActivity(context, INTENT_OPENGUI, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.widget_big_cover, mainPendingIntent);

            // Set application icon outline as a image again
            views.setImageViewResource(R.id.widget_big_cover, R.drawable.icon_outline_256dp);

            views.setViewVisibility(R.id.widget_control_layout, View.GONE);
            views.setViewVisibility(R.id.widget_disconnected_layout, View.VISIBLE);
        }

        AppWidgetManager.getInstance(context).updateAppWidget(new ComponentName(context, WidgetProvider.class), views);
    }

    /**
     * This is the broadcast receiver for NowPlayingInformation objects sent by the PBS
     *
     * @param context Context used for this receiver
     * @param intent  Intent containing the NowPlayingInformation as a payload.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // Type checks
        if (intent.getAction().equals(BackgroundService.ACTION_STATUS_CHANGED)) {

            // Extract the payload from the intent
            MPDCurrentStatus status = intent.getParcelableExtra(BackgroundService.INTENT_EXTRA_STATUS);

            // Check if a payload was sent
            if (null != status) {
                // Save the information for later usage (when the asynchronous bitmap loader finishes)
                mLastStatus = status;
            }
        } else if (intent.getAction().equals(BackgroundService.ACTION_TRACK_CHANGED)) {

            // Extract the payload from the intent
            MPDTrack track = intent.getParcelableExtra(BackgroundService.INTENT_EXTRA_TRACK);

            // Check if a payload was sent
            if (null != track) {
                boolean newImage = false;
                // Check if new album is played and remove image if it is.
                if (mLastTrack == null || !track.getTrackAlbum().equals(mLastTrack.getTrackAlbum()) || !track.getTrackAlbumMBID().equals(mLastTrack.getTrackAlbumMBID())) {
                    mLastCover = null;
                    newImage = true;

                }

                // Save the information for later usage (when the asynchronous bitmap loader finishes)
                mLastTrack = track;

                if (newImage) {
                    CoverBitmapLoader coverLoader = new CoverBitmapLoader(context, new CoverReceiver(context, this));
                    coverLoader.getImage(track, false);
                }
            }
        } else if (intent.getAction().equals(BackgroundService.ACTION_SERVER_DISCONNECTED)) {
            mLastStatus = null;
            mLastTrack = null;
        } else if ( intent.getAction().equals(ArtworkManager.ACTION_NEW_ARTWORK_READY)) {
            // Check if the new artwork matches the currently playing track. If so reload artwork
            if ( mLastTrack != null && mLastTrack.getTrackAlbum().equals(intent.getStringExtra(ArtworkManager.INTENT_EXTRA_KEY_ALBUM_NAME))) {
                // Got new artwork
                mLastCover = null;
                CoverBitmapLoader coverLoader = new CoverBitmapLoader(context, new CoverReceiver(context, this));
                coverLoader.getImage(mLastTrack, false);
            }
        }
        // Refresh the widget with the new information
        updateWidget(context);
    }

    private class CoverReceiver implements CoverBitmapLoader.CoverBitmapListener {
        WeakReference<Context> mContext;
        WeakReference<WidgetProvider> mProvider;

        public CoverReceiver(Context context, WidgetProvider provider) {
            mContext = new WeakReference<Context>(context);
            mProvider = new WeakReference<WidgetProvider>(provider);
        }

        /**
         * Sets the global image variable for this track and recall the update method to refresh
         * the views.
         *
         * @param bm Bitmap fetched for the currently running track.
         */
        @Override
        public void receiveBitmap(Bitmap bm) {
            // Check if a valid image was found.
            if (bm != null) {
                // Set the globally used variable
                mLastCover = bm;

                // Call the update method to refresh the view
                mProvider.get().updateWidget(mContext.get());
            }
        }
    }
}
