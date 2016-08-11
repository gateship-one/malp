/*
 * Copyright (C) 2016  Hendrik Borghorst
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package andrompd.org.andrompd.mpdservice.handlers.responsehandler;


import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.util.List;
import java.util.Set;

import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDFile;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDFileEntry;

public abstract class MPDResponseFileList extends MPDResponseHandler {
    public static final String EXTRA_WINDOW_START = "windowstart";
    public static final String EXTRA_WINDOW_END = "windowend";

    public MPDResponseFileList() {

    }

    /**
     * Handle function for the track list. This only calls the abstract method
     * which needs to get implemented by the user of this class.
     * @param msg Message object containing a list of MPDFile items.
     */
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        Bundle args = msg.getData();
        int windowStart = msg.getData().getInt(EXTRA_WINDOW_START);
        int windowEnd = msg.getData().getInt(EXTRA_WINDOW_END);

        /* Call album response handler */
        List<MPDFileEntry> trackList = (List<MPDFileEntry>)msg.obj;
        handleTracks(trackList, windowStart, windowEnd);
    }

    /**
     * Abstract method to be implemented by the user of the MPD implementation.
     * This should be a callback for the UI thread and run in the UI thread.
     * This can be used for updating lists of adapters and views.
     * @param trackList List of MPDFile objects containing a list of mpds tracks response.
     */
    abstract public void handleTracks(List<MPDFileEntry> fileList, int windowstart, int windowend);
}
