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

package org.gateshipone.malp.mpdservice.handlers.responsehandler;


import android.os.Bundle;
import android.os.Message;

import java.util.List;

import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDOutput;

public abstract class MPDResponseOutputList extends MPDResponseHandler {

    public MPDResponseOutputList() {

    }

    /**
     * Handle function for the track list. This only calls the abstract method
     * which needs to get implemented by the user of this class.
     * @param msg Message object containing a list of MPDTrack items.
     */
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);


        /* Call album response handler */
        List<MPDOutput> outputList = (List<MPDOutput>)msg.obj;
        handleOutputs(outputList);
    }

    /**
     * Abstract method to be implemented by the user of the MPD implementation.
     * This should be a callback for the UI thread and run in the UI thread.
     * This can be used for updating lists of adapters and views.
     * @param outputList List of MPDOutput objects containing a list of available MPD outputs
     */
    abstract public void handleOutputs(List<MPDOutput> outputList);
}
