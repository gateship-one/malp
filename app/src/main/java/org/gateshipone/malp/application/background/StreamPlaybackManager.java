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

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;


public class StreamPlaybackManager {
    private static final String TAG = StreamPlaybackManager.class.getSimpleName();
    private MediaPlayer mPlayer;

    private BackgroundService mService;

    private PreparedListener mPreparedListener;
    private CompletionListener mCompletionListener;

    private String mSource;

    public StreamPlaybackManager(BackgroundService service) {
        mService = service;

        mPlayer = new MediaPlayer();
        mPreparedListener = new PreparedListener();
        mCompletionListener = new CompletionListener();

        mPlayer.setOnPreparedListener(mPreparedListener);
        mPlayer.setOnCompletionListener(mCompletionListener);
    }

    public void playURL(String url) {
        mSource = url;

        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        startPlayback();
    }

    private void startPlayback() {
        try {
            mPlayer.setDataSource(mSource);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mPlayer.prepareAsync();
    }

    private void pause() {
        mPlayer.stop();
    }

    private class PreparedListener implements MediaPlayer.OnPreparedListener {

        @Override
        public void onPrepared(MediaPlayer mp) {
            Log.v(TAG,"Prepared, start playback");
            mp.setWakeMode(mService.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mp.start();
        }
    }

    private class CompletionListener implements MediaPlayer.OnCompletionListener {

        @Override
        public void onCompletion(MediaPlayer mp) {

        }
    }
}
