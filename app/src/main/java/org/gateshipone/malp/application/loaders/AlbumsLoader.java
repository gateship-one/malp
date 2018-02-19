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
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.Loader;

import java.lang.ref.WeakReference;
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

    private boolean mUseArtistSort;

    public AlbumsLoader(Context context, String artistName, String albumsPath) {
        super(context);

        pAlbumsResponseHandler = new AlbumResponseHandler(this);

        mArtistName = artistName;
        mAlbumsPath = albumsPath;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        mSortOrder = PreferenceHelper.getMPDAlbumSortOrder(sharedPref, context);
        mUseArtistSort =  sharedPref.getBoolean(context.getString(R.string.pref_use_artist_sort_key), context.getResources().getBoolean(R.bool.pref_use_artist_sort_default));
    }


    private static class AlbumResponseHandler extends MPDResponseAlbumList {
        private WeakReference<AlbumsLoader> mAlbumsLoader;

        private AlbumResponseHandler(AlbumsLoader loader) {
            mAlbumsLoader = new WeakReference<>(loader);
        }

        @Override
        public void handleAlbums(List<MPDAlbum> albumList) {
            AlbumsLoader loader = mAlbumsLoader.get();

            if (loader != null) {
                // If artist albums and sort by year is active, resort the list
                if (loader.mSortOrder == MPDAlbum.MPD_ALBUM_SORT_ORDER.DATE && !((null == loader.mArtistName) || loader.mArtistName.isEmpty())) {
                    Collections.sort(albumList, new MPDAlbum.MPDAlbumDateComparator());
                }
                loader.deliverResult(albumList);
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
        if ( (null == mArtistName) || mArtistName.isEmpty() ) {
            if ( null == mAlbumsPath || mAlbumsPath.isEmpty()) {
                MPDQueryHandler.getAlbums(pAlbumsResponseHandler);
            } else {
                MPDQueryHandler.getAlbumsInPath(mAlbumsPath, pAlbumsResponseHandler);
            }
        } else {
            if (!mUseArtistSort) {
                MPDQueryHandler.getArtistAlbums(pAlbumsResponseHandler, mArtistName);
            } else {
                MPDQueryHandler.getArtistSortAlbums(pAlbumsResponseHandler, mArtistName);
            }
        }
    }
}
