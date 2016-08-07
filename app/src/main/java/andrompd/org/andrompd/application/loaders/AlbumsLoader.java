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

import andrompd.org.andrompd.mpdservice.handlers.serverhandler.MPDQueryHandler;
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseAlbumList;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDAlbum;


public class AlbumsLoader extends Loader<List<MPDAlbum>> {

    private MPDResponseAlbumList pAlbumsResponseHandler;

    private String mArtistName;

    public AlbumsLoader(Context context, String artistName) {
        super(context);

        pAlbumsResponseHandler = new AlbumResponseHandler();

        mArtistName = artistName;
    }


    private class AlbumResponseHandler extends MPDResponseAlbumList {


        @Override
        public void handleAlbums(List<MPDAlbum> albumList) {
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
    public void onReset() {
        forceLoad();
    }

    @Override
    public void onForceLoad() {
        if ( (null == mArtistName) || mArtistName.equals("") ) {
            MPDQueryHandler.getAlbums(pAlbumsResponseHandler);
        } else {
            MPDQueryHandler.getArtistAlbums(pAlbumsResponseHandler,mArtistName);
        }
    }
}
