package org.gateshipone.malp.application.utils;

/**
 * Created by hendrik on 26.11.16.
 */

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

    private final static int VOLUME_CONTROL_REPEAT_PERIOD = 175;
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