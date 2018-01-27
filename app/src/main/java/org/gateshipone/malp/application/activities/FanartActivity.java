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

package org.gateshipone.malp.application.activities;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.artworkdatabase.network.responses.FanartFetchError;
import org.gateshipone.malp.application.artworkdatabase.network.artprovider.FanartTVManager;
import org.gateshipone.malp.application.artworkdatabase.network.MALPRequestQueue;
import org.gateshipone.malp.application.artworkdatabase.fanartcache.FanartCacheManager;
import org.gateshipone.malp.application.utils.ThemeUtils;
import org.gateshipone.malp.application.utils.VolumeButtonLongClickListener;
import org.gateshipone.malp.mpdservice.handlers.MPDStatusChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDCommandHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDStateMonitoringHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.MPDException;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class FanartActivity extends GenericActivity {
    private static final String TAG = FanartActivity.class.getSimpleName();

    private static final String STATE_ARTWORK_POINTER = "artwork_pointer";
    private static final String STATE_ARTWORK_POINTER_NEXT = "artwork_pointer_next";
    private static final String STATE_LAST_TRACK = "last_track";

    /**
     * Time between two images of the slideshow
     */
    private static final int FANART_SWITCH_TIME = 12 * 1000;

    private TextView mTrackTitle;
    private TextView mTrackAlbum;
    private TextView mTrackArtist;

    private MPDTrack mLastTrack;


    private ServerStatusListener mStateListener = null;

    private ViewSwitcher mSwitcher;
    private Timer mSwitchTimer;

    private int mNextFanart;
    private int mCurrentFanart;

    private LinearLayout mInfoLayout;

    private ImageView mFanartView0;
    private ImageView mFanartView1;

    private ImageButton mNextButton;
    private ImageButton mPreviousButton;
    private ImageButton mPlayPauseButton;
    private ImageButton mStopButton;

    /**
     * Seekbar used for seeking and informing the user of the current playback position.
     */
    private SeekBar mPositionSeekbar;

    /**
     * Seekbar used for volume control of host
     */
    private SeekBar mVolumeSeekbar;
    private ImageView mVolumeIcon;
    private ImageView mVolumeIconButtons;

    private TextView mVolumeText;

    private ImageButton mVolumeMinus;
    private ImageButton mVolumePlus;

    private VolumeButtonLongClickListener mPlusListener;
    private VolumeButtonLongClickListener mMinusListener;

    private LinearLayout mHeaderTextLayout;

    private LinearLayout mVolumeSeekbarLayout;
    private LinearLayout mVolumeButtonLayout;


    private FanartCacheManager mFanartCache;

    private int mVolumeStepSize = 1;


    View mDecorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDecorView = getWindow().getDecorView();


        setContentView(R.layout.activity_artist_fanart);

        mInfoLayout = findViewById(R.id.information_layout);

        mTrackTitle = findViewById(R.id.textview_track_title);
        mTrackAlbum = findViewById(R.id.textview_track_album);
        mTrackArtist = findViewById(R.id.textview_track_artist);

        mSwitcher = findViewById(R.id.fanart_switcher);

        mFanartView0 = findViewById(R.id.fanart_view_0);
        mFanartView1 = findViewById(R.id.fanart_view_1);


        mPreviousButton = findViewById(R.id.button_previous_track);
        mNextButton = findViewById(R.id.button_next_track);
        mStopButton = findViewById(R.id.button_stop);
        mPlayPauseButton = findViewById(R.id.button_playpause);


        mPreviousButton.setOnClickListener(v -> MPDCommandHandler.previousSong());

        mNextButton.setOnClickListener(v -> MPDCommandHandler.nextSong());

        mStopButton.setOnClickListener(view -> MPDCommandHandler.stop());

        mPlayPauseButton.setOnClickListener(view -> MPDCommandHandler.togglePause());


        if (null == mStateListener) {
            mStateListener = new ServerStatusListener();
        }


        mInfoLayout.setOnClickListener(view -> {

        });

        mSwitcher.setOnClickListener(v -> {
            cancelSwitching();
            mSwitchTimer = new Timer();
            mSwitchTimer.schedule(new ViewSwitchTask(), FANART_SWITCH_TIME, FANART_SWITCH_TIME);
            updateFanartViews();
        });

        // seekbar (position)
        mPositionSeekbar = findViewById(R.id.now_playing_seekBar);
        mPositionSeekbar.setOnSeekBarChangeListener(new PositionSeekbarListener());

        mVolumeSeekbar = findViewById(R.id.volume_seekbar);
        mVolumeIcon = findViewById(R.id.volume_icon);
        mVolumeIcon.setOnClickListener(view -> MPDCommandHandler.setVolume(0));
        mVolumeSeekbar.setMax(100);
        mVolumeSeekbar.setOnSeekBarChangeListener(new VolumeSeekBarListener());

        /* Volume control buttons */
        mVolumeIconButtons = findViewById(R.id.volume_icon_buttons);
        mVolumeIconButtons.setOnClickListener(view -> MPDCommandHandler.setVolume(0));

        mVolumeText = findViewById(R.id.volume_button_text);

        mVolumeMinus = findViewById(R.id.volume_button_minus);

        mVolumeMinus.setOnClickListener(v -> MPDCommandHandler.decreaseVolume(mVolumeStepSize));

        mVolumePlus = findViewById(R.id.volume_button_plus);
        mVolumePlus.setOnClickListener(v -> MPDCommandHandler.increaseVolume(mVolumeStepSize));

        /* Create two listeners that start a repeating timer task to repeat the volume plus/minus action */
        mPlusListener = new VolumeButtonLongClickListener(VolumeButtonLongClickListener.LISTENER_ACTION.VOLUME_UP,mVolumeStepSize);
        mMinusListener = new VolumeButtonLongClickListener(VolumeButtonLongClickListener.LISTENER_ACTION.VOLUME_DOWN,mVolumeStepSize);

        /* Set the listener to the plus/minus button */
        mVolumeMinus.setOnLongClickListener(mMinusListener);
        mVolumeMinus.setOnTouchListener(mMinusListener);

        mVolumePlus.setOnLongClickListener(mPlusListener);
        mVolumePlus.setOnTouchListener(mPlusListener);

        mVolumeSeekbarLayout = findViewById(R.id.volume_seekbar_layout);
        mVolumeButtonLayout = findViewById(R.id.volume_button_layout);

        mFanartCache = new FanartCacheManager(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();

        MPDStateMonitoringHandler.getHandler().registerStatusListener(mStateListener);
        cancelSwitching();
        mSwitchTimer = new Timer();
        mSwitchTimer.schedule(new ViewSwitchTask(), FANART_SWITCH_TIME, FANART_SWITCH_TIME);

        mTrackTitle.setSelected(true);
        mTrackArtist.setSelected(true);
        mTrackAlbum.setSelected(true);

        hideSystemUI();

        setVolumeControlSetting();
    }

    @Override
    protected void onPause() {
        super.onPause();

        MPDStateMonitoringHandler.getHandler().unregisterStatusListener(mStateListener);
        cancelSwitching();
    }

    @Override
    protected void onConnected() {
        updateMPDStatus(MPDStateMonitoringHandler.getHandler().getLastStatus());
    }

    @Override
    protected void onDisconnected() {
        updateMPDStatus(new MPDCurrentStatus());
        updateMPDCurrentTrack(new MPDTrack(""));
    }

    @Override
    protected void onMPDError(MPDException.MPDServerException e) {

    }

    @Override
    protected void onMPDConnectionError(MPDException.MPDConnectionException e) {
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(STATE_ARTWORK_POINTER, mCurrentFanart);
        savedInstanceState.putInt(STATE_ARTWORK_POINTER_NEXT, mNextFanart);
        savedInstanceState.putParcelable(STATE_LAST_TRACK, mLastTrack);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        // Restore state members from saved instance
        mCurrentFanart = savedInstanceState.getInt(STATE_ARTWORK_POINTER);
        mNextFanart = savedInstanceState.getInt(STATE_ARTWORK_POINTER_NEXT);
        mLastTrack = savedInstanceState.getParcelable(STATE_LAST_TRACK);
    }


    private void updateMPDStatus(MPDCurrentStatus status) {
        MPDCurrentStatus.MPD_PLAYBACK_STATE state = status.getPlaybackState();

        // update play buttons
        switch (state) {
            case MPD_PLAYING:
                mPlayPauseButton.setImageResource(R.drawable.ic_pause_circle_fill_48dp);
                break;
            case MPD_PAUSING:
            case MPD_STOPPED:
                mPlayPauseButton.setImageResource(R.drawable.ic_play_circle_fill_48dp);
                break;
        }

        // Update volume seekbar
        int volume = status.getVolume();
        mVolumeSeekbar.setProgress(volume);

        if (volume >= 70) {
            mVolumeIcon.setImageResource(R.drawable.ic_volume_high_black_48dp);
            mVolumeIconButtons.setImageResource(R.drawable.ic_volume_high_black_48dp);
        } else if (volume >= 30 && volume < 70) {
            mVolumeIcon.setImageResource(R.drawable.ic_volume_medium_black_48dp);
            mVolumeIconButtons.setImageResource(R.drawable.ic_volume_medium_black_48dp);
        } else if (volume > 0 && volume < 30) {
            mVolumeIcon.setImageResource(R.drawable.ic_volume_low_black_48dp);
            mVolumeIconButtons.setImageResource(R.drawable.ic_volume_low_black_48dp);
        } else {
            mVolumeIcon.setImageResource(R.drawable.ic_volume_mute_black_48dp);
            mVolumeIconButtons.setImageResource(R.drawable.ic_volume_mute_black_48dp);
        }
        mVolumeIcon.setImageTintList(ColorStateList.valueOf(ThemeUtils.getThemeColor(this, R.attr.malp_color_text_accent)));
        mVolumeIconButtons.setImageTintList(ColorStateList.valueOf(ThemeUtils.getThemeColor(this, R.attr.malp_color_text_accent)));

        mVolumeText.setText(String.valueOf(volume) + '%');

        // Update position seekbar & textviews
        mPositionSeekbar.setMax(status.getTrackLength());
        mPositionSeekbar.setProgress(status.getElapsedTime());
    }

    /**
     * Reacts to new MPD tracks. Shows new track name, album, artist and triggers the fetching
     * of the Fanart.
     *
     * @param track New {@link MPDTrack} that is playing
     */
    private void updateMPDCurrentTrack(final MPDTrack track) {
        String title;

        title = track.getVisibleTitle();

        mTrackTitle.setText(title);
        mTrackAlbum.setText(track.getTrackAlbum());
        mTrackArtist.setText(track.getTrackArtist());
        if (null == mLastTrack || !track.getTrackArtist().equals(mLastTrack.getTrackArtist())) {
            // FIXME only cancel fanart requests
            MALPRequestQueue.getInstance(getApplicationContext()).cancelAll(request -> true);

            cancelSwitching();
            mFanartView0.setImageBitmap(null);
            mFanartView1.setImageBitmap(null);
            mNextFanart = 0;


            // FIXME refresh artwork shown
            mCurrentFanart = -1;
            mLastTrack = track;


            // Initiate the actual Fanart fetching
            checkFanartAvailable();
        }


    }

    /**
     * Checks if the currently playing track already has a MBID or not. If not it tries to resolve
     * one from the MusicBrainz database.
     */
    private void checkTracksMBID() {
        // Check if this track has an MBID otherwise try to get one.
        if ((mLastTrack.getTrackArtistMBID() == null || mLastTrack.getTrackArtistMBID().isEmpty()) && downloadAllowed()) {
            FanartTVManager.getInstance(getApplicationContext()).getTrackArtistMBID(mLastTrack, response -> {
                mLastTrack.setTrackArtistMBID(response);
                if (mLastTrack == mLastTrack) {
                    checkFanartAvailable();
                }
            }, new FanartFetchError() {
                @Override
                public void imageListFetchError() {
                }

                @Override
                public void fanartFetchError(MPDTrack track) {

                }
            });
        }
    }

    /**
     * Checks if fanart is already available for this artists MBIDs and shows the first image if.
     * <p>
     * After this it syncs fanart with the server (or downloads it if no fanart was available before).
     */
    private void checkFanartAvailable() {
        // Make sure the track contain an MBID
        checkTracksMBID();
        if (mFanartCache.getFanartCount(mLastTrack.getTrackArtistMBID()) != 0) {
            mNextFanart = 0;
            updateFanartViews();
        }

        // Sync/download fanart here.
        syncFanart(mLastTrack);
    }

    /**
     * Checks if new fanart is available for the given artist. This ensures that the user
     * gets new images from time to time if they have old images in cache.
     *
     * @param track Track to check for new fanart for.
     */
    private void syncFanart(final MPDTrack track) {
        // Get a list of fanart urls for the current artist
        if (!downloadAllowed()) {
            return;
        }
        FanartTVManager.getInstance(getApplicationContext()).getArtistFanartURLs(track.getTrackArtistMBID(), response -> {
            for (final String url : response) {
                // Check if the given image is in the cache already.
                if (mFanartCache.inCache(track.getTrackArtistMBID(), String.valueOf(url.hashCode()))) {
                    continue;
                }

                // If not try to download the image.
                FanartTVManager.getInstance(getApplicationContext()).getFanartImage(track, url, response1 -> {
                    mFanartCache.addFanart(track.getTrackArtistMBID(), String.valueOf(response1.url.hashCode()), response1.image);

                    int fanartCount = mFanartCache.getFanartCount(response1.track.getTrackArtistMBID());
                    if (fanartCount == 1) {
                        updateFanartViews();
                    }
                    if (mCurrentFanart == (fanartCount - 2)) {
                        mNextFanart = (mCurrentFanart + 1) % fanartCount;
                    }
                }, error -> {

                });
            }
        }, new FanartFetchError() {
            @Override
            public void imageListFetchError() {

            }

            @Override
            public void fanartFetchError(MPDTrack track) {

            }
        });
    }

    /**
     * Callback handler to react to changes in server status or a new playing track.
     */
    private class ServerStatusListener extends MPDStatusChangeHandler {

        @Override
        protected void onNewStatusReady(MPDCurrentStatus status) {
            updateMPDStatus(status);
        }

        @Override
        protected void onNewTrackReady(MPDTrack track) {
            updateMPDCurrentTrack(track);
        }
    }

    /**
     * Helper class to switch the views periodically. (Slideshow)
     */
    private class ViewSwitchTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(FanartActivity.this::updateFanartViews);
        }
    }

    // This snippet hides the system bars.
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    // This snippet shows the system bars. It does this by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }


    /**
     * Shows the next image if available. Blank if not.
     */
    private void updateFanartViews() {
        // Check if a track is available, cancel otherwise
        if (mLastTrack == null || mLastTrack.getTrackArtistMBID() == null || mLastTrack.getTrackArtistMBID().isEmpty()) {
            return;
        }
        int fanartCount = mFanartCache.getFanartCount(mLastTrack.getTrackArtistMBID());

        String mbid = mLastTrack.getTrackArtistMBID();

        if (mSwitcher.getDisplayedChild() == 0) {
            if (mNextFanart < fanartCount) {
                mCurrentFanart = mNextFanart;
                File fanartFile = mFanartCache.getFanart(mbid, mNextFanart);
                if (null == fanartFile) {
                    return;
                }
                Bitmap image = BitmapFactory.decodeFile(fanartFile.getPath());
                if (image != null) {
                    mFanartView1.setImageBitmap(image);

                    // Move pointer with wraparound
                    mNextFanart = (mNextFanart + 1) % fanartCount;
                }
            }
            mSwitcher.setDisplayedChild(1);
        } else {
            if (mNextFanart < fanartCount) {
                mCurrentFanart = mNextFanart;
                File fanartFile = mFanartCache.getFanart(mbid, mNextFanart);
                if (null == fanartFile) {
                    return;
                }
                Bitmap image = BitmapFactory.decodeFile(fanartFile.getPath());
                if (image != null) {
                    mFanartView0.setImageBitmap(image);

                    // Move pointer with wraparound
                    mNextFanart = (mNextFanart + 1) % fanartCount;
                }
            }
            mSwitcher.setDisplayedChild(0);
        }

        if (mSwitchTimer == null) {
            mSwitchTimer = new Timer();
            mSwitchTimer.schedule(new ViewSwitchTask(), FANART_SWITCH_TIME, FANART_SWITCH_TIME);
        }
    }

    /**
     * Cancels the view switching task that alternates between images.
     */
    private void cancelSwitching() {
        if (null != mSwitchTimer) {
            mSwitchTimer.cancel();
            mSwitchTimer.purge();
            mSwitchTimer = null;
        }
    }

    /**
     * Checks if downloading of images is allowed by the users policy.
     *
     * @return True if internet downloads are allowed. False otherwise.
     */
    private boolean downloadAllowed() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        ConnectivityManager cm =
                (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (null == netInfo) {
            return false;
        }
        boolean wifiOnly = sharedPref.getBoolean(getString(R.string.pref_download_wifi_only_key), getResources().getBoolean(R.bool.pref_download_wifi_default));
        String artistProvider = sharedPref.getString(getString(R.string.pref_artist_provider_key), getString(R.string.pref_artwork_provider_artist_default));
        boolean artistDownloadEnabled = !artistProvider.equals(getString(R.string.provider_off));
        boolean isWifi = netInfo.getType() == ConnectivityManager.TYPE_WIFI || netInfo.getType() == ConnectivityManager.TYPE_ETHERNET;
        return (isWifi || !wifiOnly) && artistDownloadEnabled;
    }

    /**
     * Listener class for the volume seekbar.
     */
    private class VolumeSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        /**
         * Called if the user drags the seekbar to a new position or the seekbar is altered from
         * outside. Just do some seeking, if the action is done by the user.
         *
         * @param seekBar  Seekbar of which the progress was changed.
         * @param progress The new position of the seekbar.
         * @param fromUser If the action was initiated by the user.
         */
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                MPDCommandHandler.setVolume(progress);

                if (progress >= 70) {
                    mVolumeIcon.setImageResource(R.drawable.ic_volume_high_black_48dp);
                } else if (progress >= 30 && progress < 70) {
                    mVolumeIcon.setImageResource(R.drawable.ic_volume_medium_black_48dp);
                } else if (progress > 0 && progress < 30) {
                    mVolumeIcon.setImageResource(R.drawable.ic_volume_low_black_48dp);
                } else {
                    mVolumeIcon.setImageResource(R.drawable.ic_volume_mute_black_48dp);
                }
            }
        }

        /**
         * Called if the user starts moving the seekbar. We do not handle this for now.
         *
         * @param seekBar SeekBar that is used for dragging.
         */
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub
        }

        /**
         * Called if the user ends moving the seekbar. We do not handle this for now.
         *
         * @param seekBar SeekBar that is used for dragging.
         */
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub
        }
    }

    /**
     * Listener class for the position seekbar.
     */
    private class PositionSeekbarListener implements SeekBar.OnSeekBarChangeListener {
        /**
         * Called if the user drags the seekbar to a new position or the seekbar is altered from
         * outside. Just do some seeking, if the action is done by the user.
         *
         * @param seekBar  Seekbar of which the progress was changed.
         * @param progress The new position of the seekbar.
         * @param fromUser If the action was initiated by the user.
         */
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                // FIXME Check if it is better to just update if user releases the seekbar
                // (network stress)
                MPDCommandHandler.seekSeconds(progress);
            }
        }

        /**
         * Called if the user starts moving the seekbar. We do not handle this for now.
         *
         * @param seekBar SeekBar that is used for dragging.
         */
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub
        }

        /**
         * Called if the user ends moving the seekbar. We do not handle this for now.
         *
         * @param seekBar SeekBar that is used for dragging.
         */
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // TODO Auto-generated method stub
        }
    }

    /**
     * Helper function to show the right volume control, requested by the user.
     */
    private void setVolumeControlSetting() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String volumeControlView = sharedPref.getString(this.getString(R.string.pref_volume_controls_key), this.getString(R.string.pref_volume_control_view_default));

        if (volumeControlView.equals(this.getString(R.string.pref_volume_control_view_off_key))) {
            mVolumeSeekbarLayout.setVisibility(View.GONE);
            mVolumeButtonLayout.setVisibility(View.GONE);
        } else if (volumeControlView.equals(this.getString(R.string.pref_volume_control_view_seekbar_key))) {
            mVolumeSeekbarLayout.setVisibility(View.VISIBLE);
            mVolumeButtonLayout.setVisibility(View.GONE);
        } else if (volumeControlView.equals(this.getString(R.string.pref_volume_control_view_buttons_key))) {
            mVolumeSeekbarLayout.setVisibility(View.GONE);
            mVolumeButtonLayout.setVisibility(View.VISIBLE);
        }

        mVolumeStepSize = sharedPref.getInt(getString(R.string.pref_volume_steps_key), getResources().getInteger(R.integer.pref_volume_steps_default));
        mPlusListener.setVolumeStepSize(mVolumeStepSize);
        mMinusListener.setVolumeStepSize(mVolumeStepSize);
    }

}
