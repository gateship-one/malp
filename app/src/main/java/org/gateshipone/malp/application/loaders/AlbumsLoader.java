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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.Loader;

import java.util.Collections;
import java.util.List;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.utils.PreferenceHelper;
import org.gateshipone.malp.mpdservice.handlers.serverhandler.MPDQueryHandler;
import org.gateshipone.malp.mpdservice.handlers.responsehandler.MPDResponseAlbumList;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;


public class AlbumsLoader extends Loader<List<MPDAlbum>> {

    private MPDResponseAlbumList pAlbumsResponseHandler;

    private String mArtistName;

    private String mAlbumsPath;

    private MPDAlbum.MPD_ALBUM_SORT_ORDER mSortOrder;

    public AlbumsLoader(Context context, String artistName, String albumsPath) {
        super(context);

        pAlbumsResponseHandler = new AlbumResponseHandler();

        mArtistName = artistName;
        mAlbumsPath = albumsPath;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        mSortOrder = PreferenceHelper.getMPDAlbumSortOrder(sharedPref, context);
    }


    private class AlbumResponseHandler extends MPDResponseAlbumList {


        @Override
        public void handleAlbums(List<MPDAlbum> albumList) {
            // If artist albums and sort by year is active, resort the list
            if ( mSortOrder == MPDAlbum.MPD_ALBUM_SORT_ORDER.DATE && !((null == mArtistName) || mArtistName.isEmpty() ) ) {
                Collections.sort(albumList, new MPDAlbum.MPDAlbumDateComparator());
            }
            deliverResult(albumList);
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
        if ( (null == mArtistName) || mArtistName.isEmpty() ) {
            if ( null == mAlbumsPath || mAlbumsPath.isEmpty()) {
                MPDQueryHandler.getAlbums(pAlbumsResponseHandler);
            } else {
                MPDQueryHandler.getAlbumsInPath(mAlbumsPath, pAlbumsResponseHandler);
            }
        } else {
            MPDQueryHandler.getArtistAlbums(pAlbumsResponseHandler,mArtistName);
        }
    }
}
