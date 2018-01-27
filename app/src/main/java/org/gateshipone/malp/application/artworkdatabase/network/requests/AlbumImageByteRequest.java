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

package org.gateshipone.malp.application.artworkdatabase.network.requests;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;

import org.gateshipone.malp.application.artworkdatabase.network.responses.AlbumImageResponse;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDAlbum;


public class AlbumImageByteRequest extends MALPRequest<AlbumImageResponse> {

    private final Response.Listener<AlbumImageResponse> mListener;

    private MPDAlbum mAlbum;
    private String mUrl;


    public AlbumImageByteRequest(String url, MPDAlbum album, Response.Listener<AlbumImageResponse> listener, Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);

        mListener = listener;
        mAlbum = album;
        mUrl = url;
    }

    @Override
    protected Response<AlbumImageResponse> parseNetworkResponse(NetworkResponse response) {
        AlbumImageResponse imageResponse = new AlbumImageResponse();
        imageResponse.album = mAlbum;
        imageResponse.image = response.data;
        imageResponse.url = mUrl;
        return Response.success(imageResponse, null);
    }

    @Override
    protected void deliverResponse(AlbumImageResponse response) {
        mListener.onResponse(response);
    }

}
