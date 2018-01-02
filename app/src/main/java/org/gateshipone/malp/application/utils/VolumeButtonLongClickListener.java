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

package org.gateshipone.malp.application.utils;

import android.view.MotionEvent;
import android.view.View;

import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDCommandHandler;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Class to handle long button clicks on the volume buttons. Repeats the action until the
 * user removes his finger from the button again, which cancels the spawned TimerTask.
 */
public class VolumeButtonLongClickListener implements View.OnLongClickListener, View.OnTouchListener {
    public enum LISTENER_ACTION {
        VOLUME_UP,
        VOLUME_DOWN
    }

    private final static int VOLUME_CONTROL_REPEAT_PERIOD = 100;
    private LISTENER_ACTION mAction;

    private Timer mRepeater = null;

    public VolumeButtonLongClickListener(LISTENER_ACTION action) {
        mAction = action;
    }

    @Override
    public boolean onLongClick(View v) {
        if ( mAction == LISTENER_ACTION.VOLUME_UP) {
            mRepeater = new Timer();
            mRepeater.scheduleAtFixedRate(new IncreaseVolumeTask(),0 , VOLUME_CONTROL_REPEAT_PERIOD );
            return true;
        } else if (mAction == LISTENER_ACTION.VOLUME_DOWN) {
            mRepeater = new Timer();
            mRepeater.scheduleAtFixedRate(new DecreaseVolumeTask(),0 , VOLUME_CONTROL_REPEAT_PERIOD );
            return true;
        }
        return false;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction()== MotionEvent.ACTION_UP) {
            if ( null != mRepeater ) {
                mRepeater.cancel();
                mRepeater.purge();
                mRepeater = null;
            }
        }
        return false;
    }

    private class IncreaseVolumeTask extends TimerTask {

        @Override
        public void run() {
            MPDCommandHandler.increaseVolume();
        }
    }

    private class DecreaseVolumeTask extends TimerTask {

        @Override
        public void run() {
            MPDCommandHandler.decreaseVolume();
        }
    }
}