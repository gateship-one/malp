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

package org.gateshipone.malp.application.artworkdatabase.network.requests;

import android.util.Pair;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;

import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;


public class AlbumImageByteRequest extends Request<Pair<byte[], MPDAlbum>> {

    private final Response.Listener<Pair<byte[], MPDAlbum>> mListener;

    private MPDAlbum mAlbum;


    public AlbumImageByteRequest(String url, MPDAlbum album, Response.Listener<Pair<byte[], MPDAlbum>> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);

        mListener = listener;
        mAlbum = album;
    }

    @Override
    protected Response<Pair<byte[], MPDAlbum>> parseNetworkResponse(NetworkResponse response) {
        return Response.success(new Pair<byte[], MPDAlbum>(response.data, mAlbum), null);
    }

    @Override
    protected void deliverResponse(Pair<byte[], MPDAlbum> response) {
        mListener.onResponse(response);
    }

}
