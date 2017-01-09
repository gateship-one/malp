/*
 * Copyright (C) 2016 Team Gateship-One
 * (Hendrik Borghorst & Frederik Luetkes)
 *
 * The AUTHORS.md file contains a detailed contributors list:
 * <https://github.com/gateship-one/malp/blob/master/AUTHORS.md>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gateshipone.malp.application.artworkdatabase.network.requests;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;

import org.gateshipone.malp.application.artworkdatabase.network.responses.FanartResponse;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDTrack;

public class FanartImageRequest extends MALPRequest<FanartResponse> {

    private final Response.Listener<FanartResponse> mListener;

    private String mURL;
    private MPDTrack mTrack;

    public FanartImageRequest(String url, MPDTrack track, Response.Listener<FanartResponse> listener, Response.ErrorListener errorListener) {
        super(Request.Method.GET, url, errorListener);

        mListener= listener;
        mURL = url;
        mTrack = track;
    }

    @Override
    protected Response<FanartResponse> parseNetworkResponse(NetworkResponse response) {
        FanartResponse fanart = new FanartResponse();
        fanart.url = mURL;
        fanart.image = response.data;
        fanart.track = mTrack;
        return Response.success(fanart,null);
    }

    @Override
    protected void deliverResponse(FanartResponse response) {
        mListener.onResponse(response);
    }

}
