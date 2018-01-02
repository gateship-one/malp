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

package org.gateshipone.malp.mpdservice.handlers;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public abstract class MPDIdleChangeHandler extends Handler {
    public MPDIdleChangeHandler(Looper looper) {
        super(looper);
    }

    private enum IDLE_STATE {
        IDLE,
        NOIDLE,
    }
    /**
     * Handles the change of the connection of the MPDConnection. Can be used
     * to get notified on connect & disconnect.
     * @param msg Message object
     */
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        IDLE_STATE state = (IDLE_STATE) msg.obj;
        switch (state) {
            case IDLE: {
                onIdle();
                break;
            }
            case NOIDLE: {
                onNoIdle();
                break;
            }
        }
    }

    public void idle() {
        Message msg = obtainMessage();
        msg.obj = IDLE_STATE.IDLE;
        sendMessage(msg);
    }

    public void noIdle() {
        Message msg = obtainMessage();
        msg.obj = IDLE_STATE.NOIDLE;
        sendMessage(msg);
    }

    protected abstract void onIdle();
    protected abstract void onNoIdle();
}
