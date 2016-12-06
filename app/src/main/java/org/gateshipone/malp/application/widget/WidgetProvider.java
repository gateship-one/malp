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
import org.gateshipone.malp.application.activities.SplashActivity;
import org.gateshipone.malp.application.utils.CoverBitmapLoader;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFile;

import java.lang.ref.WeakReference;

public class WidgetProvider extends AppWidgetProvider {
    private RemoteViews mViews;

    private static String mPackageName;

    private static MPDFile mLastTrack;
    private static MPDCurrentStatus mLastStatus;
    private static Bitmap mLastCover = null;


    private final static int INTENT_OPENGUI = 0;
    private final static int INTENT_PREVIOUS = 1;
    private final static int INTENT_PLAYPAUSE = 2;
    private final static int INTENT_STOP = 3;
    private final static int INTENT_NEXT = 4;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        mPackageName = context.getPackageName();
        // Perform this loop procedure for each App Widget that belongs to this
        // provider

        updateWidget(context);

    }

    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        mLastTrack = null;
        mLastStatus = null;
    }

    /**
     * Updates the widget by creating a new RemoteViews object and setting all the intents for the
     * buttons and the TextViews correctly.
     *
     * @param context
     */
    private void updateWidget(Context context) {
        boolean nowPlaying = false;

        // Create a new RemoteViews object containing the default widget layout
        mViews = new RemoteViews(mPackageName, R.layout.widget_malp_big);
        // Check if valid object
        if (mLastStatus != null && mLastTrack != null) {

            mViews.setTextViewText(R.id.widget_big_trackName, mLastTrack.getTrackTitle());
            mViews.setTextViewText(R.id.widget_big_ArtistAlbum, mLastTrack.getTrackArtist() + " - " + mLastTrack.getTrackAlbum());

            if (mLastCover != null) {
                // Use the saved image
                mViews.setImageViewBitmap(R.id.widget_big_cover, mLastCover);
            } else {
                // Reuse the image from last calls if the album is the same
                mViews.setImageViewResource(R.id.widget_big_cover, R.drawable.icon_outline_256dp);
            }


            // Set the images of the play button dependent on the playback state.
            MPDCurrentStatus.MPD_PLAYBACK_STATE playState = mLastStatus.getPlaybackState();

            if (playState == MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PLAYING) {
                // Show pause icon
                nowPlaying = true;
                mViews.setImageViewResource(R.id.widget_big_play, R.drawable.ic_pause_48dp);
            } else {
                // Show play icon
                mViews.setImageViewResource(R.id.widget_big_play, R.drawable.ic_play_arrow_48dp);
            }


            // set button actions
            // Main action
            Intent mainIntent = new Intent(context, SplashActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            if (nowPlaying) {
                // add intent only if playing is active
                //mainIntent.putExtra(MainActivity.MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW, OdysseyMainActivity.MAINACTIVITY_INTENT_EXTRA_REQUESTEDVIEW_NOWPLAYINGVIEW);
            }
            PendingIntent mainPendingIntent = PendingIntent.getActivity(context, INTENT_OPENGUI, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mViews.setOnClickPendingIntent(R.id.widget_big_cover, mainPendingIntent);

            // Play/Pause action
            Intent playPauseIntent = new Intent(context, WidgetService.class);
            playPauseIntent.setAction(WidgetService.ACTION_PLAY);
            PendingIntent playPausePendingIntent = PendingIntent.getService(context, INTENT_PLAYPAUSE, playPauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            mViews.setOnClickPendingIntent(R.id.widget_big_play, playPausePendingIntent);

            // Stop action
            Intent stopIntent = new Intent(context, WidgetService.class);
            stopIntent.setAction(WidgetService.ACTION_STOP);
            PendingIntent stopPendingIntent = PendingIntent.getService(context, INTENT_STOP, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            mViews.setOnClickPendingIntent(R.id.widget_big_stop, stopPendingIntent);

            // Previous song action
            Intent prevIntent = new Intent(context, WidgetService.class);
            prevIntent.setAction(WidgetService.ACTION_PREVIOUS);
            PendingIntent prevPendingIntent = PendingIntent.getService(context, INTENT_PREVIOUS, prevIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            mViews.setOnClickPendingIntent(R.id.widget_big_previous, prevPendingIntent);

            // Next song action
            Intent nextIntent = new Intent(context, WidgetService.class);
            nextIntent.setAction(WidgetService.ACTION_NEXT);
            PendingIntent nextPendingIntent = PendingIntent.getService(context, INTENT_NEXT, nextIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            mViews.setOnClickPendingIntent(R.id.widget_big_next, nextPendingIntent);
            mViews.setViewVisibility(R.id.widget_control_layout, View.VISIBLE);
            mViews.setViewVisibility(R.id.widget_disconnected_layout, View.GONE);
        } else {
            // connect action
            Intent connectIntent = new Intent(context, WidgetService.class);
            connectIntent.setAction(WidgetService.ACTION_CONNECT);
            PendingIntent connectPendingIntent = PendingIntent.getService(context, INTENT_NEXT, connectIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            mViews.setOnClickPendingIntent(R.id.widget_connect_button, connectPendingIntent);

            // Main action
            Intent mainIntent = new Intent(context, SplashActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            PendingIntent mainPendingIntent = PendingIntent.getActivity(context, INTENT_OPENGUI, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mViews.setOnClickPendingIntent(R.id.widget_big_cover, mainPendingIntent);

            // Set application icon outline as a image again
            mViews.setImageViewResource(R.id.widget_big_cover, R.drawable.icon_outline_256dp);

            mViews.setViewVisibility(R.id.widget_control_layout, View.GONE);
            mViews.setViewVisibility(R.id.widget_disconnected_layout, View.VISIBLE);
        }

        AppWidgetManager.getInstance(context).updateAppWidget(new ComponentName(context,WidgetProvider.class), mViews);
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
        if (intent.getAction().equals(WidgetService.ACTION_STATUS_CHANGED)) {

            // Extract the payload from the intent
            MPDCurrentStatus status = intent.getParcelableExtra(WidgetService.INTENT_EXTRA_STATUS);

            // Check if a payload was sent
            if (null != status) {
                // Save the information for later usage (when the asynchronous bitmap loader finishes)
                mLastStatus = status;
            }
        } else if (intent.getAction().equals(WidgetService.ACTION_TRACK_CHANGED)) {

            // Extract the payload from the intent
            MPDFile track = intent.getParcelableExtra(WidgetService.INTENT_EXTRA_TRACK);

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
                    coverLoader.getImage(track);
                }
            }
        } else if (intent.getAction().equals(WidgetService.ACTION_SERVER_DISCONNECTED)) {
            mLastStatus = null;
            mLastTrack = null;
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
