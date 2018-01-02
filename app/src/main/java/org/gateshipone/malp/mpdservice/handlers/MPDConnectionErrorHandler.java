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

import org.gateshipone.malp.mpdservice.mpdprotocol.MPDException;

public abstract class MPDConnectionErrorHandler extends Handler {
    public enum MPD_CONNECTION_ERROR_TYPE {
        MPD_SERVER_ERROR,
        MPD_CONNECTION_ERROR,
    }

    /**
     * Handles incoming MPD connection errors
     * @param msg Message object
     */
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        if ( msg.obj instanceof MPDException.MPDConnectionException) {
            onMPDConnectionError((MPDException.MPDConnectionException) msg.obj);
        } else if ( msg.obj instanceof MPDException.MPDServerException) {
            onMPDError((MPDException.MPDServerException) msg.obj);
        }
    }

    public void newMPDError(MPDException e) {
        Message msg = this.obtainMessage();
        msg.obj = e;

        this.sendMessage(msg);
    }

    abstract protected void onMPDError(MPDException.MPDServerException e);
    abstract protected void onMPDConnectionError(MPDException.MPDConnectionException e);
}
