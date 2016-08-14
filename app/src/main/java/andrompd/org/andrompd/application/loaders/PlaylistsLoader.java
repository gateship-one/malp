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

package andrompd.org.andrompd.application.loaders;


import android.content.Context;
import android.support.v4.content.Loader;

import java.util.List;

import andrompd.org.andrompd.R;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseFileList;
import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDQueryHandler;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpdobjects.MPDPlaylist;


public class PlaylistsLoader extends Loader<List<MPDFileEntry>> {

    private PlaylistResponseHandler mPlaylistResponseHandler;

    Context mContext;
    boolean mAddHeader;

    public PlaylistsLoader(Context context, boolean addHeader) {
        super(context);
        mContext = context;
        mAddHeader = addHeader;
        mPlaylistResponseHandler = new PlaylistResponseHandler();
    }


    private class PlaylistResponseHandler extends MPDResponseFileList {
        @Override
        public void handleTracks(List<MPDFileEntry> fileList, int start, int end) {
            if ( mAddHeader ) {
                fileList.add(0, new MPDPlaylist(mContext.getString(R.string.create_new_playlist)));
            }
            deliverResult(fileList);
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
    public void onReset() {
        deliverResult(null);
    }

    @Override
    public void onForceLoad() {
        MPDQueryHandler.getSavedPlaylists(mPlaylistResponseHandler);
    }
}
