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

package org.gateshipone.malp.application.loaders;


import android.content.Context;
import android.support.v4.content.Loader;

import java.util.List;

import org.gateshipone.malp.R;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseFileList;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseOutputList;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDOutput;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDPlaylist;


public class OutputsLoader extends Loader<List<MPDOutput>> {

    private OutputResponseHandler mOutputResponseHandler;

    Context mContext;

    public OutputsLoader(Context context) {
        super(context);
        mContext = context;
        mOutputResponseHandler = new OutputResponseHandler();
    }


    private class OutputResponseHandler extends MPDResponseOutputList {
        @Override
        public void handleOutputs(List<MPDOutput> outputList) {
            deliverResult(outputList);
        }
    }


    @Override
    public void onStartLoading() {
        forceLoad();
    }

    @Override
    public void onStopLoading() {

    }

    @Override
    public void onForceLoad() {
        MPDQueryHandler.getOutputs(mOutputResponseHandler);
    }
}
