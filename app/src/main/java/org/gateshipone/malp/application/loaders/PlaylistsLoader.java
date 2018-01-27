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

package org.gateshipone.malp.application.loaders;


import android.content.Context;
import android.support.v4.content.Loader;

import java.lang.ref.WeakReference;
import java.util.List;

import org.gateshipone.malp.R;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseFileList;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFileEntry;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDPlaylist;


public class PlaylistsLoader extends Loader<List<MPDFileEntry>> {

    private PlaylistResponseHandler mPlaylistResponseHandler;

    boolean mAddHeader;

    public PlaylistsLoader(Context context, boolean addHeader) {
        super(context);
        mAddHeader = addHeader;
        mPlaylistResponseHandler = new PlaylistResponseHandler(this, context);
    }


    private static class PlaylistResponseHandler extends MPDResponseFileList {
        private WeakReference<PlaylistsLoader> mPlaylistsLoader;

        private PlaylistResponseHandler(PlaylistsLoader loader, Context context) {
            mPlaylistsLoader = new WeakReference<>(loader);
        }

        @Override
        public void handleTracks(List<MPDFileEntry> fileList, int start, int end) {
            PlaylistsLoader loader = mPlaylistsLoader.get();

            if (loader != null) {
                if (loader.mAddHeader) {
                    fileList.add(0, new MPDPlaylist(mPlaylistsLoader.get().getContext().getString(R.string.create_new_playlist)));
                }
                loader.deliverResult(fileList);
            }
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
        MPDQueryHandler.getSavedPlaylists(mPlaylistResponseHandler);
    }
}
