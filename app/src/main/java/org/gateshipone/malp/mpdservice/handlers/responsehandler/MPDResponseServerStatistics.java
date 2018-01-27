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

package org.gateshipone.malp.mpdservice.handlers.responsehandler;


import android.os.Message;

import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDStatistics;

public abstract class MPDResponseServerStatistics extends MPDResponseHandler {

    public MPDResponseServerStatistics() {

    }

    /**
     * Handle function for the server statistics. This only calls the abstract method
     * which needs to get implemented by the user of this class.
     * @param msg Message object containing a MPDStatistics object
     */
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);


        /* Call album response handler */
        MPDStatistics stats = (MPDStatistics)msg.obj;
        handleStatistic(stats);
    }

    /**
     * Send statistics to the receiving handler
     * @param statistics Object to send
     */
    public void sendServerStatistics(MPDStatistics statistics) {
        Message responseMessage = this.obtainMessage();
        responseMessage.obj = statistics;
        sendMessage(responseMessage);
    }

    /**
     * Abstract method to be implemented by the user of the MPD implementation.
     * This should be a callback for the UI thread and run in the UI thread.
     * This can be used for updating lists of adapters and views.
     * @param statistics Current MPD statistics
     */
    abstract public void handleStatistic(MPDStatistics statistics);
}
