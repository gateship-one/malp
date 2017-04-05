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


import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BackgroundServiceHandler extends Handler {
    private static final String TAG = BackgroundServiceHandler.class.getSimpleName();

    public enum HANDLER_ACTION_TYPE {
        ACTION_START_STREAMING,
        ACTION_STOP_STREAMING
    }

    BackgroundService mService;

    public BackgroundServiceHandler(BackgroundService service) {
        mService = service;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        Log.v(TAG,"Handle message: " + msg);

        HANDLER_ACTION_TYPE action = ((HandlerAction)msg.obj).getType();
        switch (action) {
            case ACTION_START_STREAMING:
                mService.startStreamingPlayback();
                break;
            case ACTION_STOP_STREAMING:
                mService.stopStreamingPlayback();
                break;
            default:
                return;
        }
    }

    public static class HandlerAction {
        private HANDLER_ACTION_TYPE mType;
        public HandlerAction(HANDLER_ACTION_TYPE type) {
            mType = type;
        }

        public HANDLER_ACTION_TYPE getType() {
            return mType;
        }
    }
}
