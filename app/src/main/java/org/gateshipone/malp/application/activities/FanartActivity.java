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

package org.gateshipone.malp.application.activities;


import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.artworkdatabase.FanartFetchError;
import org.gateshipone.malp.application.artworkdatabase.FanartResponse;
import org.gateshipone.malp.application.artworkdatabase.FanartTVManager;
import org.gateshipone.malp.application.artworkdatabase.MALPRequestQueue;
import org.gateshipone.malp.application.artworkdatabase.fanartcache.FanartCacheManager;
import org.gateshipone.malp.mpdservice.ConnectionManager;
import org.gateshipone.malp.mpdservice.handlers.MPDStatusChangeHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDCommandHandler;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDStateMonitoringHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFile;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class FanartActivity extends Activity {
    private static final String TAG = FanartActivity.class.getSimpleName();
    private TextView mTrackTitle;
    private TextView mTrackAlbum;
    private TextView mTrackArtist;

    private MPDFile mLastTrack;
    private MPDCurrentStatus mLastStatus;


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


    private FanartCacheManager mFanartCache;

    View mDecorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Read theme preference
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String themePref = sharedPref.getString("pref_theme", "indigo");

        switch (themePref) {
            case "indigo":
                setTheme(R.style.AppTheme_indigo);
                break;
            case "orange":
                setTheme(R.style.AppTheme_orange);
                break;
            case "deeporange":
                setTheme(R.style.AppTheme_deepOrange);
                break;
            case "blue":
                setTheme(R.style.AppTheme_blue);
                break;
            case "darkgrey":
                setTheme(R.style.AppTheme_darkGrey);
                break;
            case "brown":
                setTheme(R.style.AppTheme_brown);
                break;
        }
        super.onCreate(savedInstanceState);
        mDecorView = getWindow().getDecorView();
        // Hide the status bar.
        // Remember that you should never show the action bar if the
        // status bar is hidden, so hide that too if necessary.


        setContentView(R.layout.activity_artist_fanart);

        mInfoLayout = (LinearLayout)findViewById(R.id.information_layout);

        mTrackTitle = (TextView) findViewById(R.id.textview_track_title);
        mTrackAlbum = (TextView) findViewById(R.id.textview_track_album);
        mTrackArtist = (TextView) findViewById(R.id.textview_track_artist);

        mSwitcher = (ViewSwitcher) findViewById(R.id.fanart_switcher);

        mFanartView0 = (ImageView) findViewById(R.id.fanart_view_0);
        mFanartView1 = (ImageView) findViewById(R.id.fanart_view_1);


        mPreviousButton = (ImageButton) findViewById(R.id.button_previous_track);
        mNextButton = (ImageButton) findViewById(R.id.button_next_track);
        mStopButton = (ImageButton) findViewById(R.id.button_stop);
        mPlayPauseButton = (ImageButton) findViewById(R.id.button_playpause);


        mPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MPDCommandHandler.previousSong();
            }
        });

        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MPDCommandHandler.nextSong();
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MPDCommandHandler.stop();
            }
        });

        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null != mLastStatus) {
                    if (mLastStatus.getPlaybackState() == MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PLAYING) {
                        MPDCommandHandler.pause();
                    } else if (mLastStatus.getPlaybackState() == MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_PAUSING) {
                        MPDCommandHandler.play();
                    } else if (mLastStatus.getPlaybackState() == MPDCurrentStatus.MPD_PLAYBACK_STATE.MPD_STOPPED) {
                        int lastIndex = mLastStatus.getCurrentSongIndex();
                        if (lastIndex >= 0) {
                            MPDCommandHandler.playSongIndex(mLastStatus.getCurrentSongIndex());
                        } else {
                            MPDCommandHandler.playSongIndex(0);
                        }
                    }
                }
            }
        });


        if (null == mStateListener) {
            mStateListener = new ServerStatusListener();
        }

        mInfoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        mSwitcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateFanartViews();
            }
        });

        // seekbar (position)
        mPositionSeekbar = (SeekBar) findViewById(R.id.now_playing_seekBar);
        mPositionSeekbar.setOnSeekBarChangeListener(new PositionSeekbarListener());

        mVolumeSeekbar = (SeekBar) findViewById(R.id.volume_seekbar);
        mVolumeIcon = (ImageView)findViewById(R.id.volume_icon);
        mVolumeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MPDCommandHandler.setVolume(0);
            }
        });
        mVolumeSeekbar.setMax(100);
        mVolumeSeekbar.setOnSeekBarChangeListener(new VolumeSeekBarListener());

        mFanartCache = new FanartCacheManager(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        ConnectionManager.reconnectLastServer(getApplicationContext());

        MPDStateMonitoringHandler.registerStatusListener(mStateListener);
        cancelSwitching();
        mSwitchTimer = new Timer();
        mSwitchTimer.schedule(new ViewSwitchTask(), 5000, 5000);

        hideSystemUI();
    }

    @Override
    protected void onPause() {
        super.onPause();

        MPDStateMonitoringHandler.unregisterStatusListener(mStateListener);
        cancelSwitching();
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

        if ( volume >= 70 ) {
            mVolumeIcon.setImageResource(R.drawable.ic_volume_high_black_48dp);
        } else if ( volume >= 30 && volume < 70) {
            mVolumeIcon.setImageResource(R.drawable.ic_volume_medium_black_48dp);
        } else if ( volume > 0 && volume < 30 ) {
            mVolumeIcon.setImageResource(R.drawable.ic_volume_low_black_48dp);
        } else {
            mVolumeIcon.setImageResource(R.drawable.ic_volume_mute_black_48dp);
        }

        // Update position seekbar & textviews
        mPositionSeekbar.setMax(status.getTrackLength());
        mPositionSeekbar.setProgress(status.getElapsedTime());

        mLastStatus = status;
    }

    private void updateMPDCurrentTrack(final MPDFile track) {
        Log.v(TAG, "New track ready, updating: " + track.getTrackTitle() + " - " + track.getTrackArtist());
        mTrackTitle.setText(track.getTrackTitle());
        mTrackAlbum.setText(track.getTrackAlbum());
        mTrackArtist.setText(track.getTrackArtist());
        if (null == mLastTrack || !track.getTrackArtist().equals(mLastTrack.getTrackArtist())) {
            // FIXME only cancel fanart requests
            MALPRequestQueue.getInstance(getApplicationContext()).cancelAll(new RequestQueue.RequestFilter() {
                @Override
                public boolean apply(Request<?> request) {
                    return true;
                }
            });

            cancelSwitching();
            mFanartView0.setImageBitmap(null);
            mFanartView1.setImageBitmap(null);
            mNextFanart = 0;

            // FIXME refresh artwork shown
            mCurrentFanart = -1;
            mLastTrack = track;
            if (track.getTrackArtistMBID() == null || track.getTrackArtistMBID().isEmpty()) {
                Log.v(TAG, "Track is without MBID, manually resolving");
                FanartTVManager.getInstance(getApplicationContext()).getTrackArtistMBID(track, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        mLastTrack.setTrackArtistMBID(response);
                        Log.v(TAG, "Track: " + track + " mLasttrack: " + mLastTrack);
                        if (track == mLastTrack) {
                            Log.v(TAG, "Got MBID for track");
                            checkFanartAvailable();
                        }
                    }
                }, new FanartFetchError() {
                    @Override
                    public void imageListFetchError() {

                    }

                    @Override
                    public void fanartFetchError(MPDFile track) {

                    }
                });
            } else {
                checkFanartAvailable();
            }
        }


    }

    private void checkFanartAvailable() {
        Log.v(TAG, "Check fanart for: " + mLastTrack.getTrackArtistMBID() + " - " + mLastTrack.getTrackArtist());
        if (mFanartCache.getFanartCount(mLastTrack.getTrackArtistMBID()) == 0) {
//            FanartTVManager.getInstance(getApplicationContext()).fetchArtistFanarts(mLastTrack, new Response.Listener<Pair<byte[], MPDArtist>>() {
//                @Override
//                public void onResponse(Pair<byte[], MPDArtist> response) {
//                    Log.v(TAG, "Received fanart");
//                    mFanartCache.addFanart(response.second.getMBID(0), String.valueOf(response.first.hashCode()),response.first);
//                    int fanartCount = mFanartCache.getFanartCount(mLastTrack.getTrackArtistMBID());
//                    if (fanartCount == 1) {
//                        updateFanartViews();
//                    }
//                    if (mCurrentFanart == (fanartCount - 2)) {
//                        mNextFanart = (mCurrentFanart + 1) % fanartCount;
//                    }
//                    Log.v(TAG, "Fanarts available: " + fanartCount + "pointer at: " + mNextFanart);
//                }
//            }, new FanartFetchError() {
//                @Override
//                public void fanartFetchError(MPDFile track) {
//                    Log.v(TAG, "Fanart fetch error");
//                }
//            });
            syncFanart(mLastTrack);
        } else {
            Log.v(TAG, "Fanart available, use cached without fetching");
            mNextFanart = 0;
            updateFanartViews();
            syncFanart(mLastTrack);
        }
    }

    private void syncFanart(final MPDFile track) {
        // Get a list of fanart urls for the current artist
        FanartTVManager.getInstance(getApplicationContext()).getArtistFanartURLs(track.getTrackArtistMBID(), new Response.Listener<List<String>>() {
            @Override
            public void onResponse(List<String> response) {
                // FIXME if already in cache
                for (final String url : response) {
                    if ( mFanartCache.inCache(track.getTrackArtistMBID(),String.valueOf(url.hashCode())) ) {
                        continue;
                    }
                    FanartTVManager.getInstance(getApplicationContext()).getFanartImage(track, url, new Response.Listener<FanartResponse>() {
                        @Override
                        public void onResponse(FanartResponse response) {
                            Log.v(TAG, "Received fanart with size: " + response.image.length);
                            mFanartCache.addFanart(track.getTrackArtistMBID(), String.valueOf(response.url.hashCode()), response.image);

                            int fanartCount = mFanartCache.getFanartCount(response.track.getTrackArtistMBID());
                            if (fanartCount == 1) {
                                updateFanartViews();
                            }
                            if (mCurrentFanart == (fanartCount - 2)) {
                                mNextFanart = (mCurrentFanart + 1) % fanartCount;
                            }
                            Log.v(TAG, "Fanarts available: " + fanartCount + "pointer at: " + mNextFanart);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                        }
                    });
                }
            }
        }, new FanartFetchError() {
            @Override
            public void imageListFetchError() {

            }

            @Override
            public void fanartFetchError(MPDFile track) {

            }
        });
    }

    private class ServerStatusListener extends MPDStatusChangeHandler {

        @Override
        protected void onNewStatusReady(MPDCurrentStatus status) {
            updateMPDStatus(status);
        }

        @Override
        protected void onNewTrackReady(MPDFile track) {
            updateMPDCurrentTrack(track);
        }
    }

    private class ViewSwitchTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateFanartViews();
                }
            });
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


    private void updateFanartViews() {
        Log.v(TAG, "View update requested");
        int fanartCount = mFanartCache.getFanartCount(mLastTrack.getTrackAlbumArtistMBID());

        if (mLastTrack == null || mLastTrack.getTrackArtistMBID() == null || mLastTrack.getTrackArtistMBID().isEmpty()) {
            Log.v(TAG, "No mbid in file");
            return;
        }

        String mbid = mLastTrack.getTrackArtistMBID();

        if (mSwitcher.getDisplayedChild() == 0) {
            Log.v(TAG, "Switching to view 1");
            if (mNextFanart < fanartCount) {
                mCurrentFanart = mNextFanart;
                Bitmap image = BitmapFactory.decodeFile(mFanartCache.getFanart(mbid, mNextFanart).getPath());
                if (image != null) {
                    mFanartView1.setImageBitmap(image);

                    // Move pointer with wraparound
                    mNextFanart = (mNextFanart + 1) % fanartCount;
                }
            }
            mSwitcher.setDisplayedChild(1);
        } else {
            Log.v(TAG, "Switching to view 0");
            if (mNextFanart < fanartCount) {
                mCurrentFanart = mNextFanart;
                Bitmap image = BitmapFactory.decodeFile(mFanartCache.getFanart(mbid, mNextFanart).getPath());
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
            mSwitchTimer.schedule(new ViewSwitchTask(), 5000, 5000);
        }
    }

    private void cancelSwitching() {
        if (null != mSwitchTimer) {
            mSwitchTimer.cancel();
            mSwitchTimer.purge();
            mSwitchTimer = null;
        }
    }

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

                if ( progress >= 70 ) {
                    mVolumeIcon.setImageResource(R.drawable.ic_volume_high_black_48dp);
                } else if ( progress >= 30 && progress < 70) {
                    mVolumeIcon.setImageResource(R.drawable.ic_volume_medium_black_48dp);
                } else if ( progress > 0 && progress < 30 ) {
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
}
