package org.gateshipone.malp.application.artworkdatabase;

import android.util.Pair;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;

import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDArtist;
import org.gateshipone.malp.mpdservice.mpdprotocol.mpdobjects.MPDFile;

public class FanartImageRequest extends Request<FanartResponse> {

    private final Response.Listener<FanartResponse> mListener;

    private String mURL;
    private MPDFile mTrack;

    public FanartImageRequest(String url, MPDFile track, Response.Listener<FanartResponse> listener, Response.ErrorListener errorListener) {
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
