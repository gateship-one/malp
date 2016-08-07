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
import andrompd.org.andrompd.mpdservice.handlers.responsehandler.MPDResponseArtistList;
import andrompd.org.andrompd.mpdservice.mpdprotocol.mpddatabase.MPDArtist;


public class ArtistsLoader extends Loader<List<MPDArtist>> {

    private MPDResponseArtistList pArtistResponseHandler;

    public ArtistsLoader(Context context) {
        super(context);

        pArtistResponseHandler = new ArtistResponseHandler();
    }


    private class ArtistResponseHandler extends MPDResponseArtistList {

        @Override
        public void handleArtists(List<MPDArtist> artistList) {
            deliverResult(artistList);
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
        MPDQueryHandler.getArtists(pArtistResponseHandler);
    }

    @Override
    public void onForceLoad() {
        MPDQueryHandler.getArtists(pArtistResponseHandler);
    }
}
