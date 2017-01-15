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

package org.gateshipone.malp.application.utils;

import android.view.KeyEvent;

import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDCommandHandler;

import java.util.Timer;
import java.util.TimerTask;

public class HardwareKeyHandler {
    /**
     * Interval for repeating the volume events in ms.
     */
    private final static int VOLUME_CONTROL_REPEAT_PERIOD = 100;

    private static HardwareKeyHandler mInstance;

    private Timer mRepeatTimer;

    /**
     * Singleton pattern
     * @return The singleton instance for this handler.
     */
    public static HardwareKeyHandler getInstance() {
        if (mInstance == null) {
            // Create singleton instance
            mInstance = new HardwareKeyHandler();
        }
        return mInstance;
    }

    /**
     * Can be called from the {@link android.app.Activity}s. that catches the key event.
     * This method ensures consistent behavior for button handling in all {@link android.app.Activity}s.
     * @param event
     * @return
     */
    public boolean handleKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    // If this event is emitted the first time start an timer to repeat this action
                    if (mRepeatTimer == null) {
                        mRepeatTimer = new Timer();
                        mRepeatTimer.scheduleAtFixedRate(new IncreaseVolumeTask(), 0, VOLUME_CONTROL_REPEAT_PERIOD);
                    }
                } else {
                    // Key is released. Stop running timmer.
                    if (null != mRepeatTimer) {
                        mRepeatTimer.cancel();
                        mRepeatTimer.purge();
                        mRepeatTimer = null;
                    }
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    // If this event is emitted the first time start an timer to repeat this action
                    if (mRepeatTimer == null) {
                        mRepeatTimer = new Timer();
                        mRepeatTimer.scheduleAtFixedRate(new DecreaseVolumeTask(), 0, VOLUME_CONTROL_REPEAT_PERIOD);
                    }
                } else {
                    // Key is released. Stop running timmer.
                    if (null != mRepeatTimer) {
                        mRepeatTimer.cancel();
                        mRepeatTimer.purge();
                        mRepeatTimer = null;
                    }
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY: {
                if (action == KeyEvent.ACTION_UP) {
                    MPDCommandHandler.play();
                }
                return true;
            }
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: {
                if (action == KeyEvent.ACTION_UP) {
                    MPDCommandHandler.togglePause();
                }
                return true;
            }
            case KeyEvent.KEYCODE_MEDIA_PAUSE: {
                if (action == KeyEvent.ACTION_UP) {
                    MPDCommandHandler.pause();
                }
                return true;
            }
            case KeyEvent.KEYCODE_MEDIA_STOP: {
                if (action == KeyEvent.ACTION_UP) {
                    MPDCommandHandler.stop();
                }
                return true;
            }
            case KeyEvent.KEYCODE_MEDIA_NEXT: {
                if (action == KeyEvent.ACTION_UP) {
                    MPDCommandHandler.nextSong();
                }
                return true;
            }
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS: {
                if (action == KeyEvent.ACTION_UP) {
                    MPDCommandHandler.previousSong();
                }
                return true;
            }
            default:
                return false;
        }
    }

    /**
     * Simple class to repeatably increase the volume.
     */
    private class IncreaseVolumeTask extends TimerTask {

        @Override
        public void run() {
            MPDCommandHandler.increaseVolume();
        }
    }


    /**
     * Simple class to repeatably decrease the volume.
     */
    private class DecreaseVolumeTask extends TimerTask {

        @Override
        public void run() {
            MPDCommandHandler.decreaseVolume();
        }
    }
}
