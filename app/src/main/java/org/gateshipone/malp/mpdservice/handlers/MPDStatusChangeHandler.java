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
import android.os.Message;

import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDCurrentStatus;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;

/**
 * Class to handle status updates handled by the MPDStateMonitoringHandler
 */
public abstract class MPDStatusChangeHandler extends Handler {
    public enum MPD_STATUS_RESPONSE_ACTION {
        MPD_STATUS_RESPONSE_ACTION_NEW_STATUS,
        MPD_STATUS_RESPONSE_ACTION_NEW_TRACK,
    }

    /**
     * Handles the change of the status and track of MPD
     * @param msg Message object
     */
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        if ( msg.obj instanceof MPDCurrentStatus ) {
            onNewStatusReady((MPDCurrentStatus)msg.obj);
        } else if ( msg.obj instanceof MPDTrack) {
            onNewTrackReady((MPDTrack) msg.obj);
        }


    }

    public void newMPDStatusReady(MPDCurrentStatus status ) {
        Message msg = this.obtainMessage();
        msg.obj = status;

        this.sendMessage(msg);
    }

    public void newMPDTrackReady(MPDTrack track) {
        Message msg = this.obtainMessage();
        msg.obj = track;

        this.sendMessage(msg);
    }

    abstract protected void onNewStatusReady(MPDCurrentStatus status);
    abstract protected void onNewTrackReady(MPDTrack track);
}
